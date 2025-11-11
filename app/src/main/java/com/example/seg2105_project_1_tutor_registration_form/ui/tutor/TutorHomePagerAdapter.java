package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TutorHomePagerAdapter extends FragmentStateAdapter {

    private final String tutorId;

    public static final int INDEX_AVAILABILITY = 0;
    public static final int INDEX_REQUESTS     = 1;
    public static final int INDEX_SESSIONS     = 2;

    public TutorHomePagerAdapter(@NonNull androidx.fragment.app.FragmentActivity activity,
                                 @NonNull String tutorId) {
        super(activity);
        this.tutorId = tutorId;
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case INDEX_AVAILABILITY:
                return AvailabilityFragment.newInstance(tutorId);
            case INDEX_REQUESTS:
                return RequestsFragment.newInstance(tutorId);
            case INDEX_SESSIONS:
            default:
                return SessionsFragment.newInstance(tutorId);
        }
    }

    @Override
    public int getItemCount() { return 3; }
}
