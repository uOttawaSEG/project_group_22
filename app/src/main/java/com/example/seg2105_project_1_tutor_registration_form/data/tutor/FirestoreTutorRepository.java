package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreTutorRepository implements TutorRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /* ---------- refs ---------- */
    private DocumentReference slotRef(String tutorId, String slotId) {
        return db.collection("users").document(tutorId)
                .collection("availabilitySlots").document(slotId);
    }

    private CollectionReference requestsCol(String tutorId) {
        return db.collection("users").document(tutorId)
                .collection("sessionRequests");
    }

    private DocumentReference requestRef(String tutorId, String requestId) {
        return requestsCol(tutorId).document(requestId);
    }

    private CollectionReference sessionsCol(String tutorId) {
        return db.collection("users").document(tutorId)
                .collection("sessions");
    }

    /* ----------------------------------------------------------
       STUDENT — submit request (NEVER book if approval required)
       ---------------------------------------------------------- */
    @Override
    public void submitSessionRequest(String tutorId, String studentId, String slotId,
                                     RequestCreateCallback cb) {

        DocumentReference sRef = slotRef(tutorId, slotId);
        DocumentReference rRef = requestsCol(tutorId).document(); // new request doc

        db.runTransaction(trx -> {
                    DocumentSnapshot slot = trx.get(sRef);
                    if (!slot.exists()) throw new IllegalStateException("Slot missing");

                    boolean booked = Boolean.TRUE.equals(slot.getBoolean("booked"));
                    if (booked) throw new IllegalStateException("Slot already booked");

                    boolean requiresApproval = Boolean.TRUE.equals(slot.getBoolean("requiresApproval"));

                    Map<String, Object> req = new HashMap<>();
                    req.put("id", rRef.getId());                 // your model uses 'id'
                    req.put("tutorId", tutorId);
                    req.put("studentId", studentId);
                    req.put("slotId", slotId);
                    req.put("requestedAtMillis", Timestamp.now()); // Timestamp

                    if (requiresApproval) {
                        // PENDING ONLY — do NOT book the slot here
                        req.put("status", "pending");
                        trx.set(rRef, req);
                    } else {
                        // AUTO-APPROVE: create session + book slot atomically
                        req.put("status", "approved");
                        DocumentReference sessionRef = sessionsCol(tutorId).document();
                        Map<String, Object> session = new HashMap<>();
                        session.put("sessionId", sessionRef.getId());
                        session.put("slotId", slotId);
                        session.put("studentId", studentId);
                        session.put("status", "approved");

                        trx.set(rRef, req);
                        trx.set(sessionRef, session);
                        trx.update(sRef, "booked", true);
                    }
                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess(rRef.getId()))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ----------------------------------------------------------
       TUTOR — approve request (THIS sets booked=true)
       ---------------------------------------------------------- */
    @Override
    public void approveRequest(String tutorId, String requestId, SimpleCallback cb) {
        DocumentReference rRef = requestRef(tutorId, requestId);

        db.runTransaction(trx -> {
                    DocumentSnapshot req = trx.get(rRef);
                    if (!req.exists()) throw new IllegalStateException("Request missing");

                    String status = req.getString("status");
                    if (!"pending".equals(status)) {
                        throw new IllegalStateException("Request not pending");
                    }

                    String slotId = req.getString("slotId");
                    String studentId = req.getString("studentId");
                    DocumentReference sRef = slotRef(tutorId, slotId);

                    DocumentSnapshot slot = trx.get(sRef);
                    if (!slot.exists()) throw new IllegalStateException("Slot missing");
                    if (Boolean.TRUE.equals(slot.getBoolean("booked"))) {
                        throw new IllegalStateException("Slot already booked");
                    }

                    // Create session + book slot + flip request status — atomically
                    DocumentReference sessionRef = sessionsCol(tutorId).document();
                    Map<String, Object> session = new HashMap<>();
                    session.put("sessionId", sessionRef.getId());
                    session.put("slotId", slotId);
                    session.put("studentId", studentId);
                    session.put("status", "approved");

                    trx.set(sessionRef, session);
                    trx.update(sRef, "booked", true);      // only place booked=true (approval path)
                    trx.update(rRef, "status", "approved");

                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ----------------------------------------------------------
       TUTOR — reject request (do not touch slot.booked)
       ---------------------------------------------------------- */
    @Override
    public void rejectRequest(String tutorId, String requestId, SimpleCallback cb) {
        requestRef(tutorId, requestId).update("status", "rejected")
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ----------------------------------------------------------
       TUTOR — delete an availability slot
       ---------------------------------------------------------- */
    @Override
    public void deleteAvailabilitySlot(String tutorId, String slotId, SimpleCallback cb) {
        slotRef(tutorId, slotId).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ---------- helpers used by your UI ---------- */

    @Override
    public void getPendingRequests(String tutorId, RequestsListCallback cb) {
        requestsCol(tutorId)
                .whereEqualTo("status", "pending")
                .orderBy("requestedAtMillis", Query.Direction.DESCENDING) // Timestamp field
                .get()
                .addOnSuccessListener(snap -> {
                    List<SessionRequest> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        SessionRequest r = d.toObject(SessionRequest.class);
                        if (r == null) continue;
                        if (r.getId() == null || r.getId().isEmpty()) r.setId(d.getId());
                        list.add(r);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getSlotById(String tutorId, String slotId, SingleSlotCallback cb) {
        slotRef(tutorId, slotId).get()
                .addOnSuccessListener(d -> {
                    AvailabilitySlot s = d.toObject(AvailabilitySlot.class);
                    if (s == null) { cb.onError("Slot missing"); return; }
                    if (s.getEndTime() == null) s.setEndTime(d.getString("endTime"));
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getStudent(String studentId, StudentCallback cb) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(d -> {
                    Student s = d.toObject(Student.class);
                    if (s == null) { cb.onError("Student missing"); return; }
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getAvailabilitySlots(String tutorId, SlotsListCallback cb) {
        db.collection("users").document(tutorId)
                .collection("availabilitySlots")
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    List<AvailabilitySlot> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        AvailabilitySlot s = d.toObject(AvailabilitySlot.class);
                        if (s == null) continue;

                        // defensive: normalize potential nulls/legacy field names
                        if (s.getStartTime() == null) {
                            String legacyStart = d.getString("start");
                            if (legacyStart != null) s.setStartTime(legacyStart);
                        }
                        if (s.getEndTime() == null) {
                            String legacyEnd = d.getString("endTime");
                            if (legacyEnd != null) s.setEndTime(legacyEnd);
                        }

                        out.add(s);
                    }

                    // sort chronologically: date then startTime
                    Collections.sort(out, Comparator
                            .comparing(AvailabilitySlot::getDate, String::compareTo)
                            .thenComparing(AvailabilitySlot::getStartTime, String::compareTo));

                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void createAvailabilitySlot(String tutorId,
                                       String date,             // "yyyy-MM-dd"
                                       String startTime,        // "HH:mm"
                                       boolean requiresApproval,
                                       SlotCallback cb) {
        if (tutorId == null || tutorId.isEmpty()) { cb.onError("Missing tutorId"); return; }
        if (date == null || date.isEmpty() || startTime == null || startTime.isEmpty()) {
            cb.onError("Pick date and time"); return;
        }

        // Validate HH:mm and :00/:30
        int h, m;
        try {
            String[] parts = startTime.split(":");
            h = Integer.parseInt(parts[0]);
            m = Integer.parseInt(parts[1]);
            if (m % 30 != 0) { cb.onError("Time must be on :00 or :30"); return; }
            if (h < 0 || h > 23 || m < 0 || m > 59) { cb.onError("Invalid time"); return; }
        } catch (Exception e) {
            cb.onError("Invalid time"); return;
        }

        // Must be in the future
        Calendar cal = Calendar.getInstance();
        try {
            int yyyy = Integer.parseInt(date.substring(0, 4));
            int MM   = Integer.parseInt(date.substring(5, 7));
            int dd   = Integer.parseInt(date.substring(8, 10));
            cal.set(yyyy, MM - 1, dd, h, m, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            cb.onError("Invalid date"); return;
        }
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cb.onError("Start time must be in the future"); return;
        }

        // Compute end = start + 30 minutes
        int total = h * 60 + m + 30;
        int eh = (total / 60) % 24;
        int em = total % 60;
        String endTime = String.format(Locale.US, "%02d:%02d", eh, em);

        // Prevent overlap: same date + start
        db.collection("users").document(tutorId)
                .collection("availabilitySlots")
                .whereEqualTo("date", date)
                .whereEqualTo("startTime", startTime)       // canonical field name
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    if (!snap.isEmpty()) {
                        cb.onError("A slot at that time already exists");
                        return;
                    }

                    // Create the slot
                    AvailabilitySlot slot = new AvailabilitySlot();
                    slot.setDate(date);
                    slot.setStartTime(startTime);
                    slot.setEndTime(endTime);
                    slot.setRequiresApproval(requiresApproval);
                    slot.setBooked(false);

                    DocumentReference docRef = db.collection("users").document(tutorId)
                            .collection("availabilitySlots")
                            .document(); // auto id
                    try {
                        // if AvailabilitySlot has setId(String) use it so UI can read it later
                        slot.getClass().getMethod("setId", String.class).invoke(slot, docRef.getId());
                    } catch (Exception ignore) { /* id field may not exist */ }

                    docRef.set(slot)
                            .addOnSuccessListener(v -> cb.onSuccess(slot))
                            .addOnFailureListener(e -> cb.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ----------------------------------------------------------
       TUTOR — list sessions (upcoming & past)
       ---------------------------------------------------------- */
    @Override
    public void getTutorSessions(String tutorId, SessionsListCallback cb) {
        sessionsCol(tutorId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Session> all = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Session s = d.toObject(Session.class);
                        if (s == null) continue;
                        // ensure id is set on the model if it has a setter
                        try {
                            if (s.getId() == null || s.getId().isEmpty()) {
                                s.getClass().getMethod("setId", String.class).invoke(s, d.getId());
                            }
                        } catch (Exception ignore) { /* id field may not exist */ }
                        all.add(s);
                    }

                    long now = System.currentTimeMillis();
                    List<Session> upcoming = new ArrayList<>();
                    List<Session> past = new ArrayList<>();

                    for (Session s : all) {
                        long startMs = toMillisSafe(s.getDate(), s.getStartTime());
                        if (startMs >= now) upcoming.add(s); else past.add(s);
                    }

                    // sort upcoming ascending, past descending
                    Comparator<Session> byStart =
                            Comparator.comparing((Session s) -> toMillisSafe(s.getDate(), s.getStartTime()));
                    Collections.sort(upcoming, byStart);
                    Collections.sort(past, byStart.reversed());

                    cb.onSuccess(upcoming, past);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* convert "yyyy-MM-dd", "HH:mm" -> millis; 0L if bad */
    private long toMillisSafe(String yMd, String hm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse((yMd == null ? "" : yMd) + " " + (hm == null ? "" : hm))
                    .getTime();
        } catch (Exception e) {
            return 0L;
        }
    }
}
