package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Availability tab: shows the tutor’s availability slots.
 * Uses fragment_simple_list.xml (progress + empty + RecyclerView).
 * - Call refresh() to reload.
 * - TODO: Replace fake data in loadSlots() with real repo.getAvailabilitySlots(...).
 */
public class AvailabilityFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    /** Factory so the pager can pass tutorId */
    public static AvailabilityFragment newInstance(@NonNull String tutorId) {
        AvailabilityFragment f = new AvailabilityFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    // State
    private String tutorId;

    // Views
    private View progress;
    private View empty;
    private RecyclerView list;

    // Adapter
    private SlotAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty()) {
            throw new IllegalStateException("AvailabilityFragment requires a tutorId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) {
            ((TextView) empty).setText(R.string.empty_availability);
        }
        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SlotAdapter();
        list.setAdapter(adapter);

        loadSlots(); // initial load (fake for now)

        return v;
    }

    /** Public method the Activity can call after creating/deleting a slot */
    public void refresh() {
        loadSlots();
    }

    /** Replace this with your real repository call later */
    private void loadSlots() {
        showLoading(true);
        // TODO: swap this fake data with:
        // repo.getAvailabilitySlots(tutorId, new TutorRepository.SlotsListCallback() { ... });

        // ---- Fake demo data so UI is visible now ----
        list.postDelayed(() -> {
            List<SlotRow> demo = new ArrayList<>();
            demo.add(new SlotRow("2025-11-15", "14:00", "14:30", true, false));
            demo.add(new SlotRow("2025-11-16", "10:30", "11:00", false, true));
            bindSlots(demo);
            showLoading(false);
        }, 300);
        // ---------------------------------------------
    }

    private void bindSlots(@NonNull List<SlotRow> rows) {
        if (rows.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.submitList(rows);
        }
    }

    private void showError(@NonNull String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
        }
    }

    // --------- Simple view model for a row (keeps UI independent of repo model) ----------
    static class SlotRow {
        final String date;          // "YYYY-MM-DD"
        final String start;         // "HH:mm"
        final String end;           // "HH:mm"
        final boolean requiresApproval;
        final boolean booked;

        SlotRow(String date, String start, String end, boolean requiresApproval, boolean booked) {
            this.date = date;
            this.start = start;
            this.end = end;
            this.requiresApproval = requiresApproval;
            this.booked = booked;
        }
    }

    // ---------------------- RecyclerView ListAdapter ----------------------
    private static class SlotAdapter extends ListAdapter<SlotRow, SlotVH> {
        protected SlotAdapter() {
            super(new DiffUtil.ItemCallback<SlotRow>() {
                @Override public boolean areItemsTheSame(@NonNull SlotRow a, @NonNull SlotRow b) {
                    // no ids in demo → use composite key
                    return a.date.equals(b.date) && a.start.equals(b.start);
                }
                @Override public boolean areContentsTheSame(@NonNull SlotRow a, @NonNull SlotRow b) {
                    return a.end.equals(b.end)
                            && a.requiresApproval == b.requiresApproval
                            && a.booked == b.booked;
                }
            });
        }

        @NonNull @Override
        public SlotVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Use built-in 2-line layout to avoid creating a new XML file.
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new SlotVH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull SlotVH h, int position) {
            SlotRow s = getItem(position);
            String title = s.date + " • " + s.start + "–" + s.end;
            String badge = s.booked ? "Booked"
                    : (s.requiresApproval ? "Manual approval" : "Auto");
            h.title.setText(title);
            h.subtitle.setText(badge);
        }
    }

    private static class SlotVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        SlotVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
