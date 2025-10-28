package com.example.seg2105_project_1_tutor_registration_form.ui.admin;

import android.content.Intent; // <-- added
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.example.seg2105_project_1_tutor_registration_form.data.FirestoreRegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.RegistrationRepository;
import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * AdminInboxActivity — human-readable overview
 * ─────────────────────────────────────────────────────────────────────────────
 * Purpose
 * • Admin dashboard to review registration requests (Pending / Approved / Rejected).
 * • Supports search, approve, and reject (with optional reason).
 *
 * Screen structure
 * • RecyclerView + PendingExpandableAdapter to render requests.
 * • Search box filters by name, email, or role (client-side).
 * • ProgressBar + empty-state view for loading/empty lists.
 *
 * Data flow
 * • statusToShow is chosen from Intent extra "filter_status" ("PENDING"/"APPROVED"/"REJECTED").
 * • loadList() → repo.listByStatus(statusToShow) → cache (in-memory) → applyFilter(query) → adapter.submit().
 * • Approve/Reject actions call repo.approve(...) / repo.reject(...), then either reload or navigate to the new tab.
 *
 * UX notes
 * • Snackbars communicate success/failure and provide shortcuts (“View approved/rejected”).
 * • Search updates on text change; filtering is case-insensitive and trims whitespace.
 *
 * Safety
 * • getAdminUid() reads FirebaseAuth current user; if null, action is blocked with a snackbar.
 * • All repository calls handle onFailure with user-visible errors.
 *
 * Maintenance tips
 * • If you add pagination/server-side filtering later, replace applyFilter() with query params.
 * • Keep role/status strings consistent with Firestore and the rest of the app.
 * • Localize strings (toasts/snackbars/dialog titles) before production.
 */
public class AdminInboxActivity extends AppCompatActivity implements PendingExpandableAdapter.Actions {

    private RegistrationRepository repo;
    private PendingExpandableAdapter adapter;

    private ProgressBar progress;
    private View empty;
    private EditText search;

    private final List<RegRequest> cache = new ArrayList<>();
    private RequestStatus statusToShow = RequestStatus.PENDING; // default

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_inbox);

        // Which list to show (Inbox=Pending; Approved; Rejected)
        String filter = getIntent().getStringExtra("filter_status");
        if ("APPROVED".equalsIgnoreCase(filter)) {
            statusToShow = RequestStatus.APPROVED;
        } else if ("REJECTED".equalsIgnoreCase(filter)) {
            statusToShow = RequestStatus.REJECTED;
        }

        repo = new FirestoreRegistrationRepository();

        progress = findViewById(R.id.progress);
        empty    = findViewById(R.id.empty);
        search   = findViewById(R.id.search);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingExpandableAdapter(this); // callbacks below
        rv.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override protected void onResume() {
        super.onResume();
        loadList();
    }

    // Loads from Firestore via repository
    private void loadList() {
        showLoading(true);
        Task<List<RegRequest>> task = repo.listByStatus(statusToShow);
        task.addOnSuccessListener(list -> {
            cache.clear();
            if (list != null) cache.addAll(list);
            applyFilter(search.getText() == null ? "" : search.getText().toString());
            showLoading(false);
        }).addOnFailureListener(e -> {
            showLoading(false);
            empty.setVisibility(View.VISIBLE);
            Snackbar.make(findViewById(android.R.id.content),
                    "Failed to load: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        });
    }

    private void applyFilter(String q) {
        String query = q == null ? "" : q.trim().toLowerCase();
        List<RegRequest> filtered = new ArrayList<>();
        for (RegRequest r : cache) {
            String name  = ((safe(r.getFirstName()) + " " + safe(r.getLastName())).trim()).toLowerCase();
            String email = safe(r.getEmail()).toLowerCase();
            String role  = r.getRole() == null ? "" : r.getRole().toString().toLowerCase();
            if (query.isEmpty() || name.contains(query) || email.contains(query) || role.contains(query)) {
                filtered.add(r);
            }
        }
        adapter.submit(filtered);
        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) empty.setVisibility(View.GONE);
    }

    // <-- added helper
    private void openInbox(String status) {
        Intent i = new Intent(this, AdminInboxActivity.class);
        i.putExtra("filter_status", status);   // "PENDING" | "APPROVED" | "REJECTED"
        startActivity(i);
        finish();
    }

    // --- Adapter callbacks ---

    @Override public void onApprove(RegRequest item) {
        String adminUid = getAdminUid();
        if (adminUid == null) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Not signed in as admin.", Snackbar.LENGTH_LONG).show();
            return;
        }
        showLoading(true);
        repo.approve(item.getId(), adminUid).addOnSuccessListener(unused -> {
            Snackbar.make(findViewById(android.R.id.content),
                            R.string.approved_snack, Snackbar.LENGTH_LONG)
                    .setAction("View approved", v -> openInbox("APPROVED"))
                    .show();

            if (statusToShow == RequestStatus.PENDING) {
                openInbox("APPROVED");
            } else {
                loadList();
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.approve_failed) + ": " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        });
    }

    @Override public void onReject(RegRequest item) {
        // Ask for optional reason
        View dlg = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null, false);
        EditText reasonEt = dlg.findViewById(R.id.reason);

        new AlertDialog.Builder(this)
                .setTitle(R.string.reject)
                .setView(dlg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reject, (d, which) -> {
                    String adminUid = getAdminUid();
                    if (adminUid == null) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Not signed in as admin.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    String reason = reasonEt.getText() == null ? null : reasonEt.getText().toString().trim();
                    showLoading(true);
                    repo.reject(item.getId(), adminUid, reason).addOnSuccessListener(unused -> {
                        Snackbar.make(findViewById(android.R.id.content),
                                        R.string.rejected_snack, Snackbar.LENGTH_LONG)
                                .setAction("View rejected", v -> openInbox("REJECTED"))
                                .show();

                        if (statusToShow == RequestStatus.PENDING) {
                            openInbox("REJECTED");
                        } else {
                            loadList();
                        }
                    }).addOnFailureListener(e -> {
                        showLoading(false);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.reject_failed) + ": " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    });
                })
                .show();
    }

    private @Nullable String getAdminUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * End of AdminInboxActivity — TL;DR
 * ─────────────────────────────────────────────────────────────────────────────
 * • Loads requests by status (from Intent), caches them, filters client-side, shows list.
 * • Approve/Reject call repository, show snackbars, and navigate to the new status tab.
 * • Search matches name/email/role; loading + empty states are handled gracefully.
 * • Actions require an authenticated admin (checked via FirebaseAuth).
 */
