package com.example.seg2105_project_1_tutor_registration_form.model;

import androidx.annotation.Nullable;
import com.example.seg2105_project_1_tutor_registration_form.data.RequestStatus;

public class RegRequest {

    private String id;
    private String userUid;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Stored in Firestore as strings
    private String role;    // "STUDENT"|"TUTOR"|...
    private String status;  // "PENDING"|"APPROVED"|"REJECTED"

    private String coursesWantedSummary;
    private String coursesOfferedSummary;

    private String decidedBy;
    private Long decidedAt;
    private Long submittedAt;
    private String reason;

    public RegRequest() {}

    public RegRequest(
            String id,
            String userUid,
            String firstName,
            String lastName,
            String email,
            String phone,
            Role role,
            String coursesWantedSummary,
            String coursesOfferedSummary,
            RequestStatus status,
            String decidedBy,
            Long decidedAt,
            Long submittedAt,
            String reason
    ) {
        this.id = id;
        this.userUid = userUid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        // store as strings for Firestore
        this.role = role == null ? null : role.name();
        this.status = status == null ? null : status.name();
        this.coursesWantedSummary = coursesWantedSummary;
        this.coursesOfferedSummary = coursesOfferedSummary;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.submittedAt = submittedAt;
        this.reason = reason;
    }

    // --- Firestore-mapped getters/setters (strings) ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }          // <- string for Firestore
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }      // <- string for Firestore
    public void setStatus(String status) { this.status = status; }

    public String getCoursesWantedSummary() { return coursesWantedSummary; }
    public void setCoursesWantedSummary(String s) { this.coursesWantedSummary = s; }

    public String getCoursesOfferedSummary() { return coursesOfferedSummary; }
    public void setCoursesOfferedSummary(String s) { this.coursesOfferedSummary = s; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public Long getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Long decidedAt) { this.decidedAt = decidedAt; }

    public Long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Long submittedAt) { this.submittedAt = submittedAt; }

    public @Nullable String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    // --- Helpers for your UI (enum views) — NOT used by Firestore mapping ---
    public @Nullable Role getRoleEnum() {
        if (role == null) return null;
        try { return Role.valueOf(role.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    public @Nullable RequestStatus getStatusEnum() {
        if (status == null) return null;
        try { return RequestStatus.valueOf(status.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    // Convenience
    public String fullName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName  == null ? "" : lastName.trim();
        return (fn + " " + ln).trim();
    }
}
