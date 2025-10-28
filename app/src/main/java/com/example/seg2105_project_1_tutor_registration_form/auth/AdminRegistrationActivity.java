package com.example.seg2105_project_1_tutor_registration_form.auth;

/*
 * AdminRegistrationActivity
 *
 * Responsibility:
 * - Provision exactly one administrative account for the application.
 * - Enforce a strict allowlist consisting of a fixed admin email + invite code.
 * - Create the admin in Firebase Auth and persist a minimal role-bearing profile in Firestore.
 * - If the email already exists, attempt sign-in instead of surfacing a hard failure.
 *
 * Notes on correctness and constraints:
 * - This screen should be reachable only in controlled scenarios (e.g., first-time setup or internal tooling).
 * - ADMIN_EMAIL and ADMIN_INVITE_CODE must be managed as a pair. Consider Remote Config or a secure server-side
 *   bootstrap flow if rotation is required.
 * - Minimal profile fields (role, email, name) are persisted to support role-based routing and display.
 *
 * Failure handling:
 * - Input validation failures are surfaced immediately via toasts; network and backend failures also surface via toasts
 *   (user-visible) and logs where appropriate (developer-visible).
 * - When sign-up fails with ERROR_EMAIL_ALREADY_IN_USE, a sign-in attempt is made using the provided credentials.
 *   If sign-in fails, the likely cause is an incorrect password; this is communicated to the user.
 *
 * Security considerations:
 * - The hard-coded allowlist is a simple gate and assumes the APK is not widely distributed or is complemented
 *   by backend rules preventing non-admin users from escalating privileges.
 * - Firestore security rules should ensure that only the authenticated admin can create/modify the admin profile
 *   or perform privileged actions elsewhere in the app.
 *
 * Threading / lifecycle:
 * - All Firebase callbacks run on the main thread by default (Android Task API). Navigation occurs only after
 *   profile persistence succeeds (or after successful sign-in). finish() is called to prevent back navigation.
 *
 * Future work:
 * - Replace hard-coded constants with Remote Config or a secure backend bootstrap endpoint.
 * - Add explicit error codes/UX for common Firebase exceptions (weak-password, network, rate limit).
 * - Replace toasts with a consistent error surface (e.g., inline error banners or Material dialogs).
 */

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity;
import com.example.seg2105_project_1_tutor_registration_form.data.FirebaseRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.RegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.HashMap;
import java.util.Map;

public class AdminRegistrationActivity extends AppCompatActivity {

    // Hard allowlist: exactly one admin identity. Keep email and invite code in sync when rotating.
    private static final String ADMIN_EMAIL       = "admin@otams22.com";
    private static final String ADMIN_INVITE_CODE = "SEG2105-ADMIN-ONLY";

    private TextInputEditText etName, etEmail, etPassword, etInvite;
    private final FirebaseRepository repo = new FirebaseRepository();

    // Provides access to registration requests (pending/approve/reject). Used by loadPending/approve/reject.
    private final RegistrationRepository adminRepo = new FirestoreRegistrationRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        // View binding (manual). All inputs are later read via a null-safe helper (text()).
        etName     = findViewById(R.id.etName);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etInvite   = findViewById(R.id.etInvite);

        // Primary action: attempt to create (or sign in) the admin with the given credentials.
        findViewById(R.id.btnCreateAdmin).setOnClickListener(v -> tryCreateAdmin());

        // Optional diagnostic: fetch pending requests for visibility during admin setup.
        // loadPending();
    }

    private void tryCreateAdmin() {
        String name     = text(etName);
        String email    = text(etEmail);
        String password = text(etPassword);
        String invite   = text(etInvite);

        // Input validation — fail fast to reduce backend calls and improve UX.
        if (!email.equalsIgnoreCase(ADMIN_EMAIL)) {
            toast("Only the designated admin email can be registered.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email.");
            return;
        }
        if (password.length() < 6) {
            // Keep this in sync with Firebase min password requirements if changed.
            toast("Password must be at least 6 characters.");
            return;
        }
        // Case-insensitive comparison to tolerate minor user input variance.
        if (!invite.equalsIgnoreCase(ADMIN_INVITE_CODE)) {
            toast("Invalid invite code.");
            return;
        }

        // Primary path: create the admin in Firebase Auth. On success, persist the role-bearing profile.
        repo.signUp(email, password).addOnSuccessListener(authResult -> {
            String uid = repo.uid();

            // Minimal profile data required for downstream role checks and display.
            Map<String, Object> profile = new HashMap<>();
            profile.put("role", "admin");
            profile.put("email", email);
            profile.put("name", name);

            // Persist profile before navigation to ensure role is available to next screen/flows.
            repo.saveUserProfile(uid, profile).addOnSuccessListener(x -> {
                toast("Admin created.");

                // Navigation contract: pass role/email/name as extras for downstream context.
                Intent i = new Intent(this, WelcomeActivity.class);
                i.putExtra("role", "Administrator");
                i.putExtra("email", email);
                i.putExtra("name", name);
                startActivity(i);
                finish();

            }).addOnFailureListener(e ->
                    // Profile write failed; surface to user. Log details if needed at call-site.
                    toast("Failed to save profile: " + e.getMessage()));

        }).addOnFailureListener(e -> {
            // Recovery path: if the email already exists, attempt sign-in to avoid blocking setup.
            if (e instanceof FirebaseAuthException &&
                    "ERROR_EMAIL_ALREADY_IN_USE".equals(((FirebaseAuthException) e).getErrorCode())) {

                repo.signIn(email, password).addOnSuccessListener(r -> {
                    String uid = repo.uid();

                    // Ensure a consistent profile is present. This call is non-blocking for navigation.
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("role", "admin");
                    profile.put("email", email);
                    profile.put("name", name);
                    repo.saveUserProfile(uid, profile);

                    Intent i = new Intent(this, WelcomeActivity.class);
                    i.putExtra("role", "Administrator");
                    i.putExtra("email", email);
                    i.putExtra("name", name);
                    startActivity(i);
                    finish();
                }).addOnFailureListener(err ->
                        // Sign-in failed; most common cause is wrong password for an existing admin.
                        toast("Admin already exists. Wrong password."));
            } else {
                // Non-recoverable sign-up error (e.g., network, invalid credentials, throttling).
                toast("Sign up failed: " + e.getMessage());
            }
        });
    }

    // Defensive read of input text fields. Trims whitespace; returns empty string when null.
    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    // Centralized toast helper to ensure consistent display duration and context use.
    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /**
     * Diagnostics utility: lists PENDING registration requests to logs.
     * This method is non-UI; intended for verification during admin setup or debugging.
     * Hook it to a UI control or ViewModel in the future if you expose an inbox screen.
     */
    private void loadPending() {
        adminRepo.listByStatus(RequestStatus.PENDING)
                .addOnSuccessListener(list -> {
                    for (RegRequest r : list) {
                        Log.d(
                                "AdminInbox",
                                "Pending: " + r.getFirstName() + " " + r.getLastName()
                                        + " (" + r.getRole() + ") id=" + r.getId()
                        );
                    }
                    // TODO: Bind to RecyclerView adapter once an Admin Inbox UI is implemented.
                })
                .addOnFailureListener(e -> Log.e("AdminInbox", "Failed to load pending", e));
    }

    /**
     * Approves a pending registration request.
     * Preconditions:
     * - The caller must supply a valid requestId and the authenticated admin's UID.
     * - Firestore security rules should validate that adminUid is authorized to approve.
     */
    private void approveRequest(String requestId, String adminUid) {
        adminRepo.approve(requestId, adminUid)
                .addOnSuccessListener(v -> Log.d("AdminInbox", "Approved: " + requestId))
                .addOnFailureListener(e -> Log.e("AdminInbox", "Approve failed", e));
    }

    /**
     * Rejects a pending registration request with a reason for auditability and user feedback.
     * The reason should be concise and user-readable (e.g., missing documents, eligibility, etc.).
     */
    private void rejectRequest(String requestId, String adminUid, String reason) {
        adminRepo.reject(requestId, adminUid, reason)
                .addOnSuccessListener(v -> Log.d("AdminInbox", "Rejected: " + requestId))
                .addOnFailureListener(e -> Log.e("AdminInbox", "Reject failed", e));
    }
}

/*
 * End of file.
 *
 * Operational checklist if issues occur:
 * 1) Confirm ADMIN_EMAIL/ADMIN_INVITE_CODE values match expected deployment configuration.
 * 2) Verify network connectivity and Firebase project configuration (google-services.json, SHA keys).
 * 3) Check Firebase Auth sign-up/sign-in logs and Firestore security rules for permission denials.
 * 4) Clear app data between attempts to avoid stale auth state when changing credentials.
 * 5) Ensure downstream screens (WelcomeActivity) correctly handle "role=Administrator" for routing.
 */
