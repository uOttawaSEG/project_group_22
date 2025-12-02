package com.example.seg2105_project_1_tutor_registration_form.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tutor extends User implements Serializable {
    private String degree;
    private double averageRating;
    private int ratingsCount;
    private int ratingsSum;
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

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(int ratingsCount) { this.ratingsCount = ratingsCount; }

    public int getRatingsSum() { return ratingsSum; }
    public void setRatingsSum(int ratingsSum) { this.ratingsSum = ratingsSum; }

    public void applyNewRating(int stars) {
        if (stars < 1 || stars > 5) {
            return; // ignore invalid ratings
        }
        this.ratingsSum += stars;
        this.ratingsCount += 1;
        this.averageRating = (ratingsCount == 0)
                ? 0.0
                : (double) ratingsSum / ratingsCount;
    }

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
