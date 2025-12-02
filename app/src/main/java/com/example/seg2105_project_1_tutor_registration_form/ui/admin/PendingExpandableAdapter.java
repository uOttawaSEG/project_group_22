package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PendingExpandableAdapter extends RecyclerView.Adapter<PendingExpandableAdapter.VH> {

    public interface Actions {
        void onApprove(RegRequest item);
        void onReject(RegRequest item);
    }

    private final List<RegRequest> items = new ArrayList<>();
    private final Actions actions;
    private final Set<Integer> expanded = new HashSet<>();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

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
            // Name
            String fullName = (safe(r.getFirstName()) + " " + safe(r.getLastName())).trim();
            txtName.setText(fullName.isEmpty() ? "—" : fullName);

            // Role (String in your model)
            String role = safe(r.getRole());
            txtRole.setText("Role: " + (role.isEmpty() ? "—" : role.toUpperCase(Locale.ROOT)));

            // Status (String) + pill background
            String status = safe(r.getStatus());
            String statusUpper = status.isEmpty() ? "PENDING" : status.toUpperCase(Locale.ROOT);
            txtStatus.setText(statusUpper);

            if ("APPROVED".equals(statusUpper)) {
                txtStatus.setBackgroundResource(R.drawable.pill_status_approved);
            } else if ("REJECTED".equals(statusUpper)) {
                txtStatus.setBackgroundResource(R.drawable.pill_status_rejected);
            } else {
                // default
                txtStatus.setBackgroundResource(R.drawable.pill_pending);
            }

            // Email / phone lines
            txtEmail.setText("Email: " + (safe(r.getEmail()).isEmpty() ? "—" : safe(r.getEmail())));
            txtPhone.setText("Phone: " + (safe(r.getPhone()).isEmpty() ? "—" : safe(r.getPhone())));

            // Extra (multi-line): degree/teaches for tutors OR coursesWanted for students + submitted date
            StringBuilder extra = new StringBuilder();

            if ("TUTOR".equalsIgnoreCase(role)) {
                String teaches = safe(r.getCoursesOfferedSummary());
                String degree  = safe(r.getDegree()); // ok if your model has degree; safe("") if not
                if (!degree.isEmpty()) extra.append("Degree: ").append(degree);
                if (!teaches.isEmpty()) {
                    if (extra.length() > 0) extra.append("\n");
                    extra.append("Teaches: ").append(teaches);
                }
            } else if ("STUDENT".equalsIgnoreCase(role)) {
                String wants = safe(r.getCoursesWantedSummary());
                if (!wants.isEmpty()) extra.append("Looking for: ").append(wants);
            }

            // Submitted date (from Long millis)
            Long submittedAt = r.getSubmittedAt(); // expecting Long in your model
            if (submittedAt != null && submittedAt > 0) {
                String when = DATE_FMT.format(new java.util.Date(submittedAt));
                if (extra.length() > 0) extra.append("\n");
                extra.append("Submitted: ").append(when);
            }

            txtExtra.setText(extra.toString());

            // Expand/collapse
            details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            // ✅ Always keep actions enabled so admin can flip Approved ↔ Rejected
            btnApprove.setEnabled(true);
            btnReject.setEnabled(true);
            btnApprove.setAlpha(1f);
            btnReject.setAlpha(1f);
        }

        private static String safe(String s) { return s == null ? "" : s; }
    }
}
