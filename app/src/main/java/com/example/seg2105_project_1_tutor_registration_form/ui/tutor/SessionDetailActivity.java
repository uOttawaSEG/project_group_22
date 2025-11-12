package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/*
 * SessionDetailActivity
 * ---------------------
 * Purpose:
 *   Shows a single tutoring session’s details. Values come primarily from Intent extras
 *   provided by the list screen; if we also receive a studentUid, we enrich the name/email
 *   from Firestore (/users/{studentUid}) when available.
 *
 * Notes:
 *   - All UI fields are defensively populated (fallback to "—" or empty string).
 *   - Firestore enrichment is optional and non-blocking; errors are ignored to keep UX smooth.
 */
public class SessionDetailActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        // --- View references (TextViews expected in activity_session_detail) ---
        TextView tvWhen   = findViewById(R.id.tvWhen);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvName   = findViewById(R.id.tvStudentName);
        TextView tvEmail  = findViewById(R.id.tvStudentEmail);
        TextView tvNotes  = findViewById(R.id.tvNotes);

        // --- Intent extras coming from the list item / adapter ---
        // Example values:
        //   when   = "2025-11-11 • 10:00–10:30"
        //   status = "APPROVED" | "PENDING" | "REJECTED"
        //   name   = student's display name (may be absent if not resolved earlier)
        //   email  = student's email (optional)
        //   notes  = tutor-facing notes entered by the student (optional)
        //   studentUid = Firestore user document id for enrichment (optional)
        String when       = getIntent().getStringExtra("when");
        String status     = getIntent().getStringExtra("status");
        String name       = getIntent().getStringExtra("studentName");
        String email      = getIntent().getStringExtra("studentEmail");
        String notes      = getIntent().getStringExtra("notes");
        String studentUid = getIntent().getStringExtra("studentUid");

        // --- Bind base values with safe fallbacks so the UI never shows "null" ---
        tvWhen.setText(when == null ? "" : when);
        tvStatus.setText(status == null ? "" : status);
        tvName.setText(name == null || name.isEmpty() ? "—" : name);
        tvEmail.setText(email == null || email.isEmpty() ? "—" : email);
        tvNotes.setText(notes == null || notes.isEmpty() ? "—" : notes);

        // --- Optional Firestore enrichment: prefer authoritative user profile if available ---
        if (studentUid != null && !studentUid.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(studentUid)
                    .get()
                    // On success, replace name/email if the user doc has them
                    .addOnSuccessListener(d -> fillFromUserDoc(d, tvName, tvEmail))
                    // On failure, keep whatever we already showed (likely from intent)
                    .addOnFailureListener(e -> { /* ignore to keep UI responsive */ });
        }
    }

    /**
     * Safely pull full name and email from the user document and update the UI.
     * This only overwrites existing fields if the fetched values are non-empty.
     */
    private void fillFromUserDoc(DocumentSnapshot d, TextView tvName, TextView tvEmail) {
        if (d == null || !d.exists()) return;

        // Build a full name from first/last; trim to handle missing parts gracefully
        String first = d.getString("firstName");
        String last  = d.getString("lastName");
        String nm    = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();

        // Pull email if present
        String em = d.getString("email");

        // Only overwrite UI if we have meaningful values
        if (nm != null && !nm.isEmpty()) tvName.setText(nm);
        if (em != null && !em.isEmpty()) tvEmail.setText(em);
    }
}
