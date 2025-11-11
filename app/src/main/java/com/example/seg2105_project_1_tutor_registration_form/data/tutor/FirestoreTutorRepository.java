package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import androidx.annotation.NonNull;
// Import the following classes for Firestore
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FirestoreTutorRepository implements TutorRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /* ---------- helpers ---------- */

    private CollectionReference slotsCol(String tutorId) {
        return db.collection("users").document(tutorId).collection("availabilitySlots");
    }
    private DocumentReference slotDoc(String tutorId, String slotId) {
        return slotsCol(tutorId).document(slotId);
    }
    private CollectionReference reqsCol(String tutorId) {
        return db.collection("users").document(tutorId).collection("sessionRequests");
    }
    private DocumentReference reqDoc(String tutorId, String requestId) {
        return reqsCol(tutorId).document(requestId);
    }
    private CollectionReference sessionsCol(String tutorId) {
        return db.collection("users").document(tutorId).collection("sessions");
    }
    private long parseMillis(String yMd, String hm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(yMd + " " + hm).getTime();
        } catch (Exception e) { return 0L; }
    }

    private static boolean isHalfHour(String startTime) {
        return startTime.endsWith(":00") || startTime.endsWith(":30");
    }
    private static String add30(String startTime) {
        String[] p = startTime.split(":");
        int m = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]) + 30;
        return String.format(Locale.US, "%02d:%02d", (m/60)%24, m%60);
    }

    /* ---------- Availability ---------- */

    @Override
    public void createAvailabilitySlot(String tutorId, String date, String startTime,
                                       boolean requiresApproval, SlotCallback cb) {
        try {
            if (!isHalfHour(startTime)) { cb.onError("Start time must end with :00 or :30"); return; }
            String endTime = add30(startTime);
            String slotId = date + "_" + startTime.replace(":", "");

            DocumentReference slotRef = slotDoc(tutorId, slotId);
            // Overlap guard = same date+start = same id
            slotRef.get().addOnSuccessListener(s -> {
                if (s.exists()) { cb.onError("A slot at this time already exists"); return; }

                AvailabilitySlot slot = new AvailabilitySlot(
                        slotId, tutorId, date, startTime, endTime, requiresApproval, false, null
                );
                slotRef.set(slot).addOnSuccessListener(v -> cb.onSuccess(slot))
                        .addOnFailureListener(e -> cb.onError(e.getMessage()));
            }).addOnFailureListener(e -> cb.onError(e.getMessage()));
        } catch (Exception ex) { cb.onError(ex.getMessage()); }
    }

    @Override
    public void getAvailabilitySlots(String tutorId, SlotsListCallback cb) {
        slotsCol(tutorId).get().addOnSuccessListener(snap -> {
            List<AvailabilitySlot> list = new ArrayList<>();
            for (DocumentSnapshot d : snap) {
                AvailabilitySlot a = d.toObject(AvailabilitySlot.class);
                if (a != null) list.add(a);
            }
            list.sort(Comparator.comparing(AvailabilitySlot::getDate)
                    .thenComparing(AvailabilitySlot::getStartTime));
            cb.onSuccess(list);
        }).addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void deleteAvailabilitySlot(String tutorId, String slotId, SimpleCallback cb) {
        slotDoc(tutorId, slotId).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getSlotById(String tutorId, String slotId, SingleSlotCallback cb) {
        slotDoc(tutorId, slotId).get()
                .addOnSuccessListener(d -> {
                    AvailabilitySlot a = d.toObject(AvailabilitySlot.class);
                    if (a == null) cb.onError("Slot not found"); else cb.onSuccess(a);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ---------- Requests (tutor) ---------- */

    @Override
    public void getPendingRequests(String tutorId, RequestsListCallback cb) {
        reqsCol(tutorId).whereEqualTo("status", "PENDING")
                .orderBy("requestedAtMillis", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(snap -> {
                    List<SessionRequest> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        SessionRequest r = d.toObject(SessionRequest.class);
                        if (r != null) list.add(r);
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void approveRequest(String tutorId, String requestId, SimpleCallback cb) {
        DocumentReference reqRef = reqDoc(tutorId, requestId);
        db.runTransaction((Transaction.Function<Void>) tr -> {
                    DocumentSnapshot rSnap = tr.get(reqRef);
                    if (!rSnap.exists()) throw new RuntimeException("Request not found");
                    SessionRequest r = rSnap.toObject(SessionRequest.class);
                    if (r == null) throw new RuntimeException("Request parse error");
                    if (!"PENDING".equalsIgnoreCase(r.getStatus()))
                        throw new RuntimeException("Request is not pending");

                    DocumentReference slotRef = slotDoc(tutorId, r.getSlotId());
                    DocumentSnapshot sSnap = tr.get(slotRef);
                    if (!sSnap.exists()) throw new RuntimeException("Slot missing");
                    AvailabilitySlot a = sSnap.toObject(AvailabilitySlot.class);
                    if (a == null) throw new RuntimeException("Slot parse error");
                    if (a.isBooked()) throw new RuntimeException("Slot already booked");

                    String sessionId = a.getId(); // reuse slotId
                    Session session = new Session();
                    session.setId(sessionId);
                    session.setSlotId(a.getId());
                    session.setTutorId(tutorId);
                    session.setStudentId(r.getStudentId());
                    session.setStudentName(r.getStudentName());
                    session.setStudentEmail(r.getStudentEmail());
                    session.setDate(a.getDate());
                    session.setStartTime(a.getStartTime());
                    session.setEndTime(a.getEndTime());
                    session.setSubject(a.getSubject());
                    session.setStatus("UPCOMING");

                    tr.update(slotRef, "booked", true);
                    tr.update(reqRef, "status", "APPROVED");
                    tr.set(sessionsCol(tutorId).document(sessionId), session);
                    // mirror to student
                    tr.set(db.collection("users").document(r.getStudentId())
                            .collection("sessions").document(sessionId), session);
                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void rejectRequest(String tutorId, String requestId, SimpleCallback cb) {
        reqDoc(tutorId, requestId).update("status", "REJECTED")
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getRequestById(String tutorId, String requestId, SingleRequestCallback cb) {
        reqDoc(tutorId, requestId).get()
                .addOnSuccessListener(d -> {
                    SessionRequest r = d.toObject(SessionRequest.class);
                    if (r == null) cb.onError("Request not found"); else cb.onSuccess(r);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ---------- Sessions ---------- */

    @Override
    public void getTutorSessions(String tutorId, SessionsListCallback cb) {
        sessionsCol(tutorId).get().addOnSuccessListener(snap -> {
            List<Session> all = new ArrayList<>();
            for (DocumentSnapshot d : snap) {
                Session s = d.toObject(Session.class);
                if (s != null) all.add(s);
            }
            long now = System.currentTimeMillis();
            List<Session> upcoming = new ArrayList<>(), past = new ArrayList<>();
            for (Session s : all) {
                long t = parseMillis(s.getDate(), s.getStartTime());
                if (t >= now) upcoming.add(s); else past.add(s);
            }
            upcoming.sort(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime));
            past.sort(Comparator.comparing(Session::getDate).thenComparing(Session::getStartTime).reversed());
            cb.onSuccess(upcoming, past);
        }).addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void cancelSession(String tutorId, String sessionId, SimpleCallback cb) {
        DocumentReference sRef = sessionsCol(tutorId).document(sessionId);
        db.runTransaction((Transaction.Function<Void>) tr -> {
                    DocumentSnapshot sSnap = tr.get(sRef);
                    if (!sSnap.exists()) throw new RuntimeException("Session not found");
                    Session s = sSnap.toObject(Session.class);
                    if (s == null) throw new RuntimeException("Session parse error");

                    tr.update(sRef, "status", "CANCELLED");
                    tr.update(slotDoc(tutorId, s.getSlotId()), "booked", false);
                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    @Override
    public void getSessionById(String tutorId, String sessionId, SingleSessionCallback cb) {
        sessionsCol(tutorId).document(sessionId).get()
                .addOnSuccessListener(d -> {
                    Session s = d.toObject(Session.class);
                    if (s == null) cb.onError("Session not found"); else cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /* ---------- Student profile ---------- */

    @Override
    // FirestoreTutorRepository#getStudent(...)
    public void getStudent(String studentId, StudentCallback cb) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError("Student not found"); return; }

                    com.example.seg2105_project_1_tutor_registration_form.model.Student s =
                            new com.example.seg2105_project_1_tutor_registration_form.model.Student();

                    s.setUid(studentId);  // <— was setId(...)
                    s.setFirstName((String) d.get("firstName"));
                    s.setLastName((String) d.get("lastName"));
                    s.setEmail((String) d.get("email"));
                    s.setPhone((String) d.get("phone"));
                    s.setStudentId((String) d.get("studentId"));
                    s.setProgram((String) d.get("program"));
                    s.setStudyYear((String) d.get("studyYear"));

                    Object courses = d.get("coursesInterested");
                    if (courses instanceof java.util.List) {
                        //noinspection unchecked
                        s.setCoursesInterested((java.util.List<String>) courses);
                    } else {
                        s.setCoursesInterested(new java.util.ArrayList<>());
                    }

                    s.setNotes((String) d.get("notes"));
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }


    /* ---------- Student action: submit session request ---------- */

    @Override
    public void submitSessionRequest(String tutorId, String studentId, String slotId, RequestCreateCallback cb) {
        DocumentReference slotRef = slotDoc(tutorId, slotId);
        slotRef.get().addOnSuccessListener(s -> {
            if (!s.exists()) { cb.onError("Slot not found"); return; }
            AvailabilitySlot a = s.toObject(AvailabilitySlot.class);
            if (a == null) { cb.onError("Slot parse error"); return; }
            if (a.isBooked()) { cb.onError("Slot already booked"); return; }

            String requestId = UUID.randomUUID().toString();
            Map<String, Object> req = new HashMap<>();
            req.put("id", requestId);
            req.put("slotId", slotId);
            req.put("tutorId", tutorId);
            req.put("studentId", studentId);
            req.put("studentName", null);   // fill in UI if you capture
            req.put("studentEmail", null);
            req.put("note", null);
            req.put("grade", null);
            req.put("subject", a.getSubject());
            req.put("requestedAtMillis", FieldValue.serverTimestamp());

            if (a.isRequiresApproval()) {
                req.put("status", "PENDING");
                WriteBatch b = db.batch();
                b.set(reqsCol(tutorId).document(requestId), req);
                b.update(slotRef, "booked", true); // soft hold until approve/reject
                b.commit().addOnSuccessListener(v -> cb.onSuccess(requestId))
                        .addOnFailureListener(e -> cb.onError(e.getMessage()));
            } else {
                // auto-approve path
                req.put("status", "APPROVED");
                String sessionId = a.getId();
                Session session = new Session();
                session.setId(sessionId);
                session.setSlotId(a.getId());
                session.setTutorId(tutorId);
                session.setStudentId(studentId);
                session.setDate(a.getDate());
                session.setStartTime(a.getStartTime());
                session.setEndTime(a.getEndTime());
                session.setSubject(a.getSubject());
                session.setStatus("UPCOMING");

                WriteBatch b = db.batch();
                b.set(reqsCol(tutorId).document(requestId), req);
                b.set(sessionsCol(tutorId).document(sessionId), session);
                b.update(slotRef, "booked", true);
                b.commit().addOnSuccessListener(v -> cb.onSuccess(requestId))
                        .addOnFailureListener(e -> cb.onError(e.getMessage()));
            }
        }).addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

}
