package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentMySessionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SessionAdapter adapter;
    private List<Session> sessionList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_my_sessions); // you'll make this simple RecyclerView layout
        recyclerView = findViewById(R.id.recyclerViewSessions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        currentUserId = AuthIdProvider.getCurrentUserId();

        adapter = new SessionAdapter(sessionList, this::onCancelClicked);
        recyclerView.setAdapter(adapter);

        loadSessions();
    }

    private void loadSessions() {
        CollectionReference sessionsRef = db.collection("sessions");
        sessionsRef.whereEqualTo("studentId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessionList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Session session = doc.toObject(Session.class);
                        if (session != null) {
                            sessionList.add(session);
                        }
                    }
                    sortByDateDescending();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
    }

    private void sortByDateDescending() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Collections.sort(sessionList, (a, b) -> {
            try {
                Date d1 = sdf.parse(a.getDate() + " " + a.getStartTime());
                Date d2 = sdf.parse(b.getDate() + " " + b.getStartTime());
                return d2.compareTo(d1);
            } catch (Exception e) {
                return 0;
            }
        });
    }

    // --- Cancel Logic ---
    private void onCancelClicked(Session session) {
        String status = session.getStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            cancelSession(session);
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            if (canCancel(session)) {
                cancelSession(session);
            } else {
                Toast.makeText(this,
                        "Cannot cancel within 24 hours of the session start time.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "You cannot cancel this session.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canCancel(Session session) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date sessionDate = sdf.parse(session.getDate() + " " + session.getStartTime());
            if (sessionDate == null) return false;

            long now = System.currentTimeMillis();
            long diff = sessionDate.getTime() - now;
            return diff > 24 * 60 * 60 * 1000; // 24 hours
        } catch (ParseException e) {
            return false;
        }
    }

    private void cancelSession(Session session) {
        db.collection("sessions").document(session.getId())
                .update("status", "CANCELLED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Session cancelled", Toast.LENGTH_SHORT).show();
                    loadSessions();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel session", Toast.LENGTH_SHORT).show());
    }
}
