package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

/** Firestore model for an approved tutoring session. */
public class Session {
    private String id;           // sessionId (we reuse slotId)
    private String slotId;

    private String tutorId;

    private String studentId;
    private String studentName;
    private String studentEmail;

    private Integer rating;

    private String date;         // yyyy-MM-dd
    private String startTime;    // HH:mm
    private String endTime;      // HH:mm
    private String subject;      // e.g., "Physics"
    private String status;       // "UPCOMING", "DONE", "CANCELLED"

    /** Required empty constructor for Firestore. */
    public Session() {}

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

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
