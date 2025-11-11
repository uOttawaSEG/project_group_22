package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;

/**
 * Shows details for a single tutoring session.
 *
 * Works in two modes:
 * 1) Preferred: launch with "sessionId" (and we fetch session+student via repo).
 * 2) Legacy:   launch with prefilled extras:
 *      - "when" ("YYYY-MM-DD • HH:mm–HH:mm")
 *      - "status"
 *      - "studentName", "studentEmail", "notes" (optional)
 *      - "studentUid"  (optional; if present we enrich name/email via repo)
 */
public class SessionDetailActivity extends AppCompatActivity {

    // Views
    private TextView tvWhen, tvStatus, tvName, tvEmail, tvNotes;

    private final TutorRepository repo = new FirestoreTutorRepository();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        tvWhen   = findViewById(R.id.tvWhen);
        tvStatus = findViewById(R.id.tvStatus);
        tvName   = findViewById(R.id.tvStudentName);
        tvEmail  = findViewById(R.id.tvStudentEmail);
        tvNotes  = findViewById(R.id.tvNotes);

        // Preferred: resolve by sessionId (then fetch everything)
        String sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            loadBySessionId(sessionId);
            return;
        }

        // Legacy fallback: render what was passed in; optionally enrich with studentUid
        String when       = getIntent().getStringExtra("when");
        String status     = getIntent().getStringExtra("status");
        String name       = getIntent().getStringExtra("studentName");
        String email      = getIntent().getStringExtra("studentEmail");
        String notes      = getIntent().getStringExtra("notes");
        String studentUid = getIntent().getStringExtra("studentUid");

        bindWhenStatus(when, status);
        bindStudentBasics(name, email, notes);

        if (studentUid != null && !studentUid.trim().isEmpty()) {
            enrichStudent(studentUid);
        }
    }

    // -------------------- Repo-backed loading --------------------

    private void loadBySessionId(String sessionId) {
        String tutorId = AuthIdProvider.requireCurrentUserId();

        repo.getSessionById(tutorId, sessionId, new TutorRepository.SingleSessionCallback() {
            @Override public void onSuccess(Session s) {
                // Build "when" string safely
                String when = safe(s.getDate());
                String st   = safe(s.getStartTime());
                String et   = safe(s.getEndTime());
                if (!st.isEmpty() && !et.isEmpty()) {
                    when = (when.isEmpty() ? "" : when + " • ") + st + "–" + et;
                }
                bindWhenStatus(when, safe(s.getStatus()));

                // Load student details
                String studentId = s.getStudentId();
                if (studentId != null && !studentId.trim().isEmpty()) {
                    repo.getStudent(studentId, new TutorRepository.StudentCallback() {
                        @Override public void onSuccess(Student st) {
                            String name  = (safe(st.getFirstName()) + " " + safe(st.getLastName())).trim();
                            String email = safe(st.getEmail());
                            bindStudentBasics(name, email, /*notes*/ null);
                        }
                        @Override public void onError(String msg) {
                            // Keep session info shown; show a subtle toast for student load failure
                            toast(msg);
                        }
                    });
                }
            }

            @Override public void onError(String msg) {
                toast(msg);
            }
        });
    }

    private void enrichStudent(String studentUid) {
        repo.getStudent(studentUid, new TutorRepository.StudentCallback() {
            @Override public void onSuccess(Student st) {
                String name  = (safe(st.getFirstName()) + " " + safe(st.getLastName())).trim();
                String email = safe(st.getEmail());
                if (!name.isEmpty())  tvName.setText(name);
                if (!email.isEmpty()) tvEmail.setText(email);
            }
            @Override public void onError(String msg) { /* non-fatal */ }
        });
    }

    // -------------------- UI binding helpers --------------------

    private void bindWhenStatus(String when, String status) {
        tvWhen.setText(safe(when));
        tvStatus.setText(safe(status));
    }

    private void bindStudentBasics(String name, String email, String notes) {
        tvName.setText(isBlank(name) ? "—" : name);
        tvEmail.setText(isBlank(email) ? "—" : email);
        tvNotes.setText(isBlank(notes) ? "—" : notes);
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
