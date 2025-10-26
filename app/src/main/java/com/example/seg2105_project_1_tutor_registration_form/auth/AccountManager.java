package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.seg2105_project_1_tutor_registration_form.data.FirebaseRepository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.RegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountManager {

    public interface Callback { void onResult(boolean ok, String message); }

    private final FirebaseRepository repo;

    public AccountManager(Context context) {
        this.repo = new FirebaseRepository();
    }

    // -------- LOGIN (role-agnostic) --------
    public void login(@NonNull String email, @NonNull String password, @NonNull Callback cb) {
        repo.signIn(email, password).addOnCompleteListener(task ->
                cb.onResult(task.isSuccessful(), task.isSuccessful() ? "OK" : "Login failed"));
    }

    // -------- STUDENT REGISTER --------
    public void registerStudent(
            String firstName, String lastName, String email, String password,
            String phone, String studentId, String program, String studyYear,
            List<String> coursesWanted, String notes, @NonNull Callback cb) {

        repo.signUp(email, password).addOnCompleteListener((Task<AuthResult> signUp) -> {
            if (!signUp.isSuccessful() || repo.uid() == null) {
                cb.onResult(false, "Sign-up failed");
                return;
            }

            String uid = repo.uid();
            Map<String, Object> data = new HashMap<>();
            data.put("role", "Student");
            data.put("firstName", firstName);
            data.put("lastName", lastName);
            data.put("email", email);
            data.put("phone", phone);
            data.put("studentId", studentId);
            data.put("program", program);
            data.put("studyYear", studyYear);
            data.put("coursesWanted", coursesWanted);
            data.put("notes", notes);

            repo.saveUserProfile(uid, data).addOnCompleteListener((Task<Void> save) -> {
                cb.onResult(save.isSuccessful(),
                        save.isSuccessful() ? "Student saved" : "Profile save failed");

                if (save.isSuccessful()) {
                    RegistrationRepository regRepo = new FirestoreRegistrationRepository();
                    RegRequest req = RegRequest.pending(
                            uid,
                            "Student",
                            firstName,
                            lastName,
                            email,
                            phone,
                            System.currentTimeMillis()
                    );

                    regRepo.createFromRegistration(req)
                            .addOnSuccessListener(id ->
                                    Log.d("AccountManager", "Student registration request created: " + id))
                            .addOnFailureListener(e ->
                                    Log.e("AccountManager", "Failed to create student registration request", e));
                }
            });
        });
    }

    // -------- TUTOR REGISTER --------
    public void registerTutor(
            String firstName,
            String lastName,
            String email,
            String password,
            String phone,
            String highestDegree,
            List<String> coursesOffered,
            @NonNull Callback cb) {

        // Create account with Firebase Authentication
        repo.signUp(email, password).addOnCompleteListener((Task<AuthResult> signUp) -> {
            if (!signUp.isSuccessful() || repo.uid() == null) {
                cb.onResult(false, "Sign-up failed");
                return;
            }

            String uid = repo.uid();
            Map<String, Object> data = new HashMap<>();
            data.put("role", "Tutor");
            data.put("firstName", firstName);
            data.put("lastName", lastName);
            data.put("email", email);
            data.put("phone", phone);
            data.put("highestDegree", highestDegree);
            data.put("coursesOffered", coursesOffered);

            // Save tutor profile in Firestore
            repo.saveUserProfile(uid, data).addOnCompleteListener((Task<Void> save) -> {
                cb.onResult(save.isSuccessful(),
                        save.isSuccessful() ? "Tutor saved" : "Profile save failed");

                // Only create a registration request if the save succeeded
                if (save.isSuccessful()) {
                    RegistrationRepository regRepo = new FirestoreRegistrationRepository();
                    RegRequest req = RegRequest.pending(
                            uid,
                            "Tutor",                 // role (string)
                            firstName,
                            lastName,
                            email,
                            phone,
                            System.currentTimeMillis()
                    );

                    regRepo.createFromRegistration(req)
                            .addOnSuccessListener(id ->
                                    Log.d("AccountManager", "Tutor registration request created: " + id))
                            .addOnFailureListener(e ->
                                    Log.e("AccountManager", "Failed to create tutor registration request", e));
                }
            });
        });
    }

    // -------- ADMIN REGISTER (optional) --------
    // In AccountManager.java
    public void registerAdmin(String name,
                              String email,
                              String password,
                              String inviteCode,
                              @NonNull Callback cb) {

        // Basic invite check (keeps things simple). Replace with repo.validateAdminCode(...)
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            cb.onResult(false, "Invite code required");
            return;
        }

        // If you created FirebaseRepository.validateAdminCode(...), you can do:
        // repo.validateAdminCode(inviteCode).addOnCompleteListener(validTask -> {
        //     boolean ok = validTask.isSuccessful() && Boolean.TRUE.equals(validTask.getResult());
        //     if (!ok) { cb.onResult(false, "Invalid invite code"); return; }
        //
        //     // then continue with signUp below...
        // });

        repo.signUp(email, password).addOnCompleteListener(signUp -> {
            if (!signUp.isSuccessful() || repo.uid() == null) {
                cb.onResult(false, "Sign-up failed");
                return;
            }

            String uid = repo.uid();
            Map<String, Object> data = new HashMap<>();
            data.put("role", "Admin");
            data.put("name", name);
            data.put("email", email);

            repo.saveUserProfile(uid, data).addOnCompleteListener(save ->
                    cb.onResult(save.isSuccessful(),
                            save.isSuccessful() ? "Admin saved" : "Profile save failed"));
        });
    }
}
