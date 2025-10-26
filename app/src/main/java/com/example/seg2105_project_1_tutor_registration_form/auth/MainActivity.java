package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
     *  - Pass profile fields or route to Admin home
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

                                // Route admins to AdminHomeActivity; everyone else → WelcomeActivity
                                String role = doc.getString("role");
                                if (role != null && (
                                        role.equalsIgnoreCase("admin") ||
                                                role.equalsIgnoreCase("ADMIN") ||
                                                role.equalsIgnoreCase("Administrator"))) {
                                    startActivity(new Intent(
                                            this,
                                            com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity.class
                                    ));
                                    finish();
                                    return;
                                }

                                Intent i = new Intent(
                                        this,
                                        com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity.class
                                );

                                // Core fields
                                i.putExtra("role",      role);
                                i.putExtra("firstName", doc.getString("firstName"));
                                i.putExtra("lastName",  doc.getString("lastName"));
                                i.putExtra("email",     doc.getString("email"));
                                i.putExtra("phone",     doc.getString("phone"));

                                // Student-specific
                                i.putExtra("program",   doc.getString("program"));
                                i.putExtra("studyYear", doc.getString("studyYear"));
                                java.util.ArrayList<String> wanted = new java.util.ArrayList<>();
                                Object w = doc.get("coursesWanted");
                                if (w instanceof java.util.List<?>) {
                                    for (Object o : (java.util.List<?>) w) {
                                        if (o != null) wanted.add(String.valueOf(o));
                                    }
                                }
                                i.putStringArrayListExtra("coursesWanted", wanted);

                                // Tutor-specific
                                i.putExtra("degree", doc.getString("degree"));
                                java.util.ArrayList<String> offered = new java.util.ArrayList<>();
                                Object o = doc.get("coursesOffered");
                                if (o instanceof java.util.List<?>) {
                                    for (Object ob : (java.util.List<?>) o) {
                                        if (ob != null) offered.add(String.valueOf(ob));
                                    }
                                }
                                i.putStringArrayListExtra("coursesOffered", offered);

                                currentUserRole = role;
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Could not load your profile.", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }
}
