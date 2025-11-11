package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student; // adjust if your Student class lives elsewhere
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RequestsFragment extends Fragment {

    /* ---------- factory & args ---------- */
    private static final String ARG_TUTOR_ID = "tutor_id";

    public static RequestsFragment newInstance(@NonNull String tutorId) {
        RequestsFragment f = new RequestsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    /* ---------- state ---------- */
    private String tutorId;
    private TutorRepository repo;

    /* ---------- views ---------- */
    private View progress, empty;
    private RecyclerView list;
    private RequestsAdapter adapter;

    /* ---------- optional host callbacks ---------- */
    public interface Host { void onRequestHandled(); }
    private Host host;

    @Override public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof Host) host = (Host) ctx;
    }
    @Override public void onDetach() {
        super.onDetach();
        host = null;
    }

    /* ---------- lifecycle ---------- */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        tutorId = (args != null) ? args.getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty()) {
            throw new IllegalStateException("RequestsFragment requires tutorId");
        }
        repo = new FirestoreTutorRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Must contain: progress, empty, list
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText("No pending requests.");

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RequestsAdapter();
        list.setAdapter(adapter);

        load();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        load(); // refresh when returning to tab
    }

    /* ---------- data ---------- */
    private void load() {
        showLoading(true);
        repo.getPendingRequests(tutorId, new TutorRepository.RequestsListCallback() {
            @Override public void onSuccess(List<SessionRequest> reqs) {
                List<SessionRequest> data = (reqs == null) ? new ArrayList<>() : reqs;
                if (data.isEmpty()) { bind(new ArrayList<>()); return; }

                final int[] remaining = { data.size() };
                for (SessionRequest r : data) {
                    // already have a name
                    if (!safe(r.getStudentName()).isEmpty()) {
                        if (--remaining[0] == 0) bind(data);
                        continue;
                    }
                    String sid = safe(r.getStudentId());
                    if (sid.isEmpty()) {
                        if (--remaining[0] == 0) bind(data);
                        continue;
                    }
                    repo.getStudent(sid, new TutorRepository.StudentCallback() {
                        @Override public void onSuccess(Student s) {
                            if (s != null) {
                                String fn = safe(s.getFirstName());
                                String ln = safe(s.getLastName());
                                r.setStudentName((fn + " " + ln).trim());
                            }
                            if (--remaining[0] == 0) bind(data);
                        }
                        @Override public void onError(String msg) {
                            if (--remaining[0] == 0) bind(data);
                        }
                    });
                }
            }

            @Override public void onError(String msg) {
                showLoading(false);
                Toast.makeText(requireContext(), (msg == null ? "Failed to load" : msg),
                        Toast.LENGTH_SHORT).show();
                bind(new ArrayList<>());
            }
        });
    }

    private void bind(List<SessionRequest> data) {
        showLoading(false);
        if (data.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.setItems(data);
        }
    }

    private void showLoading(boolean loading) {
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            if (list != null) list.setVisibility(View.GONE);
            if (empty != null) empty.setVisibility(View.GONE);
        }
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    private static String safe(String s){ return s == null ? "" : s; }

    private static String formatWhenLine(SessionRequest r) {
        String subject = safe(r.getSubject());
        String grade = safe(r.getGrade());

        String fromMeta = "";
        if (!grade.isEmpty()) fromMeta = grade;
        if (!subject.isEmpty()) fromMeta = fromMeta.isEmpty() ? subject : (fromMeta + " • " + subject);

        try {
            Timestamp ts = r.getRequestedAtMillis(); // ok if null
            if (ts != null) {
                Date d = ts.toDate();
                String when = DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(d);
                return fromMeta.isEmpty() ? ("Requested " + when) : (fromMeta + " • " + when);
            }
        } catch (Throwable ignored) { }
        return fromMeta.isEmpty() ? "Request" : fromMeta;
    }

    /* ---------- adapter ---------- */
    class RequestsAdapter extends RecyclerView.Adapter<ReqVH> {
        private final List<SessionRequest> items = new ArrayList<>();

        void setItems(List<SessionRequest> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull @Override public ReqVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Must contain: tvWhen, tvStudent, btnApprove, btnReject
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_row, parent, false);
            return new ReqVH(row);
        }

        @Override public void onBindViewHolder(@NonNull ReqVH h, int pos) {
            SessionRequest r = items.get(pos);

            h.when.setText(formatWhenLine(r));
            String name = safe(r.getStudentName());
            h.student.setText("Student: " + (name.isEmpty() ? "—" : name));

            h.approve.setOnClickListener(v -> {
                String id = safe(r.getId());
                if (id.isEmpty()) { toast("Missing request id"); return; }
                h.approve.setEnabled(false); h.reject.setEnabled(false);
                repo.approveRequest(tutorId, id, new TutorRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        toast("Approved");
                        int p = getBindingAdapterPositionSafe(h, pos);
                        if (p != -1) { items.remove(p); notifyItemRemoved(p); }
                        if (items.isEmpty()) bind(new ArrayList<>());
                        if (host != null) host.onRequestHandled();
                    }
                    @Override public void onError(String msg) {
                        toast(msg == null ? "Failed to approve" : msg);
                        h.approve.setEnabled(true); h.reject.setEnabled(true);
                    }
                });
            });

            h.reject.setOnClickListener(v -> {
                String id = safe(r.getId());
                if (id.isEmpty()) { toast("Missing request id"); return; }
                h.approve.setEnabled(false); h.reject.setEnabled(false);
                repo.rejectRequest(tutorId, id, new TutorRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        toast("Rejected");
                        int p = getBindingAdapterPositionSafe(h, pos);
                        if (p != -1) { items.remove(p); notifyItemRemoved(p); }
                        if (items.isEmpty()) bind(new ArrayList<>());
                        if (host != null) host.onRequestHandled();
                    }
                    @Override public void onError(String msg) {
                        toast(msg == null ? "Failed to reject" : msg);
                        h.approve.setEnabled(true); h.reject.setEnabled(true);
                    }
                });
            });
        }

        @Override public int getItemCount() { return items.size(); }

        private int getBindingAdapterPositionSafe(@NonNull RecyclerView.ViewHolder vh, int fallback) {
            int p = vh.getBindingAdapterPosition();
            return (p == RecyclerView.NO_POSITION) ? fallback : p;
        }
    }

    static class ReqVH extends RecyclerView.ViewHolder {
        final TextView when, student;
        final MaterialButton approve, reject;
        ReqVH(@NonNull View v) {
            super(v);
            when = v.findViewById(R.id.tvWhen);
            student = v.findViewById(R.id.tvStudent);
            approve = v.findViewById(R.id.btnApprove);
            reject = v.findViewById(R.id.btnReject);
        }
    }
}
