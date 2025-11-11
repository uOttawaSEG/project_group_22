package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

public class AvailabilitySlot {
    private String id;          // e.g., "2025-11-15_1400"
    private String tutorId;
    private String date;        // yyyy-MM-dd
    private String startTime;   // HH:mm
    private String endTime;     // HH:mm
    private boolean requiresApproval;
    private boolean booked;
    private String subject;     // optional

    public AvailabilitySlot() {}

    public AvailabilitySlot(String id, String tutorId, String date, String startTime, String endTime,
                            boolean requiresApproval, boolean booked, String subject) {
        this.id = id; this.tutorId = tutorId; this.date = date; this.startTime = startTime;
        this.endTime = endTime; this.requiresApproval = requiresApproval; this.booked = booked; this.subject = subject;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}
