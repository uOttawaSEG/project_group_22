package com.example.seg2105_project_1_tutor_registration_form.auth;

/*
 Login & routing entrypoint. Signs users in with Firebase Auth, loads /users/{uid},
 then checks /registrationRequests to gate access: REJECTED → RejectedScreen,
 PENDING/unknown → toast only, APPROVED → routes by role (Admin → AdminHome,
 Tutor → TutorHome, Student/other → Welcome). Also exposes register links per role
 and a simple password-reset dialog.
*/

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

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_STUDENT = "Student";
    private static final String ROLE_TUTOR   = "Tutor";

    private RadioGroup roleRadioGroup;
    private EditText   usernameEditText;
    private EditText   passwordEditText;
    private Button     loginButton;
    private TextView   tvRegister;
    private TextView   tvForgot;
    private AccountManager accountManager;

    private String currentUserRole;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        roleRadioGroup   = findViewById(R.id.radio_group_role);
        usernameEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        loginButton      = findViewById(R.id.btnLogin);
        tvRegister       = findViewById(R.id.tvRegister);
        tvForgot         = findViewById(R.id.tvForgot);
        accountManager   = new AccountManager(this);

        loginButton.setOnClickListener(v -> handleLogin());

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
        final TextInputEditText etNew   = view.findViewById(R.id.etResetNewPassword);

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
                                String role = doc.getString("role");
                                gateByRequestStatusAndRoute(uid, role);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Could not load your profile.", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    private void gateByRequestStatusAndRoute(String uid, String roleFromProfile) {
        if (roleFromProfile != null &&
                (roleFromProfile.equalsIgnoreCase("admin") ||
                        roleFromProfile.equalsIgnoreCase("administrator"))) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity.class));
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("registrationRequests").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        handleGateDecision(doc.getString("status"), doc.getString("reason"), roleFromProfile);
                    } else {
                        db.collection("registrationRequests")
                                .whereEqualTo("userUid", uid)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        handleGateDecision(d.getString("status"), d.getString("reason"), roleFromProfile);
                                    } else {
                                        // No registrationRequests doc found → treat as pending and show Pending screen
                                        Intent i = new Intent(this, PendingApprovalActivity.class);
                                        startActivity(i);
                                        finish();
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
        // REJECTED → RejectedScreen
        if ("REJECTED".equalsIgnoreCase(status)) {
            Intent r = new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.auth.RejectedScreen.class);
            if (reason != null && !reason.isEmpty()) {
                r.putExtra("EXTRA_REASON", reason);
            }
            startActivity(r);
            finish();
            return;
        }

        // Anything not APPROVED (including null / PENDING) → PendingApprovalActivity
        if (!"APPROVED".equalsIgnoreCase(status)) {
            Intent i = new Intent(this, PendingApprovalActivity.class);
            startActivity(i);
            finish();
            return;
        }

        // APPROVED → route by role
        if (roleFromProfile != null &&
                (roleFromProfile.equalsIgnoreCase("admin") ||
                        roleFromProfile.equalsIgnoreCase("administrator"))) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity.class));
        } else if ("tutor".equalsIgnoreCase(roleFromProfile)) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.tutor.TutorHomeActivity.class));
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
