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
 *   (and optionally program) from Firestore (/users/{studentUid}) when available.
 *
 * Intent extras used:
 *   "when"        -> formatted time range string
 *   "status"      -> session status (APPROVED / PENDING / REJECTED)
 *   "studentName" -> student display name (optional)
 *   "studentEmail"-> student email (optional)
 *   "program"     -> student's program of study for this session (optional)
 *   "subject"     -> course/subject string (e.g., "CSI 2110 – Data Structures")
 *   "notes"       -> notes attached to this session (optional)
 *   "studentUid"  -> Firestore /users document id (optional)
 */
public class SessionDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        // --- View references ---
        TextView tvWhen    = findViewById(R.id.tvWhen);
        TextView tvStatus  = findViewById(R.id.tvStatus);
        TextView tvName    = findViewById(R.id.tvStudentName);
        TextView tvEmail   = findViewById(R.id.tvStudentEmail);
        TextView tvProgram = findViewById(R.id.tvProgram);
        TextView tvSubject = findViewById(R.id.tvSubject);
        TextView tvNotes   = findViewById(R.id.tvNotes);

        // --- Intent extras coming from the list item / adapter ---
        String when       = getIntent().getStringExtra("when");
        String status     = getIntent().getStringExtra("status");
        String name       = getIntent().getStringExtra("studentName");
        String email      = getIntent().getStringExtra("studentEmail");
        String program    = getIntent().getStringExtra("program");
        String subject    = getIntent().getStringExtra("subject");
        String notes      = getIntent().getStringExtra("notes");
        String studentUid = getIntent().getStringExtra("studentUid");

        // --- Bind base values with safe fallbacks so the UI never shows "null" ---
        tvWhen.setText(when == null ? "" : when);
        tvStatus.setText(status == null ? "" : status);

        tvName.setText(name == null || name.isEmpty() ? "—" : name);
        tvEmail.setText(email == null || email.isEmpty() ? "—" : email);

        tvProgram.setText(program == null || program.isEmpty() ? "—" : program);
        tvSubject.setText(subject == null || subject.isEmpty() ? "—" : subject);

        tvNotes.setText(notes == null || notes.isEmpty() ? "—" : notes);

        // --- Optional Firestore enrichment: prefer authoritative user profile if available ---
        if (studentUid != null && !studentUid.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(studentUid)
                    .get()
                    .addOnSuccessListener(d -> fillFromUserDoc(d, tvName, tvEmail, tvProgram))
                    .addOnFailureListener(e -> {
                        // ignore to keep UI responsive and keep existing values
                    });
        }
    }

    /**
     * Safely pull full name, email and (optionally) program from the user document and update the UI.
     * This only overwrites existing fields if the fetched values are non-empty.
     *
     * Expected user fields (if you choose to store them):
     *   firstName, lastName, email, program
     */
    private void fillFromUserDoc(DocumentSnapshot d,
                                 TextView tvName,
                                 TextView tvEmail,
                                 TextView tvProgram) {
        if (d == null || !d.exists()) return;

        // Build a full name from first/last; trim to handle missing parts gracefully
        String first = d.getString("firstName");
        String last  = d.getString("lastName");
        String nm    = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();

        // Pull email if present
        String em = d.getString("email");

        // Optional: pull program from user profile if you're storing it
        String prog = d.getString("program");

        // Only overwrite UI if we have meaningful values
        if (nm != null && !nm.isEmpty()) tvName.setText(nm);
        if (em != null && !em.isEmpty()) tvEmail.setText(em);
        if (prog != null && !prog.isEmpty()) tvProgram.setText(prog);
    }
}
