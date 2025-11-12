package com.example.seg2105_project_1_tutor_registration_form.ui.tutor;

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
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AvailabilityFragment extends Fragment {

    private static final String ARG_TUTOR_ID = "tutor_id";

    public static AvailabilityFragment newInstance(@NonNull String tutorId) {
        AvailabilityFragment f = new AvailabilityFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TUTOR_ID, tutorId);
        f.setArguments(b);
        return f;
    }

    private String tutorId;
    private TutorRepository repo;

    private View progress;
    private View empty;
    private RecyclerView list;

    // 🔹 use the adapter with the Delete button
    private AvailabilityAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorId = getArguments() != null ? getArguments().getString(ARG_TUTOR_ID) : null;
        if (tutorId == null || tutorId.trim().isEmpty())
            throw new IllegalStateException("AvailabilityFragment requires a tutorId");
        repo = new FirestoreTutorRepository();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_simple_list, container, false);

        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.empty);
        if (empty instanceof TextView) ((TextView) empty).setText(R.string.empty_availability);

        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 🔹 METHOD C: confirm → delete via repo → update adapter
        adapter = new AvailabilityAdapter((slot, position) -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete slot?")
                    .setMessage("Remove " + slot.getDate() + " • " + slot.getStartTime() + "–" + slot.getEndTime() + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        repo.deleteAvailabilitySlot(tutorId, slot.getId(), new TutorRepository.SimpleCallback() {
                            @Override public void onSuccess() {
                                adapter.removeAt(position);
                                Snackbar.make(list, "Slot deleted", Snackbar.LENGTH_SHORT).show();
                                // show empty state if list became empty
                                if (adapter.getItemCount() == 0) {
                                    list.setVisibility(View.GONE);
                                    empty.setVisibility(View.VISIBLE);
                                }
                            }
                            @Override public void onError(String msg) {
                                Snackbar.make(list, msg == null ? "Failed to delete" : msg,
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
                    })
                    .show();
        });

        list.setAdapter(adapter);

        loadSlots(); // initial load
        return v;
    }

    /** Auto-refresh every time we return to this tab/screen */
    @Override public void onResume() {
        super.onResume();
        loadSlots();
    }

    /** Optional external trigger from host activity */
    public void refresh() { loadSlots(); }

    private void loadSlots() {
        showLoading(true);
        repo.getAvailabilitySlots(tutorId, new TutorRepository.SlotsListCallback() {
            @Override public void onSuccess(List<AvailabilitySlot> slots) {
                showLoading(false);
                if (slots == null || slots.isEmpty()) {
                    list.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                } else {
                    empty.setVisibility(View.GONE);
                    list.setVisibility(View.VISIBLE);
                    adapter.setData(slots); // adapter expects AvailabilitySlot list
                }
            }
            @Override public void onError(String msg) {
                showLoading(false);
                showError(msg != null ? msg : getString(R.string.error_generic));
            }
        });
    }

    private void showError(@NonNull String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
        }
    }
}
