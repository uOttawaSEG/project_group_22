package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TutorHomeActivity extends AppCompatActivity {

    private ViewPager2 pager;                 // <-- matches @+id/pager in XML
    private TabLayout tabs;                   // <-- @+id/tabs
    private FloatingActionButton fab;         // <-- @+id/fabAddSlot (optional)
    private MaterialButton btnCreateSlot;     // <-- @+id/btnCreateSlot (bottom CTA)

    private TutorHomePagerAdapter adapter;
    private String tutorId;

    private final ActivityResultLauncher<Intent> createSlotLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // After creating a slot, go back to Availability and refresh
                pager.setCurrentItem(TutorHomePagerAdapter.INDEX_AVAILABILITY, true);
                refreshAvailabilityTab();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_home);

        tutorId = AuthIdProvider.getCurrentUserId();
        if (tutorId == null || tutorId.trim().isEmpty()) {
            Toast.makeText(this, "Missing tutor id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ---- bind views (ids must match XML) ----
        pager = findViewById(R.id.pager);              // was R.id.viewPager → fix
        tabs  = findViewById(R.id.tabs);
        fab   = findViewById(R.id.fabAddSlot);
        btnCreateSlot = findViewById(R.id.btnCreateSlot);

        adapter = new TutorHomePagerAdapter(this, tutorId);
        pager.setAdapter(adapter);

        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            switch (position) {
                case TutorHomePagerAdapter.INDEX_AVAILABILITY: tab.setText("Availability"); break;
                case TutorHomePagerAdapter.INDEX_REQUESTS:     tab.setText("Requests");     break;
                case TutorHomePagerAdapter.INDEX_SESSIONS:     tab.setText("Sessions");     break;
            }
        }).attach();

        // Show the FAB only on the Availability tab; button stays always if you want
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                if (fab != null) {
                    fab.setVisibility(position == TutorHomePagerAdapter.INDEX_AVAILABILITY
                            ? View.VISIBLE : View.GONE);
                }
            }
        });

        View.OnClickListener launchCreate = v -> {
            Intent i = new Intent(this, CreateSlotActivity.class);
            createSlotLauncher.launch(i);
        };
        if (fab != null) fab.setOnClickListener(launchCreate);
        if (btnCreateSlot != null) btnCreateSlot.setOnClickListener(launchCreate);
    }

    private void refreshAvailabilityTab() {
        final int pos = TutorHomePagerAdapter.INDEX_AVAILABILITY;
        // Try to find fragment by default ViewPager2 tag "f" + position
        String tag = "f" + pos;
        androidx.fragment.app.Fragment f =
                getSupportFragmentManager().findFragmentByTag(tag);
        if (f instanceof AvailabilityFragment) {
            ((AvailabilityFragment) f).refresh();
        }
        // If null, it will refresh next time it's recreated; this is safe.
    }
}
