package com.example.seg2105_project_1_tutor_registration_form.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tutor extends User implements Serializable {
    private String degree;
    private List<String> courses; // never null in getters

    public Tutor() {
        // keep fields default; courses lazily guarded
    }

    public Tutor(String id, String f, String l, String e, String p, String ph,
                 String degree, List<String> courses) {
        super(id, f, l, e, p, ph);
        this.degree  = degree;
        this.courses = courses;
    }

    @Override public Role getRole() { return Role.TUTOR; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public List<String> getCourses() {
        if (courses == null) courses = new ArrayList<>();
        return courses;
    }
    public void setCourses(List<String> courses) {
        this.courses = (courses == null) ? new ArrayList<>() : new ArrayList<>(courses);
    }
    public void addCourse(String course) {
        getCourses().add(course);
    }
}
