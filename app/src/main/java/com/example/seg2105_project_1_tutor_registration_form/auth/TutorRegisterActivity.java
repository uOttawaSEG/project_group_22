package com.example.seg2105_project_1_tutor_registration_form.auth;

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
import com.google.android.material.textfield.TextInputEditText;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TutorRegisterActivity — human-readable overview
 * ─────────────────────────────────────────────────────────────────────────────
 * Purpose
 * • Collect tutor info (name, contact, degrees, courses) and submit a registration.
 *
 * What happens on “Register”
 * 1) repo.signUp(email, pass) — create Firebase Auth user.
 * 2) saveUserProfile(uid, profile) — write /users/{uid} with role and form fields.
 * 3) FirestoreRegistrationRepository.createPendingForUid(...) — create a PENDING
 *    row in the admin inbox /registrationRequests so an Admin can approve/reject.
 * 4) On success, navigate to WelcomeActivity and pass a few extras for display.
 *
 * UI details
 * • MultiAutoCompleteTextView for degrees and courses (comma tokenizer).
 * • Simple Toasts for user feedback on success/failure.
 *
 * Failure modes (user feedback)
 * • Auth failure → “Sign up failed: …”
 * • Profile write failure → “Failed to save profile: …”
 * • Inbox write failure → “Saved to inbox failed: …”
 *
 * Notes & maintenance
 * • Keep role strings consistent across app (“tutor” in profile, “TUTOR” in request).
 * • If you add validation (recommended), do it at the top of handleRegistration().
 * • Localize strings for production (toasts, dropdown values).
 * • Degree/Course options are hard-coded here; consider centralizing or fetching.
 */
public class TutorRegisterActivity extends AppCompatActivity {

    // text inputs
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etPhone;

    // dropdowns (multi-select)
    private MultiAutoCompleteTextView actDegree, actCourses;

    // submit
    private Button btnRegister;

    // Firebase
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
        String[] degreeOptions = new String[]{
                "High School Diploma","Associate’s","Bachelor of Arts","Bachelor of Science",
                "Master’s","PhD / Doctorate","Post-doc"
        };
        String[] courseOptions = new String[]{
                "Mathematics","Science","English","History","Computer Science",
                "Physics","Chemistry","Biology","Economics"
        };

        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, degreeOptions);
        actDegree.setAdapter(degreeAdapter);
        actDegree.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actDegree.setThreshold(1);

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, courseOptions);
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

        // ... your validation stays the same ...

        repo.signUp(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = repo.uid();

                    // 1) Save profile (you already had this)
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("role", "tutor");
                    profile.put("email", email);
                    profile.put("firstName", first);
                    profile.put("lastName", last);
                    profile.put("phone", phone);
                    profile.put("degreesCsv", actText(actDegree));
                    profile.put("coursesCsv", actText(actCourses));

                    repo.saveUserProfile(uid, profile)
                            .addOnSuccessListener(x -> {
                                // 2) Create the PENDING inbox entry
                                new com.example.seg2105_project_1_tutor_registration_form.data
                                        .FirestoreRegistrationRepository()
                                        .createPendingForUid(uid, email, first, last, phone, "TUTOR")
                                        .addOnSuccessListener(v -> {
                                            toast("Application submitted for review.");
                                            // 3) Navigate
                                            Intent i = new Intent(this,
                                                    com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity.class);
                                            i.putExtra("role", "Tutor");
                                            i.putExtra("email", email);
                                            i.putExtra("name", first + " " + last);
                                            i.putExtra("phone", phone);
                                            i.putExtra("degreeCsv", actText(actDegree));
                                            i.putExtra("coursesCsv", actText(actCourses));
                                            startActivity(i);
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                toast("Saved to inbox failed: " + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    toast("Failed to save profile: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        toast("Sign up failed: " + e.getMessage()));
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

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of TutorRegisterActivity — TL;DR
 * ─────────────────────────────────────────────────────────────────────────────
 * • Signs up the tutor, saves /users/{uid}, creates PENDING request, then navigates.
 * • Multi-select fields use comma tokenization; values are stored as CSV strings.
 * • Add stronger validation and i18n before production; centralize options if needed.
 */
