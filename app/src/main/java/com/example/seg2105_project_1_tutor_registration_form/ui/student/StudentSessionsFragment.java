package com.example.seg2105_project_1_tutor_registration_form.ui.student;

import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthIdProvider;
import com.google.firebase.firestore.*;

import java.util.*;

public class StudentSessionsFragment extends Fragment {

    private View progress, empty;
    private RecyclerView list;
    private SessionsAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);
        progress = v.findViewById(R.id.progress);
        empty    = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_sessions);
        list     = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter  = new SessionsAdapter();
        list.setAdapter(adapter);
        load();
        return v;
    }

    @Override public void onResume() { super.onResume(); load(); }

    private void load() {
        showLoading(true);
        String studentId = AuthIdProvider.getCurrentUserId();

        FirebaseFirestore.getInstance()
                .collectionGroup("sessions")
                .whereEqualTo("studentId", studentId)
                .orderBy("date", Query.Direction.ASCENDING) // safe: "YYYY-MM-DD" lexicographic
                .get()
                .addOnSuccessListener(snap -> {
                    List<Row> rows = new ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        String date = d.getString("date");
                        String start = d.getString("startTime");
                        String end = d.getString("endTime");
                        String status = d.getString("status");
                        rows.add(new Row(date, start, end, status));
                    }
                    adapter.submitList(rows);
                    empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> toast(e.getMessage()))
                .addOnCompleteListener(t -> showLoading(false));
    }

    private void showLoading(boolean on) { progress.setVisibility(on ? View.VISIBLE : View.GONE); }
    private void toast(String s) { Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show(); }

    // simple list adapter
    static class Row { final String date, start, end, status;
        Row(String d, String s, String e, String st) { date=d; start=s; end=e; status=st; } }
    static class SessionsAdapter extends ListAdapter<Row, VH> {
        SessionsAdapter() { super(new DiffUtil.ItemCallback<Row>() {
            @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) { return a.date.equals(b.date) && a.start.equals(b.start); }
            @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) { return a.end.equals(b.end) && a.status.equals(b.status); }
        }); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Row r = getItem(pos);
            h.title.setText(r.date + " • " + r.start + "–" + r.end);
            h.subtitle.setText(r.status == null ? "" : r.status);
        }
    }
    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle;
        VH(@NonNull View item) { super(item); title = item.findViewById(android.R.id.text1); subtitle = item.findViewById(android.R.id.text2); }
    }
}

