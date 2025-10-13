package ca.SEG2105group22.otams.auth;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ca.SEG2105group22.otams.model.User;

public interface AuthService {
    CompletableFuture<User> signUpStudent(String email, String password,
                                          String firstName, String lastName,
                                          String phone, String programOfStudy);

    CompletableFuture<User> signUpTutor(String email, String password,
                                        String firstName, String lastName,
                                        String phone, String highestDegree,
                                        List<String> courses);

    // Log in with email/password and return the Firestore user profile.
    CompletableFuture<User> signIn(String email, String password);

    // Sign out current user
    void signOut();
}