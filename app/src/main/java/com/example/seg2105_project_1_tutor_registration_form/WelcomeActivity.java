package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private TextView tvUserInfo;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout  = findViewById(R.id.btnLogout);

        // Read what we know about the user
        Intent i = getIntent();
        String role   = n(i.getStringExtra("role"));
        String email  = n(i.getStringExtra("email"));
        String name   = n(i.getStringExtra("name"));     // optional
        String phone  = n(i.getStringExtra("phone"));    // optional
        String degree = n(i.getStringExtra("degreeCsv")); // optional CSV
        String courses= n(i.getStringExtra("coursesCsv")); // optional CSV

        StringBuilder sb = new StringBuilder();
        if (!name.isEmpty())   sb.append("Name: ").append(name).append("\n");
        if (!role.isEmpty())   sb.append("Role: ").append(role).append("\n");
        if (!email.isEmpty())  sb.append("Email: ").append(email).append("\n");
        if (!phone.isEmpty())  sb.append("Phone: ").append(phone).append("\n");
        if (!degree.isEmpty()) sb.append("Degree(s): ").append(degree).append("\n");
        if (!courses.isEmpty())sb.append("Courses: ").append(courses).append("\n");

        if (sb.length()==0) {
            sb.append("You're signed in. We couldn't find more details to show.");
        }
        tvUserInfo.setText(sb.toString().trim());

        btnLogout.setOnClickListener(v -> {
            // simple logout: go back to Main
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity.class));
            finish();
        });
    }

    private String n(String s) { return s == null ? "" : s.trim(); }
}
