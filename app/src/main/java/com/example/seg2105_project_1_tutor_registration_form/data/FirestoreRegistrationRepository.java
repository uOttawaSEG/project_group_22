package com.example.seg2105_project_1_tutor_registration_form.data;

// Import the Following:
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * FirestoreRegistrationRepository — human-readable overview
 * ─────────────────────────────────────────────────────────────────────────────
 * Responsibility
 * • Reads/writes the "registrationRequests" collection in Firestore.
 * • Exposes CRUD-style methods used by Admin tools and login “gate” logic.
 *
 * Design choices worth noting
 * • Timestamps are stored as epoch millis (Long) rather than Firestore Timestamp.
 *   This avoids client deserialization mismatches and keeps sorting predictable.
 * • Manual mapping in listByStatus() tolerates mixed types for legacy data
 *   (e.g., submittedAt may be Long OR Timestamp in old docs).
 *
 * Data model (document fields)
 * • id (string, doc id), userUid (string), userId (string legacy alias),
 *   firstName, lastName, email, phone
 * • role ("STUDENT" | "TUTOR"), status ("PENDING" | "APPROVED" | "REJECTED")
 * • submittedAt (Long millis), decidedAt (Long millis or null), decidedBy (string or null)
 * • reason (string or missing when deleted)
 *
 * Usage
 * • createPendingForUid(...)  → create/overwrite a PENDING request row.
 * • ensureRequestExists(...)  → backfill a request on login for old users.
 * • approve(...), reject(...) → update status + decision metadata.
 * • listByStatus(...)         → fetch all requests with a given status.
 */
public class FirestoreRegistrationRepository implements RegistrationRepository {

    private static final String COL = "registrationRequests";
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();

    /**
     * List requests filtered by a given status (PENDING/APPROVED/REJECTED).
     * Returns a Task that resolves to a List<RegRequest>.
     *
     * Implementation notes
     * • Firestore query is simple whereEqualTo("status", status.name()).
     * • We map each DocumentSnapshot to RegRequest manually to:
     *   - normalize role/status to UPPERCASE for the UI
     *   - tolerate Long or Timestamp for date fields via asMillis(...)
     * • On any failure or empty result, returns an empty list (not null).
     */
    @Override
    public Task<List<RegRequest>> listByStatus(RequestStatus status) {
        return fs.collection(COL)
                .whereEqualTo("status", status.name())
                .get()
                .continueWith(t -> {
                    List<RegRequest> out = new ArrayList<>();
                    if (!t.isSuccessful() || t.getResult() == null) return out;
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        // Manual mapping so we can tolerate Long OR Timestamp in Firestore
                        RegRequest r = new RegRequest();
                        r.setId(d.getId());
                        r.setUserUid(asString(d.get("userUid")));
                        r.setFirstName(asString(d.get("firstName")));
                        r.setLastName(asString(d.get("lastName")));
                        r.setEmail(asString(d.get("email")));
                        r.setPhone(asString(d.get("phone")));
                        r.setRole(asStringUpper(d.get("role")));      // store as upper for UI
                        r.setStatus(asStringUpper(d.get("status")));
                        r.setCoursesWantedSummary(asString(d.get("coursesWantedSummary")));
                        r.setCoursesOfferedSummary(asString(d.get("coursesOfferedSummary")));
                        r.setDecidedBy(asString(d.get("decidedBy")));
                        r.setDecidedAt(asMillis(d.get("decidedAt")));
                        r.setSubmittedAt(asMillis(d.get("submittedAt")));
                        r.setReason(asString(d.get("reason")));
                        out.add(r);
                    }
                    return out;
                });
    }

    /**
     * Create (or merge/overwrite) a PENDING request document for a given uid.
     *
     * Parameters
     * • uid/email are required. Names/phone are nullable and default to "" for consistency.
     * • role is normalized to uppercase; defaults to "STUDENT" if null.
     *
     * Behavior
     * • Writes a deterministic doc id (= uid) so lookups are O(1) by uid.
     * • Stores submittedAt as System.currentTimeMillis() (Long) to avoid Timestamp issues.
     * • decidedBy/decidedAt are set to null (pending). reason is initialized as empty string.
     *
     * Merge semantics
     * • Uses SetOptions.merge() so future writes can add fields without removing unchanged ones.
     */
    // Create/overwrite a PENDING request for a uid
    public Task<Void> createPendingForUid(@NonNull String uid,
                                          @NonNull String email,
                                          @Nullable String firstName,
                                          @Nullable String lastName,
                                          @Nullable String phone,
                                          @Nullable String role /* "STUDENT" | "TUTOR" */) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", uid);
        data.put("userUid", uid);
        data.put("userId", uid); // legacy alias used by some older queries
        data.put("email", email);
        data.put("firstName", firstName == null ? "" : firstName);
        data.put("lastName",  lastName  == null ? "" : lastName);
        data.put("phone",     phone     == null ? "" : phone);
        data.put("role", role == null ? "STUDENT" : role.toUpperCase());
        data.put("status", "PENDING");
        // ✅ Always store millis (Long) to avoid Timestamp crashes
        data.put("submittedAt", System.currentTimeMillis());
        data.put("decidedBy", null);
        data.put("decidedAt", null);
        data.put("reason", "");

        return fs.collection(COL)
                .document(uid)
                .set(data, SetOptions.merge());
    }

    /**
     * Ensure a request document exists for a user.
     * Use this during login to backfill older accounts that never created a request row.
     *
     * Flow
     * • Read /registrationRequests/{uid}.
     * • If it exists → no-op.
     * • If not → create a PENDING request with minimal fields via createPendingForUid(...).
     */
    // Ensure there is a request doc on login (for old users)
    public Task<Void> ensureRequestExists(@NonNull String uid, @NonNull String email) {
        DocumentReference doc = fs.collection(COL).document(uid);
        return doc.get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                return Tasks.forResult(null);
            }
            return createPendingForUid(uid, email, "", "", "", null);
        });
    }

    /**
     * Approve a request.
     * • Sets status = APPROVED, decidedAt = now (millis), decidedBy = adminUid.
     * • Removes "reason" field entirely (not just empty string) since it no longer applies.
     */
    @Override
    public Task<Void> approve(String requestId, String adminUid) {
        // ✅ Write millis (Long) instead of server Timestamp
        return fs.collection(COL).document(requestId)
                .update(
                        "status", RequestStatus.APPROVED.name(),
                        "decidedAt", System.currentTimeMillis(),
                        "decidedBy", adminUid,
                        "reason", FieldValue.delete()
                );
    }

    /**
     * Reject a request.
     * • Sets status = REJECTED, decidedAt = now (millis), decidedBy = adminUid.
     * • "reason" is stored only if a non-blank value is provided; otherwise delete field.
     *   This keeps the document clean and conditional logic simple on the client.
     */
    @Override
    public Task<Void> reject(String requestId, String adminUid, @Nullable String reasonNullable) {
        Object reasonField = (reasonNullable == null || reasonNullable.isBlank())
                ? FieldValue.delete()
                : reasonNullable;

        return fs.collection(COL).document(requestId)
                .update(
                        "status", RequestStatus.REJECTED.name(),
                        "decidedAt", System.currentTimeMillis(),   // ✅ millis
                        "decidedBy", adminUid,
                        "reason", reasonField
                );
    }

    // -------- helpers --------

    /** Safely convert any object to String; returns null for null input. */
    private static String asString(Object o) { return o == null ? null : String.valueOf(o); }

    /** Uppercase string helper with null-safety and trimming. */
    private static String asStringUpper(Object o) {
        String s = asString(o);
        return s == null ? null : s.trim().toUpperCase();
    }

    /**
     * Convert a Firestore value to epoch millis (Long).
     * Accepts:
     * • Number (Long/Integer/Double) → longValue()
     * • Timestamp → toDate().getTime()
     * Any other type (or null) returns null to avoid ClassCastExceptions.
     */
    private static Long asMillis(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof Timestamp) return ((Timestamp) v).toDate().getTime();
        return null;
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of FirestoreRegistrationRepository — TL;DR
 * ─────────────────────────────────────────────────────────────────────────────
 * • Uses deterministic doc id = uid; stores times as epoch millis (Long).
 * • listByStatus(...) is robust to legacy Timestamp fields.
 * • approve/reject stamp decision metadata; reject optionally keeps a reason.
 * • ensureRequestExists(...) backfills missing rows during user login.
 */
