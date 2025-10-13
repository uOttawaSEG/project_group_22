package ca.SEG2105group22.otams.model;

import java.util.List;

public class User {
    public String uid;
    public String email;
    public Role role;
    public ApprovalStatus status;

    // common profile
    public String firstName;
    public String lastName;
    public String phone;

    // student-only
    public String programOfStudy;

    // tutor-only
    public String highestDegree;
    public List<String> courses;

    public User() {}

    public static User student(String uid, String email, String first, String last,
                               String phone, String programOfStudy) {
        User u = new User();
        u.uid = uid; u.email = email; u.role = Role.STUDENT; u.status = ApprovalStatus.APPROVED;
        u.firstName = first; u.lastName = last; u.phone = phone; u.programOfStudy = programOfStudy;
        return u;
    }

    public static User tutor(String uid, String email, String first, String last,
                             String phone, String highestDegree, List<String> courses) {
        User u = new User();
        u.uid = uid; u.email = email; u.role = Role.TUTOR; u.status = ApprovalStatus.APPROVED;
        u.firstName = first; u.lastName = last; u.phone = phone;
        u.highestDegree = highestDegree; u.courses = courses;
        return u;
    }

    public static User admin(String uid, String email, String first, String last) {
        User u = new User();
        u.uid = uid; u.email = email; u.role = Role.ADMIN; u.status = ApprovalStatus.APPROVED;
        u.firstName = first; u.lastName = last;
        return u;
    }
}
