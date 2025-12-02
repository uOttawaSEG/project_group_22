package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;

import java.util.Calendar;
import java.util.Locale;

public class CreateSlotActivity extends AppCompatActivity {

    private TextView tvDate, tvStart, tvEnd;
    private Switch swAutoApprove;
    private Button btnPickDate, btnPickEnd, btnSave, btnBack;
    private EditText etStartTime;
    private Spinner spStartAmPm;

    // we still keep minutes for validation & future-time check
    private final Calendar pickedDay = Calendar.getInstance();
    private int startMin = -1, endMin = -1;

    private TutorRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_slot);

        // Up (←) button in the top app bar navigates back to TutorHomeActivity
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("+ Add Slot");
        }

        repo = new FirestoreTutorRepository();

        tvDate        = findViewById(R.id.tvDate);
        tvStart       = findViewById(R.id.tvStart);
        tvEnd         = findViewById(R.id.tvEnd);
        swAutoApprove = findViewById(R.id.swAutoApprove);

        btnPickDate   = findViewById(R.id.btnPickDate);
        btnPickEnd    = findViewById(R.id.btnPickEnd);   // disabled; end is auto-computed
        btnSave       = findViewById(R.id.btnSave);
        btnBack       = findViewById(R.id.btnBack);

        etStartTime   = findViewById(R.id.etStartTime);
        spStartAmPm   = findViewById(R.id.spStartAmPm);

        btnPickDate.setOnClickListener(v -> showDatePicker());

        // We’re auto-computing end = start + 30, so disable manual end picking
        btnPickEnd.setEnabled(false);
        btnPickEnd.setAlpha(0.5f);

        btnSave.setOnClickListener(v -> saveSlot());

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBackToTutorHome());
        }
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (d, y, m, day) -> {
                    pickedDay.set(Calendar.YEAR, y);
                    pickedDay.set(Calendar.MONTH, m);
                    pickedDay.set(Calendar.DAY_OF_MONTH, day);
                    tvDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Reads the typed time (HH:MM) + AM/PM from the UI, validates, and
     * populates startMin/endMin + tvStart/tvEnd. Returns true if valid.
     */
    private boolean computeStartAndEndFromInput() {
        String dateStr = tvDate.getText().toString().trim();
        String timeStr = etStartTime.getText().toString().trim();

        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            toast("Pick date and start time");
            return false;
        }

        String[] parts = timeStr.split(":");
        if (parts.length != 2) {
            toast("Use HH:MM format (e.g., 11:30)");
            return false;
        }

        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            toast("Invalid time. Use numbers like 09:00 or 11:30.");
            return false;
        }

        if (minute != 0 && minute != 30) {
            toast("Pick :00 or :30 only");
            return false;
        }

        // Interpret hour with AM/PM dropdown (12-hour style). If user types 12:xx,
        // AM → 00:xx, PM → 12:xx. For 1–11, PM adds +12h.
        String ampm = spStartAmPm.getSelectedItem() != null
                ? spStartAmPm.getSelectedItem().toString()
                : "AM";

        if ("PM".equalsIgnoreCase(ampm) && hour < 12) {
            hour += 12;
        } else if ("AM".equalsIgnoreCase(ampm) && hour == 12) {
            hour = 0;
        }

        if (hour < 0 || hour > 23) {
            toast("Hour must be between 1 and 12");
            return false;
        }

        startMin = hour * 60 + minute;
        endMin   = startMin + 30;

        int eh = endMin / 60;
        int em = endMin % 60;

        // Echo chosen times in 24-hour HH:MM format
        tvStart.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
        tvEnd.setText(String.format(Locale.US, "%02d:%02d", eh, em));

        return true;
    }

    private void saveSlot() {
        // Parse and validate typed time
        if (!computeStartAndEndFromInput()) {
            return;
        }

        if (endMin - startMin != 30) {
            toast("Slot must be exactly 30 minutes");
            return;
        }

        // block past times
        Calendar startCal = (Calendar) pickedDay.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startMin / 60);
        startCal.set(Calendar.MINUTE, startMin % 60);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        if (startCal.getTimeInMillis() <= System.currentTimeMillis()) {
            toast("Start time must be in the future");
            return;
        }

        String tutorId = AuthIdProvider.requireCurrentUserId();
        String dateStr = tvDate.getText().toString().trim(); // yyyy-MM-dd
        String startStr = String.format(Locale.US, "%02d:%02d", startMin / 60, startMin % 60);
        boolean requiresApproval = !swAutoApprove.isChecked(); // switch text = “auto approve”

        // repository computes end = start + 30 and writes it
        repo.createAvailabilitySlot(
                tutorId,
                dateStr,
                startStr,
                requiresApproval,
                new TutorRepository.SlotCallback() {
                    @Override
                    public void onSuccess(AvailabilitySlot s) {
                        toast("Slot created");
                        navigateBackToTutorHome();
                    }

                    @Override
                    public void onError(String msg) {
                        toast(msg == null ? "Failed to create slot" : msg);
                    }
                }
        );
    }

    private void navigateBackToTutorHome() {
        Intent up = new Intent(this, TutorHomeActivity.class);
        up.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(up);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateBackToTutorHome();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
