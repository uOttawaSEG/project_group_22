package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

public class AvailabilitySlot {
    private String slotId;
    private String tutorId;
    private String date;          // "YYYY-MM-DD"
    private String startTime;     // "HH:mm"
    private String endTime;       // "HH:mm"
    private boolean requiresApproval;
    private boolean booked;
    private long startMillis;
    private long createdAt;
    private String status;        // "OPEN" | "BOOKED" | etc.

    // Getters/Setters
    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
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
    public long getStartMillis() { return startMillis; }
    public void setStartMillis(long startMillis) { this.startMillis = startMillis; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

