package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;

import java.util.List;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.VH> {

    public interface OnTutorClick {
        void onTutorClick(TutorListActivity.TutorRow tutor);
    }

    private List<TutorListActivity.TutorRow> items;
    private final OnTutorClick clickListener; // may be null

    public TutorAdapter(List<TutorListActivity.TutorRow> items) {
        this(items, null);
    }

    public TutorAdapter(List<TutorListActivity.TutorRow> items, OnTutorClick clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    public void setItems(List<TutorListActivity.TutorRow> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutor_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TutorListActivity.TutorRow t = items.get(position);

        holder.name.setText(t.getDisplay());
        holder.email.setText(t.getEmail());

        // Optional degree support (only if item_tutor_card has @id/tutor_degree)
        if (holder.degree != null) {
            // We don't have degree in TutorRow; leave blank/hidden. Availability screen shows it.
            holder.degree.setVisibility(View.GONE);
        }

        if (clickListener != null) {
            holder.viewBtn.setOnClickListener(v -> clickListener.onTutorClick(t));
            holder.viewBtn.setVisibility(View.VISIBLE);
        } else {
            holder.viewBtn.setOnClickListener(null);
            holder.viewBtn.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, email, degree; // degree is optional in the layout
        Button viewBtn;
        VH(@NonNull View v) {
            super(v);
            name    = v.findViewById(R.id.tutor_name);
            email   = v.findViewById(R.id.tutor_email);
            viewBtn = v.findViewById(R.id.btn_view_availability);
            // Will be null if your card layout doesn't define it; that's OK.
            int degreeId = v.getResources().getIdentifier("tutor_degree", "id", v.getContext().getPackageName());
            degree = degreeId != 0 ? v.findViewById(degreeId) : null;
        }
    }
}
