package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
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
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;

import java.util.ArrayList;
import java.util.List;

/** Tutor “Requests” tab: pending requests newest-first, approve/reject actions. */
public class RequestsFragment extends Fragment {

    public interface Host {
        /** Optional: host can refresh the Sessions tab right away after approve/reject. */
        void refreshSessionsTab();
    }

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static RequestsFragment newInstance(@NonNull String tutorId) {
        RequestsFragment f = new RequestsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    private String tutorId;
    private TutorRepository repo;

    private View progress, empty;
    private RecyclerView list;
    private RequestAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.isEmpty()) throw new IllegalStateException("RequestsFragment requires tutorId");
        repo = new FirestoreTutorRepository();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_requests);

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RequestAdapter(this::showRowMenu);
        list.setAdapter(adapter);

        loadPending();
        return v;
    }

    /** Call to refresh (e.g., pull-to-refresh or returning to tab). */
    public void refresh() { loadPending(); }

    private void loadPending() {
        showLoading(true);
        repo.getPendingRequests(tutorId, new TutorRepository.RequestsListCallback() {
            @Override public void onSuccess(List<SessionRequest> reqs) {
                List<Row> rows = new ArrayList<>();
                // If your SessionRequest doesn’t carry date/start/end, fetch slot to display time.
                // We’ll try to use fields directly, else fall back to slot lookup.
                if (reqs.isEmpty()) {
                    bind(rows);
                    showLoading(false);
                    return;
                }
                final int[] remaining = { reqs.size() };
                for (SessionRequest r : reqs) {
                    String date = safe(r.getDate());
                    String start = safe(r.getStartTime());
                    String end = safe(r.getEndTime());
                    if (!date.isEmpty() && !start.isEmpty() && !end.isEmpty()) {
                        rows.add(new Row(r.getId(), r.getStudentId(), date, start, end,
                                r.getRequestedAtMillis()!=null ? r.getRequestedAtMillis().toDate().getTime() : System.currentTimeMillis()));
                        if (--remaining[0] == 0) { bind(rows); showLoading(false); }
                    } else {
                        // hydrate from slot
                        repo.getSlotById(tutorId, r.getSlotId(), new TutorRepository.SingleSlotCallback() {
                            @Override public void onSuccess(AvailabilitySlot s) {
                                rows.add(new Row(r.getId(), r.getStudentId(), s.getDate(), s.getStartTime(), s.getEndTime(),
                                        r.getRequestedAtMillis()!=null ? r.getRequestedAtMillis().toDate().getTime() : System.currentTimeMillis()));
                                if (--remaining[0] == 0) { bind(rows); showLoading(false); }
                            }
                            @Override public void onError(String msg) {
                                // still show a row with minimal info
                                rows.add(new Row(r.getId(), r.getStudentId(), "—", "—", "—",
                                        System.currentTimeMillis()));
                                if (--remaining[0] == 0) { bind(rows); showLoading(false); }
                            }
                        });
                    }
                }
            }
            @Override public void onError(String msg) {
                showLoading(false);
                toast(msg);
            }
        });
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void bind(@NonNull List<Row> rows) {
        if (rows.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            // repo already returns newest-first; if you want, sort here by requestedAtMillis desc.
            adapter.submitList(rows);
        }
    }

    // ---- Actions ----
    private void approve(String requestId) {
        repo.approveRequest(tutorId, requestId, new TutorRepository.SimpleCallback() {
            @Override public void onSuccess() {
                toast("Approved");
                removeRow(requestId);
                notifyHostRefreshSessions();
            }
            @Override public void onError(String msg) { toast(msg); }
        });
    }

    private void reject(String requestId) {
        repo.rejectRequest(tutorId, requestId, new TutorRepository.SimpleCallback() {
            @Override public void onSuccess() {
                toast("Rejected");
                removeRow(requestId);
                // Sessions list may not change on reject, but safe to refresh if host wants.
                notifyHostRefreshSessions();
            }
            @Override public void onError(String msg) { toast(msg); }
        });
    }

    private void notifyHostRefreshSessions() {
        if (getActivity() instanceof Host) {
            ((Host) getActivity()).refreshSessionsTab();
        }
    }

    private void removeRow(String requestId) {
        List<Row> current = new ArrayList<>(adapter.getCurrentList());
        current.removeIf(r -> r.requestId.equals(requestId));
        adapter.submitList(current);
        if (current.isEmpty()) { list.setVisibility(View.GONE); empty.setVisibility(View.VISIBLE); }
    }

    private void showRowMenu(View anchor, Row row) {
        PopupMenu m = new PopupMenu(requireContext(), anchor);
        m.getMenu().add("Approve");
        m.getMenu().add("Reject");
        m.setOnMenuItemClickListener(item -> {
            if ("Approve".contentEquals(item.getTitle())) approve(row.requestId);
            else reject(row.requestId);
            return true;
        });
        m.show();
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) { empty.setVisibility(View.GONE); list.setVisibility(View.GONE); }
    }

    private void toast(String msg) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }

    // ---- Row model & adapter ----
    static class Row {
        final String requestId, studentId, date, start, end;
        final long requestedAtMillis;
        Row(String requestId, String studentId, String date, String start, String end, long at) {
            this.requestId = requestId; this.studentId = studentId;
            this.date = date; this.start = start; this.end = end; this.requestedAtMillis = at;
        }
    }

    private static class RequestAdapter extends ListAdapter<Row, VH> {
        interface OnMoreClick { void onClick(View anchor, Row row); }
        private final OnMoreClick onMore;

        RequestAdapter(OnMoreClick onMore) {
            super(new DiffUtil.ItemCallback<Row>() {
                @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) { return a.requestId.equals(b.requestId); }
                @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    return a.date.equals(b.date) && a.start.equals(b.start) && a.end.equals(b.end);
                }
            });
            this.onMore = onMore;
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(row);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Row r = getItem(position);
            h.title.setText(r.date + " • " + r.start + "–" + r.end);
            h.subtitle.setText("Request • " + r.studentId); // can be hydrated with getStudent(...)
            h.itemView.setOnClickListener(v -> onMore.onClick(v, r)); // whole row opens menu
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
