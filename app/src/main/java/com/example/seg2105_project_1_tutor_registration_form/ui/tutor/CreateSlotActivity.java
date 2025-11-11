package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;

import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;

import java.util.Calendar;
import java.util.Locale;

public class CreateSlotActivity extends AppCompatActivity {

    private TextView tvDate, tvStart, tvEnd;
    private Switch swAutoApprove;
    private Button btnPickDate, btnPickStart, btnPickEnd, btnSave;

    private final Calendar pickedDay = Calendar.getInstance();
    private int startMin = -1, endMin = -1;

    private TutorRepository repo;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_slot);

        repo = new FirestoreTutorRepository();

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
            pickedDay.set(Calendar.YEAR, y);
            pickedDay.set(Calendar.MONTH, m);
            pickedDay.set(Calendar.DAY_OF_MONTH, day);
            tvDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStart) {
        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            if (m % 30 != 0) { toast("Pick :00 or :30 only"); return; }
            int mins = h * 60 + m;
            if (isStart) { startMin = mins; tvStart.setText(String.format(Locale.US,"%02d:%02d", h, m)); }
            else { endMin = mins; tvEnd.setText(String.format(Locale.US,"%02d:%02d", h, m)); }
        }, 9, 0, true);
        tpd.show();
    }

    private void saveSlot() {
        if (tvDate.getText().length() == 0 || startMin < 0 || endMin < 0) {
            toast("Pick date, start and end");
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
        String dateStr = tvDate.getText().toString(); // yyyy-MM-dd
        String startStr = String.format(Locale.US, "%02d:%02d", startMin/60, startMin%60);
        boolean requiresApproval = !swAutoApprove.isChecked(); // switch = "auto approve"

        // delegate creation + endTime computation to the repository
        repo.createAvailabilitySlot(tutorId, dateStr, startStr, requiresApproval,
                new TutorRepository.SlotCallback() {
                    @Override public void onSuccess(AvailabilitySlot s) {
                        toast("Slot created");
                        finish(); // returning to fragment triggers refresh()
                    }
                    @Override public void onError(String msg) {
                        toast(msg);
                    }
                });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
