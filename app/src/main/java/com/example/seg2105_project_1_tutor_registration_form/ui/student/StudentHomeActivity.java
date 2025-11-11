package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StudentHomeActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ProgressBar progress;
    private View empty;
    private TutorSectionAdapter adapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TutorRepository repo = new FirestoreTutorRepository();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.student_content, new StudentTutorListFragment())
                    .commitNow();
        }

        setTitle(R.string.student_home_title);

        rv = findViewById(R.id.rvTutors);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TutorSectionAdapter(
                this::toggleTutorExpanded,
                this::onSlotClicked
        );
        rv.setAdapter(adapter);

        // lightweight progress/empty — re-use headerCard’s area if you want a spinner elsewhere
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        empty = new TextView(this);
        ((TextView) empty).setText(R.string.empty_tutor_list);
        empty.setVisibility(View.GONE);

        load();
    }

    private void load() {
        showLoading(true);

        // 1) Find all OPEN, unbooked slots across tutors
        db.collectionGroup("availabilitySlots")
                .whereEqualTo("status", "OPEN")
                .whereEqualTo("booked", false)
                .get()
                .addOnSuccessListener(this::onSlotsSnapshot)
                .addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Failed to load: " + e.getMessage());
                    adapter.submitList(Collections.emptyList());
                    showEmpty(true);
                });
    }

    private void onSlotsSnapshot(QuerySnapshot q) {
        // Group slots by tutorId
        Map<String, List<AvailabilitySlot>> byTutor = new LinkedHashMap<>();
        for (DocumentSnapshot d : q.getDocuments()) {
            AvailabilitySlot s = d.toObject(AvailabilitySlot.class);
            if (s == null) continue;
            if (s.getSlotId() == null || s.getSlotId().isEmpty()) s.setSlotId(d.getId());
            String tid = s.getTutorId();
            if (tid == null || tid.isEmpty()) continue;
            byTutor.computeIfAbsent(tid, k -> new ArrayList<>()).add(s);
        }

        if (byTutor.isEmpty()) {
            showLoading(false);
            adapter.submitList(Collections.emptyList());
            showEmpty(true);
            return;
        }

        // 2) Fetch tutor names in a small fan-out
        db.collection("users")
                .whereIn("uid", new ArrayList<>(byTutor.keySet()))
                .get()
                .addOnSuccessListener(usersSnap -> {
                    Map<String,String> tutorName = new HashMap<>();
                    for (DocumentSnapshot d : usersSnap.getDocuments()) {
                        String uid = value(d.getString("uid"));
                        String fn  = value(d.getString("firstName"));
                        String ln  = value(d.getString("lastName"));
                        String nm  = (fn + " " + ln).trim();
                        tutorName.put(uid, nm.isEmpty()? "Tutor "+uid.substring(0, Math.min(6, uid.length())) : nm);
                    }

                    // 3) Build rows (sections + children) with initial collapsed state
                    List<Row> rows = new ArrayList<>();
                    for (Map.Entry<String,List<AvailabilitySlot>> e : byTutor.entrySet()) {
                        String tid = e.getKey();
                        String name = tutorName.getOrDefault(tid, "Tutor");
                        rows.add(Row.tutorHeader(tid, name, e.getValue().size(), false));
                        // children are added dynamically on expand; keep adapter simple by adding now but hidden by state
                        for (AvailabilitySlot s : e.getValue()) {
                            rows.add(Row.slot(tid, s));
                        }
                    }

                    // Start collapsed: adapter will filter non-expanded children in bind()
                    adapter.setFlatRows(rows);
                    adapter.buildVisibleRows();
                    rv.scheduleLayoutAnimation();

                    showLoading(false);
                    showEmpty(adapter.getItemCount() == 0);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Failed to load tutors: " + e.getMessage());
                    adapter.submitList(Collections.emptyList());
                    showEmpty(true);
                });
    }

    private void toggleTutorExpanded(@NonNull String tutorId) {
        adapter.toggleSection(tutorId);
    }

    private void onSlotClicked(@NonNull AvailabilitySlot slot) {
        String studentId = AuthIdProvider.getCurrentUserId();
        if (studentId == null || studentId.isEmpty()) {
            toast("Not signed in.");
            return;
        }

        boolean requiresApproval = slot.isRequiresApproval();
        // Call your repo to create a request or auto-approve book
        repo.requestSession(
                slot.getTutorId(),
                studentId,
                slot.getSlotId(),
                requiresApproval,
                new TutorRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        toast(requiresApproval ? "Request sent." : "Session booked!");
                        // refresh to remove that slot
                        load();
                    }
                    @Override public void onError(String msg) {
                        toast(msg == null ? "Action failed." : msg);
                    }
                }
        );
    }

    private void showLoading(boolean show) {
        // If you have a spinner in the layout, wire it here.
        // For now this is a no-op so the UI doesn’t jump.
    }

    private void showEmpty(boolean show) {
        // If you prefer an inline “empty” TextView in XML, show/hide it here.
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private static String value(String s) { return s == null ? "" : s; }

    // --------------------------------------------------------------------
    // Sectioned Adapter (Tutor headers + Slot rows in a single RecyclerView)
    // --------------------------------------------------------------------

    /** Row model with two types: header (tutor) or item (slot). */
    static class Row {
        static final int TUTOR = 0;
        static final int SLOT  = 1;

        final int type;
        final String tutorId;

        // header
        final String tutorName;
        final int slotCount;
        boolean expanded;

        // slot
        final AvailabilitySlot slot;

        private Row(int type, String tutorId, String tutorName, int slotCount, boolean expanded, AvailabilitySlot slot) {
            this.type = type;
            this.tutorId = tutorId;
            this.tutorName = tutorName;
            this.slotCount = slotCount;
            this.expanded = expanded;
            this.slot = slot;
        }
        static Row tutorHeader(String tutorId, String tutorName, int count, boolean expanded) {
            return new Row(TUTOR, tutorId, tutorName, count, expanded, null);
        }
        static Row slot(String tutorId, AvailabilitySlot s) {
            return new Row(SLOT, tutorId, null, 0, false, s);
        }
    }

    /** Builds the visible list based on which sections are expanded. */
    static class TutorSectionAdapter extends ListAdapter<Row, RecyclerView.ViewHolder> {

        interface OnTutorClick { void onTutor(String tutorId); }
        interface OnSlotClick  { void onSlot(AvailabilitySlot slot); }

        private static final int LAYOUT_HEADER = android.R.layout.simple_list_item_2;
        private static final int LAYOUT_SLOT   = R.layout.item_pending_request_expandable; // reuse a 2-line row if you want

        private final OnTutorClick onTutorClick;
        private final OnSlotClick onSlotClick;

        // The full, flat list (headers followed by their children)
        private List<Row> flatRows = new ArrayList<>();
        // The actually visible rows
        private final List<Row> visible = new ArrayList<>();

        TutorSectionAdapter(OnTutorClick onTutorClick, OnSlotClick onSlotClick) {
            super(new DiffUtil.ItemCallback<Row>() {
                @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                    if (a.type != b.type) return false;
                    if (a.type == Row.TUTOR) return a.tutorId.equals(b.tutorId);
                    return a.slot != null && b.slot != null && a.slot.getSlotId().equals(b.slot.getSlotId());
                }
                @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    if (a.type != b.type) return false;
                    if (a.type == Row.TUTOR) return a.expanded == b.expanded && a.slotCount == b.slotCount && a.tutorName.equals(b.tutorName);
                    AvailabilitySlot x = a.slot, y = b.slot;
                    return x != null && y != null &&
                            eq(x.getDate(), y.getDate()) &&
                            eq(x.getStartTime(), y.getStartTime()) &&
                            eq(x.getEndTime(), y.getEndTime()) &&
                            x.isRequiresApproval() == y.isRequiresApproval();
                }
                private boolean eq(String s1, String s2) { return s1 == null ? s2 == null : s1.equals(s2); }
            });
            this.onTutorClick = onTutorClick;
            this.onSlotClick = onSlotClick;
        }

        void setFlatRows(List<Row> rows) {
            this.flatRows = rows == null ? new ArrayList<>() : rows;
        }

        /** Recompute visible list from flatRows and submit. */
        void buildVisibleRows() {
            visible.clear();
            boolean include = false;
            for (int i = 0; i < flatRows.size(); i++) {
                Row r = flatRows.get(i);
                if (r.type == Row.TUTOR) {
                    visible.add(r);
                    include = r.expanded;
                } else {
                    if (include) visible.add(r);
                }
                // If next is a header, close section
                if (r.type == Row.SLOT &&
                        (i + 1 >= flatRows.size() || flatRows.get(i + 1).type == Row.TUTOR)) {
                    include = false;
                }
            }
            submitList(new ArrayList<>(visible));
        }

        void toggleSection(String tutorId) {
            // flip the header’s expanded and rebuild visible rows
            for (Row r : flatRows) {
                if (r.type == Row.TUTOR && r.tutorId.equals(tutorId)) {
                    r.expanded = !r.expanded;
                }
            }
            buildVisibleRows();
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Row.TUTOR) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                return new TutorVH(v);
            } else {
                // You can use simple_list_item_2 as well; using a custom layout if you want a button.
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                return new SlotVH(v);
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
            Row r = getItem(position);
            if (r.type == Row.TUTOR) {
                TutorVH vh = (TutorVH) h;
                vh.title.setText(r.tutorName);
                vh.subtitle.setText(r.slotCount + " open slot" + (r.slotCount == 1 ? "" : "s") + (r.expanded ? " • tap to collapse" : " • tap to expand"));
                vh.itemView.setOnClickListener(v -> onTutorClick.onTutor(r.tutorId));
            } else {
                SlotVH vh = (SlotVH) h;
                AvailabilitySlot s = r.slot;
                String title = value(s.getDate()) + " • " + value(s.getStartTime()) + "–" + value(s.getEndTime());
                vh.title.setText(title);
                vh.subtitle.setText(s.isRequiresApproval() ? "Manual approval" : "Auto-approve");
                vh.itemView.setOnClickListener(v -> onSlotClick.onSlot(s));
            }
        }

        static class TutorVH extends RecyclerView.ViewHolder {
            final TextView title, subtitle;
            TutorVH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }

        static class SlotVH extends RecyclerView.ViewHolder {
            final TextView title, subtitle;
            SlotVH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}

