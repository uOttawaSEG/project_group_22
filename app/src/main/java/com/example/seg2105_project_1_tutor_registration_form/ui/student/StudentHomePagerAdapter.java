package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter for StudentHomeActivity tabs.
 * Tab 0: My Sessions
 * Tab 1: Search Tutors
 */
public class StudentHomePagerAdapter extends FragmentStateAdapter {

    private final String studentId;

    public StudentHomePagerAdapter(@NonNull FragmentActivity fa, String studentId) {
        super(fa);
        this.studentId = studentId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return StudentSessionsFragment.newInstance(studentId);
            case 1:
                return StudentSearchFragment.newInstance(studentId);
            default:
                return StudentSessionsFragment.newInstance(studentId);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
