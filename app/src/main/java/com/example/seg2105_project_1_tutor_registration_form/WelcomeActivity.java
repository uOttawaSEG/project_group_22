package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class WelcomeActivity extends AppCompatActivity {
    private TextView tvWelcome, tvUserInfo;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout  = findViewById(R.id.btnLogout);

        // Retrieve the Tutor object safely (API 33+ overload)
        Tutor tutor;
        if (Build.VERSION.SDK_INT >= 33) {
            tutor = getIntent().getSerializableExtra("tutor", Tutor.class);
        } else {
            tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        }

        if (tutor != null) {
            tvWelcome.setText("Welcome! You are logged in as Tutor");

            String first  = nz(tutor.getFirstName());
            String last   = nz(tutor.getLastName());
            String email  = nz(tutor.getEmail());
            String phone  = nz(tutor.getPhone());
            String degree = nz(tutor.getDegree());

            List<String> list = tutor.getCourses();
            String courses = (list == null || list.isEmpty()) ? "—" : TextUtils.join(", ", list);

            String info =
                    "Name: " + first + " " + last + "\n" +
                            "Email: " + email + "\n" +
                            "Phone: " + phone + "\n" +
                            "Highest Degree: " + degree + "\n" +
                            "Courses Offered: " + courses;

            tvUserInfo.setText(info);
        } else {
            // Fallback if nothing was passed (prevents crash)
            tvWelcome.setText("Welcome!");
            tvUserInfo.setText("We couldn’t load your tutor info.");
        }

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private static String nz(String s) { return s == null ? "—" : s; }
}
