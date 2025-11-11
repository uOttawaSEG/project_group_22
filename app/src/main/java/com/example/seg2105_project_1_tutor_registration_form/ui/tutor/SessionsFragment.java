package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tutor “Sessions” tab: upcoming + past. */
public class SessionsFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static SessionsFragment newInstance(@NonNull String tutorId) {
        SessionsFragment f = new SessionsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    private String tutorId;
    private TutorRepository repo;

    private View progress, empty;
    private RecyclerView list;
    private SessionsAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.isEmpty()) throw new IllegalStateException("SessionsFragment requires tutorId");
        repo = new FirestoreTutorRepository();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_sessions);

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionsAdapter();
        list.setAdapter(adapter);

        loadSessions();
        return v;
    }

    /** Public refresh (host can call this after approval). */
    public void refresh() { loadSessions(); }

    @Override public void onResume() {
        super.onResume();
        // Also refresh when user swipes back to this tab.
        loadSessions();
    }

    private void loadSessions() {
        showLoading(true);
        repo.getTutorSessions(tutorId, new TutorRepository.SessionsListCallback() {
            @Override public void onSuccess(List<Session> up, List<Session> past) {
                List<SessionRow> upcoming = new ArrayList<>();
                List<SessionRow> pastRows = new ArrayList<>();

                for (Session s : up) {
                    upcoming.add(new SessionRow(
                            s.getId(),
                            combineToLocalDateTime(s.getDate(), s.getStartTime()),
                            s.getStartTime(),
                            s.getEndTime(),
                            s.getStudentId(),
                            s.getStatus()
                    ));
                }
                for (Session s : past) {
                    pastRows.add(new SessionRow(
                            s.getId(),
                            combineToLocalDateTime(s.getDate(), s.getStartTime()),
                            s.getStartTime(),
                            s.getEndTime(),
                            s.getStudentId(),
                            s.getStatus()
                    ));
                }

                bind(upcoming, pastRows);
                showLoading(false);
            }
            @Override public void onError(String msg) {
                showLoading(false);
                toast(msg);
            }
        });
    }

    private static LocalDateTime combineToLocalDateTime(String dateIso, String hhmm) {
        int y = Integer.parseInt(dateIso.substring(0,4));
        int mo = Integer.parseInt(dateIso.substring(5,7));
        int d = Integer.parseInt(dateIso.substring(8,10));
        int h = Integer.parseInt(hhmm.substring(0,2));
        int m = Integer.parseInt(hhmm.substring(3,5));
        return LocalDateTime.of(y, mo, d, h, m);
    }

    private void bind(@NonNull List<SessionRow> upcoming, @NonNull List<SessionRow> past) {
        Collections.sort(upcoming, (a, b) -> a.startDateTime.compareTo(b.startDateTime));
        Collections.sort(past, (a, b) -> b.startDateTime.compareTo(a.startDateTime));

        List<Row> rows = new ArrayList<>();
        if (!upcoming.isEmpty()) {
            rows.add(Row.header("Upcoming"));
            for (SessionRow s : upcoming) rows.add(Row.item(s));
        }
        if (!past.isEmpty()) {
            rows.add(Row.header("Past"));
            for (SessionRow s : past) rows.add(Row.item(s));
        }

        if (rows.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.submitList(rows);
        }
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) { empty.setVisibility(View.GONE); list.setVisibility(View.GONE); }
    }

    private void toast(String msg) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }

    // ---- row/view models ----
    private static class SessionRow {
        final String id;
        final LocalDateTime startDateTime;
        final String start, end, studentId, status;
        SessionRow(String id, LocalDateTime day, String start, String end, String studentId, String status) {
            this.id = id;
            this.startDateTime = day;
            this.start = start; this.end = end; this.studentId = studentId; this.status = status;
        }
    }

    private static class Row {
        static final int TYPE_HEADER = 0, TYPE_ITEM = 1;
        final int type; final String headerTitle; final SessionRow item;
        private Row(int type, String headerTitle, SessionRow item) { this.type = type; this.headerTitle = headerTitle; this.item = item; }
        static Row header(String title) { return new Row(TYPE_HEADER, title, null); }
        static Row item(SessionRow s) { return new Row(TYPE_ITEM, null, s); }
    }

    // ---- adapter with 2 view types ----
    private static class SessionsAdapter extends ListAdapter<Row, RecyclerView.ViewHolder> {
        private static final int LAYOUT_HEADER = android.R.layout.simple_list_item_1;
        private static final int LAYOUT_ITEM = android.R.layout.simple_list_item_2;

        SessionsAdapter() {
            super(new DiffUtil.ItemCallback<Row>() {
                @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                    if (a.type != b.type) return false;
                    if (a.type == Row.TYPE_HEADER) return a.headerTitle.equals(b.headerTitle);
                    return a.item.id.equals(b.item.id);
                }
                @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    if (a.type != b.type) return false;
                    if (a.type == Row.TYPE_HEADER) return true;
                    SessionRow x = a.item, y = b.item;
                    return x.start.equals(y.start) && x.end.equals(y.end) && x.status.equals(y.status);
                }
            });
        }

        @Override public int getItemViewType(int position) { return getItem(position).type; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == Row.TYPE_HEADER) {
                View v = inf.inflate(LAYOUT_HEADER, parent, false);
                return new HeaderVH(v);
            } else {
                View v = inf.inflate(LAYOUT_ITEM, parent, false);
                return new ItemVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row r = getItem(position);
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText(r.headerTitle);
            } else if (holder instanceof ItemVH) {
                SessionRow s = r.item;
                String dateIso = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(s.startDateTime);
                ((ItemVH) holder).title.setText(dateIso + " • " + s.start + "–" + s.end);
                ((ItemVH) holder).subtitle.setText("Student: " + s.studentId + " • " + s.status);

                holder.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(
                            v.getContext(),
                            com.example.seg2105_project_1_tutor_registration_form.ui.tutor.SessionDetailActivity.class
                    );
                    i.putExtra("when", dateIso + " • " + s.start + "–" + s.end);
                    i.putExtra("status", s.status);
                    i.putExtra("studentName", s.studentId);   // replace with real name via repo.getStudent if desired
                    i.putExtra("studentEmail", "");
                    i.putExtra("notes", "");
                    i.putExtra("studentUid", s.studentId);
                    v.getContext().startActivity(i);
                });
            }
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            final TextView title;
            HeaderVH(@NonNull View itemView) { super(itemView); title = itemView.findViewById(android.R.id.text1); }
        }
        static class ItemVH extends RecyclerView.ViewHolder {
            final TextView title, subtitle;
            ItemVH(@NonNull View itemView) { super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
