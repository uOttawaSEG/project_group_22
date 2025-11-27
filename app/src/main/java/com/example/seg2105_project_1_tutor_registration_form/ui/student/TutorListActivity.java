package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.content.Intent;  // <-- add this import
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/*
 *  Students can browse tutors who have at least one FUTURE, unbooked * availability slot on this screen. After requesting users with the role "TUTOR" from Firestore (with a * fallback for mixed casing/alternate field names), we search * users/{tutorId}/availabilitySlots for each tutor to see if there are any open slots that haven't been reserved in * the past. A RecyclerView displays matching tutors; tapping a card brings up * TutorAvailabilityActivity with the tutorId and tutorName.
 * Defensive/safe options: * - Manages the role fields "role" or "userRole" as well as any variations of "tutor." Either "startTime" or "start" in slot documents are accepted. AtomicInteger + synchronized list is used to coordinate async fetches. Tutors are sorted by display name; the empty state is hidden until loading is complete.
 */

public class TutorListActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView empty;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<TutorRow> rows = Collections.synchronizedList(new ArrayList<>());
    private TutorAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_list);

        recycler = findViewById(R.id.tutor_list);   // matches your XML
        progress = findViewById(R.id.progress);
        empty    = findViewById(R.id.empty);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        // ENABLE the click: open TutorAvailabilityActivity with tutorId + tutorName
        adapter = new TutorAdapter(rows, row -> {
            Intent i = new Intent(
                    TutorListActivity.this,
                    com.example.seg2105_project_1_tutor_registration_form.ui.student.TutorAvailabilityActivity.class
            );
            i.putExtra("tutorId", row.getTutorId());
            i.putExtra("tutorName", row.getDisplay());
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        loadTutors();
    }

    private void loadTutors() {
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);

        // 1) Try fast path: role == "TUTOR"
        db.collection("users")
                .whereEqualTo("role", "TUTOR")
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (!userSnap.isEmpty()) {
                        fetchTutorsWithSlots(userSnap.getDocuments());
                    } else {
                        // 2) Fallback: pull all users and filter role case-insensitively (handles "Tutor"/"tutor")
                        db.collection("users").get()
                                .addOnSuccessListener(allSnap -> {
                                    List<DocumentSnapshot> tutors = new ArrayList<>();
                                    for (DocumentSnapshot d : allSnap.getDocuments()) {
                                        String role = safeString(d.getString("role"));
                                        String role2 = safeString(d.getString("userRole")); // optional alt field
                                        if ("tutor".equalsIgnoreCase(role) || "tutor".equalsIgnoreCase(role2)) {
                                            tutors.add(d);
                                        }
                                    }
                                    fetchTutorsWithSlots(tutors);
                                })
                                .addOnFailureListener(e -> onAllTutorsChecked(rows));
                    }
                })
                .addOnFailureListener(e -> onAllTutorsChecked(rows));
    }

    private void fetchTutorsWithSlots(List<DocumentSnapshot> tutorDocs) {
        if (tutorDocs == null || tutorDocs.isEmpty()) {
            onAllTutorsChecked(rows);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(tutorDocs.size());
        long now = System.currentTimeMillis();

        for (DocumentSnapshot doc : tutorDocs) {
            // Prefer explicit uid field, fallback to doc id
            String docUid = safeString(doc.getString("uid"));
            if (docUid.isEmpty()) docUid = doc.getId();

            String email   = safeString(doc.getString("email"));
            String first   = safeString(doc.getString("firstName"));
            String last    = safeString(doc.getString("lastName"));
            String display = (first + " " + last).trim();
            if (display.isEmpty()) display = email;
            final String fTutorId = docUid;
            final String fDisplay = display;
            final String fEmail   = email;

            // Pull rating fields from the tutor doc
            Double avgObj = doc.getDouble("averageRating");
            double avg = (avgObj == null) ? 0.0 : avgObj;
            Long countLong = doc.getLong("ratingCount");
            int count = (countLong == null) ? 0 : countLong.intValue();
            final double fAvg = avg;
            final int fCount = count;

            db.collection("users").document(fTutorId)
                    .collection("availabilitySlots")        // MUST match where you WRITE slots
                    .get()
                    .addOnSuccessListener(slotSnap -> {
                        boolean hasFuture = false;

                        for (DocumentSnapshot s : slotSnap.getDocuments()) {
                            Boolean booked = s.getBoolean("booked");
                            boolean isBooked = booked != null && booked;

                            String date  = safeString(s.getString("date"));          // "yyyy-MM-dd"
                            String start = safeString(s.getString("startTime"));
                            if (start.isEmpty()) start = safeString(s.getString("start")); // fallback

                            long when = parseMillis(date, start);
                            if (!isBooked && when >= now) {
                                hasFuture = true;
                                break;
                            }
                        }

                        if (hasFuture) {
                            synchronized (rows) {
                                rows.add(new TutorRow(fTutorId, fDisplay, fEmail, fAvg, fCount));
                            }
                        }

                        if (remaining.decrementAndGet() == 0) onAllTutorsChecked(rows);
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) onAllTutorsChecked(rows);
                    });
        }
    }


    private void onAllTutorsChecked(List<TutorRow> data) {
        synchronized (rows) {
            rows.sort(Comparator.comparing(TutorRow::getDisplay, String::compareToIgnoreCase));
        }
        runOnUiThread(() -> {
            progress.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private static long parseMillis(String yMd, String hm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse(yMd + " " + hm).getTime();
        } catch (Exception e) { return 0L; }
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    /** Row model used by the adapter */
    public static class TutorRow {
        private final String tutorId;
        private final String display;
        private final String email;
        private final double averageRating;
        private final int ratingsCount;

        public TutorRow(String tutorId, String display, String email,
                        double averageRating, int ratingsCount) {
            this.tutorId = tutorId;
            this.display = display;
            this.email = email;
            this.averageRating = averageRating;
            this.ratingsCount = ratingsCount;
        }
        public String getTutorId() { return tutorId; }
        public String getDisplay() { return display; }
        public String getEmail()   { return email; }
        public double getAverageRating() { return averageRating; }
        public int getRatingsCount()     { return ratingsCount; }
    }
}
