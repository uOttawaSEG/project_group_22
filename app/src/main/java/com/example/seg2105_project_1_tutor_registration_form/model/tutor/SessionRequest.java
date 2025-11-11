package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

import com.google.firebase.Timestamp;

/**
 * SessionRequest
 * Represents a booking request for a specific availability slot.
 * Now includes date/start/end so UI can display time without slot lookup.
 */
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
    private String status;                 // PENDING, APPROVED, REJECTED
    private Timestamp requestedAtMillis;   // when the request was created

    // NEW: time carried on the request itself
    private String date;       // e.g., "2025-11-15"
    private String startTime;  // e.g., "14:00"
    private String endTime;    // e.g., "14:30"

    // Required empty constructor for Firestore
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

    public Timestamp getRequestedAtMillis() { return requestedAtMillis; }
    public void setRequestedAtMillis(Timestamp requestedAtMillis) { this.requestedAtMillis = requestedAtMillis; }

    // ---- NEW: date/time on the request ----
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // Keep legacy names used in your UI code:
    public String getStart() { return startTime; }
    public void setStart(String start) { this.startTime = start; }

    public String getEnd() { return endTime; }
    public void setEnd(String end) { this.endTime = end; }

    // Optional additional getters/setters if you prefer explicit names elsewhere:
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
