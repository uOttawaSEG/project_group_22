package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.os.Bundle;
import android.widget.TextView;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * RejectedScreen — human-readable overview
 * ─────────────────────────────────────────────────────────────────────────────
 * Purpose
 * • Displays a simple “application rejected” screen.
 * • Optionally shows the Admin’s rejection reason if passed in the Intent as
 *   EXTRA_REASON.
 * • Provides a button to go back (finish this Activity → returns to login).
 *
 * How it works
 * • EdgeToEdge.enable(this) sets up edge-to-edge UI (safe to keep).
 * • setContentView(...) inflates activity_rejected_screen layout.
 * • Looks up tvReason and, if a non-empty reason exists, sets the text.
 * • Looks up btnBackToLogin and wires it to finish().
 *
 * Expected inputs
 * • Intent extra "EXTRA_REASON" (String, optional).
 *
 * Maintenance tips
 * • If you change the layout ids, update R.id.tvReason and R.id.btnBackToLogin.
 * • If you add “appeal” or “contact support” actions, wire new buttons here.
 * • Consider localization for the reason label/static copy in the XML.
 */
public class RejectedScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge is fine to keep; no need to attach insets to a non-existent view id.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rejected_screen);

        // Show reason (if provided by admin)
        TextView tv = findViewById(R.id.tvReason);
        String reason = getIntent().getStringExtra("EXTRA_REASON");
        if (tv != null && reason != null && !reason.isEmpty()) {
            tv.setText(reason);
        }

        // Back to login / close screen
        View btn = findViewById(R.id.btnBackToLogin);
        if (btn != null) {
            btn.setOnClickListener(v -> finish());
        }
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of RejectedScreen — TL;DR
 * ─────────────────────────────────────────────────────────────────────────────
 * • Shows optional rejection reason and a single “Back to login” action.
 * • No navigation side effects beyond finish(); returns to whatever launched it.
 * • Add more actions (appeal, help) if product requirements evolve.
 */
