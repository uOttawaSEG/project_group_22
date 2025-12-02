package com.example.seg2105_project_1_tutor_registration_form.auth;


import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;

/*
 * PendingAccountActivity
 * ----------------------
 * Shown after a user (student or tutor) registers,
 * or logs in while their account is still waiting
 * for admin approval.
 *
 * StudentRegistrationActivity now redirects here on success,
 * so students won't go straight into the app anymore.
 */
public class PendingAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_account); // your cute yellow XML

        // Optional: tweak message based on role (STUDENT/TUTOR)
        String role = getIntent().getStringExtra("role"); // might be "STUDENT"

        TextView tvMessage = findViewById(R.id.tvPendingMessage);
        if (tvMessage != null && role != null) {
            if ("STUDENT".equalsIgnoreCase(role)) {
                tvMessage.setText(
                        "Your student account has been submitted and is waiting for an admin to review it. " +
                                "Once you are approved, you’ll be able to log in and book tutoring sessions."
                );
            } else if ("TUTOR".equalsIgnoreCase(role)) {
                tvMessage.setText(
                        "Your tutor profile has been submitted and is waiting for an admin to review it. " +
                                "Once you are approved, you’ll be able to log in and manage your sessions."
                );
            }
        }

        // If you want to be extra strict, you *could* sign them out here:
        // FirebaseAuth.getInstance().signOut();
    }
}

