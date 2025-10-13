package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity;
import com.example.seg2105_project_1_tutor_registration_form.data.FirebaseRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.HashMap;
import java.util.Map;

public class AdminRegistrationActivity extends AppCompatActivity {

    // Allow exactly one admin email + one invite code
    private static final String ADMIN_EMAIL       = "admin@otams22.com";
    private static final String ADMIN_INVITE_CODE = "SEG2105-ADMIN-ONLY";

    private TextInputEditText etName, etEmail, etPassword, etInvite;
    private final FirebaseRepository repo = new FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        etName     = findViewById(R.id.etName);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etInvite   = findViewById(R.id.etInvite);

        findViewById(R.id.btnCreateAdmin).setOnClickListener(v -> tryCreateAdmin());
    }

    private void tryCreateAdmin() {
        String name     = text(etName);
        String email    = text(etEmail);
        String password = text(etPassword);
        String invite   = text(etInvite);

        // Validate fixed credential + format
        if (!email.equalsIgnoreCase(ADMIN_EMAIL)) {
            toast("Only the designated admin email can be registered.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email.");
            return;
        }
        if (password.length() < 6) {
            toast("Password must be at least 6 characters.");
            return;
        }
        if (!TextUtils.equals(invite, ADMIN_INVITE_CODE)) {
            toast("Invalid invite code.");
            return;
        }

        // Create account in Firebase Auth
        repo.signUp(email, password).addOnSuccessListener(authResult -> {
            String uid = repo.uid();

            // Save admin profile to Firestore
            Map<String, Object> profile = new HashMap<>();
            profile.put("role", "admin");
            profile.put("email", email);
            profile.put("name", name);

            repo.saveUserProfile(uid, profile).addOnSuccessListener(x -> {
                toast("Admin created.");

                // Go to Welcome and show role
                Intent i = new Intent(this, WelcomeActivity.class);
                i.putExtra("role", "Administrator");
                i.putExtra("email", email);
                i.putExtra("name", name);
                startActivity(i);
                finish();

            }).addOnFailureListener(e ->
                    toast("Failed to save profile: " + e.getMessage()));

        }).addOnFailureListener(e -> {
            // If admin already exists, allow sign-in instead of failing hard
            if (e instanceof FirebaseAuthException &&
                    "ERROR_EMAIL_ALREADY_IN_USE".equals(((FirebaseAuthException) e).getErrorCode())) {
                // Try sign in with the provided password
                repo.signIn(email, password).addOnSuccessListener(r -> {
                    String uid = repo.uid();
                    // (Optional) ensure profile exists; otherwise write it
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
                        toast("Admin already exists. Wrong password."));
            } else {
                toast("Sign up failed: " + e.getMessage());
            }
        });
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
