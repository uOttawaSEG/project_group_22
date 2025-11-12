package com.example.seg2105_project_1_tutor_registration_form.model.tutor;
/* * AvailabilitySlot — POJO for a single tutoring window of thirty minutes. * * * Goal * - Indicates a single atomic slot that a student may reserve or request. * * Field meanings are stored in Firestore under users/{tutorId}/availabilitySlots/{slotId}.
 * - id: Firestore document id (used by UI/actions; set after create).
 * - tutorId: the slot's owner (denormalized for ease of use or inquiries).
 * - date: "yyyy-MM-dd" (safely sorted string).
 * - startTime/endTime: "HH:mm" (24 hours). StartTime + 30 minutes equals endTime.
 * * requiresApproval: false → auto-approve (session created instantly); * requiresApproval: true → requests are PENDING until tutor approves.
 * - booked: true upon approval (or auto-approval) of a request.
 * Subject: optional label (such as "PHY1121") that is not necessary for core flow
 * Invariants (required by the repository when creating or updating)
 * - startTime minutes that fall between {00, 30} * - (date, startTime) distinct for each tutor * */


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
