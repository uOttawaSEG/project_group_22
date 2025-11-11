package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TutorAvailabilityActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView empty, title;
    private View progress;

    private String tutorId, tutorName;
    private SlotAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_availability);

        tutorId   = getIntent().getStringExtra("tutorId");
        tutorName = getIntent().getStringExtra("tutorName");

        title = findViewById(R.id.title);
        if (tutorName != null && !tutorName.isEmpty()) {
            title.setText("Availability for " + tutorName);
        }

        recyclerView = findViewById(R.id.recycler);
        progress     = findViewById(R.id.progress);
        empty        = findViewById(R.id.empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadSlots();
    }

    private void loadSlots() {
        if (tutorId == null || tutorId.isEmpty()) {
            Toast.makeText(this, "Missing tutor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        FirebaseFirestore.getInstance()
                .collection("users").document(tutorId)
                .collection("availabilitySlots")
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    List<AvailabilitySlot> list = new ArrayList<>();
                    long now = System.currentTimeMillis();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        AvailabilitySlot s = d.toObject(AvailabilitySlot.class);
                        if (s == null) continue;

                        // Normalize/defend nulls
                        String date  = nz(s.getDate());
                        String start = nz(s.getStartTime());
                        if (start.isEmpty()) start = nz(d.getString("start")); // fallback if model uses 'start'

                        boolean isBooked = s.isBooked(); // default false if boolean primitive; guard if Boolean

                        long when = parseMillis(date, start);
                        if (!isBooked && when >= now) {
                            // Ensure endTime is present on UI
                            if (s.getEndTime() == null) {
                                s.setEndTime(nz(d.getString("endTime")));
                            }
                            list.add(s);
                        }
                    }

                    // sort by date then time
                    Collections.sort(list, Comparator
                            .comparing(AvailabilitySlot::getDate, String::compareTo)
                            .thenComparing(AvailabilitySlot::getStartTime, String::compareTo));

                    adapter.setSlots(list);
                    showLoading(false);

                    if (list.isEmpty()) {
                        empty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        empty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    empty.setText("Failed to load slots");
                    empty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static long parseMillis(String yMd, String hm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse(yMd + " " + hm).getTime();
        } catch (Exception e) { return 0L; }
    }

    /* ------------------ Adapter ------------------ */
    static class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.VH> {
        private List<AvailabilitySlot> slots;

        SlotAdapter(List<AvailabilitySlot> slots) {
            this.slots = slots;
        }

        void setSlots(List<AvailabilitySlot> newSlots) {
            this.slots = newSlots;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AvailabilitySlot s = slots.get(position);
            String end = s.getEndTime() == null ? "" : s.getEndTime();
            holder.title.setText(s.getDate() + " • " + s.getStartTime() + (end.isEmpty() ? "" : ("–" + end)));
            holder.subtitle.setText(s.isBooked() ? "Booked" : "Available");
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(android.R.id.text1);
                subtitle = v.findViewById(android.R.id.text2);
            }
        }
    }
}
