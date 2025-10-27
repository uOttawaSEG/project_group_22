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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TutorRegisterActivity extends AppCompatActivity {

    // text inputs
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etPhone;

    // dropdowns (multi-select)
    private MultiAutoCompleteTextView actDegree, actCourses;

    // submit
    private Button btnRegister;

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

        String degreesCsv = actText(actDegree);
        String coursesCsv = actText(actCourses);

        if (first.isEmpty() || last.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            toast("Please fill all required fields."); return;
        }
        if (pass.length() < 6) { toast("Password must be at least 6 characters."); return; }
        if (degreesCsv.isEmpty()) { toast("Please select at least one degree."); return; }
        if (coursesCsv.isEmpty()) { toast("Please select at least one course."); return; }

        List<String> degrees = csvToList(degreesCsv);
        List<String> courses = csvToList(coursesCsv);

        // pick one string for highestDegree
        String highestDegree = degrees.isEmpty() ? "" : degrees.get(0);

        btnRegister.setEnabled(false);

        //AccountManager so auth + user profile + registration request happen together
        AccountManager am = new AccountManager(this);
        am.registerTutor(
                first, last, email, pass, phone,
                highestDegree,
                courses,
                (ok, message) -> runOnUiThread(() -> {
                    toast(message);
                    btnRegister.setEnabled(true);
                    if (ok) {

                        FirebaseAuth.getInstance().signOut();

                        Intent i = new Intent(this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                })
        );
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String actText(MultiAutoCompleteTextView v) {
        CharSequence cs = v.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private List<String> csvToList(String csv) {
        if (TextUtils.isEmpty(csv)) return Collections.emptyList();
        return Arrays.asList(csv.split("\\s*,\\s*"));
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
