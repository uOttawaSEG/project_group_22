package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.VH> {

    public interface SlotActionListener {
        void onDeleteClicked(AvailabilitySlot slot, int position);
    }

    private final List<AvailabilitySlot> data = new ArrayList<>();
    private final SlotActionListener actions;

    public AvailabilityAdapter(@NonNull SlotActionListener actions) {
        this.actions = actions;
    }

    /** Optional helper so the fragment can read an item by position */
    public AvailabilitySlot getItem(int position) {
        return data.get(position);
    }

    /** Replace all rows */
    public void setData(@NonNull List<AvailabilitySlot> slots) {
        data.clear();
        data.addAll(slots);
        notifyDataSetChanged();
    }

    /** Remove a row after successful delete */
    public void removeAt(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        data.remove(pos);
        notifyItemRemoved(pos);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_availability_slot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AvailabilitySlot s = data.get(pos);

        h.when.setText((s.getDate() == null ? "" : s.getDate())
                + " • "
                + (s.getStartTime() == null ? "" : s.getStartTime())
                + "–"
                + (s.getEndTime() == null ? "" : s.getEndTime()));

        h.meta.setText((s.isRequiresApproval() ? "manual approval" : "auto")
                + " • "
                + (s.isBooked() ? "booked" : "open"));

        h.btnDelete.setOnClickListener(v -> {
            int adapterPos = h.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                actions.onDeleteClicked(s, adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView when, meta;
        final MaterialButton btnDelete;

        VH(@NonNull View v) {
            super(v);
            when = v.findViewById(R.id.text_when);
            meta = v.findViewById(R.id.text_meta);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
