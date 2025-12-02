package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.RegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;   // ✅ NEW

public class RequestDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REQ = "extra_req";   // pass the whole RegRequest (Serializable)

    private RegistrationRepository repo;

    private TextView name, role, email, phone, extra1, extra2;
    private RegRequest req; // what we're showing

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        repo = new FirestoreRegistrationRepository();

        // Get the request object that was passed in
        Object o = getIntent().getSerializableExtra(EXTRA_REQ);
        if (!(o instanceof RegRequest)) {
            Snackbar.make(findViewById(android.R.id.content), "Missing request", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }
        req = (RegRequest) o;

        name  = findViewById(R.id.name);
        role  = findViewById(R.id.role);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        extra1 = findViewById(R.id.extra1);
        extra2 = findViewById(R.id.extra2);

        bind(req);

        findViewById(R.id.btnApprove).setOnClickListener(v -> approve());
        findViewById(R.id.btnReject).setOnClickListener(v -> showRejectDialog());
    }

    private void bind(RegRequest r) {
        name.setText(r.fullName());

        // role is a String in your model
        String roleStr = r.getRole();
        role.setText("Role: " + (roleStr == null ? "—" : roleStr));

        email.setText("Email: " + nn(r.getEmail()));
        phone.setText("Phone: " + nn(r.getPhone()));

        // Show the most relevant extra info per role (compare strings case-insensitively)
        if (roleStr != null && roleStr.equalsIgnoreCase("STUDENT")) {
            extra1.setText("Looking for: " + nn(r.getCoursesWantedSummary()));
            extra2.setText("");
        } else if (roleStr != null && roleStr.equalsIgnoreCase("TUTOR")) {
            extra1.setText("Teaches: " + nn(r.getCoursesOfferedSummary()));
            extra2.setText("");
        } else {
            extra1.setText("");
            extra2.setText("");
        }
    }

    private void approve() {
        String adminUid = getAdminUid();
        if (adminUid == null) {
            Snackbar.make(findViewById(android.R.id.content), "Not signed in as admin.", Snackbar.LENGTH_LONG).show();
            return;
        }
        repo.approve(req.getId(), adminUid)
                .addOnSuccessListener(unused -> {
                    // ✅ keep /users and /registrationRequests in sync
                    syncUserStatus("APPROVED");
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.approved_snack, Snackbar.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.approve_failed) + ": " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show()
                );
    }

    private void showRejectDialog() {
        View d = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null, false);
        EditText input = d.findViewById(R.id.reason); // make sure dialog_reject_reason uses @id/reason

        new AlertDialog.Builder(this)
                .setTitle(R.string.reject)
                .setView(d)
                .setPositiveButton(R.string.reject, (dlg, which) ->
                        reject(input.getText() == null ? "" : input.getText().toString().trim()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void reject(String reason) {
        String adminUid = getAdminUid();
        if (adminUid == null) {
            Snackbar.make(findViewById(android.R.id.content), "Not signed in as admin.", Snackbar.LENGTH_LONG).show();
            return;
        }
        repo.reject(req.getId(), adminUid, reason.isEmpty() ? null : reason)
                .addOnSuccessListener(unused -> {
                    // ✅ keep /users and /registrationRequests in sync
                    syncUserStatus("REJECTED");
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.rejected_snack, Snackbar.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.reject_failed) + ": " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show());
    }

    // --- helper to sync status in /users/{uid} ---
    private void syncUserStatus(String newStatus) {
        if (req == null) return;

        // Prefer explicit userUid if your RegRequest has it; fall back to id (you used uid as doc id)
        String userUid = null;
        try {
            userUid = req.getUserUid();   // if your model has this
        } catch (Exception ignored) { }

        if (userUid == null || userUid.isEmpty()) {
            userUid = req.getId();
        }
        if (userUid == null || userUid.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userUid)
                .update("status", newStatus)
                .addOnFailureListener(e -> {
                    // optional: log or show a small snack, but don’t block the admin
                    // Snackbar.make(findViewById(android.R.id.content),
                    //         "Profile status sync failed: " + e.getMessage(),
                    //         Snackbar.LENGTH_SHORT).show();
                });
    }

    private static String nn(String s) { return (s == null || s.isEmpty()) ? "—" : s; }

    private @Nullable String getAdminUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }
}
