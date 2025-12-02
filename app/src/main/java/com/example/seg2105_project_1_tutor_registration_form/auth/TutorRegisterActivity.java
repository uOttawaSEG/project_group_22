package com.example.seg2105_project_1_tutor_registration_form.auth;

/* This action creates a tutor account and then requires a new login.
 After submission, the tutor is written, a Firebase Auth user is created, and inputs are validated.
 creates a matching PENDING record in /registrationRequests and adds the profile to /users/{uid}. Only after logging in and clearing the approval gate can tutors access Tutor Home. If all steps are completed successfully, the user is signed out and taken to MainActivity, the login screen. Additionally, the screen initializes basic multi-select dropdowns for the course and degree.
*/

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.FirebaseRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorRegisterActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etPhone;
    private MultiAutoCompleteTextView actDegree, actCourses;
    private Button btnRegister;

    private final FirebaseRepository repo = new FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_register);
        bindViews();
        setupDropdowns();
        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    private void bindViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etPhone     = findViewById(R.id.etPhone);
        actDegree   = findViewById(R.id.actDegree);
        actCourses  = findViewById(R.id.actCourses);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupDropdowns() {
        // Use arrays from res/values/arrays.xml
        String[] degreeOptions = getResources().getStringArray(R.array.degrees_array);
        String[] courseOptions = getResources().getStringArray(R.array.courses_interested);

        ArrayAdapter<String> degreeAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, degreeOptions);
        actDegree.setAdapter(degreeAdapter);
        actDegree.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actDegree.setThreshold(1);

        ArrayAdapter<String> courseAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, courseOptions);
        actCourses.setAdapter(courseAdapter);
        actCourses.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actCourses.setThreshold(1);
    }

    private void handleRegistration() {
        String first = textOf(etFirstName);
        String last  = textOf(etLastName);
        String email = textOf(etEmail);
        String pass  = textOf(etPassword);
        String phone = textOf(etPhone);

        if (first.isEmpty() || last.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            toast("Please fill in first name, last name, email, and password.");
            return;
        }

        repo.signUp(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = repo.uid();
                    if (uid == null || uid.isEmpty()) { toast("Sign up succeeded but UID missing."); return; }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("role", "tutor");
                    profile.put("email", email);
                    profile.put("firstName", first);
                    profile.put("lastName", last);
                    profile.put("phone", phone);
                    profile.put("degreesCsv", actText(actDegree));
                    profile.put("coursesCsv", actText(actCourses));
                    profile.put("updatedAt", System.currentTimeMillis());
                    profile.put("averageRating", 0.0);
                    profile.put("ratingsCount", 0);
                    profile.put("ratingsSum", 0);
                    profile.put("status", "PENDING"); // (optional, but good to have)

                    repo.saveUserProfile(uid, profile)
                            .addOnSuccessListener(x -> {
                                new FirestoreRegistrationRepository()
                                        .createPendingForUid(uid, email, first, last, phone, "TUTOR")
                                        .addOnSuccessListener(v -> {
                                            toast("Application submitted. Please log in.");
                                            FirebaseAuth.getInstance().signOut();
                                            Intent i = new Intent(
                                                    this,
                                                    com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity.class
                                            );
                                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(i);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> toast("Saved to inbox failed: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> toast("Failed to save profile: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("Sign up failed: " + e.getMessage()));
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String actText(MultiAutoCompleteTextView v) {
        CharSequence cs = v.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private List<String> csvToList(String csv) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(csv)) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
