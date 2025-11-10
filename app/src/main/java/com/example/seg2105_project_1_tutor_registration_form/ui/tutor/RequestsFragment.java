package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.os.Bundle;
import android.view.*;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.seg2105_project_1_tutor_registration_form.R;

import java.util.*;

/**
 * Requests tab: shows tutor's pending requests.
 * Layout: fragment_simple_list.xml (progress + empty + RecyclerView).
 *
 * TODO (swap fake with real):
 *  - repo.getPendingRequests(tutorId, ...)
 *  - repo.approveRequest(tutorId, requestId, ...)
 *  - repo.rejectRequest(tutorId, requestId, ...)
 *  - (optional) repo.getStudent(r.studentId, ...) for detail subtitle
 */
public class RequestsFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static RequestsFragment newInstance(@NonNull String tutorId) {
        RequestsFragment f = new RequestsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    private String tutorId;

    // Views
    private View progress, empty;
    private RecyclerView list;
    private RequestAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.isEmpty()) throw new IllegalStateException("RequestsFragment requires tutorId");
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

        loadPending(); // fake for now
        return v;
    }

    /** Call to refresh after approve/reject */
    public void refresh() { loadPending(); }

    // -------------------- Data loading (fake now; replace with repo) --------------------
    private void loadPending() {
        showLoading(true);

        // TODO: replace with repo.getPendingRequests(tutorId, cb) and sort newest-first
        list.postDelayed(() -> {
            List<Row> demo = new ArrayList<>();
            // newest first
            demo.add(new Row("req_003", "stu_9", "2025-11-16", "10:30", "11:00", System.currentTimeMillis()));
            demo.add(new Row("req_002", "stu_7", "2025-11-15", "14:00", "14:30", System.currentTimeMillis() - 60000));
            bind(demo);
            showLoading(false);
        }, 250);
    }

    private void bind(@NonNull List<Row> rows) {
        if (rows.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.submitList(rows);
        }
    }

    // -------------------- Actions --------------------
    private void approve(String requestId) {
        // TODO: repo.approveRequest(tutorId, requestId, cb)
        toast("Approved");
        removeRow(requestId);
    }

    private void reject(String requestId) {
        // TODO: repo.rejectRequest(tutorId, requestId, cb)
        toast("Rejected");
        removeRow(requestId);
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

    // -------------------- Row model --------------------
    static class Row {
        final String requestId;
        final String studentId;
        final String date, start, end;
        final long requestedAtMillis; // for newest-first sort / display
        Row(String requestId, String studentId, String date, String start, String end, long at) {
            this.requestId = requestId; this.studentId = studentId;
            this.date = date; this.start = start; this.end = end; this.requestedAtMillis = at;
        }
    }

    // -------------------- Adapter --------------------
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
            // simple 2-line row with a trailing menu button
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(row);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Row r = getItem(position);
            String title = r.date + " • " + r.start + "–" + r.end;
            h.title.setText(title);
            h.subtitle.setText("Request • " + r.studentId); // later: replace with getStudent() name/phone
            // Use the whole row as the “more” anchor for now
            h.itemView.setOnClickListener(v -> onMore.onClick(v, r));
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
