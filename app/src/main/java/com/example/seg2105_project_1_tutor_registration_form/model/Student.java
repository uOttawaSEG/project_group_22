package com.example.seg2105_project_1_tutor_registration_form.model;

import java.io.Serializable;

/**
 * Student profile stored in /users/{uid}.
 * Used when the tutor taps into SessionDetailActivity or Pending Requests.
 */
public class Student implements Serializable {

    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Optional fields depending on how your student registration form works
    private String studentId;
    private String program;
    private String studyYear;
    private String notes;

    /** Required by Firestore */
    public Student() {}

    // --- Getters & Setters ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getStudyYear() { return studyYear; }
    public void setStudyYear(String studyYear) { this.studyYear = studyYear; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // --- Convenience for UI ---
    public String fullName() {
        String fn = firstName == null ? "" : firstName;
        String ln = lastName == null ? "" : lastName;
        return (fn + " " + ln).trim();
    }
}
