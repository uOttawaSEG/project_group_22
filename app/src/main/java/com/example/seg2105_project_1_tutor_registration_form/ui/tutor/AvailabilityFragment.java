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
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;

import java.util.ArrayList;
import java.util.List;

public class AvailabilityFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static AvailabilityFragment newInstance(@NonNull String tutorId) {
        AvailabilityFragment f = new AvailabilityFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    private String tutorId;
    private View progress, empty;
    private RecyclerView list;
    private SlotAdapter adapter;
    private final TutorRepository repo = new FirestoreTutorRepository();

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty()) throw new IllegalStateException("AvailabilityFragment requires tutorId");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_availability);

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SlotAdapter();
        list.setAdapter(adapter);

        loadSlots();
        return v;
    }

    public void refresh() { loadSlots(); }

    private void loadSlots() {
        showLoading(true);
        repo.getAvailabilitySlots(tutorId, new TutorRepository.SlotsListCallback() {
            @Override public void onSuccess(@NonNull List<AvailabilitySlot> slots) {
                List<SlotRow> rows = new ArrayList<>();
                for (AvailabilitySlot s : slots) {
                    rows.add(new SlotRow(s.getDate(), s.getStartTime(), s.getEndTime(),
                            s.isRequiresApproval(), s.isBooked()));
                }
                bindSlots(rows);
                showLoading(false);
            }
            @Override public void onError(@NonNull String msg) {
                showLoading(false);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
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

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) { empty.setVisibility(View.GONE); list.setVisibility(View.GONE); }
    }

    // -------- view model & adapter --------
    static class SlotRow {
        final String date, start, end;
        final boolean requiresApproval, booked;
        SlotRow(String date, String start, String end, boolean req, boolean booked) {
            this.date = date; this.start = start; this.end = end; this.requiresApproval = req; this.booked = booked;
        }
    }

    private static class SlotAdapter extends ListAdapter<SlotRow, SlotVH> {
        protected SlotAdapter() {
            super(new DiffUtil.ItemCallback<SlotRow>() {
                @Override public boolean areItemsTheSame(@NonNull SlotRow a, @NonNull SlotRow b) {
                    return a.date.equals(b.date) && a.start.equals(b.start);
                }
                @Override public boolean areContentsTheSame(@NonNull SlotRow a, @NonNull SlotRow b) {
                    return a.end.equals(b.end) && a.requiresApproval == b.requiresApproval && a.booked == b.booked;
                }
            });
        }
        @NonNull @Override public SlotVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new SlotVH(row);
        }
        @Override public void onBindViewHolder(@NonNull SlotVH h, int position) {
            SlotRow s = getItem(position);
            h.title.setText(s.date + " • " + s.start + "–" + s.end);
            h.subtitle.setText(s.booked ? "Booked" : (s.requiresApproval ? "Manual approval" : "Auto"));
        }
    }

    private static class SlotVH extends RecyclerView.ViewHolder {
        final TextView title, subtitle;
        SlotVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
