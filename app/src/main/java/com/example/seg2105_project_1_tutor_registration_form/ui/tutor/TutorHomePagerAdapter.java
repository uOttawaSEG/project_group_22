package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
/*

/*
 * TutorHomePagerAdapter
 * ---------------------
 * Purpose:
 *   Provides the three tutor home tabs (Availability | Requests | Sessions) to the ViewPager2.
 *
 * Tab specifications:
 *   • PAGE 0 → AvailabilityFragment: manage or remove 30-minute availability slots.
 *   • PAGE 1 → RequestsFragment: review and approve/reject incoming session requests.
 *   • PAGE 2 → SessionsFragment: view confirmed or archived sessions (read-only or actionable).
 *
 * Notes:
 *   • The tutorId is injected once and passed to each fragment via newInstance().
 *   • If an unexpected index is requested, we safely fall back to AvailabilityFragment.
 */

public class TutorHomePagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 3; // Availability | Requests | Sessions
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
            default: return AvailabilityFragment.newInstance(tutorId);
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
