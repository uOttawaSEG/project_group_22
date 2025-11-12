package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/*
* TutorAvailabilityActivity (Student -> View tutor availability)
*
* PURPOSE
*   Include a tutor's header: Name • Degree • Email.
* *- List only FUTURE, UNBOOKED 30-min slots.*
* – The logged-in student may request a session.
In this regard, the signed-in
*
* FLOW
* onCreate(): Retrieve tutorId/name, initialize views, RecyclerView, and execute loadTutorHeader() + loadSlots().
*   loadTutorHeader() -> users/{tutorId} -> first/last
* – `loadSlots()`: users
*      · Map to AvailabilitySlot, accept legacy “start”/“endTime”.
*      · Keep if !booked and start >= now; sort by date, then by start.
*      · Bind to adapter, toggle empty/progress.
* Adapter: displays “YYYY-MM-DD • HH:mm–HH:mm” and mode of approval; button press:
*   repo.submitSessionRequest(tutorId, studentId, slotId, cb), which is disabled during
*
* DATA / LAYOUTS
* – Firestore: users/{tutorId},
*   (date,.startTime, end.time, requires, .booked)
* - Layouts: activity_tutor_availability
*
* NOTES
* – Button text: “Requesting…”, if sending, “Requested” if successful, “re-enable” if an error occurs.
* - Extend by filtering (subject/location) or paging if lists grow.
*/


public class TutorAvailabilityActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView empty, title, headerNameDegree, headerEmail;
    private View progress;

    String tutorId, tutorName; // package-private so inner adapter can read
    private SlotAdapter adapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TutorRepository repo = new FirestoreTutorRepository();

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

        headerNameDegree = findViewById(R.id.header_name_degree);
        headerEmail      = findViewById(R.id.header_email);

        recyclerView = findViewById(R.id.recycler);
        progress     = findViewById(R.id.progress);
        empty        = findViewById(R.id.empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadTutorHeader();  // header: name · degree · email
        loadSlots();        // list + request button
    }

    /** Loads the header (name · degree · email) */
    private void loadTutorHeader() {
        if (tutorId == null) return;
        db.collection("users").document(tutorId).get()
                .addOnSuccessListener(doc -> {
                    String first  = nz(doc.getString("firstName"));
                    String last   = nz(doc.getString("lastName"));
                    String degree = nz(doc.getString("degree"));   // adjust key if different in your schema
                    String email  = nz(doc.getString("email"));

                    String name = (first + " " + last).trim();
                    if (name.isEmpty()) name = tutorName != null ? tutorName : "(Tutor)";

                    headerNameDegree.setText(degree.isEmpty() ? name : (name + " · " + degree));
                    headerEmail.setText(email);
                });
    }

    /** Loads future, unbooked slots and binds to list */
    private void loadSlots() {
        if (tutorId == null || tutorId.isEmpty()) {
            Toast.makeText(this, "Missing tutor", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        db.collection("users").document(tutorId)
                .collection("availabilitySlots")
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    List<SlotVM> list = new ArrayList<>();
                    long now = System.currentTimeMillis();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        AvailabilitySlot s = d.toObject(AvailabilitySlot.class);
                        if (s == null) continue;

                        String date  = nz(s.getDate());
                        String start = nz(s.getStartTime());
                        if (start.isEmpty()) start = nz(d.getString("start"));  // fallback if older field name
                        String end   = s.getEndTime() == null ? nz(d.getString("endTime")) : s.getEndTime();

                        boolean booked = s.isBooked();
                        Boolean reqAppr = d.getBoolean("requiresApproval");
                        boolean requiresApproval = reqAppr != null && reqAppr;

                        long when = parseMillis(date, start);
                        if (!booked && when >= now) {
                            list.add(new SlotVM(d.getId(), date, start, end, requiresApproval));
                        }
                    }

                    // Sort by date then start time
                    Collections.sort(list, Comparator
                            .comparing((SlotVM vm) -> vm.date)
                            .thenComparing(vm -> vm.start));

                    adapter.setSlots(list);
                    showLoading(false);

                    boolean isEmpty = list.isEmpty();
                    empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

    /** Lightweight view model that includes the Firestore doc id */
    static class SlotVM {
        final String id, date, start, end;
        final boolean requiresApproval;
        SlotVM(String id, String date, String start, String end, boolean requiresApproval) {
            this.id = id; this.date = date; this.start = start; this.end = end; this.requiresApproval = requiresApproval;
        }
    }

    /** Adapter that inflates our custom row WITH the "Request session" button */
    class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.VH> {
        private List<SlotVM> slots;
        SlotAdapter(List<SlotVM> slots) { this.slots = slots; }
        void setSlots(List<SlotVM> newSlots) { this.slots = newSlots; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_slot_row, parent, false); // <-- custom row (title, subtitle, button)
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SlotVM s = slots.get(pos);
            h.title.setText(s.date + " • " + s.start + (s.end.isEmpty() ? "" : ("–" + s.end)));
            h.subtitle.setText(s.requiresApproval ? "manual approval · open" : "auto-approve · open");

            h.btn.setEnabled(true);
            h.btn.setText("Request session");
            h.btn.setOnClickListener(v -> {
                String studentId = FirebaseAuth.getInstance().getUid();
                if (studentId == null) {
                    Toast.makeText(TutorAvailabilityActivity.this, "Please sign in again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                h.btn.setEnabled(false);
                h.btn.setText("Requesting…");

                repo.submitSessionRequest(
                        tutorId, studentId, s.id,
                        new TutorRepository.RequestCreateCallback() {
                            @Override public void onSuccess(String requestId) {
                                Toast.makeText(TutorAvailabilityActivity.this, "Request sent", Toast.LENGTH_SHORT).show();
                                h.btn.setText("Requested");
                            }
                            @Override public void onError(String msg) {
                                Toast.makeText(TutorAvailabilityActivity.this, msg, Toast.LENGTH_SHORT).show();
                                h.btn.setEnabled(true);
                                h.btn.setText("Request session");
                            }
                        }
                );
            });
        }

        @Override public int getItemCount() { return slots.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle; Button btn;
            VH(@NonNull View v) {
                super(v);
                title    = v.findViewById(R.id.slot_title);
                subtitle = v.findViewById(R.id.slot_subtitle);
                btn      = v.findViewById(R.id.btn_request);
            }
        }
    }
}
