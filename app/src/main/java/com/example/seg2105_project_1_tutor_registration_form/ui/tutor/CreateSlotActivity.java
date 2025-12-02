package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
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
    private Button btnPickDate, btnPickStart, btnPickEnd, btnSave, btnBack;

    // pickedDay = calendar date only; startMin / endMin = minutes from midnight (24h)
    private final Calendar pickedDay = Calendar.getInstance();
    private int startMin = -1, endMin = -1;

    private TutorRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_slot);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("+ Add Slot");
        }

        repo = new FirestoreTutorRepository();

        tvDate = findViewById(R.id.tvDate);
        tvStart = findViewById(R.id.tvStart);
        tvEnd   = findViewById(R.id.tvEnd);
        swAutoApprove = findViewById(R.id.swAutoApprove);
        btnPickDate  = findViewById(R.id.btnPickDate);
        btnPickStart = findViewById(R.id.btnPickStart);
        btnPickEnd   = findViewById(R.id.btnPickEnd);
        btnSave      = findViewById(R.id.btnSave);
        btnBack      = findViewById(R.id.btnBack);

        // Default behaviour: MANUAL APPROVAL (switch OFF)
        // Switch text in XML should be something like "Auto-approve"
        // OFF  => requiresApproval = true  (go through RequestsFragment)
        // ON   => requiresApproval = false (instant approve)
        swAutoApprove.setChecked(false);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickStart.setOnClickListener(v -> showTimePickerForStart());

        // End time is auto-computed as start + 30 minutes
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
     * Time picker: 12-hour UI with AM/PM, but we store minutes from midnight (24h)
     * and still enforce :00 / :30 only.
     */
    private void showTimePickerForStart() {
        TimePickerDialog tpd = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    if (minute % 30 != 0) {
                        toast("Pick :00 or :30 only");
                        return;
                    }

                    // store in minutes from midnight (24h)
                    startMin = hourOfDay * 60 + minute;

                    // compute 12-hour display string for start
                    String startDisplay = format12Hour(hourOfDay, minute);
                    tvStart.setText(startDisplay);

                    // auto compute end = start + 30 minutes (still in 24h)
                    endMin = startMin + 30;
                    int endHour24 = endMin / 60;
                    int endMinute = endMin % 60;

                    String endDisplay = format12Hour(endHour24, endMinute);
                    tvEnd.setText(endDisplay);
                },
                9,
                0,
                false // 12-hour clock with AM/PM
        );
        tpd.show();
    }

    /** Convert 24-hour hour+minute into a "hh:mm AM/PM" string for display. */
    private String format12Hour(int hour24, int minute) {
        String ampm = (hour24 >= 12) ? "PM" : "AM";
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, ampm);
    }

    private void saveSlot() {
        if (tvDate.getText().length() == 0 || startMin < 0 || endMin < 0) {
            toast("Pick date and start time");
            return;
        }
        if (endMin - startMin != 30) {
            toast("Slot must be exactly 30 minutes");
            return;
        }

        // Block slots in the past for the chosen date
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
        String dateStr = tvDate.getText().toString(); // yyyy-MM-dd

        // Store 24-hour "HH:mm" for the backend (all existing code expects this)
        String startStr = String.format(
                Locale.US,
                "%02d:%02d",
                startMin / 60,
                startMin % 60
        );

        // switch label = "Auto-approve"
        // ON  => requiresApproval = false (instant booking)
        // OFF => requiresApproval = true  (manual approval)
        boolean requiresApproval = !swAutoApprove.isChecked();

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
                        toast(msg);
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
