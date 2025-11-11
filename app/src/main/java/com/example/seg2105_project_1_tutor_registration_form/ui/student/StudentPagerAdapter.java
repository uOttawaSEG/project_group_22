package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StudentPagerAdapter extends FragmentStateAdapter {

    private String tutorId; // passed to OpenSlotsFragment

    public StudentPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
        // tell the Open Slots fragment to refresh when visible
        notifyDataSetChanged();
    }

    @NonNull @Override public Fragment createFragment(int position) {
        if (position == 0) return StudentOpenSlotsFragment.newInstance(tutorId);
        return new StudentSessionsFragment(); // always student-scoped
    }

    @Override public int getItemCount() { return 2; }

    @Override public long getItemId(int position) {
        // include tutorId in stable id so ViewPager2 recreates OpenSlots on tutor change
        return (position == 0 ? ("open_" + (tutorId == null ? "" : tutorId)) : "mySessions").hashCode();
    }

    @Override public boolean containsItem(long itemId) { return true; }
}
