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

/**
 * SessionAdapter
 *
 * This RecyclerView.Adapter is used to display a list of Session objects
 * (date, time, tutor, status) for a student.
 *
 * Responsibilities:
 * - Inflate the layout for a single session row (item_session_row.xml).
 * - Bind Session data (date, time, tutorId, status) to the row's TextViews.
 * - Show/hide the "Cancel" button depending on the session status.
 * - Expose a callback (OnCancelClickListener) so the hosting screen
 *   (Activity/Fragment) can handle the actual cancel logic (e.g. Firestore update).
 */
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    /**
     * Callback interface so the parent (Fragment/Activity) can react when the user
     * taps the "Cancel" button on a particular Session item.
     */
    public interface OnCancelClickListener {
        void onCancelClick(Session session);
    }

    // List of sessions to display in the RecyclerView.
    private List<Session> sessionList;

    // Listener provided by the parent screen to handle cancel actions.
    private OnCancelClickListener cancelListener;

    /**
     * Constructor that takes the data set (list of sessions) and a cancel callback.
     *
     * @param sessionList     list of Session objects to display
     * @param cancelListener  callback invoked when "Cancel" is pressed on a row
     */
    public SessionAdapter(List<Session> sessionList, OnCancelClickListener cancelListener) {
        this.sessionList = sessionList;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the XML layout that defines how a single session row looks.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_row, parent, false);
        return new SessionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        // Get the Session object corresponding to this row.
        Session session = sessionList.get(position);

        // Display date and time in a single "when" line, e.g. "2025-11-21 • 11:30–12:00".
        holder.tvWhen.setText(session.getDate() + " • " + session.getStartTime() + "–" + session.getEndTime());

        // Show which tutor this session is with (by tutor ID here).
        holder.tvStudent.setText("Tutor ID: " + session.getTutorId());

        // Show the session status (PENDING, APPROVED, CANCELLED, etc.).
        holder.tvMeta.setText(session.getStatus());

        // If the session is already CANCELLED or REJECTED, hide the Cancel button.
        // Otherwise, show it so the user can still cancel.
        holder.btnCancel.setVisibility(
                "CANCELLED".equalsIgnoreCase(session.getStatus()) ||
                        "REJECTED".equalsIgnoreCase(session.getStatus()) ? View.GONE : View.VISIBLE
        );

        // When the Cancel button is pressed, notify the parent via the callback
        // so it can perform the actual cancel logic (e.g., update Firestore).
        holder.btnCancel.setOnClickListener(v -> cancelListener.onCancelClick(session));
    }

    @Override
    public int getItemCount() {
        // Return how many sessions we have to show.
        return sessionList.size();
    }

    /**
     * SessionViewHolder
     *
     * Holds references to the views inside a single row for better performance.
     * Each ViewHolder represents ONE row in the RecyclerView.
     */
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvWhen, tvStudent, tvMeta;
        Button btnCancel;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find references to the TextViews defined in item_session_row.xml.
            tvWhen = itemView.findViewById(R.id.tvWhen);
            tvStudent = itemView.findViewById(R.id.tvStudent);
            tvMeta = itemView.findViewById(R.id.tvMeta);

            // Programmatically create a "Cancel" button and add it to the layout.
            // (This assumes the root of item_session_row.xml is a ViewGroup.)
            btnCancel = new Button(itemView.getContext());
            btnCancel.setText("Cancel");
            ((ViewGroup) itemView).addView(btnCancel);
        }
    }
}

/*
 * Summary:
 * --------
 * SessionAdapter is a simple bridge between:
 *   - The "sessionList" data (list of Session objects),
 *   - And the RecyclerView UI that shows those sessions on screen.
 *
 * For each Session:
 *   - onCreateViewHolder inflates the row layout.
 *   - onBindViewHolder fills in the labels (date/time, tutor, status).
 *   - It decides whether to show the Cancel button based on the status.
 *   - When Cancel is pressed, the adapter calls OnCancelClickListener, so
 *     the Fragment/Activity can handle removing or updating the session in the backend.
 */
