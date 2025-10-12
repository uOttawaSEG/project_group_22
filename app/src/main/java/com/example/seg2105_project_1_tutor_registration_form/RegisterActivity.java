package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etPhone, etDegree;
    private CheckBox cbMath, cbScience, cbEnglish, cbHistory, cbComputerScience;
    private Button btnRegister;
    private TextView tvError;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseHelper = new DatabaseHelper(this);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etDegree = findViewById(R.id.etDegree);

        cbMath = findViewById(R.id.cbMath);
        cbScience = findViewById(R.id.cbScience);
        cbEnglish = findViewById(R.id.cbEnglish);
        cbHistory = findViewById(R.id.cbHistory);
        cbComputerScience = findViewById(R.id.cbComputerScience);

        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String degree = etDegree.getText().toString().trim();
        List<String> courses = getSelectedCourses();

        if (!validateRegistrationInput(firstName, lastName, email, password, phone, degree, courses)) {
            return;
        }

        if (databaseHelper.emailExists(email)) {
            showError("Email already registered");
            return;
        }

        Tutor tutor = new Tutor(firstName, lastName, email, password, phone, degree, courses);
        boolean success = databaseHelper.addTutor(tutor);

        if (success) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    private List<String> getSelectedCourses() {
        List<String> courses = new ArrayList<>();
        if (cbMath.isChecked()) courses.add("Mathematics");
        if (cbScience.isChecked()) courses.add("Science");
        if (cbEnglish.isChecked()) courses.add("English");
        if (cbHistory.isChecked()) courses.add("History");
        if (cbComputerScience.isChecked()) courses.add("Computer Science");
        return courses;
    }

    private boolean validateRegistrationInput(String firstName, String lastName, String email,
                                              String password, String phone, String degree,
                                              List<String> courses) {
        if (!Validator.isValidName(firstName)) {
            showError("Please enter a valid first name (min 2 characters)");
            return false;
        }

        if (!Validator.isValidName(lastName)) {
            showError("Please enter a valid last name (min 2 characters)");
            return false;
        }

        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return false;
        }

        if (!Validator.isValidPassword(password)) {
            showError("Password must be at least 6 characters");
            return false;
        }

        if (!Validator.isValidPhone(phone)) {
            showError("Please enter a valid phone number (min 10 digits)");
            return false;
        }

        if (!Validator.isValidDegree(degree)) {
            showError("Please enter your highest degree");
            return false;
        }

        if (!Validator.hasSelectedCourses(courses)) {
            showError("Please select at least one course");
            return false;
        }

        showError("");
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

    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}