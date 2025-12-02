package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * StudentHomeActivity - Main hub for students.
 *
 * Features:
 * - Tab 1: My Sessions (pending, approved, completed with status indicators)
 * - Tab 2: Search Tutors (search by course, view available slots)
 *
 * Navigation: Students can view their sessions, search for tutors,
 * and book new sessions from this screen.
 */
public class StudentHomeActivity extends AppCompatActivity
        implements StudentSessionsFragment.Host {

    private ViewPager2 pager;

    @Override
    public void onSessionUpdated() {
        // Called after booking/cancelling in search or sessions fragments
        refreshSessionsTab();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        String studentId = user.getUid();

        pager = findViewById(R.id.pager);
        StudentHomePagerAdapter adapter = new StudentHomePagerAdapter(this, studentId);
        pager.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            switch (pos) {
                case 0:
                    tab.setText(R.string.tab_my_sessions);
                    break;
                case 1:
                    tab.setText(R.string.tab_search);
                    break;
            }
        }).attach();

        // Logout button
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }
    }

    /** Helper: refresh the Sessions tab fragment if attached. */
    private void refreshSessionsTab() {
        if (pager == null || pager.getAdapter() == null) return;
        long itemId = pager.getAdapter().getItemId(0); // Sessions tab index = 0
        String tag = "f" + itemId;
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f instanceof StudentSessionsFragment) {
            ((StudentSessionsFragment) f).refresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSessionsTab();
    }
}
