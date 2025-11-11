
package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TutorHomePagerAdapter extends FragmentStateAdapter {

    private final String tutorId;

    public TutorHomePagerAdapter(@NonNull FragmentActivity activity, @NonNull String tutorId) {
        super(activity);
        this.tutorId = tutorId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return AvailabilityFragment.newInstance(tutorId);
            case 1: return RequestsFragment.newInstance(tutorId);
            case 2: return SessionsFragment.newInstance(tutorId);
            default: throw new IllegalArgumentException("Invalid page index: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Availability | Requests | Sessions
    }
}
