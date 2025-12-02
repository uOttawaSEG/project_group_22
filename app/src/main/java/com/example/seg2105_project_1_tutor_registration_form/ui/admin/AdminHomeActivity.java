package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeActivity extends AppCompatActivity {

    // badge counters (optional if you want to use them elsewhere)
    private int countPending = 0, countApproved = 0, countRejected = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // Inbox → Pending list (AdminInboxActivity defaults to PENDING)
        findViewById(R.id.cardInbox).setOnClickListener(v ->
                startActivity(new Intent(AdminHomeActivity.this, AdminInboxActivity.class)));

        // Approved / Rejected (reuse Inbox screen with filter)
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

    @Override
    protected void onResume() {
        super.onResume();
        updateBadges(); // refresh counts whenever screen returns
    }

    private void openStatusScreen(String status) {
        Intent i = new Intent(AdminHomeActivity.this, AdminInboxActivity.class);
        i.putExtra("filter_status", status);
        startActivity(i);
    }

    // --- badge counts (no more debug dump) ---
    private void updateBadges() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        countPending = countApproved = countRejected = 0;

        db.collection("registrationRequests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(s -> {
                    countPending = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeInbox, countPending);
                })
                .addOnFailureListener(e ->
                        setBadgeText(R.id.badgeInbox, 0));

        db.collection("registrationRequests")
                .whereEqualTo("status", "APPROVED")
                .get()
                .addOnSuccessListener(s -> {
                    countApproved = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeApproved, countApproved);
                })
                .addOnFailureListener(e ->
                        setBadgeText(R.id.badgeApproved, 0));

        db.collection("registrationRequests")
                .whereEqualTo("status", "REJECTED")
                .get()
                .addOnSuccessListener(s -> {
                    countRejected = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeRejected, countRejected);
                })
                .addOnFailureListener(e ->
                        setBadgeText(R.id.badgeRejected, 0));
    }

    private void setBadgeText(int viewId, int count) {
        android.widget.TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(String.valueOf(count));
    }
}
