package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * MainActivity (Login & role-based entry point) — human-readable overview
 * ─────────────────────────────────────────────────────────────────────────────
 * What this screen does
 * • Lets a user pick a role (Student / Tutor / Admin), enter email + password,
 *   and log in using Firebase Auth.
 * • After login, it fetches the user profile from /users/{uid} in Firestore and
 *   routes the user *only* through a registration “gate” that checks the
 *   /registrationRequests status (APPROVED / PENDING / REJECTED).
 *
 * Key UI pieces
 * • RadioGroup for role selection (Student / Tutor / Admin)
 * • Email & Password inputs
 * • Login button
 * • “Register” link (navigates to the right registration screen based on role)
 * • “Forgot password?” link (opens dialog, sends Firebase reset email)
 *
 * Why the “gate” matters
 * • Even if Auth succeeds, users shouldn’t proceed until an Admin has approved
 *   their registration. The gate queries /registrationRequests and:
 *   - REJECTED → navigates to RejectedScreen (shows reason if available)
 *   - not APPROVED → shows “pending” toast (no navigation)
 *   - APPROVED → routes to AdminHome or WelcomeActivity
 *
 * Failure modes to expect (and how the UI responds)
 * • Bad email/password → toast “Invalid email/password” and clears password.
 * • Profile doc missing → toast “Profile not found.”
 * • Request status lookup fails → toast “Could not verify request status.”
 * • Request not found → toast “Your registration is pending admin approval.”
 *
 * Security / trust notes
 * • Client-side checks are convenience only; make sure Firestore rules protect
 *   which role can access which path, and that Admin approval is enforced
 *   server-side by security rules or backend logic where possible.
 *
 * Maintenance tips
 * • If you rename roles, update both the RadioButton labels and the string
 *   checks here (e.g., "Student", "Tutor", "Admin/Administrator").
 * • Keep the “gate” routing logic in one place to avoid bypasses.
 * • Localize toast strings if adding i18n.
 */
public class MainActivity extends AppCompatActivity {

    private static final String ROLE_STUDENT = "Student";
    private static final String ROLE_TUTOR   = "Tutor";

    private RadioGroup roleRadioGroup;
    private EditText   usernameEditText;
    private EditText   passwordEditText;
    private Button     loginButton;
    private TextView   tvRegister;   // "Don't have an account? Register here"
    private TextView   tvForgot;     // "Forgot password?"
    private AccountManager accountManager; // kept for other flows (e.g., registration screens)

    private String currentUserRole;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        roleRadioGroup   = findViewById(R.id.radio_group_role);
        usernameEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        loginButton      = findViewById(R.id.btnLogin);
        tvRegister       = findViewById(R.id.tvRegister);
        tvForgot         = findViewById(R.id.tvForgot);
        accountManager   = new AccountManager(this);

        // Login
        loginButton.setOnClickListener(v -> handleLogin());

        // “Don’t have an account? Register here”
        tvRegister.setOnClickListener(v -> {
            int checkedId = roleRadioGroup.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(this, "Please select a role first.", Toast.LENGTH_SHORT).show();
                return;
            }

            String role = ((RadioButton) findViewById(checkedId)).getText().toString().trim();

            if (ROLE_STUDENT.equalsIgnoreCase(role)) {
                startActivity(new Intent(this, StudentRegistrationActivity.class));
            } else if (ROLE_TUTOR.equalsIgnoreCase(role)) {
                startActivity(new Intent(this, TutorRegisterActivity.class));
            } else if ("Admin".equalsIgnoreCase(role) || "Administrator".equalsIgnoreCase(role)) {
                startActivity(new Intent(this, AdminRegistrationActivity.class));
            } else {
                Toast.makeText(this, "Unknown role selected.", Toast.LENGTH_SHORT).show();
            }
        });

        // Forgot password (Firebase email reset)
        tvForgot.setOnClickListener(v -> showResetDialog());
    }

    private void showResetDialog() {
        int selected = roleRadioGroup.getCheckedRadioButtonId();
        if (selected == -1) {
            Toast.makeText(this, "Select your role first.", Toast.LENGTH_SHORT).show();
            return;
        }

        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        final TextInputEditText etEmail = view.findViewById(R.id.etResetEmail);
        final TextInputEditText etNew   = view.findViewById(R.id.etResetNewPassword); // kept in UI; Firebase link sets the new password

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset password")
                .setView(view)
                .setPositiveButton("Reset", (d, w) -> {
                    String email = text(etEmail);
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Enter your email.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    /**
     * Firebase login:
     *  - Sign in with Auth
     *  - Fetch /users/{uid} from Firestore
     *  - Route ONLY through the registration gate
     */
    private void handleLogin() {
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = usernameEditText.getText().toString().trim().toLowerCase();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) { Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show(); return; }
        if (password.isEmpty()) { Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show(); return; }

        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);

                    if (!task.isSuccessful() || mAuth.getCurrentUser() == null) {
                        Toast.makeText(this, "Invalid email/password", Toast.LENGTH_SHORT).show();
                        passwordEditText.setText("");
                        currentUserRole = null;
                        return;
                    }

                    String uid = mAuth.getCurrentUser().getUid();

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc == null || !doc.exists()) {
                                    Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // ⬇️ Do NOT navigate here anymore.
                                // ⬇️ Only call the gate to decide (Rejected / Pending / Approved).
                                String role = doc.getString("role");
                                gateByRequestStatusAndRoute(uid, role);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Could not load your profile.", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    // ---------- Registration gate (with resilient fallback) ----------
    private void gateByRequestStatusAndRoute(String uid, String roleFromProfile) {
        // ─────────────────────────────────────────────────────────────
        // Bypass the registration gate entirely for Admin accounts
        // ─────────────────────────────────────────────────────────────
        if (roleFromProfile != null &&
                (roleFromProfile.equalsIgnoreCase("admin") ||
                        roleFromProfile.equalsIgnoreCase("administrator"))) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity.class));
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fast path: /registrationRequests/{uid}
        db.collection("registrationRequests").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        handleGateDecision(doc.getString("status"), doc.getString("reason"), roleFromProfile);
                    } else {
                        // Fallback: some rows may not use UID as document id
                        db.collection("registrationRequests")
                                .whereEqualTo("userUid", uid)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        handleGateDecision(d.getString("status"), d.getString("reason"), roleFromProfile);
                                    } else {
                                        Toast.makeText(this,
                                                "Your registration is pending admin approval.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Could not verify request status.",
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not verify request status.", Toast.LENGTH_SHORT).show());
    }

    private void handleGateDecision(String status, String reason, String roleFromProfile) {
        if ("REJECTED".equalsIgnoreCase(status)) {
            Intent r = new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.auth.RejectedScreen.class);
            if (reason != null && !reason.isEmpty()) r.putExtra("EXTRA_REASON", reason);
            startActivity(r);
            finish();
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Your registration is pending admin approval.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Approved → route normally
        if (roleFromProfile != null &&
                (roleFromProfile.equalsIgnoreCase("admin") ||
                        roleFromProfile.equalsIgnoreCase("administrator"))) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity.class));
        } else {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity.class));
        }
        finish();
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of MainActivity — TL;DR !
 * ─────────────────────────────────────────────────────────────────────────────
 * • Auth first, then Firestore profile, then the “registration gate.”
 * • Gate decides: REJECTED → RejectedScreen; PENDING/unknown → toast; APPROVED → route.
 * • Register link sends users to the correct registration activity based on selected role.
 * • Forgot password triggers Firebase email flow via dialog.
 *
 * Quick test ideas
 * • Wrong password → should show toast and clear password field.
 * • Missing profile doc → should show “Profile not found.”
 * • No request row → should show “pending admin approval.”
 * • Status changes in Firestore → verify each branch routes correctly.
 *
 * Notes
 * • Keep role string comparisons in sync with UI labels and server-side rules.
 * • If adding MFA or email verification, extend handleLogin() before hitting the gate.
 */
