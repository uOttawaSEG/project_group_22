package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TutorHomeActivity extends AppCompatActivity
        implements RequestsFragment.Host {

    private ViewPager2 pager;

    @Override
    public void onRequestHandled() {
        // Called after approve/reject in RequestsFragment
        refreshSessionsTab();            // trigger your refresh
        // Optional: pager.setCurrentItem(2, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this,
                    com.example.seg2105_project_1_tutor_registration_form.auth.MainActivity.class));
            finish();
            return;
        }
        String tutorId = user.getUid();

        pager = findViewById(R.id.pager);
        TutorHomePagerAdapter adapter = new TutorHomePagerAdapter(this, tutorId);
        pager.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            switch (pos) {
                case 0: tab.setText(R.string.tab_availability); break;
                case 1: tab.setText(R.string.tab_requests); break;
                case 2: tab.setText(R.string.tab_sessions); break;
            }
        }).attach();

        MaterialButton btnCreate = findViewById(R.id.btnCreateSlot);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                btnCreate.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }
        });

        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.seg2105_project_1_tutor_registration_form.ui.tutor.CreateSlotActivity.class))
        );
    }

    /** Helper: refresh the Sessions tab fragment if attached. */
    private void refreshSessionsTab() {      // ❗ no @Override here
        if (pager == null || pager.getAdapter() == null) return;
        long itemId = pager.getAdapter().getItemId(2); // Sessions tab index = 2
        String tag = "f" + itemId;
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f instanceof SessionsFragment) {
            ((SessionsFragment) f).refresh();
        }
    }
}
