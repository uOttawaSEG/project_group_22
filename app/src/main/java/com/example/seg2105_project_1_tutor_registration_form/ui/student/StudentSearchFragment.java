package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StudentSearchFragment - Search for available tutoring sessions by course.
 *
 * Features:
 * - Enter course code to search
 * - View available slots with tutor name and average rating
 * - Request a session (removed from results after requesting)
 * - Prevents booking conflicting time slots
 */
public class StudentSearchFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "student_id";

    public static StudentSearchFragment newInstance(@NonNull String studentId) {
        StudentSearchFragment f = new StudentSearchFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    public interface Host {
        void onSessionUpdated();
    }

    // Data class for available slots
    public static class AvailableSlotItem {
        public String slotId;
        public String tutorId;
        public String tutorName;
        public String tutorEmail;
        public float tutorAverageRating;
        public int tutorRatingCount;
        public String date;
        public String startTime;
        public String endTime;
        public String subject;
        public boolean requiresApproval;
    }

    private Host host;
    private String studentId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText etSearch;
    private Button btnSearch;
    private View progress;
    private TextView empty;
    private RecyclerView list;
    private SlotsAdapter adapter;

    private Set<String> requestedSlotIds = new HashSet<>();

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
        View v = inflater.inflate(R.layout.fragment_student_search, container, false);

        etSearch = v.findViewById(R.id.etSearch);
        btnSearch = v.findViewById(R.id.btnSearch);
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        list = v.findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SlotsAdapter();
        list.setAdapter(adapter);

        btnSearch.setOnClickListener(btn -> performSearch());

        etSearch.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        empty.setText(R.string.search_hint);
        empty.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);

        return v;
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            toast("Please enter a course code");
            return;
        }

        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        requestedSlotIds.clear();
        showLoading(true);
        searchSlotsByCourse(query);
    }

    // ========== FIRESTORE SEARCH LOGIC ==========

    private void searchSlotsByCourse(String courseCode) {
        String searchCode = courseCode.trim().toUpperCase();

        getStudentBookedTimes(bookedTimes -> {
            db.collection("users").get().addOnSuccessListener(allUsers -> {
                List<DocumentSnapshot> matchingTutors = new ArrayList<>();

                for (DocumentSnapshot doc : allUsers.getDocuments()) {
                    String role = nz(doc.getString("role"));
                    if (!"TUTOR".equalsIgnoreCase(role)) continue;

                    boolean matches = false;

                    // Check 'coursesOffered' field (List format)
                    List<String> coursesList = (List<String>) doc.get("coursesOffered");
                    if (coursesList != null) {
                        for (String c : coursesList) {
                            if (c != null && c.toUpperCase().contains(searchCode)) {
                                matches = true;
                                break;
                            }
                        }
                    }

                    // Also check 'coursesCsv' field (String format)
                    if (!matches) {
                        String coursesCsv = nz(doc.getString("coursesCsv"));
                        if (coursesCsv.toUpperCase().contains(searchCode)) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        matchingTutors.add(doc);
                    }
                }

                if (matchingTutors.isEmpty()) {
                    showLoading(false);
                    empty.setText(R.string.no_slots_found);
                    showEmpty(true);
                    adapter.setItems(new ArrayList<>());
                    return;
                }

                fetchAvailableSlots(matchingTutors, bookedTimes);
            }).addOnFailureListener(e -> {
                showLoading(false);
                empty.setText("Search failed");
                showEmpty(true);
            });
        });
    }

    private void fetchAvailableSlots(List<DocumentSnapshot> tutorDocs, Set<String> bookedTimes) {
        List<AvailableSlotItem> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(tutorDocs.size());
        long now = System.currentTimeMillis();

        for (DocumentSnapshot tutorDoc : tutorDocs) {
            String tutorId = tutorDoc.getId();
            String tutorName = getTutorDisplayName(tutorDoc);
            String tutorEmail = nz(tutorDoc.getString("email"));

            Double avgRating = tutorDoc.getDouble("averageRating");
            Long ratingCount = tutorDoc.getLong("ratingCount");
            float rating = avgRating != null ? avgRating.floatValue() : 0f;
            int count = ratingCount != null ? ratingCount.intValue() : 0;

            db.collection("users").document(tutorId).collection("availabilitySlots")
                    .whereEqualTo("booked", false)
                    .get()
                    .addOnSuccessListener(slotSnap -> {
                        for (DocumentSnapshot d : slotSnap.getDocuments()) {
                            String date = nz(d.getString("date"));
                            String startTime = nz(d.getString("startTime"));
                            if (startTime.isEmpty()) startTime = nz(d.getString("start"));
                            String endTime = nz(d.getString("endTime"));

                            long slotStart = toMillisSafe(date, startTime);
                            if (slotStart <= now) continue;

                            String timeKey = date + "_" + startTime;
                            if (bookedTimes.contains(timeKey)) continue;

                            AvailableSlotItem item = new AvailableSlotItem();
                            item.slotId = d.getId();
                            item.tutorId = tutorId;
                            item.tutorName = tutorName;
                            item.tutorEmail = tutorEmail;
                            item.tutorAverageRating = rating;
                            item.tutorRatingCount = count;
                            item.date = date;
                            item.startTime = startTime;
                            item.endTime = endTime;
                            item.subject = nz(d.getString("subject"));

                            Boolean reqApproval = d.getBoolean("requiresApproval");
                            item.requiresApproval = reqApproval != null && reqApproval;

                            results.add(item);
                        }

                        if (remaining.decrementAndGet() == 0) {
                            List<AvailableSlotItem> sorted = new ArrayList<>(results);
                            Collections.sort(sorted, Comparator
                                    .comparing((AvailableSlotItem s) -> s.date)
                                    .thenComparing(s -> s.startTime));

                            showLoading(false);
                            if (sorted.isEmpty()) {
                                empty.setText(R.string.no_slots_found);
                                showEmpty(true);
                            } else {
                                showEmpty(false);
                            }
                            adapter.setItems(sorted);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            showLoading(false);
                            adapter.setItems(new ArrayList<>(results));
                        }
                    });
        }
    }

    private interface BookedTimesCallback {
        void onResult(Set<String> bookedTimes);
    }

    private void getStudentBookedTimes(BookedTimesCallback cb) {
        Set<String> bookedTimes = Collections.synchronizedSet(new HashSet<>());

        db.collection("users").get().addOnSuccessListener(allUsers -> {
            List<DocumentSnapshot> tutorDocs = new ArrayList<>();
            for (DocumentSnapshot d : allUsers.getDocuments()) {
                String role = nz(d.getString("role"));
                if ("TUTOR".equalsIgnoreCase(role)) tutorDocs.add(d);
            }

            if (tutorDocs.isEmpty()) {
                cb.onResult(bookedTimes);
                return;
            }

            AtomicInteger remaining = new AtomicInteger(tutorDocs.size() * 2);

            for (DocumentSnapshot tutorDoc : tutorDocs) {
                String tutorId = tutorDoc.getId();

                db.collection("users").document(tutorId).collection("sessionRequests")
                        .whereEqualTo("studentId", studentId)
                        .get()
                        .addOnSuccessListener(snap -> {
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                String status = nz(d.getString("status")).toUpperCase();
                                if ("PENDING".equals(status) || "APPROVED".equals(status)) {
                                    String date = nz(d.getString("date"));
                                    String start = nz(d.getString("startTime"));
                                    if (!date.isEmpty() && !start.isEmpty()) {
                                        bookedTimes.add(date + "_" + start);
                                    }
                                }
                            }
                            if (remaining.decrementAndGet() == 0) cb.onResult(bookedTimes);
                        })
                        .addOnFailureListener(e -> {
                            if (remaining.decrementAndGet() == 0) cb.onResult(bookedTimes);
                        });

                db.collection("users").document(tutorId).collection("sessions")
                        .whereEqualTo("studentId", studentId)
                        .get()
                        .addOnSuccessListener(snap -> {
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                String status = nz(d.getString("status")).toUpperCase();
                                if (!"CANCELLED".equals(status)) {
                                    String date = nz(d.getString("date"));
                                    String start = nz(d.getString("startTime"));
                                    if (!date.isEmpty() && !start.isEmpty()) {
                                        bookedTimes.add(date + "_" + start);
                                    }
                                }
                            }
                            if (remaining.decrementAndGet() == 0) cb.onResult(bookedTimes);
                        })
                        .addOnFailureListener(e -> {
                            if (remaining.decrementAndGet() == 0) cb.onResult(bookedTimes);
                        });
            }
        }).addOnFailureListener(e -> cb.onResult(bookedTimes));
    }

    private void bookSession(AvailableSlotItem item, SlotsAdapter.VH h) {
        h.btnRequest.setEnabled(false);
        h.btnRequest.setText("Requesting...");

        DocumentReference slotRef = db.collection("users").document(item.tutorId)
                .collection("availabilitySlots").document(item.slotId);

        slotRef.get().addOnSuccessListener(slotDoc -> {
            if (!slotDoc.exists()) {
                toast("Slot not found");
                h.btnRequest.setEnabled(true);
                h.btnRequest.setText("Request Session");
                return;
            }

            Boolean booked = slotDoc.getBoolean("booked");
            if (booked != null && booked) {
                toast("Slot is already booked");
                h.btnRequest.setEnabled(true);
                h.btnRequest.setText("Request Session");
                return;
            }

            DocumentReference reqRef = db.collection("users").document(item.tutorId)
                    .collection("sessionRequests").document();

            db.runTransaction(trx -> {
                DocumentSnapshot slot = trx.get(slotRef);
                if (!slot.exists()) throw new IllegalStateException("Slot missing");

                Boolean isBooked = slot.getBoolean("booked");
                if (isBooked != null && isBooked) {
                    throw new IllegalStateException("Slot already booked");
                }

                Boolean requiresApproval = slot.getBoolean("requiresApproval");
                boolean needsApproval = requiresApproval != null && requiresApproval;

                Map<String, Object> req = new HashMap<>();
                req.put("id", reqRef.getId());
                req.put("tutorId", item.tutorId);
                req.put("studentId", studentId);
                req.put("slotId", item.slotId);
                req.put("date", item.date);
                req.put("startTime", item.startTime);
                req.put("endTime", item.endTime);
                req.put("requestedAtMillis", Timestamp.now());

                if (needsApproval) {
                    req.put("status", "PENDING");
                    trx.set(reqRef, req);
                } else {
                    req.put("status", "APPROVED");

                    DocumentReference sessionRef = db.collection("users").document(item.tutorId)
                            .collection("sessions").document();

                    Map<String, Object> session = new HashMap<>();
                    session.put("id", sessionRef.getId());
                    session.put("slotId", item.slotId);
                    session.put("studentId", studentId);
                    session.put("tutorId", item.tutorId);
                    session.put("status", "APPROVED");
                    session.put("date", item.date);
                    session.put("startTime", item.startTime);
                    session.put("endTime", item.endTime);

                    trx.set(reqRef, req);
                    trx.set(sessionRef, session);
                    trx.update(slotRef, "booked", true);
                }
                return null;
            }).addOnSuccessListener(v -> {
                toast("Session requested!");
                requestedSlotIds.add(item.slotId);
                adapter.removeItem(item.slotId);
                if (host != null) host.onSessionUpdated();
            }).addOnFailureListener(e -> {
                toast(e.getMessage() != null ? e.getMessage() : "Request failed");
                h.btnRequest.setEnabled(true);
                h.btnRequest.setText("Request Session");
            });
        }).addOnFailureListener(e -> {
            toast("Request failed");
            h.btnRequest.setEnabled(true);
            h.btnRequest.setText("Request Session");
        });
    }

    // ========== UI HELPERS ==========

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!loading);
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

    // ========== ADAPTER ==========

    private class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.VH> {

        private List<AvailableSlotItem> items = new ArrayList<>();

        void setItems(List<AvailableSlotItem> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        void removeItem(String slotId) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).slotId.equals(slotId)) {
                    items.remove(i);
                    notifyItemRemoved(i);
                    if (items.isEmpty()) showEmpty(true);
                    break;
                }
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_available_slot_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AvailableSlotItem item = items.get(position);

            String when = item.date + " • " + item.startTime;
            if (item.endTime != null && !item.endTime.isEmpty()) {
                when += "–" + item.endTime;
            }
            h.tvWhen.setText(when);
            h.tvTutorName.setText(item.tutorName);

            if (item.tutorRatingCount > 0) {
                String ratingText = String.format(Locale.getDefault(), "%.1f ★ (%d)",
                        item.tutorAverageRating, item.tutorRatingCount);
                h.tvRating.setText(ratingText);
            } else {
                h.tvRating.setText("No ratings yet");
            }
            h.tvRating.setVisibility(View.VISIBLE);

            h.tvMode.setText(item.requiresApproval ? "Requires approval" : "Auto-approve");

            h.btnRequest.setEnabled(true);
            h.btnRequest.setText("Request Session");
            h.btnRequest.setOnClickListener(v -> bookSession(item, h));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvWhen, tvTutorName, tvRating, tvMode;
            Button btnRequest;

            VH(@NonNull View v) {
                super(v);
                tvWhen = v.findViewById(R.id.tvWhen);
                tvTutorName = v.findViewById(R.id.tvTutorName);
                tvRating = v.findViewById(R.id.tvRating);
                tvMode = v.findViewById(R.id.tvMode);
                btnRequest = v.findViewById(R.id.btnRequest);
            }
        }
    }
}
