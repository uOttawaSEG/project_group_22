package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;

public class StudentRegistrationActivity extends AppCompatActivity {

    private static final String[] STUDY_YEARS = new String[] {
            "1st year", "2nd year", "3rd year", "4th year", "5th+", "Graduate"
    };

    // Replace with your real list or pull from resources later
    private static final String[] COURSES = new String[] {
            "SEG 2105", "CEG 2136", "CSI 2110", "MAT 1348", "MAT 2377",
            "CSI 2101", "ELG 2138", "ITI 1100"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register); // this is your XML from earlier

        // Study Year (single-select)
        AutoCompleteTextView actStudyYear = findViewById(R.id.actStudyYear);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                STUDY_YEARS
        );
        actStudyYear.setAdapter(yearAdapter);
        actStudyYear.setOnClickListener(v -> actStudyYear.showDropDown()); // open on tap

        // Courses Interested (multi-select)
        MultiAutoCompleteTextView actCourses = findViewById(R.id.actCoursesInterested);
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                COURSES
        );
        actCourses.setAdapter(courseAdapter);
        actCourses.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actCourses.setThreshold(1); // type-ahead after 1 char
    }
}
