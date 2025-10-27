package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RejectedScreen extends AppCompatActivity {

    public static final String EXTRA_REASON = "reason";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rejected_screen);

        TextView tvReason        = findViewById(R.id.tvReason);
        Button   btnBack         = findViewById(R.id.btnBackToLogin);

        // Fill UI
        String reason = getIntent().getStringExtra(EXTRA_REASON);
        String msg = (reason != null && !reason.trim().isEmpty())
                ? "Registration rejected: " + reason.trim()
                : "Your registration was rejected. Please contact support for more information.";
        tvReason.setText(msg);

        // Button: sign out and return to MainActivity
        btnBack.setOnClickListener(v -> {
            try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finishAffinity();
        });
    }
}