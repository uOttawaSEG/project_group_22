package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Locale;

public class CreateSlotActivity extends AppCompatActivity {

    private TextView tvDate;
    private SwitchMaterial swAutoApprove;
    private Button btnPickDate, btnSave;
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
        swAutoApprove = findViewById(R.id.swAutoApprove);

        btnPickDate   = findViewById(R.id.btnPickDate);
        btnSave       = findViewById(R.id.btnSave);

        etStartTime   = findViewById(R.id.etStartTime);
        spStartAmPm   = findViewById(R.id.spStartAmPm);

        // Populate AM/PM dropdown
        if (spStartAmPm != null) {
            ArrayAdapter<String> ampmAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"AM", "PM"}
            );
            ampmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spStartAmPm.setAdapter(ampmAdapter);
        }

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveSlot());
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
     * Reads the typed time (HH:MM, 12-hour) + AM/PM from the UI, validates, and
     * populates startMin/endMin. Returns true if valid.
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
            toast("Use HH:MM format (e.g., 2:00)");
            return false;
        }

        int hour12, minute;
        try {
            hour12 = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            toast("Invalid time. Use numbers like 2:00 or 11:30.");
            return false;
        }

        // 12-hour validation
        if (hour12 < 1 || hour12 > 12) {
            toast("Hour must be between 1 and 12");
            return false;
        }

        if (minute != 0 && minute != 30) {
            toast("Pick :00 or :30 only");
            return false;
        }

        // Read AM/PM from spinner (default AM)
        String ampm = "AM";
        if (spStartAmPm != null && spStartAmPm.getSelectedItem() != null) {
            ampm = spStartAmPm.getSelectedItem().toString();
        }

        // Convert 12-hour -> 24-hour
        int hour24 = hour12;
        if ("PM".equalsIgnoreCase(ampm) && hour12 < 12) {
            hour24 = hour12 + 12;        // 1–11 PM → 13–23
        } else if ("AM".equalsIgnoreCase(ampm) && hour12 == 12) {
            hour24 = 0;                  // 12 AM → 00
        }

        startMin = hour24 * 60 + minute;
        endMin   = startMin + 30;

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

        // Switch label is "Manual approval" → checked = manual approval required
        boolean requiresApproval = swAutoApprove.isChecked();

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
