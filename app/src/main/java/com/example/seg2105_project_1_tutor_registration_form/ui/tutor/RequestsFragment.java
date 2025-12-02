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
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * RequestsFragment
 * ----------------
 * Screen for tutors to see pending session requests and approve/reject them.
 *
 * Data:
 *   • newInstance(tutorId) to pass tutor UID.
 *   • Uses TutorRepository (FirestoreTutorRepository) to:
 *       - getPendingRequests(tutorId)
 *       - getStudent(studentId) to enrich names
 *       - approveRequest / rejectRequest
 *
 * UI:
 *   • Shows RecyclerView of requests, empty view when none, and progress while loading.
 *   • Each row shows:
 *       - When line (grade/subject + slot date/time if available)
 *       - Student name
 *       - Approve / Reject buttons
 */
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

    /* ---------- lifecycle ---------- */
    public interface Host {
        void onRequestHandled();
    }
    private Host host;

    @Override public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof Host) host = (Host) ctx;
    }

    @Override public void onDetach() {
        super.onDetach();
        host = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ⚠️ no throwing here — be graceful if args are missing during system recreation
        Bundle args = getArguments();
        tutorId = (args != null) ? args.getString(ARG_TUTOR_ID) : null;
        if (tutorId == null) tutorId = "";
        repo = new FirestoreTutorRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    /* ---------- data ---------- */
    private void load() {
        // ✅ Guard against missing tutorId so we don't crash when fragment is recreated by the system
        if (tutorId.isEmpty()) {
            bind(new ArrayList<>());
            Toast.makeText(requireContext(), "Tutor not signed in yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        repo.getPendingRequests(tutorId, new TutorRepository.RequestsListCallback() {
            @Override public void onSuccess(List<SessionRequest> reqs) {
                if (reqs == null) reqs = new ArrayList<>();
                final List<SessionRequest> data = reqs;

                if (data.isEmpty()) {
                    bind(new ArrayList<>());
                    return;
                }

                final int[] remaining = { data.size() };
                for (SessionRequest r : data) {
                    if (safe(r.getStudentName()).length() > 0) {
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
                Toast.makeText(requireContext(), msg == null ? "Failed to load" : msg, Toast.LENGTH_SHORT).show();
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
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) { list.setVisibility(View.GONE); empty.setVisibility(View.GONE); }
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    private static String safe(String s){ return s==null ? "" : s; }

    /**
     * Try to get slot date & start/end time from the SessionRequest so we can show:
     *   Grade • Subject • 2025-11-21 • 11:30–12:00
     *
     * Because I don't know your exact model field names, this uses reflection to
     * check a few common getters (getDate, getSlotDate, getStartTime, getEndTime...).
     * If nothing is found, it falls back to the original "Requested <timestamp>" logic,
     * and finally "Request".
     */
    private static String formatWhenLine(SessionRequest r) {
        // 1) Build meta from grade + subject
        String subject = safe(r.getSubject());
        String grade = safe(r.getGrade());

        String fromMeta = "";
        if (!grade.isEmpty()) fromMeta = grade;
        if (!subject.isEmpty()) {
            fromMeta = fromMeta.isEmpty() ? subject : (fromMeta + " • " + subject);
        }

        // 2) Try to read explicit slot date + times from the model (if present)
        String date = safe(callStringGetter(r, "getDate"));
        if (date.isEmpty()) date = safe(callStringGetter(r, "getSlotDate"));

        String start = safe(callStringGetter(r, "getStartTime"));
        if (start.isEmpty()) start = safe(callStringGetter(r, "getStartLabel"));
        if (start.isEmpty()) start = safe(callStringGetter(r, "getStartTimeLabel"));

        String end = safe(callStringGetter(r, "getEndTime"));
        if (end.isEmpty()) end = safe(callStringGetter(r, "getEndLabel"));
        if (end.isEmpty()) end = safe(callStringGetter(r, "getEndTimeLabel"));

        if (!date.isEmpty() && !start.isEmpty()) {
            // We have slot date + start time – this is what we really want to show
            String range = (end.isEmpty()) ? start : (start + "–" + end);
            if (fromMeta.isEmpty()) {
                return date + " • " + range;
            } else {
                return fromMeta + " • " + date + " • " + range;
            }
        }

        // 3) Fallback: use the original "Requested <timestamp>" logic
        try {
            Timestamp ts = r.getRequestedAtMillis();
            if (ts != null) {
                Date d = ts.toDate();
                String when = DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()
                ).format(d);
                return fromMeta.isEmpty() ? ("Requested " + when) : (fromMeta + " • " + when);
            }
        } catch (Throwable ignored) { }

        // 4) Last resort: just show meta or "Request"
        return fromMeta.isEmpty() ? "Request" : fromMeta;
    }

    /**
     * Small helper: call a no-arg String getter on SessionRequest by name, but
     * swallow any reflection errors and return "" if not found.
     */
    private static String callStringGetter(SessionRequest r, String methodName) {
        try {
            java.lang.reflect.Method m = r.getClass().getMethod(methodName);
            Object val = m.invoke(r);
            return val == null ? "" : val.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /* ---------- adapter ---------- */
    class RequestsAdapter extends RecyclerView.Adapter<ReqVH> {
        private final List<SessionRequest> items = new ArrayList<>();
        void setItems(List<SessionRequest> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull @Override public ReqVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request_row, parent, false);
            return new ReqVH(row);
        }

        @Override public void onBindViewHolder(@NonNull ReqVH h, int pos) {
            SessionRequest r = items.get(pos);

            h.when.setText(formatWhenLine(r));
            h.student.setText("Student: " + (safe(r.getStudentName()).isEmpty() ? "—" : r.getStudentName()));

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
            return p == RecyclerView.NO_POSITION ? fallback : p;
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

    /*
     *   • Lifecycle:
     *       Parent Activities can implement Host callback (optional) to notify Host after
     *       an item is handled (e.g., to refresh badges/tabs).
     *       To maintain the list's freshness, onCreateView and onResume call load().
     *   • formatWhenLine(r) now prefers slot date/time fields if available, and only
     *     falls back to "Requested <time>" or "Request" as a last resort.
     */
}
