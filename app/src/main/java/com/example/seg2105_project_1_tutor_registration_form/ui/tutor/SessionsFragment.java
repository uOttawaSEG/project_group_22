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
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.RepoProvider;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sessions tab: Upcoming (>= now) and Past (< now).
 * Uses fragment_simple_list.xml.
 */
public class SessionsFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static SessionsFragment newInstance(@NonNull String tutorId) {
        SessionsFragment f = new SessionsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    // --- deps/state ---
    private final TutorRepository repo = RepoProvider.tutor();
    private String tutorId;

    // --- views ---
    private View progress, empty;
    private RecyclerView list;
    private SessionsAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty()) {
            throw new IllegalStateException("SessionsFragment requires tutorId");
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) {
            ((TextView) empty).setText(R.string.empty_sessions);
        }
        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SessionsAdapter();
        list.setAdapter(adapter);

        loadSessions();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        loadSessions();
    }

    public void refresh() { loadSessions(); }

    // -------------------- Data loading --------------------
    private void loadSessions() {
        showLoading(true);

        repo.getTutorSessions(tutorId, new TutorRepository.SessionsListCallback() {
            @Override public void onSuccess(@NonNull List<Session> sessions) {
                long now = System.currentTimeMillis();
                List<SessionRow> upcoming = new ArrayList<>();
                List<SessionRow> past = new ArrayList<>();

                for (Session s : sessions) {
                    SessionRow row = toRow(s);
                    long startMs = row.startDateTimeMillis;
                    if (startMs >= now) upcoming.add(row);
                    else past.add(row);
                }

                // Sort: upcoming chronological; past reverse-chronological
                Collections.sort(upcoming, (a, b) -> Long.compare(a.startDateTimeMillis, b.startDateTimeMillis));
                Collections.sort(past, (a, b) -> Long.compare(b.startDateTimeMillis, a.startDateTimeMillis));

                bind(upcoming, past);
                showLoading(false);
            }

            @Override public void onError(@NonNull String msg) {
                showLoading(false);
                toast(msg);
            }
        });
    }

    private void bind(@NonNull List<SessionRow> upcoming, @NonNull List<SessionRow> past) {
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

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    // Convert repo Session -> UI row without java.time; use millis.
    private SessionRow toRow(@NonNull Session s) {
        long startMs = s.getStartMillis() != null ? s.getStartMillis() : 0L;

        // Prefer the stored ISO date if present; otherwise derive from startMillis.
        String dateIso = s.getDate();
        if ((dateIso == null || dateIso.isEmpty()) && startMs > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(startMs);
            int y = c.get(Calendar.YEAR);
            int m = c.get(Calendar.MONTH) + 1;
            int d = c.get(Calendar.DAY_OF_MONTH);
            dateIso = String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
        }

        return new SessionRow(
                safe(s.getSessionId()),
                startMs,
                safe(dateIso),
                safe(s.getStartTime()),
                safe(s.getEndTime()),
                safe(s.getStudentId()),
                safe(s.getStatus())
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // -------------------- Row/View models --------------------
    private static class SessionRow {
        final String id;
        final long   startDateTimeMillis; // used for split/sort
        final String dateIso;             // "YYYY-MM-DD" for display
        final String start;               // "HH:mm"
        final String end;                 // "HH:mm"
        final String studentId;
        final String status;

        SessionRow(String id,
                   long startDateTimeMillis,
                   String dateIso,
                   String start, String end,
                   String studentId,
                   String status) {
            this.id = id;
            this.startDateTimeMillis = startDateTimeMillis;
            this.dateIso = dateIso;
            this.start = start;
            this.end = end;
            this.studentId = studentId;
            this.status = status;
        }
    }

    private static class Row {
        static final int TYPE_HEADER = 0;
        static final int TYPE_ITEM   = 1;
        final int type;
        final String headerTitle;  // if header
        final SessionRow item;     // if item
        private Row(int type, String headerTitle, SessionRow item) {
            this.type = type; this.headerTitle = headerTitle; this.item = item;
        }
        static Row header(String title) { return new Row(TYPE_HEADER, title, null); }
        static Row item(SessionRow s)   { return new Row(TYPE_ITEM, null, s); }
    }

    // -------------------- Adapter (single class) --------------------
    private static class SessionsAdapter extends ListAdapter<Row, RecyclerView.ViewHolder> {
        private static final int LAYOUT_HEADER = android.R.layout.simple_list_item_1;
        private static final int LAYOUT_ITEM   = android.R.layout.simple_list_item_2;

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
                ((ItemVH) holder).title.setText(s.dateIso + " • " + s.start + "–" + s.end);
                ((ItemVH) holder).subtitle.setText("Student: " + s.studentId + " • " + s.status);

                holder.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(
                            v.getContext(),
                            com.example.seg2105_project_1_tutor_registration_form.ui.tutor.SessionDetailActivity.class
                    );
                    i.putExtra("when", s.dateIso + " • " + s.start + "–" + s.end);
                    i.putExtra("status", s.status);
                    i.putExtra("studentName", s.studentId);   // placeholder; can enrich later
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
            ItemVH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}

