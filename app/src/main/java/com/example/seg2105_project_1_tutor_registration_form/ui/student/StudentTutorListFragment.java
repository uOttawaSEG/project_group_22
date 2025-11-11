// StudentTutorListFragment.java
package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.TutorSummary;

import java.util.*;

public class StudentTutorListFragment extends Fragment {

    private final TutorRepository repo = new FirestoreTutorRepository();

    private View progress, empty;
    private RecyclerView list;
    private Adapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View v = inf.inflate(R.layout.fragment_simple_list, parent, false);
        progress = v.findViewById(R.id.progress);
        empty    = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_tutors_with_open_slots);

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter(this::onTutorClick);
        list.setAdapter(adapter);

        load();
        return v;
    }

    private void load() {
        showLoading(true);
        long now = System.currentTimeMillis();
        repo.listTutorsWithOpenSlots(now, new TutorRepository.TutorsListCallback() {
            @Override public void onSuccess(List<TutorSummary> tutors) {
                showLoading(false);
                bind(tutors);
            }
            @Override public void onError(String msg) {
                showLoading(false);
                toast(msg == null ? "Failed to load tutors" : msg);
                bind(Collections.emptyList());
            }
        });
    }

    private void bind(List<TutorSummary> items) {
        if (items == null || items.isEmpty()) {
            list.setVisibility(View.GONE); empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE); list.setVisibility(View.VISIBLE);
            adapter.submitList(items);
        }
    }

    private void onTutorClick(TutorSummary t) {
        // Navigate to that tutor’s open-slot list
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.student_content, StudentOpenSlotsFragment.newInstance(t.getTutorId()))
                .addToBackStack("open-slots")
                .commit();
    }

    private void showLoading(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        if (b) { empty.setVisibility(View.GONE); list.setVisibility(View.GONE); }
    }
    private void toast(String s){ Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show(); }

    // --- Adapter ---
    private static class Adapter extends ListAdapter<TutorSummary, VH> {
        interface OnClick { void onClick(TutorSummary t); }
        private final OnClick onClick;

        Adapter(OnClick c) {
            super(new DiffUtil.ItemCallback<TutorSummary>() {
                @Override public boolean areItemsTheSame(@NonNull TutorSummary a, @NonNull TutorSummary b) {
                    return a.getTutorId().equals(b.getTutorId());
                }
                @Override public boolean areContentsTheSame(@NonNull TutorSummary a, @NonNull TutorSummary b) {
                    return a.getOpenCount()==b.getOpenCount() &&
                            a.getDisplayName().equals(b.getDisplayName()) &&
                            safe(a.getEmail()).equals(safe(b.getEmail()));
                }
                private String safe(String s){ return s==null?"":s; }
            });
            this.onClick = c;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(row);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            TutorSummary t = getItem(pos);
            h.title.setText(t.getDisplayName());
            String sub = (t.getEmail()==null||t.getEmail().isEmpty() ? "" : t.getEmail()+" • ")
                    + t.getOpenCount() + " open slot" + (t.getOpenCount()==1?"":"s");
            h.subtitle.setText(sub);
            h.itemView.setOnClickListener(v -> onClick.onClick(t));
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

