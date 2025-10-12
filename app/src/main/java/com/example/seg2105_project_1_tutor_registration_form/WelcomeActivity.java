package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    private TextView tvWelcome, tvUserInfo;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initializeViews();
        displayUserInfo();
        setupClickListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void displayUserInfo() {
        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        if (tutor != null) {
            tvWelcome.setText("Welcome! You are logged in as Tutor");

            StringBuilder userInfo = new StringBuilder();
            userInfo.append("Name: ").append(tutor.getFirstName()).append(" ").append(tutor.getLastName()).append("\n");
            userInfo.append("Email: ").append(tutor.getEmail()).append("\n");
            userInfo.append("Phone: ").append(tutor.getPhone()).append("\n");
            // changed these two lines to match typical Tutor getters
            userInfo.append("Highest Degree: ").append(tutor.getDegree()).append("\n");
            userInfo.append("Courses Offered: ").append(TextUtils.join(", ", tutor.getCourses()));

            tvUserInfo.setText(userInfo.toString());
        }
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
