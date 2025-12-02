package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StudentSessionsFragment - Shows the student's sessions list.
 *
 * Features:
 * - List of all sessions (pending, approved, rejected, completed)
 * - Sorted by date (most recent first)
 * - Status indicator pills
 * - Cancel pending sessions
 * - Cancel approved sessions (if > 24h before start)
 * - Rate tutors for completed sessions
 */
public class StudentSessionsFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "student_id";

    public static StudentSessionsFragment newInstance(@NonNull String studentId) {
        StudentSessionsFragment f = new StudentSessionsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    public interface Host {
        void onSessionUpdated();
    }

    // Data class for student sessions
    public static class StudentSessionItem {
        public String id;
        public String tutorId;
        public String tutorName;
        public String slotId;
        public String date;
        public String startTime;
        public String endTime;
        public String subject;
        public String status;
        public long requestedAtMillis;
        public int myRating;
    }

    private Host host;
    private String studentId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private View progress, empty;
    private RecyclerView list;
    private SessionsAdapter adapter;

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof Host) host = (Host) ctx;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        host = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        studentId = (args != null) ? args.getString(ARG_STUDENT_ID) : null;
        if (studentId == null) studentId = "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) {
            ((TextView) empty).setText(R.string.empty_student_sessions);
        }

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionsAdapter();
        list.setAdapter(adapter);

        loadSessions();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSessions();
    }

    public void refresh() {
        loadSessions();
    }

    // ========== FIRESTORE SESSION LOADING ==========

    private void loadSessions() {
        if (studentId.isEmpty()) {
            showLoading(false);
            showEmpty(true);
            return;
        }

        showLoading(true);

        // Get all tutors to query their requests/sessions
        db.collection("users").whereEqualTo("role", "TUTOR").get()
                .addOnSuccessListener(tutorSnap -> {
                    List<DocumentSnapshot> tutorDocs = new ArrayList<>(tutorSnap.getDocuments());

                    // Also try lowercase role
                    db.collection("users").whereEqualTo("role", "tutor").get()
                            .addOnSuccessListener(tutorSnap2 -> {
                                for (DocumentSnapshot d : tutorSnap2.getDocuments()) {
                                    if (!containsDoc(tutorDocs, d.getId())) {
                                        tutorDocs.add(d);
                                    }
                                }
                                fetchSessionsFromTutors(tutorDocs);
                            })
                            .addOnFailureListener(e -> fetchSessionsFromTutors(tutorDocs));
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmpty(true);
                    toast("Failed to load sessions");
                });
    }

    private boolean containsDoc(List<DocumentSnapshot> docs, String id) {
        for (DocumentSnapshot d : docs) {
            if (d.getId().equals(id)) return true;
        }
        return false;
    }

    private void fetchSessionsFromTutors(List<DocumentSnapshot> tutorDocs) {
        if (tutorDocs.isEmpty()) {
            showLoading(false);
            showEmpty(true);
            adapter.setItems(new ArrayList<>());
            return;
        }

        List<StudentSessionItem> allSessions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(tutorDocs.size() * 2);

        for (DocumentSnapshot tutorDoc : tutorDocs) {
            String tutorId = tutorDoc.getId();
            String tutorName = getTutorDisplayName(tutorDoc);

            // Fetch requests for this student
            db.collection("users").document(tutorId).collection("sessionRequests")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnSuccessListener(reqSnap -> {
                        for (DocumentSnapshot d : reqSnap.getDocuments()) {
                            StudentSessionItem item = new StudentSessionItem();
                            item.id = d.getId();
                            item.tutorId = tutorId;
                            item.tutorName = tutorName;
                            item.slotId = nz(d.getString("slotId"));
                            item.date = nz(d.getString("date"));
                            item.startTime = nz(d.getString("startTime"));
                            item.endTime = nz(d.getString("endTime"));
                            item.subject = nz(d.getString("subject"));
                            item.status = nz(d.getString("status")).toUpperCase();

                            Timestamp ts = d.getTimestamp("requestedAtMillis");
                            if (ts != null) {
                                item.requestedAtMillis = ts.toDate().getTime();
                            } else {
                                Long ms = d.getLong("requestedAtMillis");
                                item.requestedAtMillis = ms != null ? ms : 0L;
                            }

                            if ("PENDING".equals(item.status) || "REJECTED".equals(item.status) || "CANCELLED".equals(item.status)) {
                                allSessions.add(item);
                            }
                        }
                        checkComplete(remaining, allSessions);
                    })
                    .addOnFailureListener(e -> checkComplete(remaining, allSessions));

            // Fetch sessions for this student
            db.collection("users").document(tutorId).collection("sessions")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnSuccessListener(sessSnap -> {
                        for (DocumentSnapshot d : sessSnap.getDocuments()) {
                            StudentSessionItem item = new StudentSessionItem();
                            item.id = d.getId();
                            item.tutorId = tutorId;
                            item.tutorName = tutorName;
                            item.slotId = nz(d.getString("slotId"));
                            item.date = nz(d.getString("date"));
                            item.startTime = nz(d.getString("startTime"));
                            item.endTime = nz(d.getString("endTime"));
                            item.subject = nz(d.getString("subject"));

                            String status = nz(d.getString("status")).toUpperCase();
                            long startMs = toMillisSafe(item.date, item.startTime);
                            if ("APPROVED".equals(status) && startMs < System.currentTimeMillis()) {
                                item.status = "COMPLETED";
                            } else {
                                item.status = status;
                            }

                            Number rating = d.getLong("studentRating");
                            item.myRating = rating != null ? rating.intValue() : 0;
                            item.requestedAtMillis = startMs;

                            allSessions.add(item);
                        }
                        checkComplete(remaining, allSessions);
                    })
                    .addOnFailureListener(e -> checkComplete(remaining, allSessions));
        }
    }

    private void checkComplete(AtomicInteger remaining, List<StudentSessionItem> items) {
        if (remaining.decrementAndGet() == 0) {
            List<StudentSessionItem> sorted = new ArrayList<>(items);
            Collections.sort(sorted, (a, b) -> {
                long aMs = toMillisSafe(a.date, a.startTime);
                long bMs = toMillisSafe(b.date, b.startTime);
                return Long.compare(bMs, aMs);
            });

            showLoading(false);
            if (sorted.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
            }
            adapter.setItems(sorted);
        }
    }

    // ========== CANCEL OPERATIONS ==========

    private void cancelPendingRequest(StudentSessionItem item) {
        DocumentReference reqRef = db.collection("users").document(item.tutorId)
                .collection("sessionRequests").document(item.id);

        reqRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                toast("Request not found");
                return;
            }
            String status = nz(doc.getString("status")).toUpperCase();
            if (!"PENDING".equals(status)) {
                toast("Can only cancel pending requests");
                return;
            }

            reqRef.update("status", "CANCELLED")
                    .addOnSuccessListener(v -> {
                        toast("Session cancelled");
                        refresh();
                        if (host != null) host.onSessionUpdated();
                    })
                    .addOnFailureListener(e ->
                            toast("Failed to cancel: " + (e.getMessage() == null ? "" : e.getMessage())));
        }).addOnFailureListener(e ->
                toast("Failed to cancel: " + (e.getMessage() == null ? "" : e.getMessage())));
    }

    /**
     * Cancel an APPROVED session if and only if it is more than 24 hours in the future.
     * Path: users/{tutorId}/sessions/{sessionId}
     */
    private void cancelApprovedSession(StudentSessionItem item) {
        DocumentReference sessionRef = db.collection("users").document(item.tutorId)
                .collection("sessions").document(item.id);

        sessionRef.get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        toast("Session not found");
                        return;
                    }

                    String date = nz(doc.getString("date"));
                    String startTime = nz(doc.getString("startTime"));
                    long sessionStart = toMillisSafe(date, startTime);
                    long now = System.currentTimeMillis();
                    long hoursUntilStart = (sessionStart - now) / (1000 * 60 * 60);

                    // 24-hour rule
                    if (hoursUntilStart < 24) {
                        toast("Cannot cancel sessions starting within 24 hours");
                        return;
                    }

                    // 1) Mark the session as CANCELLED
                    sessionRef.update("status", "CANCELLED")
                            .addOnSuccessListener(v -> {
                                // 2) Free the slot if we know which one it is
                                if (item.slotId != null && !item.slotId.isEmpty()) {
                                    DocumentReference slotRef = db.collection("users")
                                            .document(item.tutorId)
                                            .collection("availabilitySlots")
                                            .document(item.slotId);

                                    slotRef.update("booked", false);
                                }

                                toast("Session cancelled");
                                refresh();
                                if (host != null) host.onSessionUpdated();
                            })
                            .addOnFailureListener(e ->
                                    toast("Failed to cancel: " + (e.getMessage() == null ? "" : e.getMessage())));
                })
                .addOnFailureListener(e ->
                        toast("Failed to cancel: " + (e.getMessage() == null ? "" : e.getMessage())));
    }

    // ========== RATING ==========

    private void rateTutor(StudentSessionItem item, int rating) {
        if (rating < 1 || rating > 5) {
            toast("Rating must be between 1 and 5");
            return;
        }

        DocumentReference sessionRef = db.collection("users").document(item.tutorId)
                .collection("sessions").document(item.id);
        DocumentReference tutorRef = db.collection("users").document(item.tutorId);
        DocumentReference ratingRef = db.collection("users").document(item.tutorId)
                .collection("ratings").document(studentId + "_" + item.id);

        db.runTransaction(trx -> {
            DocumentSnapshot sessionSnap = trx.get(sessionRef);
            if (!sessionSnap.exists()) throw new IllegalStateException("Session not found");

            String date = nz(sessionSnap.getString("date"));
            String startTime = nz(sessionSnap.getString("startTime"));
            long sessionStart = toMillisSafe(date, startTime);

            if (sessionStart > System.currentTimeMillis()) {
                throw new IllegalStateException("Cannot rate future sessions");
            }

            DocumentSnapshot existingRating = trx.get(ratingRef);
            boolean isNewRating = !existingRating.exists();
            int oldRating = 0;
            if (!isNewRating) {
                Long old = existingRating.getLong("rating");
                oldRating = old != null ? old.intValue() : 0;
            }

            DocumentSnapshot tutorSnap = trx.get(tutorRef);
            double currentAvg = 0;
            long currentCount = 0;
            if (tutorSnap.exists()) {
                Double avg = tutorSnap.getDouble("averageRating");
                Long cnt = tutorSnap.getLong("ratingCount");
                currentAvg = avg != null ? avg : 0;
                currentCount = cnt != null ? cnt : 0;
            }

            double newAvg;
            long newCount;
            if (isNewRating) {
                newCount = currentCount + 1;
                newAvg = ((currentAvg * currentCount) + rating) / newCount;
            } else {
                newCount = currentCount;
                double totalWithoutOld = (currentAvg * currentCount) - oldRating;
                newAvg = (totalWithoutOld + rating) / newCount;
            }

            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("studentId", studentId);
            ratingData.put("sessionId", item.id);
            ratingData.put("rating", rating);
            ratingData.put("timestamp", Timestamp.now());
            trx.set(ratingRef, ratingData);

            trx.update(sessionRef, "studentRating", rating);

            Map<String, Object> tutorUpdate = new HashMap<>();
            tutorUpdate.put("averageRating", newAvg);
            tutorUpdate.put("ratingCount", newCount);
            trx.update(tutorRef, tutorUpdate);

            return null;
        }).addOnSuccessListener(v -> {
            toast("Rating saved!");
            item.myRating = rating;
        }).addOnFailureListener(e -> {
            toast(e.getMessage() != null ? e.getMessage() : "Failed to save rating");
            refresh();
        });
    }

    // ========== UI HELPERS ==========

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean isEmpty) {
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String getTutorDisplayName(DocumentSnapshot doc) {
        String first = nz(doc.getString("firstName"));
        String last = nz(doc.getString("lastName"));
        String name = (first + " " + last).trim();
        if (name.isEmpty()) name = nz(doc.getString("email"));
        return name.isEmpty() ? "Tutor" : name;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static long toMillisSafe(String yMd, String hm) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse((yMd == null ? "" : yMd) + " " + (hm == null ? "" : hm))
                    .getTime();
        } catch (Exception e) { return 0L; }
    }

    private void showCancelDialog(StudentSessionItem item, boolean isPending) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Session")
                .setMessage("Are you sure you want to cancel this session?")
                .setPositiveButton("Yes, Cancel", (d, w) -> {
                    if (isPending) {
                        cancelPendingRequest(item);
                    } else {
                        cancelApprovedSession(item);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ========== ADAPTER ==========

    private class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH> {

        private List<StudentSessionItem> items = new ArrayList<>();

        void setItems(List<StudentSessionItem> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_session_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            StudentSessionItem item = items.get(position);

            String when = item.date + " • " + item.startTime;
            if (item.endTime != null && !item.endTime.isEmpty()) {
                when += "–" + item.endTime;
            }
            h.tvWhen.setText(when);

            h.tvTutor.setText("with " + (item.tutorName.isEmpty() ? "Tutor" : item.tutorName));

            if (item.subject != null && !item.subject.isEmpty()) {
                h.tvSubject.setText(item.subject);
                h.tvSubject.setVisibility(View.VISIBLE);
            } else {
                h.tvSubject.setVisibility(View.GONE);
            }

            bindStatusPill(h.tvStatus, item.status);

            if ("COMPLETED".equals(item.status)) {
                h.ratingLayout.setVisibility(View.VISIBLE);
                h.ratingBar.setRating(item.myRating);
                h.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                    if (fromUser && rating > 0) {
                        rateTutor(item, (int) rating);
                    }
                });
            } else {
                h.ratingLayout.setVisibility(View.GONE);
            }

            boolean canCancel = false;
            if ("PENDING".equals(item.status)) {
                canCancel = true;
                h.btnCancel.setOnClickListener(v -> showCancelDialog(item, true));
            } else if ("APPROVED".equals(item.status)) {
                long startMs = toMillisSafe(item.date, item.startTime);
                long hoursUntil = (startMs - System.currentTimeMillis()) / (1000 * 60 * 60);
                if (hoursUntil >= 24) {
                    canCancel = true;
                    h.btnCancel.setOnClickListener(v -> showCancelDialog(item, false));
                }
            }
            h.btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvWhen, tvTutor, tvSubject, tvStatus;
            View ratingLayout;
            RatingBar ratingBar;
            Button btnCancel;

            VH(@NonNull View v) {
                super(v);
                tvWhen = v.findViewById(R.id.tvWhen);
                tvTutor = v.findViewById(R.id.tvTutor);
                tvSubject = v.findViewById(R.id.tvSubject);
                tvStatus = v.findViewById(R.id.tvStatus);
                ratingLayout = v.findViewById(R.id.ratingLayout);
                ratingBar = v.findViewById(R.id.ratingBar);
                btnCancel = v.findViewById(R.id.btnCancel);
            }
        }
    }

    private void bindStatusPill(TextView tv, String status) {
        tv.setText(status);
        int bgRes;
        int textColor = 0xFFFFFFFF;

        switch (status) {
            case "PENDING":
                bgRes = R.drawable.pill_pending;
                textColor = 0xFF333333;
                break;
            case "APPROVED":
                bgRes = R.drawable.pill_status_approved;
                break;
            case "REJECTED":
            case "CANCELLED":
                bgRes = R.drawable.pill_status_rejected;
                break;
            case "COMPLETED":
                bgRes = R.drawable.bg_badge_mint;
                break;
            default:
                bgRes = R.drawable.bg_badge_neutral;
                break;
        }

        tv.setBackgroundResource(bgRes);
        tv.setTextColor(textColor);
    }
}
