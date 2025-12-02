package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class StudentRegistrationActivity extends AppCompatActivity {

    // UI refs
    private TextInputEditText etFirst, etLast, etEmail, etPassword, etPhone, etStudentId, etProgram, etNotes;
    private AutoCompleteTextView actStudyYear;
    private MultiAutoCompleteTextView actCourses;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        // --- Study year dropdown using arrays.xml ---
        actStudyYear = findViewById(R.id.actStudyYear);
        String[] years = getResources().getStringArray(R.array.study_years);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, years
        );
        actStudyYear.setAdapter(yearAdapter);
        actStudyYear.setOnClickListener(v -> actStudyYear.showDropDown());

        // --- Courses dropdown using the same list as tutors/search ---
        actCourses = findViewById(R.id.actCoursesInterested);
        String[] courseOptions = getResources().getStringArray(R.array.courses_interested);
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, courseOptions
        );
        actCourses.setAdapter(courseAdapter);
        actCourses.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actCourses.setThreshold(1);

        // --- Wire up the rest of the inputs ---
        etFirst     = findViewById(R.id.etFirstName);
        etLast      = findViewById(R.id.etLastName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etPhone     = findViewById(R.id.etPhone);
        etStudentId = findViewById(R.id.etStudentId);
        etProgram   = findViewById(R.id.etProgram);
        etNotes     = findViewById(R.id.etNotes);
        btnRegister = findViewById(R.id.btnRegisterStudent);

        // --- Register flow (now uses AccountManager to also create registrationRequests) ---
        btnRegister.setOnClickListener(v -> {
            String first     = s(etFirst);
            String last      = s(etLast);
            String email     = s(etEmail);
            String password  = s(etPassword);
            String phone     = s(etPhone);
            String studentId = s(etStudentId);
            String program   = s(etProgram);
            String studyYear = s(actStudyYear);
            String courses   = s(actCourses); // comma-separated
            String notes     = s(etNotes);

            // Minimal validation
            if (TextUtils.isEmpty(first)) { toast("First name is required"); return; }
            if (TextUtils.isEmpty(last))  { toast("Last name is required"); return; }
            if (TextUtils.isEmpty(email)) { toast("Email is required"); return; }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                toast("Password must be at least 6 characters"); return;
            }

            btnRegister.setEnabled(false);

            AccountManager am = new AccountManager(this);
            am.registerStudent(
                    first, last, email, password,
                    phone, studentId, program, studyYear,
                    csvToList(courses), // convert "A, B" -> List<String>
                    notes,
                    (ok, message) -> runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        if (ok) {
                            Intent i = new Intent(this, WelcomeActivity.class);
                            i.putExtra("role", "STUDENT");
                            i.putExtra("email", email);
                            i.putExtra("name", first + " " + last);
                            i.putExtra("phone", phone);
                            i.putExtra("degreeCsv", program);
                            i.putExtra("coursesCsv", courses);
                            startActivity(i);
                            finish();
                        }
                    })
            );
        });
    }

    // --- helpers ---
    private String s(TextInputEditText et) { return et == null || et.getText()==null ? "" : et.getText().toString().trim(); }
    private String s(AutoCompleteTextView v) { return v == null || v.getText()==null ? "" : v.getText().toString().trim(); }
    private String s(MultiAutoCompleteTextView v) { return v == null || v.getText()==null ? "" : v.getText().toString().trim(); }

    private List<String> csvToList(String csv) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(csv)) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
