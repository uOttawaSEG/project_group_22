package com.example.seg2105_project_1_tutor_registration_form;

import android.util.Patterns;
import java.util.List;

import java.util.regex.Pattern;

public class Validator {

    public static boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        // Password should be at least 6 characters
        return password != null && password.length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        // Simple phone validation - at least 10 digits
        return phone != null && phone.replaceAll("\\D", "").length() >= 10;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }

    public static boolean isValidDegree(String degree) {
        return degree != null && !degree.trim().isEmpty();
    }

    public static boolean hasSelectedCourses(List<String> courses) {
        return courses != null && !courses.isEmpty();
    }
}