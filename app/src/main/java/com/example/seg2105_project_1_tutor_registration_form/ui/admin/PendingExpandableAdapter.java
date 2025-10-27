package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PendingExpandableAdapter extends RecyclerView.Adapter<PendingExpandableAdapter.VH> {

    public interface Actions {
        void onApprove(RegRequest item);
        void onReject(RegRequest item);
    }

    private final List<RegRequest> items = new ArrayList<>();
    private final Actions actions;
    private final Set<Integer> expanded = new HashSet<>();

    public PendingExpandableAdapter(Actions actions) {
        this.actions = actions;
        setHasStableIds(true);
    }

    public void submit(List<RegRequest> data) {
        items.clear();
        if (data != null) items.addAll(data);
        expanded.clear();
        notifyDataSetChanged();
    }

    @Override public long getItemId(int position) {
        RegRequest r = items.get(position);
        String id = r.getId();
        return id != null ? id.hashCode() : position;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request_expandable, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        RegRequest r = items.get(pos);
        h.bind(r, expanded.contains(pos));
        h.header.setOnClickListener(v -> toggle(pos));
        h.btnApprove.setOnClickListener(v -> actions.onApprove(r));
        h.btnReject.setOnClickListener(v -> actions.onReject(r));
    }

    private void toggle(int pos) {
        if (expanded.contains(pos)) expanded.remove(pos); else expanded.add(pos);
        notifyItemChanged(pos);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View header, details;
        TextView txtName, txtRole, txtStatus, txtEmail, txtPhone, txtExtra;
        View btnApprove, btnReject;

        VH(@NonNull View v) {
            super(v);
            header     = v.findViewById(R.id.header);
            details    = v.findViewById(R.id.details);
            txtName    = v.findViewById(R.id.txtName);
            txtRole    = v.findViewById(R.id.txtRole);
            txtStatus  = v.findViewById(R.id.txtStatus);
            txtEmail   = v.findViewById(R.id.txtEmail);
            txtPhone   = v.findViewById(R.id.txtPhone);
            txtExtra   = v.findViewById(R.id.txtExtra);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject  = v.findViewById(R.id.btnReject);
        }

        void bind(RegRequest r, boolean isExpanded) {
            String fullName = (safe(r.getFirstName()) + " " + safe(r.getLastName())).trim();
            txtName.setText(fullName);

            // role/status are Strings in your model
            String role   = r.getRole();
            String status = r.getStatus();

            txtRole.setText("Role: " + (role == null || role.isEmpty() ? "—" : role));
            txtStatus.setText(status == null || status.isEmpty() ? "PENDING" : status);

            txtEmail.setText("Email: " + safe(r.getEmail()));
            txtPhone.setText("Phone: " + safe(r.getPhone()));

            // extra line based on role (String compares)
            if ("STUDENT".equalsIgnoreCase(role)) {
                txtExtra.setText("Looking for: " + safe(r.getCoursesWantedSummary()));
            } else if ("TUTOR".equalsIgnoreCase(role)) {
                txtExtra.setText("Teaches: " + safe(r.getCoursesOfferedSummary()));
            } else {
                txtExtra.setText("");
            }

            details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }

        private static String safe(String s) { return s == null ? "" : s; }
    }
}
