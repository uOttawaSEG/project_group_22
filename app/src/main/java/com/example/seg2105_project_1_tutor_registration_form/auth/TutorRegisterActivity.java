package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.DatabaseHelper;
import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.Tutor;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class TutorRegisterActivity extends AppCompatActivity {

    // text inputs
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etPhone;

    // dropdowns (multi-select)
    private MultiAutoCompleteTextView actDegree, actCourses;

    // submit
    private Button btnRegister;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_register); // <= your XML file name

        databaseHelper = new DatabaseHelper(this);
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
        // You can move these to res/values/arrays.xml later if you prefer
        String[] degreeOptions = new String[]{
                "High School Diploma",
                "Associate’s",
                "Bachelor of Arts",
                "Bachelor of Science",
                "Master’s",
                "PhD / Doctorate",
                "Post-doc"
        };

        String[] courseOptions = new String[]{
                "Mathematics",
                "Science",
                "English",
                "History",
                "Computer Science",
                "Physics",
                "Chemistry",
                "Biology",
                "Economics"
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

        // Comma-separated strings user selected/type
        String selectedDegreesCsv = actText(actDegree);   // keep as CSV in your DB
        String selectedCoursesCsv = actText(actCourses);

        if (first.isEmpty() || last.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            toast("Please fill all required fields.");
            return;
        }
        if (selectedDegreesCsv.isEmpty()) {
            toast("Please select at least one degree.");
            return;
        }
        if (selectedCoursesCsv.isEmpty()) {
            toast("Please select at least one course.");
            return;
        }
        if (databaseHelper.emailExists(email)) {
            toast("Email already registered.");
            return;
        }

        // Convert courses CSV -> List<String>
        List<String> courses = csvToList(selectedCoursesCsv);

        // Build Tutor model
        Tutor tutor = new Tutor(
                null,            // id (DB autoincrement)
                first, last,
                email, pass,
                phone,
                selectedDegreesCsv, // store degrees as CSV in COLUMN_DEGREE
                courses
        );

        boolean ok = databaseHelper.addTutor(tutor);
        if (ok) {
            toast("Registration successful!");

            Intent i = new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.WelcomeActivity.class);
            i.putExtra("role", "Tutor");
            i.putExtra("email", email);
            i.putExtra("name", first + " " + last);
            i.putExtra("phone", phone);
            i.putExtra("degreeCsv", selectedDegreesCsv);
            i.putExtra("coursesCsv", selectedCoursesCsv);
            startActivity(i);

            finish();
        } else {
            toast("Registration failed. Please try again.");
        }

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



    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
