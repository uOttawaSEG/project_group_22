package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Firestore model for a student's request to book a tutor's availability slot.
 * Stored at: users/{tutorId}/sessionRequests/{requestId}
 *
 * Required fields (persisted):
 *  - requestId          : String (doc id; also stored as a field)
 *  - tutorId            : String
 *  - studentId          : String
 *  - slotId             : String
 *  - status             : String  ("pending" | "approved" | "rejected")
 *  - requestedAtMillis  : Long    (epoch ms; newest-first sorting)
 *
 * Optional convenience fields (persisted if provided by the writer):
 *  - date               : "YYYY-MM-DD" of the slot (so UI can show without extra fetch)
 *  - startTime          : "HH:mm"
 *  - endTime            : "HH:mm"
 *
 * Firestore requires a public no-arg constructor and public getters/setters.
 */
public class SessionRequest implements Serializable {

    private String requestId;
    private String tutorId;
    private String studentId;
    private String slotId;
    private String status;            // "pending", "approved", "rejected"
    private Long requestedAtMillis;   // use Long (not long) to tolerate nulls from Firestore

    // Optional (if writer denormalizes slot info into the request)
    private String date;              // e.g., "2025-11-15"
    private String startTime;         // e.g., "14:00"
    private String endTime;           // e.g., "14:30"

    /** Required by Firestore. */
    public SessionRequest() {}

    // --- Getters/Setters ---

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getRequestedAtMillis() { return requestedAtMillis; }
    public void setRequestedAtMillis(Long requestedAtMillis) { this.requestedAtMillis = requestedAtMillis; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // --- Convenience ---

    /** Composite label like "2025-11-15 • 14:00–14:30" (falls back safely if fields are missing). */
    public String label() {
        String d = date != null ? date : "";
        String s = startTime != null ? startTime : "";
        String e = endTime != null ? endTime : "";
        if (!d.isEmpty() && !s.isEmpty() && !e.isEmpty()) return d + " • " + s + "–" + e;
        return (d + " " + s).trim();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionRequest)) return false;
        SessionRequest that = (SessionRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override public int hashCode() { return Objects.hash(requestId); }

    @Override public String toString() {
        return "SessionRequest{" +
                "requestId='" + requestId + '\'' +
                ", tutorId='" + tutorId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", slotId='" + slotId + '\'' +
                ", status='" + status + '\'' +
                ", requestedAtMillis=" + requestedAtMillis +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
