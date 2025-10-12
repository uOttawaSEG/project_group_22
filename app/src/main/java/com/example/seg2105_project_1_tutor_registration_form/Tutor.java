package com.example.seg2105_project_1_tutor_registration_form;

import java.io.Serializable;   // added
import java.util.List;         // added

public class Tutor implements Serializable {   // implements Serializable
    private static final long serialVersionUID = 1L;  // optional but good practice

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String degree;
    private List<String> courses;

    public Tutor() {
        // Default constructor required for calls to DataSnapshot.getValue(Tutor.class)
    }

    public Tutor(String firstName, String lastName, String email, String password,
                 String phone, String degree, List<String> courses) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.degree = degree;
        this.courses = courses;
    }

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public List<String> getCourses() { return courses; }
    public void setCourses(List<String> courses) { this.courses = courses; }
}
