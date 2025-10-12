package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvError;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvError = findViewById(R.id.tvError);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateLoginInput(email, password)) {
            return;
        }

        Tutor tutor = databaseHelper.getTutor(email, password);
        if (tutor != null) {
            // Login successful
            showError("");
            navigateToWelcome(tutor);
        } else {
            showError("Invalid email or password");
        }
    }

    private boolean validateLoginInput(String email, String password) {
        if (email.isEmpty()) {
            showError("Please enter your email");
            return false;
        }

        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return false;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return false;
        }

        if (!Validator.isValidPassword(password)) {
            showError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        if (message.isEmpty()) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void navigateToWelcome(Tutor tutor) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra("tutor", tutor);
        startActivity(intent);
        finish();
    }
}