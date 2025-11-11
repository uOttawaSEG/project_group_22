package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Confirmed tutoring session (approved booking of a slot).
 * Stored at: users/{tutorId}/sessions/{sessionId}
 *
 * Persisted fields:
 *  - sessionId        : String (doc id; also stored as a field)
 *  - tutorId          : String
 *  - studentId        : String
 *  - slotId           : String
 *  - date             : "YYYY-MM-DD"
 *  - startTime        : "HH:mm"
 *  - endTime          : "HH:mm"           // == start + 30min
 *  - status           : String            // "upcoming" | "completed" | "canceled"
 *  - startMillis      : Long              // epoch ms of the start time (used for split/sort)
 *  - createdAtMillis  : Long              // epoch ms; auditing
 *  - notes            : String (optional)
 */
public class Session implements Serializable {

    private String sessionId;
    private String tutorId;
    private String studentId;
    private String slotId;

    private String date;         // e.g., "2025-11-15"
    private String startTime;    // e.g., "14:00"
    private String endTime;      // e.g., "14:30"

    private String status;       // "upcoming", "completed", "canceled"

    /** Epoch millis for the start instant (used by UI to split/sort upcoming/past). */
    private Long startMillis;

    /** Creation timestamp for auditing/sorting if needed. */
    private Long createdAtMillis;

    private String notes;        // optional

    /** Required by Firestore. */
    public Session() {}

    // --- Getters / Setters ---

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Epoch millis of the session start (used by UI). */
    public Long getStartMillis() { return startMillis; }
    public void setStartMillis(Long startMillis) { this.startMillis = startMillis; }

    public Long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(Long createdAtMillis) { this.createdAtMillis = createdAtMillis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // --- Convenience ---

    /** "YYYY-MM-DD • HH:mm–HH:mm" (safe even if some parts are missing). */
    public String whenLabel() {
        String d = safe(date), s = safe(startTime), e = safe(endTime);
        if (!d.isEmpty() && !s.isEmpty() && !e.isEmpty()) return d + " • " + s + "–" + e;
        if (!d.isEmpty() && !s.isEmpty()) return d + " • " + s;
        return (d + " " + s).trim();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session)) return false;
        Session session = (Session) o;
        return Objects.equals(sessionId, session.sessionId);
    }

    @Override public int hashCode() { return Objects.hash(sessionId); }

    @Override public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", tutorId='" + tutorId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", slotId='" + slotId + '\'' +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", status='" + status + '\'' +
                ", startMillis=" + startMillis +
                ", createdAtMillis=" + createdAtMillis +
                (notes != null ? ", notes='" + notes + '\'' : "") +
                '}';
    }
}
