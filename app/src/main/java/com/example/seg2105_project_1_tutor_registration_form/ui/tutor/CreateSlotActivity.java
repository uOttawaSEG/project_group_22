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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateSlotActivity extends AppCompatActivity {

    private TextView tvDate, tvStart, tvEnd;
    private Switch swAutoApprove;
    private Button btnPickDate, btnPickStart, btnPickEnd, btnSave;

    private final Calendar pickedDay = Calendar.getInstance(); // chosen date
    private int startMin = -1, endMin = -1; // minutes since midnight

    @Override protected void onCreate(Bundle savedInstanceState) {
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
            pickedDay.set(Calendar.YEAR, y);
            pickedDay.set(Calendar.MONTH, m);
            pickedDay.set(Calendar.DAY_OF_MONTH, day);
            tvDate.setText(String.format("%04d-%02d-%02d", y, m + 1, day));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStart) {
        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            if (m % 30 != 0) { toast("Pick :00 or :30 only"); return; } // 30-min increments
            int mins = h * 60 + m;
            if (isStart) { startMin = mins; tvStart.setText(String.format("%02d:%02d", h, m)); }
            else { endMin = mins; tvEnd.setText(String.format("%02d:%02d", h, m)); }
        }, 9, 0, true);
        tpd.show();
    }

    private void saveSlot() {
        if (tvDate.getText().length() == 0 || startMin < 0 || endMin < 0) {
            toast("Pick date, start and end");
            return;
        }
        if (endMin - startMin != 30) { // exactly 30 minutes
            toast("Slot must be exactly 30 minutes");
            return;
        }

        // Build absolute start time (millis) and block past times
        Calendar startCal = (Calendar) pickedDay.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startMin / 60);
        startCal.set(Calendar.MINUTE, startMin % 60);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startMillis = startCal.getTimeInMillis();
        if (startMillis <= System.currentTimeMillis()) {
            toast("Start time must be in the future");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dayKey = tvDate.getText().toString(); // "YYYY-MM-DD"

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tutors").document(uid)
                .collection("availabilitySlots")
                .whereEqualTo("date", dayKey)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Long sL = d.getLong("startMin");
                        Long eL = d.getLong("endMin");
                        if (sL == null || eL == null) continue;
                        int s = sL.intValue();
                        int e = eL.intValue();
                        if (startMin < e && endMin > s) {
                            toast("Overlaps an existing slot");
                            return;
                        }
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("date", dayKey);
                    data.put("startMin", startMin);
                    data.put("endMin", endMin);
                    data.put("startMillis", startMillis);
                    data.put("manualApproval", !swAutoApprove.isChecked()); // false = auto-approve
                    data.put("status", "OPEN");
                    data.put("createdAt", System.currentTimeMillis());

                    db.collection("tutors").document(uid)
                            .collection("availabilitySlots")
                            .add(data)
                            .addOnSuccessListener(r -> { toast("Slot created"); finish(); })
                            .addOnFailureListener(e -> toast("Save failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("Check failed: " + e.getMessage()));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
