package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicInteger;

public class AdminHomeActivity extends AppCompatActivity {

    // badge counters
    private final AtomicInteger remaining = new AtomicInteger(0);
    private int countPending = 0, countApproved = 0, countRejected = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // 🔎 Sanity toast: which Firebase project & which admin user are we running as?
        String pid = FirebaseApp.getInstance().getOptions().getProjectId();
        FirebaseUser au = FirebaseAuth.getInstance().getCurrentUser();
        android.widget.Toast.makeText(
                this,
                "Project=" + pid + "  adminUid=" + (au == null ? "null" : au.getUid()),
                android.widget.Toast.LENGTH_LONG
        ).show();

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

    // --- badge counts + diagnostics ---
    private void updateBadges() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // reset counters and a simple barrier so we know when all three queries finish
        countPending = countApproved = countRejected = 0;
        remaining.set(3);

        db.collection("registrationRequests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(s -> {
                    countPending = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeInbox, countPending);
                    maybeRunDebugDump();
                })
                .addOnFailureListener(e -> {
                    setBadgeText(R.id.badgeInbox, 0);
                    maybeRunDebugDump();
                });

        db.collection("registrationRequests")
                .whereEqualTo("status", "APPROVED")
                .get()
                .addOnSuccessListener(s -> {
                    countApproved = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeApproved, countApproved);
                    maybeRunDebugDump();
                })
                .addOnFailureListener(e -> {
                    setBadgeText(R.id.badgeApproved, 0);
                    maybeRunDebugDump();
                });

        db.collection("registrationRequests")
                .whereEqualTo("status", "REJECTED")
                .get()
                .addOnSuccessListener(s -> {
                    countRejected = (s == null) ? 0 : s.size();
                    setBadgeText(R.id.badgeRejected, countRejected);
                    maybeRunDebugDump();
                })
                .addOnFailureListener(e -> {
                    setBadgeText(R.id.badgeRejected, 0);
                    maybeRunDebugDump();
                });
    }

    private void maybeRunDebugDump() {
        if (remaining.decrementAndGet() != 0) return;

        // If Pending count is zero, dump what's actually in the collection to diagnose.
        if (countPending == 0) {
            FirebaseFirestore.getInstance()
                    .collection("registrationRequests")
                    .get()
                    .addOnSuccessListener(all -> {
                        int total = (all == null) ? 0 : all.size();
                        int p = 0, a = 0, r = 0, other = 0;
                        StringBuilder ids = new StringBuilder();
                        if (all != null) {
                            for (DocumentSnapshot d : all) {
                                String s = String.valueOf(d.get("status"));
                                if ("PENDING".equalsIgnoreCase(s)) p++;
                                else if ("APPROVED".equalsIgnoreCase(s)) a++;
                                else if ("REJECTED".equalsIgnoreCase(s)) r++;
                                else other++;
                                if (ids.length() < 180) {
                                    ids.append(d.getId()).append('[').append(s).append("] ");
                                }
                            }
                        }
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "registrationRequests total=" + total +
                                        "  P=" + p + " A=" + a + " R=" + r + " other=" + other +
                                        (ids.length() > 0 ? ("  ids: " + ids) : ""),
                                Snackbar.LENGTH_LONG
                        ).show();
                    })
                    .addOnFailureListener(e ->
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Debug dump failed: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).show());
        }
    }

    private void setBadgeText(int viewId, int count) {
        android.widget.TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(String.valueOf(count));
    }
}
