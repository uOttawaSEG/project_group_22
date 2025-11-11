package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lists a tutor’s OPEN 30-minute slots for the student to book. */
public class StudentOpenSlotsFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static StudentOpenSlotsFragment newInstance(@NonNull String tutorId) {
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        StudentOpenSlotsFragment f = new StudentOpenSlotsFragment();
        f.setArguments(b);
        return f;
    }

    // Repo
    private final TutorRepository repo = new FirestoreTutorRepository();

    // State
    private String tutorId;

    // Views
    private View progress, empty;
    private RecyclerView list;
    private SlotsAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty()) {
            throw new IllegalStateException("StudentOpenSlotsFragment requires tutorId");
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) {
            ((TextView) empty).setText(R.string.empty_open_slots_student);
        }

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SlotsAdapter(this::confirmAndBook);
        list.setAdapter(adapter);

        load();
        return v;
    }

    /** Public refresh if host wants to force reload. */
    public void refresh() { load(); }

    // -------------------- Data --------------------
    private void load() {
        showLoading(true);

        repo.getOpenSlots(tutorId, new TutorRepository.SlotsListCallback() {
            @Override public void onSuccess(List<AvailabilitySlot> slots) {
                // Keep only clearly open ones (defensive; repo should already filter)
                List<Row> rows = new ArrayList<>();
                for (AvailabilitySlot s : slots) {
                    if (!"OPEN".equalsIgnoreCase(s.getStatus())) continue;
                    if (Boolean.TRUE.equals(s.isBooked())) continue;

                    String date = safe(s.getDate());
                    String start = safe(s.getStartTime());
                    String end = safe(s.getEndTime());
                    boolean requiresApproval = s.isRequiresApproval();  // <-- fixed
                    String slotId = safe(s.getSlotId());

                    rows.add(new Row(slotId, date, start, end, requiresApproval));
                }

                // Sort by date/time (“YYYY-MM-DD” + “HH:mm” sorts lexicographically)
                Collections.sort(rows, (a, b) -> {
                    int d = a.date.compareTo(b.date);
                    if (d != 0) return d;
                    return a.start.compareTo(b.start);
                });

                bind(rows);
                showLoading(false);
            }

            @Override public void onError(String msg) {
                showLoading(false);
                toast(msg == null ? "Failed to load slots" : msg);
                bind(Collections.emptyList());
            }
        });
    }

    private void bind(@NonNull List<Row> rows) {
        if (rows.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.submitList(rows);
        }
    }

    private void confirmAndBook(@NonNull Row row) {
        // TODO: show a confirm dialog then call repo.requestSession(...)
        // For now:
        toast("Book slot: " + row.date + " • " + row.start + "–" + row.end);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) { empty.setVisibility(View.GONE); list.setVisibility(View.GONE); }
    }

    private void toast(String msg) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }
    private static String safe(String s) { return s == null ? "" : s; }

    // -------------------- Row / Adapter --------------------
    static class Row {
        final String slotId;
        final String date, start, end;
        final boolean requiresApproval;
        Row(String slotId, String date, String start, String end, boolean requiresApproval) {
            this.slotId = slotId; this.date = date; this.start = start; this.end = end; this.requiresApproval = requiresApproval;
        }
    }

    private static class SlotsAdapter extends ListAdapter<Row, VH> {
        interface OnClick { void onClick(Row row); }
        private final OnClick onClick;

        SlotsAdapter(OnClick onClick) {
            super(new DiffUtil.ItemCallback<Row>() {
                @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) { return a.slotId.equals(b.slotId); }
                @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    return a.date.equals(b.date) && a.start.equals(b.start) && a.end.equals(b.end) && a.requiresApproval == b.requiresApproval;
                }
            });
            this.onClick = onClick;
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(row);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Row r = getItem(position);
            h.title.setText(r.date + " • " + r.start + "–" + r.end);
            h.subtitle.setText(r.requiresApproval ? "Manual approval" : "Auto-approve");
            h.itemView.setOnClickListener(v -> onClick.onClick(r));
        }
    }

    private static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}

