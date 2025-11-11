package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;

import java.util.Calendar;

public class CreateSlotActivity extends AppCompatActivity {

    private TextView tvDate, tvStart, tvEnd;
    private Switch swAutoApprove;
    private Button btnPickDate, btnPickStart, btnPickEnd, btnSave;

    private final Calendar pickedDay = Calendar.getInstance();
    private int startMin = -1, endMin = -1;

    private final TutorRepository repo = new FirestoreTutorRepository();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_slot);

        tvDate = findViewById(R.id.tvDate);
        tvStart = findViewById(R.id.tvStart);
        tvEnd   = findViewById(R.id.tvEnd);
        swAutoApprove = findViewById(R.id.swAutoApprove);
        btnPickDate  = findViewById(R.id.btnPickDate);
        btnPickStart = findViewById(R.id.btnPickStart);
        btnPickEnd   = findViewById(R.id.btnPickEnd);
        btnSave      = findViewById(R.id.btnSave);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickStart.setOnClickListener(v -> showTimePicker(true));
        btnPickEnd.setOnClickListener(v -> showTimePicker(false));
        btnSave.setOnClickListener(v -> saveSlot());
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (d, y, m, day) -> {
            pickedDay.set(y, m, day, 0, 0, 0);
            tvDate.setText(String.format("%04d-%02d-%02d", y, m + 1, day));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStart) {
        new TimePickerDialog(this, (view, h, m) -> {
            if (m % 30 != 0) { toast("Pick :00 or :30 only"); return; }
            int mins = h * 60 + m;
            if (isStart) { startMin = mins; tvStart.setText(fmt(h, m)); }
            else { endMin = mins; tvEnd.setText(fmt(h, m)); }
        }, 9, 0, true).show();
    }

    private void saveSlot() {
        String tutorId = AuthIdProvider.getCurrentUserId();
        if (tutorId == null || tutorId.trim().isEmpty()) { toast("Not signed in"); return; }
        if (tvDate.getText().length() == 0 || startMin < 0 || endMin < 0) { toast("Pick date, start, end"); return; }
        if (endMin - startMin != 30) { toast("Slot must be exactly 30 minutes"); return; }

        Calendar startCal = (Calendar) pickedDay.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startMin / 60);
        startCal.set(Calendar.MINUTE, startMin % 60);
        startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
        long startMillis = startCal.getTimeInMillis();
        if (startMillis <= System.currentTimeMillis()) { toast("Start must be in the future"); return; }

        AvailabilitySlot s = new AvailabilitySlot();
        s.setTutorId(tutorId);
        s.setDate(tvDate.getText().toString());
        s.setStartTime(fmt(startMin / 60, startMin % 60));
        s.setEndTime(fmt(endMin / 60, endMin % 60));
        s.setRequiresApproval(!swAutoApprove.isChecked());  // switch is "Auto approve?"
        s.setBooked(false);
        s.setStatus("OPEN");
        s.setCreatedAt(System.currentTimeMillis());
        s.setStartMillis(startMillis);

        repo.createAvailabilitySlot(tutorId, s, new TutorRepository.SimpleCallback() {
            @Override public void onSuccess() { toast("Slot created"); finish(); }
            @Override public void onError(@NonNull String msg) { toast("Save failed: " + msg); }
        });
    }

    private static String fmt(int h, int m) {
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
