package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/** Represents a booking request for a specific availability slot. */
public class SessionRequest {

    private String id;
    private String slotId;
    private String tutorId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String note;
    private String grade;
    private String subject;
    private String status;   // "pending", "approved", "rejected"

    // ---- Time fields ----
    // Store ONE timestamp in Java; map it to Firestore key "requestedAtMillis"
    private Timestamp requestedAt;   // canonical in Java

    // Optional: time carried on the request to avoid slot lookup
    private String date;       // "yyyy-MM-dd"
    private String startTime;  // "HH:mm"
    private String endTime;    // "HH:mm"

    public SessionRequest() {}

    // ---- IDs & parties ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    // ---- Misc ----
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ---- Timestamp mapping ----
    // Your fragment calls getRequestedAtMillis(); keep that name.
    public Timestamp getRequestedAtMillis() { return requestedAt; }

    // Bind to Firestore field named "requestedAtMillis"
    @PropertyName("requestedAtMillis")
    public void setRequestedAt(Timestamp t) { this.requestedAt = t; }

    @PropertyName("requestedAtMillis")
    public Timestamp getRequestedAt() { return requestedAt; }

    // ---- Date/time convenience (keeps both legacy and explicit names) ----
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // Legacy names used in some UI
    public String getStart() { return startTime; }
    public void setStart(String start) { this.startTime = start; }

    public String getEnd() { return endTime; }
    public void setEnd(String end) { this.endTime = end; }

    // Explicit names
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
