package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/*
Shows details for a single tutoring session. It renders the values passed from the list
(date/time window, status, student name/email/notes). If a studentUid was provided,
it also loads /users/{studentUid} to enrich name/email (if present).
*/
public class SessionDetailActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        TextView tvWhen   = findViewById(R.id.tvWhen);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvName   = findViewById(R.id.tvStudentName);
        TextView tvEmail  = findViewById(R.id.tvStudentEmail);
        TextView tvNotes  = findViewById(R.id.tvNotes);

        // Values passed from the list item (use whatever keys your adapter sets)
        String when   = getIntent().getStringExtra("when");          // e.g., "2025-11-11 • 10:00–10:30"
        String status = getIntent().getStringExtra("status");        // e.g., "approved"
        String name   = getIntent().getStringExtra("studentName");   // may be null/placeholder
        String email  = getIntent().getStringExtra("studentEmail");  // may be null
        String notes  = getIntent().getStringExtra("notes");         // optional
        String studentUid = getIntent().getStringExtra("studentUid");// may be null

        tvWhen.setText(when == null ? "" : when);
        tvStatus.setText(status == null ? "" : status);
        tvName.setText(name == null || name.isEmpty() ? "—" : name);
        tvEmail.setText(email == null || email.isEmpty() ? "—" : email);
        tvNotes.setText(notes == null || notes.isEmpty() ? "—" : notes);

        // Optional enrichment if we have a real student uid
        if (studentUid != null && !studentUid.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(studentUid)
                    .get()
                    .addOnSuccessListener(d -> fillFromUserDoc(d, tvName, tvEmail))
                    .addOnFailureListener(e -> {/* ignore for test data */});
        }
    }

    private void fillFromUserDoc(DocumentSnapshot d, TextView tvName, TextView tvEmail) {
        if (d == null || !d.exists()) return;
        String first = d.getString("firstName");
        String last  = d.getString("lastName");
        String nm    = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        String em    = d.getString("email");
        if (nm != null && !nm.isEmpty()) tvName.setText(nm);
        if (em != null && !em.isEmpty()) tvEmail.setText(em);
    }
}
