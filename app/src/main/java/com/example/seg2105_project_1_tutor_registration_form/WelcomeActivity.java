package com.example.seg2105_project_1_tutor_registration_form;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
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
        String name    = n(i.getStringExtra("name"));        // optional
        String phone   = n(i.getStringExtra("phone"));       // optional
        String degree  = n(i.getStringExtra("degreeCsv"));   // optional CSV
        String courses = n(i.getStringExtra("coursesCsv"));  // optional CSV

        // Show immediately if we have data
        renderInfo(name, role, email, phone, degree, courses);

        // Fallback: if role (or other info) wasn’t passed, try Firestore
        if (role.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            String r  = n(doc.getString("role"));
                            String fn = n(doc.getString("firstName"));
                            String ln = n(doc.getString("lastName"));
                            String ph = n(doc.getString("phone"));
                            String dg = n(doc.getString("degreeCsv"));
                            String cs = n(doc.getString("coursesCsv"));
                            String em = email.isEmpty() ? n(user.getEmail()) : email;

                            renderInfo((fn + " " + ln).trim(), r, em, ph, dg, cs);
                        });
            }
        }

        btnLogout.setOnClickListener(v -> {
            // Sign out and return to Main
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity.class));
            finish();
        });
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

        if (sb.length() == 0) {
            sb.append("You're signed in. We couldn't find more details to show.");
        }
        tvUserInfo.setText(sb.toString().trim());
    }

    private String n(String s) { return s == null ? "" : s.trim(); }

}


