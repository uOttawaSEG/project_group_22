package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import androidx.annotation.NonNull;

import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.TutorSummary;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreTutorRepository implements TutorRepository {

    private static final String COL_USERS      = "users";
    private static final String SUB_AVAIL      = "availabilitySlots";
    private static final String SUB_REQUESTS   = "sessionRequests";
    private static final String SUB_SESSIONS   = "sessions";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference availCol(String tutorId) {
        return db.collection(COL_USERS).document(tutorId).collection(SUB_AVAIL);
    }

    // -------------------- Helpers --------------------
    private CollectionReference slotsCol(String tutorId) {
        return db.collection("tutors").document(tutorId).collection("availabilitySlots");
    }

    private CollectionReference requestsCol(String tutorId) {
        return db.collection("tutors").document(tutorId).collection("sessionRequests");
    }

    private CollectionReference sessionsCol(String tutorId) {
        return db.collection("tutors").document(tutorId).collection("sessions");
    }

    // -------------------- Availability --------------------
    @Override
    public void createAvailabilitySlot(@NonNull String tutorId, @NonNull AvailabilitySlot slot, @NonNull SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", slot.getDate());                 // "YYYY-MM-DD"
        data.put("startMin", toMins(slot.getStartTime())); // 0..1439
        data.put("endMin", toMins(slot.getEndTime()));
        data.put("startMillis", slot.getStartMillis());
        data.put("manualApproval", slot.isRequiresApproval());
        data.put("status", slot.getStatus() == null ? "OPEN" : slot.getStatus());
        data.put("booked", slot.isBooked());
        data.put("createdAt", slot.getCreatedAt() == 0L ? System.currentTimeMillis() : slot.getCreatedAt());

        slotsCol(tutorId).add(data)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Create failed" : e.getMessage()));
    }

    @Override
    public void getAvailabilitySlots(@NonNull String tutorId, @NonNull SlotsListCallback cb) {
        slotsCol(tutorId)
                .orderBy("startMillis", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<AvailabilitySlot> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        AvailabilitySlot s = new AvailabilitySlot();
                        s.setSlotId(d.getId());
                        s.setTutorId(tutorId);
                        s.setDate(d.getString("date"));
                        Integer sm = asInt(d.getLong("startMin"));
                        Integer em = asInt(d.getLong("endMin"));
                        s.setStartTime(sm == null ? "" : toHmm(sm));
                        s.setEndTime(em == null ? "" : toHmm(em));
                        s.setRequiresApproval(Boolean.TRUE.equals(d.getBoolean("manualApproval")));
                        s.setBooked(Boolean.TRUE.equals(d.getBoolean("booked")));
                        Long stMs = d.getLong("startMillis");
                        s.setStartMillis(stMs == null ? 0L : stMs);
                        Long ca = d.getLong("createdAt");
                        s.setCreatedAt(ca == null ? 0L : ca);
                        s.setStatus(d.getString("status"));
                        out.add(s);
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    @Override
    public void getSlotById(@NonNull String tutorId, @NonNull String slotId, @NonNull SingleSlotCallback cb) {
        slotsCol(tutorId).document(slotId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError("Slot not found"); return; }
                    AvailabilitySlot s = new AvailabilitySlot();
                    s.setSlotId(d.getId());
                    s.setTutorId(tutorId);
                    s.setDate(d.getString("date"));
                    Integer sm = asInt(d.getLong("startMin"));
                    Integer em = asInt(d.getLong("endMin"));
                    s.setStartTime(sm == null ? "" : toHmm(sm));
                    s.setEndTime(em == null ? "" : toHmm(em));
                    s.setRequiresApproval(Boolean.TRUE.equals(d.getBoolean("manualApproval")));
                    s.setBooked(Boolean.TRUE.equals(d.getBoolean("booked")));
                    Long stMs = d.getLong("startMillis");
                    s.setStartMillis(stMs == null ? 0L : stMs);
                    Long ca = d.getLong("createdAt");
                    s.setCreatedAt(ca == null ? 0L : ca);
                    s.setStatus(d.getString("status"));
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    @Override
    public void deleteAvailabilitySlot(@NonNull String tutorId, @NonNull String slotId, @NonNull SimpleCallback cb) {
        slotsCol(tutorId).document(slotId).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Delete failed" : e.getMessage()));
    }

    // -------------------- Requests --------------------
    @Override
    public void getPendingRequests(@NonNull String tutorId, @NonNull RequestsListCallback cb) {
        requestsCol(tutorId)
                .whereEqualTo("status", "PENDING")
                .orderBy("requestedAtMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<SessionRequest> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        SessionRequest r = new SessionRequest();
                        r.setRequestId(d.getId());
                        r.setTutorId(d.getString("tutorId"));
                        r.setStudentId(d.getString("studentId"));
                        r.setSlotId(d.getString("slotId"));
                        r.setStatus(d.getString("status"));
                        Long t = d.getLong("requestedAtMillis");
                        r.setRequestedAtMillis(t == null ? 0L : t);
                        out.add(r);
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    @Override
    public void approveRequest(@NonNull String tutorId, @NonNull String requestId, @NonNull SimpleCallback cb) {
        DocumentReference reqDoc = requestsCol(tutorId).document(requestId);
        db.runTransaction(trx -> {
                    DocumentSnapshot r = trx.get(reqDoc);
                    if (!r.exists()) throw new IllegalStateException("Request not found");
                    String slotId = r.getString("slotId");
                    String studentId = r.getString("studentId");
                    if (slotId == null || studentId == null) throw new IllegalStateException("Missing slot/student");

                    // mark request APPROVED
                    trx.update(reqDoc, "status", "APPROVED", "decidedAt", System.currentTimeMillis());

                    // mark slot booked
                    trx.update(slotsCol(tutorId).document(slotId), "booked", true, "status", "BOOKED");

                    // create a session under tutor
                    Map<String, Object> sess = new HashMap<>();
                    sess.put("tutorId", tutorId);
                    sess.put("studentId", studentId);
                    sess.put("slotId", slotId);
                    sess.put("status", "APPROVED");
                    sess.put("createdAt", System.currentTimeMillis());
                    trx.set(sessionsCol(tutorId).document(), sess);

                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Approve failed" : e.getMessage()));
    }

    @Override
    public void rejectRequest(@NonNull String tutorId, @NonNull String requestId, @NonNull SimpleCallback cb) {
        requestsCol(tutorId).document(requestId)
                .update("status", "REJECTED", "decidedAt", System.currentTimeMillis())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Reject failed" : e.getMessage()));
    }

    // -------------------- Sessions --------------------
    @Override
    public void getTutorSessions(@NonNull String tutorId, @NonNull SessionsListCallback cb) {
        sessionsCol(tutorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Session> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Session s = new Session();
                        s.setSessionId(d.getId());
                        s.setTutorId(d.getString("tutorId"));
                        s.setStudentId(d.getString("studentId"));
                        s.setSlotId(d.getString("slotId"));
                        s.setStatus(d.getString("status"));
                        Long t = d.getLong("createdAt");
                        s.setCreatedAtMillis(t == null ? 0L : t);
                        out.add(s);
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    @Override
    public void getSessionById(@NonNull String tutorId, @NonNull String sessionId, @NonNull SingleSessionCallback cb) {
        sessionsCol(tutorId).document(sessionId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError("Session not found"); return; }
                    Session s = new Session();
                    s.setSessionId(d.getId());
                    s.setTutorId(d.getString("tutorId"));
                    s.setStudentId(d.getString("studentId"));
                    s.setSlotId(d.getString("slotId"));
                    s.setStatus(d.getString("status"));
                    Long t = d.getLong("createdAt");
                    s.setCreatedAtMillis(t == null ? 0L : t);
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    // -------------------- Users --------------------
    @Override
    public void getStudent(@NonNull String studentId, @NonNull StudentCallback cb) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError("Student not found"); return; }
                    Student s = new Student();
                    s.setUid(d.getId());
                    s.setFirstName(nz(d.getString("firstName")));
                    s.setLastName(nz(d.getString("lastName")));
                    s.setEmail(nz(d.getString("email")));
                    s.setPhone(nz(d.getString("phone")));
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Load failed" : e.getMessage()));
    }

    // -------------------- Utils --------------------
    private static Integer asInt(Long v) { return v == null ? null : v.intValue(); }

    private static String toHmm(int mins) {
        int h = mins / 60, m = mins % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    private static int toMins(String hmm) {
        if (hmm == null || hmm.length() < 4) return 0;
        try {
            String[] p = hmm.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) { return 0; }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override
    public void requestBooking(String tutorId, String slotId, String studentId, SimpleCallback cb) {
        if (isEmpty(tutorId) || isEmpty(slotId) || isEmpty(studentId)) {
            cb.onError("Missing ids"); return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference slotRef   = db.collection("users").document(tutorId)
                .collection("availabilitySlots").document(slotId);
        CollectionReference reqCol  = db.collection("users").document(tutorId)
                .collection("sessionRequests");
        CollectionReference sessCol = db.collection("users").document(tutorId)
                .collection("sessions");

        slotRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { cb.onError("Slot not found"); return; }

            // Read slot snapshot
            String status  = snap.getString("status");              // "OPEN" | "CLOSED"
            Boolean booked = snap.getBoolean("booked");
            Boolean requiresApproval = snap.getBoolean("requiresApproval");
            String date     = snap.getString("date");               // "YYYY-MM-DD"
            String start    = snap.getString("startTime");          // "HH:mm"
            String end      = snap.getString("endTime");            // "HH:mm"
            Long startMs    = snap.getLong("startMillis");

            if (!"OPEN".equals(status)) { cb.onError("Slot not open"); return; }
            if (booked != null && booked) { cb.onError("Slot already booked"); return; }

            WriteBatch batch = db.batch();

            if (Boolean.TRUE.equals(requiresApproval)) {
                // Create PENDING request
                DocumentReference reqRef = reqCol.document(); // auto id
                Map<String,Object> req = new HashMap<>();
                req.put("requestId", reqRef.getId());
                req.put("tutorId", tutorId);
                req.put("studentId", studentId);
                req.put("slotId", slotId);
                req.put("date", date);
                req.put("startTime", start);
                req.put("endTime", end);
                req.put("status", "PENDING");
                req.put("requestedAtMillis", System.currentTimeMillis());
                req.put("slotStartMillis", startMs != null ? startMs : 0L);

                batch.set(reqRef, req);

            } else {
                // Auto-approve: create Session and mark slot booked
                DocumentReference sessRef = sessCol.document(); // sessionId
                Map<String,Object> sess = new HashMap<>();
                sess.put("sessionId", sessRef.getId());
                sess.put("tutorId", tutorId);
                sess.put("studentId", studentId);
                sess.put("slotId", slotId);
                sess.put("date", date);
                sess.put("startTime", start);
                sess.put("endTime", end);
                sess.put("status", "upcoming");
                sess.put("createdAtMillis", System.currentTimeMillis());

                batch.set(sessRef, sess);
                batch.update(slotRef, "booked", true, "status", "CLOSED");
            }

            batch.commit()
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(e -> cb.onError("Booking failed: " + e.getMessage()));

        }).addOnFailureListener(e -> cb.onError("Load slot failed: " + e.getMessage()));
    }

    private static boolean isEmpty(String s){ return s==null || s.trim().isEmpty(); }

    public void getOpenSlots(@NonNull String tutorId, @NonNull SlotsListCallback cb) {
        availCol(tutorId)
                .whereEqualTo("status", "OPEN")
                .whereEqualTo("booked", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<AvailabilitySlot> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        AvailabilitySlot s = toSlot(d);
                        if (s != null) out.add(s);
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private AvailabilitySlot toSlot(DocumentSnapshot d) {
        try {
            AvailabilitySlot s = new AvailabilitySlot();
            s.setSlotId(d.getId());
            s.setTutorId(nvl(d.getString("tutorId")));
            s.setDate(nvl(d.getString("date")));
            s.setStartTime(nvl(d.getString("startTime")));
            s.setEndTime(nvl(d.getString("endTime")));
            Boolean ra = d.getBoolean("requiresApproval");
            s.setRequiresApproval(ra != null && ra);
            Boolean b = d.getBoolean("booked");
            s.setBooked(b != null && b);
            s.setStatus(nvl(d.getString("status")));
            Long created = d.getLong("createdAt");
            if (created != null) s.setCreatedAt(created);
            Long startMs = d.getLong("startMillis");
            if (startMs != null) s.setStartMillis(startMs);
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    @Override
    public void requestSession(@NonNull String tutorId,
                               @NonNull String studentId,
                               @NonNull String slotId,
                               boolean requiresApproval,
                               @NonNull SimpleCallback cb) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final DocumentReference slotRef =
                db.collection("users")
                        .document(tutorId)
                        .collection("availabilitySlots")
                        .document(slotId);

        db.runTransaction(trx -> {
                    // 1) Load slot and validate it's still open
                    DocumentSnapshot slotDoc = trx.get(slotRef);
                    if (!slotDoc.exists()) {
                        throw new RuntimeException("Slot not found.");
                    }

                    Boolean booked = slotDoc.getBoolean("booked");
                    String status  = slotDoc.getString("status");
                    if (Boolean.TRUE.equals(booked) || !"OPEN".equalsIgnoreCase(status)) {
                        throw new RuntimeException("Slot is no longer available.");
                    }

                    String date      = slotDoc.getString("date");
                    String startTime = slotDoc.getString("startTime");
                    String endTime   = slotDoc.getString("endTime");
                    long now         = System.currentTimeMillis();

                    if (requiresApproval) {
                        // 2a) Create a pending request in a top-level collection
                        DocumentReference reqRef = db.collection("sessionRequests").document();

                        Map<String, Object> req = new HashMap<>();
                        req.put("requestId", reqRef.getId());
                        req.put("tutorId", tutorId);
                        req.put("studentId", studentId);
                        req.put("slotId", slotId);
                        req.put("date", date);
                        req.put("startTime", startTime);
                        req.put("endTime", endTime);
                        req.put("status", "PENDING");
                        req.put("requestedAtMillis", now);

                        trx.set(reqRef, req);
                    } else {
                        // 2b) Auto-approve: create a confirmed session and mark slot booked
                        DocumentReference sessRef =
                                db.collection("users")
                                        .document(tutorId)
                                        .collection("sessions")
                                        .document();

                        Map<String, Object> sess = new HashMap<>();
                        sess.put("sessionId", sessRef.getId());
                        sess.put("tutorId", tutorId);
                        sess.put("studentId", studentId);
                        sess.put("slotId", slotId);
                        sess.put("date", date);
                        sess.put("startTime", startTime);
                        sess.put("endTime", endTime);
                        sess.put("status", "upcoming");
                        sess.put("createdAtMillis", now);

                        trx.set(sessRef, sess);

                        // mark slot as booked
                        trx.update(slotRef, "booked", true, "status", "BOOKED");
                    }

                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void listTutorsWithOpenSlots(long nowEpochMillis, TutorsListCallback cb) {
        // 1) Find all open future slots across all tutors
        db.collectionGroup("availabilitySlots")
                .whereEqualTo("status", "OPEN")
                .whereEqualTo("booked", false)
                .whereGreaterThan("startMillis", nowEpochMillis)
                .get()
                .addOnSuccessListener(q -> {
                    // Group by tutorId
                    java.util.HashMap<String, Integer> counts = new java.util.HashMap<>();
                    java.util.HashSet<String> tutorIds = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : q.getDocuments()) {
                        String tid = null;
                        try {
                            // Prefer field
                            tid = d.getString("tutorId");
                            if (tid == null || tid.isEmpty()) {
                                // Fallback: /tutors/{uid}/availabilitySlots/{slotId}
                                tid = d.getReference().getParent().getParent().getId();
                            }
                        } catch (Exception ignored) {}
                        if (tid == null || tid.isEmpty()) continue;
                        tutorIds.add(tid);
                        counts.put(tid, counts.getOrDefault(tid, 0) + 1);
                    }
                    if (tutorIds.isEmpty()) { cb.onSuccess(java.util.Collections.emptyList()); return; }

                    // 2) Load basic name/email for each tutor from /users/{uid}
                    java.util.ArrayList<TutorSummary> out = new java.util.ArrayList<>();
                    final int total = tutorIds.size();
                    final int[] done = {0};
                    for (String tid : tutorIds) {
                        db.collection("users").document(tid).get()
                                .addOnSuccessListener(u -> {
                                    String fn = safe(u.getString("firstName"));
                                    String ln = safe(u.getString("lastName"));
                                    String name = (fn + " " + ln).trim();
                                    if (name.isEmpty()) name = tid;
                                    String email = safe(u.getString("email"));
                                    out.add(new com.example.seg2105_project_1_tutor_registration_form.model.tutor.TutorSummary(
                                            tid, name, email, counts.getOrDefault(tid, 0)
                                    ));
                                    if (++done[0] == total) {
                                        // Sort by name then by openCount desc
                                        java.util.Collections.sort(out, (a,b) -> {
                                            int d = a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
                                            if (d != 0) return d;
                                            return Integer.compare(b.getOpenCount(), a.getOpenCount());
                                        });
                                        cb.onSuccess(out);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // still add a fallback entry so we don't drop tutors completely
                                    out.add(new com.example.seg2105_project_1_tutor_registration_form.model.tutor.TutorSummary(
                                            tid, tid, "", counts.getOrDefault(tid, 0)
                                    ));
                                    if (++done[0] == total) cb.onSuccess(out);
                                });
                    }
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private static String safe(String s){ return s==null? "": s; }
}
