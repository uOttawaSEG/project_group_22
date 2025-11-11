// TutorSummary.java
package com.example.seg2105_project_1_tutor_registration_form.model.tutor;

public class TutorSummary {
    private String tutorId;
    private String displayName; // "First Last" (fallback: uid)
    private String email;
    private int openCount;

    public TutorSummary() {}
    public TutorSummary(String tutorId, String displayName, String email, int openCount) {
        this.tutorId = tutorId; this.displayName = displayName; this.email = email; this.openCount = openCount;
    }
    public String getTutorId() { return tutorId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public int getOpenCount() { return openCount; }
    public void setTutorId(String s){this.tutorId=s;}
    public void setDisplayName(String s){this.displayName=s;}
    public void setEmail(String s){this.email=s;}
    public void setOpenCount(int n){this.openCount=n;}
}

