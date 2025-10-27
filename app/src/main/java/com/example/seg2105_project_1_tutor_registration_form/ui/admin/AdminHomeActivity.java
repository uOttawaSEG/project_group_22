package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // Inbox → Pending list
        findViewById(R.id.cardInbox).setOnClickListener(v ->
                startActivity(new Intent(AdminHomeActivity.this, AdminInboxActivity.class)));

        // Approved / Rejected (reuse Inbox screen with filter for now)
        findViewById(R.id.cardApproved).setOnClickListener(v ->
                openStatusScreen("APPROVED"));

        findViewById(R.id.cardRejected).setOnClickListener(v ->
                openStatusScreen("REJECTED"));

        // Log out → back to login/landing
        findViewById(R.id.cardLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(AdminHomeActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void openStatusScreen(String status) {
        // Reuse the inbox UI; AdminInboxActivity can read this and filter
        Intent i = new Intent(AdminHomeActivity.this, AdminInboxActivity.class);
        i.putExtra("filter_status", status);
        startActivity(i);
    }
}
