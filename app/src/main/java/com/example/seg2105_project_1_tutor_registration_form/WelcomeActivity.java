package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.example.seg2105_project_1_tutor_registration_form.model.Role;
import com.example.seg2105_project_1_tutor_registration_form.ui.admin.AdminHomeActivity;
import com.example.seg2105_project_1_tutor_registration_form.ui.student.StudentHomeActivity;
import com.example.seg2105_project_1_tutor_registration_form.ui.tutor.TutorHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WelcomeActivity extends AppCompatActivity {

    private TextView tvUserInfo;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout  = findViewById(R.id.btnLogout);

        // Read what we know about the user from Intent
        Intent i = getIntent();
        String role    = n(i.getStringExtra("role"));
        String email   = n(i.getStringExtra("email"));
        String name    = n(i.getStringExtra("name"));
        String phone   = n(i.getStringExtra("phone"));
        String degree  = n(i.getStringExtra("degreeCsv"));
        String courses = n(i.getStringExtra("coursesCsv"));

        renderInfo(name, role, email, phone, degree, courses);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && role.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            handleUserDoc(doc, user);
                        }
                    });
        } else if (!role.isEmpty()) {
            // If role was passed through Intent, redirect immediately
            redirectByRole(role);
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void handleUserDoc(DocumentSnapshot doc, FirebaseUser user) {
        String role = n(doc.getString("role"));
        String fn = n(doc.getString("firstName"));
        String ln = n(doc.getString("lastName"));
        String name = (fn + " " + ln).trim();
        String phone = n(doc.getString("phone"));
        String degree = n(doc.getString("degreeCsv"));
        String courses = n(doc.getString("coursesCsv"));
        String email = n(user.getEmail());

        renderInfo(name, role, email, phone, degree, courses);
        redirectByRole(role);
    }

    private void redirectByRole(String role) {
        Intent intent = null;

        if (role.equalsIgnoreCase(Role.ADMINISTRATOR.name())) {
            intent = new Intent(this, AdminHomeActivity.class);
        } else if (role.equalsIgnoreCase(Role.TUTOR.name())) {
            intent = new Intent(this, TutorHomeActivity.class);
        } else if (role.equalsIgnoreCase(Role.STUDENT.name())) {
            intent = new Intent(this, StudentHomeActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish(); // prevent going back to Welcome
        }
    }

    private void renderInfo(String name, String role, String email,
                            String phone, String degree, String courses) {
        StringBuilder sb = new StringBuilder();
        if (!name.isEmpty())   sb.append("Name: ").append(name).append("\n");
        if (!role.isEmpty())   sb.append("Role: ").append(role).append("\n");
        if (!email.isEmpty())  sb.append("Email: ").append(email).append("\n");
        if (!phone.isEmpty())  sb.append("Phone: ").append(phone).append("\n");
        if (!degree.isEmpty()) sb.append("Degree(s): ").append(degree).append("\n");
        if (!courses.isEmpty())sb.append("Courses: ").append(courses).append("\n");

        if (sb.length() == 0)
            sb.append("You're signed in. We couldn't find more details to show.");

        tvUserInfo.setText(sb.toString().trim());
    }

    private String n(String s) { return s == null ? "" : s.trim(); }
}
