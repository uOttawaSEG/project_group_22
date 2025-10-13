package com.example.seg2105_project_1_tutor_registration_form.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Student {

    // --- Core identity ---
    private String uid;          // Firebase Auth UID (or DB id)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;        // optional

    // --- Student-specific ---
    private String studentId;    // e.g., 300xxxxxx
    private String program;      // e.g., "Bachelor's of Science"
    private String studyYear;    // e.g., "2nd year"
    private List<String> coursesInterested; // e.g., ["SEG 2105","CSI 2101"]
    private String notes;        // optional

    // --- Meta ---
    private String role = "student"; // constant

    // Required by Firestore/JSON
    public Student() { }

    public Student(String uid,
                   String firstName,
                   String lastName,
                   String email,
                   String phone,
                   String studentId,
                   String program,
                   String studyYear,
                   List<String> coursesInterested,
                   String notes) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.studentId = studentId;
        this.program = program;
        this.studyYear = studyYear;
        this.coursesInterested = coursesInterested != null ? coursesInterested : new ArrayList<>();
        this.notes = notes;
        this.role = "student";
    }

    // --- Getters/Setters ---
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

    public List<String> getCoursesInterested() { return coursesInterested; }
    public void setCoursesInterested(List<String> coursesInterested) { this.coursesInterested = coursesInterested; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; } // usually stays "student"

    // --- Firestore helpers ---
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("firstName", firstName);
        m.put("lastName", lastName);
        m.put("email", email);
        m.put("phone", phone);
        m.put("studentId", studentId);
        m.put("program", program);
        m.put("studyYear", studyYear);
        m.put("coursesInterested", coursesInterested);
        m.put("notes", notes);
        m.put("role", role);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Student fromMap(Map<String, Object> m) {
        if (m == null) return null;
        Student s = new Student();
        s.uid         = (String) m.get("uid");
        s.firstName   = (String) m.get("firstName");
        s.lastName    = (String) m.get("lastName");
        s.email       = (String) m.get("email");
        s.phone       = (String) m.get("phone");
        s.studentId   = (String) m.get("studentId");
        s.program     = (String) m.get("program");
        s.studyYear   = (String) m.get("studyYear");
        Object courses = m.get("coursesInterested");
        if (courses instanceof List) {
            s.coursesInterested = (List<String>) courses;
        } else {
            s.coursesInterested = new ArrayList<>();
        }
        s.notes = (String) m.get("notes");
        s.role  = (String) m.getOrDefault("role", "student");
        return s;
    }
}
