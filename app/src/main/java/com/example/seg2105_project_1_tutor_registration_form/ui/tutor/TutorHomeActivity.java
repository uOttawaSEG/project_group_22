package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

/*
 Tutor Home screen for signed-in tutors. Enforces an auth guard (redirects to the
 login screen if no user), wires the ViewPager+tabs for Availability/Requests/Sessions,
 and ensures the “Create Slot” button is only visible on the Availability tab and
 opens CreateSlotActivity to add a new 30-minute availability block.
*/

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TutorHomeActivity extends AppCompatActivity {

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

        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new TutorHomePagerAdapter(this, tutorId));

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
}
