package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(Session session);
    }

    private List<Session> sessionList;
    private OnCancelClickListener cancelListener;

    public SessionAdapter(List<Session> sessionList, OnCancelClickListener cancelListener) {
        this.sessionList = sessionList;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_row, parent, false);
        return new SessionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessionList.get(position);

        holder.tvWhen.setText(session.getDate() + " • " + session.getStartTime() + "–" + session.getEndTime());
        holder.tvStudent.setText("Tutor ID: " + session.getTutorId());
        holder.tvMeta.setText(session.getStatus());

        holder.btnCancel.setVisibility(
                "CANCELLED".equalsIgnoreCase(session.getStatus()) ||
                "REJECTED".equalsIgnoreCase(session.getStatus()) ? View.GONE : View.VISIBLE
        );

        holder.btnCancel.setOnClickListener(v -> cancelListener.onCancelClick(session));
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvWhen, tvStudent, tvMeta;
        Button btnCancel;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWhen = itemView.findViewById(R.id.tvWhen);
            tvStudent = itemView.findViewById(R.id.tvStudent);
            tvMeta = itemView.findViewById(R.id.tvMeta);

            btnCancel = new Button(itemView.getContext());
            btnCancel.setText("Cancel");
            ((ViewGroup) itemView).addView(btnCancel);
        }
    }
}
