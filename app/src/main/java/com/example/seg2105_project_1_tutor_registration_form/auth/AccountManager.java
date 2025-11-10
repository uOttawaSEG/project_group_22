package com.example.seg2105_project_1_tutor_registration_form.auth;

// Import the following:
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import com.example.seg2105_project_1_tutor_registration_form.data.FirebaseRepository;

import java.util.HashMap;
import java.util.List;
import com.google.android.gms.tasks.Task;          // ✅ correct Task
import com.google.firebase.auth.AuthResult;        // (optional but nice)

import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * AccountManager (high-level overview for humans)
 * ─────────────────────────────────────────────────────────────────────────────
 * What this class does
 * • Wraps FirebaseRepository to handle auth + profile writes for three roles:
 *   STUDENT, TUTOR, and ADMIN.
 * • On successful sign-up it writes a user profile under /users/{uid}
 *   (via repo.saveUserProfile), and for STUDENT/TUTOR it also creates a
 *   /registrationRequests/{uid} document so an Admin can approve/reject.
 *
 * How callbacks work
 * • All public methods accept a simple Callback (ok, message):
 *   - ok == true  → operation succeeded
 *   - ok == false → something failed; "message" is short and user-friendly
 *   Keep UI logic outside this class; just act on (ok, message).
 *
 * Data that gets written
 * • /users/{uid}: basic profile fields + "role".
 * • /registrationRequests/{uid}: queue entry for Admin review with:
 *   id, userUid, firstName, lastName, email, phone, role, status=PENDING,
 *   submittedAt (epoch millis), and a course summary depending on role.
 *
 * Failure modes you should expect
 * • signUp fails (Firebase Auth): callback returns (false, "Sign-up failed").
 * • profile write fails (Firestore): callback returns (false, "Profile save failed").
 * • queue write fails (Firestore): profile exists, but request didn’t; we surface
 *   (false, "Profile saved, but failed to queue for admin review") so you notice.
 *
 * Security / trust notes
 * • Admin registration currently checks only that an inviteCode string is provided.
 *   For real apps, validate admin codes server-side or via Firestore security rules.
 *
 * Conventions used here
 * • Role strings: "Student" (profile) vs "STUDENT" (request) — be consistent in UI.
 * • Timestamp: System.currentTimeMillis() (Long) for submittedAt to sort by time.
 *
 * Maintenance tips
 * • Adding new profile fields? Update both the profile map and (if needed) the
 *   registration request map to keep Admin review useful.
 * • Localize user messages in the callback if you add i18n later.
 * • Remove casual/test comments before shipping (see note below).
 */
// NOTE: remove this before committing to main:
// // this is a test to see if shit is getting pushed to github
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

        repo.signUp(email, password).addOnCompleteListener(signUp -> {
            if (!signUp.isSuccessful() || repo.uid() == null) {
                cb.onResult(false, "Sign-up failed"); return;
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

            repo.saveUserProfile(uid, data).addOnCompleteListener(save -> {
                if (!save.isSuccessful()) {
                    cb.onResult(false, "Profile save failed");
                    return;
                }

                // Build a summary like "SEG2105, ITI1121"
                String coursesSummary = "";
                if (coursesWanted != null && !coursesWanted.isEmpty()) {
                    coursesSummary = String.join(", ", coursesWanted);
                }

                // Create /registrationRequests/{uid}
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> req = new HashMap<>();
                req.put("id", uid);
                req.put("userUid", uid);
                req.put("firstName", firstName);
                req.put("lastName", lastName);
                req.put("email", email);
                req.put("phone", phone);
                req.put("role", "STUDENT");                 // <- String, matches your model change
                req.put("status", "PENDING");
                req.put("submittedAt", System.currentTimeMillis()); // <-- Long epoch millis
                req.put("coursesWantedSummary", coursesSummary);
                req.put("coursesOfferedSummary", "");       // not used for students
                req.put("reason", null);                    // none yet

                db.collection("registrationRequests").document(uid)
                        .set(req)
                        .addOnSuccessListener(a -> {
                            Log.d("AccountManager", "RegRequest created for student");
                            cb.onResult(true, "Student saved");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AccountManager", "Failed to create RegRequest", e);
                            // Profile exists but request didn’t — still report failure so you notice
                            cb.onResult(false, "Profile saved, but failed to queue for admin review");
                        });
            });
        });
    }

    // -------- TUTOR REGISTER --------
    public void registerTutor(
            String firstName, String lastName, String email, String password,
            String phone, String degree, List<String> coursesOffered, @NonNull Callback cb) {

        repo.signUp(email, password).addOnCompleteListener(signUp -> {
            if (!signUp.isSuccessful() || repo.uid() == null) {
                cb.onResult(false, "Sign-up failed"); return;
            }
            String uid = repo.uid();
            Map<String, Object> data = new HashMap<>();
            data.put("role", "TUTOR");
            data.put("firstName", firstName);
            data.put("lastName", lastName);
            data.put("email", email);
            data.put("phone", phone);
            data.put("degree", degree);
            data.put("coursesOffered", coursesOffered);

            repo.saveUserProfile(uid, data).addOnCompleteListener(save -> {
                if (!save.isSuccessful()) {
                    cb.onResult(false, "Profile save failed");
                    return;
                }

                String coursesSummary = "";
                if (coursesOffered != null && !coursesOffered.isEmpty()) {
                    coursesSummary = String.join(", ", coursesOffered);
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> req = new HashMap<>();
                req.put("id", uid);
                req.put("userUid", uid);
                req.put("firstName", firstName);
                req.put("lastName", lastName);
                req.put("email", email);
                req.put("phone", phone);
                req.put("role", "TUTOR");                   // <- String
                req.put("status", "PENDING");
                req.put("submittedAt", System.currentTimeMillis()); // <-- Long epoch millis
                req.put("coursesWantedSummary", "");        // not used for tutors
                req.put("coursesOfferedSummary", coursesSummary);
                req.put("reason", null);

                db.collection("registrationRequests").document(uid)
                        .set(req)
                        .addOnSuccessListener(a -> {
                            Log.d("AccountManager", "RegRequest created for tutor");
                            cb.onResult(true, "Tutor saved");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AccountManager", "Failed to create RegRequest", e);
                            cb.onResult(false, "Profile saved, but failed to queue for admin review");
                        });
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
            data.put("role", "ADMIN");
            data.put("name", name);
            data.put("email", email);

            repo.saveUserProfile(uid, data).addOnCompleteListener(save ->
                    cb.onResult(save.isSuccessful(),
                            save.isSuccessful() ? "Admin saved" : "Profile save failed"));
        });
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of AccountManager
 * ─────────────────────────────────────────────────────────────────────────────
 * TL;DR for the future:
 * • This class is the single place the app signs in and registers users.
 * • For STUDENT/TUTOR we also open an Admin review ticket in /registrationRequests.
 * • Callback gives the UI a simple yes/no + short message; keep longer logs in Logcat.
 *
 * Testing tips
 * • Try sign-up failure (invalid email, weak password) to hit negative branches.
 * • Turn off network to simulate Firestore write failures and check messages.
 *
 * Safety
 * • Don’t rely on client-side invite codes for Admins in production; validate on server
 *   and lock down with Firebase Auth/Firestore security rules.
 */
