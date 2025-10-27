package com.uottawa.eecs.rejectedmodule;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.example.seg2105_project_1_tutor_registration_form.repository.FirestoreRegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.repository.RegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.auth.FirebaseAuthService;
import com.example.seg2105_project_1_tutor_registration_form.auth.AuthService;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class RejectedRequestsFragment extends Fragment {

    private EditText searchBox;
    private RecyclerView recyclerView;
    private RejectedRequestsAdapter adapter;

    private List<RegRequest> rejectedRequests = new ArrayList<>();
    private List<RegRequest> filteredRequests = new ArrayList<>();

    private RegistrationRepository regRepo;
    private AuthService authService;
    private String adminUid = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rejected_requests, container, false);

        // Bind layout views
        searchBox = view.findViewById(R.id.et_search);
        recyclerView = view.findViewById(R.id.rv_rejected);

        // Initialize repository and authentication service
        regRepo = new FirestoreRegistrationRepository();
        authService = new FirebaseAuthService();

        // Get the current logged-in user (assumed to be an admin)
        authService.getCurrentUser().thenAccept(user -> {
            if (user != null) adminUid = user.uid;
        });

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RejectedRequestsAdapter(filteredRequests, this::approveRequest);
        recyclerView.setAdapter(adapter);

        // Load all the rejected data from Firestore
        loadRejectedRequests();

        // Enable search box filtering
        setupSearchBox();

        return view;
    }

    // Load all rejected registration requests from Firestore
    private void loadRejectedRequests() {
        regRepo.listByStatus(RequestStatus.REJECTED)
                .addOnSuccessListener(list -> {
                    rejectedRequests.clear();
                    rejectedRequests.addAll(list);
                    filteredRequests.clear();
                    filteredRequests.addAll(list);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Approve a previously rejected request
    private void approveRequest(String requestId) {
        if (adminUid.isEmpty()) {
            Toast.makeText(getContext(), "Please log in as an administrator", Toast.LENGTH_SHORT).show();
            return;
        }

        regRepo.approve(requestId, adminUid)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request approved successfully", Toast.LENGTH_SHORT).show();
                    loadRejectedRequests(); // Refresh the list after approval
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Approval failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Listen for changes in the search box and filter results
    private void setupSearchBox() {
        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String keyword = s.toString().toLowerCase().trim();
                filterRequests(keyword);
            }
        });
    }

    // Filter requests by name or email
    private void filterRequests(String keyword) {
        filteredRequests.clear();
        if (keyword.isEmpty()) {
            filteredRequests.addAll(rejectedRequests);
        } else {
            for (RegRequest req : rejectedRequests) {
                String fullName = (req.firstName + " " + req.lastName).toLowerCase();
                if (fullName.contains(keyword) || req.email.toLowerCase().contains(keyword)) {
                    filteredRequests.add(req);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // RecyclerView adapter for displaying rejected requests
    public static class RejectedRequestsAdapter extends RecyclerView.Adapter<RejectedRequestsAdapter.ViewHolder> {
        private final List<RegRequest> data;
        private final OnApproveClickListener listener;

        // Listener interface for the approve button
        interface OnApproveClickListener { void onApprove(String requestId); }

        public RejectedRequestsAdapter(List<RegRequest> data, OnApproveClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rejected, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RegRequest r = data.get(position);
            holder.name.setText(r.firstName + " " + r.lastName);
            holder.email.setText(r.email);
            holder.reason.setText("Reason: " + (r.reason == null ? "None" : r.reason));
            holder.btnApprove.setOnClickListener(v -> listener.onApprove(r.id));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, email, reason;
            View btnApprove;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tv_item_fullname);
                email = v.findViewById(R.id.tv_item_email);
                reason = v.findViewById(R.id.tv_item_reject_reason);
                btnApprove = v.findViewById(R.id.btn_item_approve);
            }
        }
    }
}
