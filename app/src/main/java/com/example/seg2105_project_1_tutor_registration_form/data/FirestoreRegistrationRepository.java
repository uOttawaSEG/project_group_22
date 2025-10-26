package com.example.seg2105_project_1_tutor_registration_form.data;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRegistrationRepository implements RegistrationRepository {

    private static final String COL = "registrationRequests";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------- Queries

    @Override
    public Task<List<RegRequest>> listByStatus(RequestStatus status) {
        TaskCompletionSource<List<RegRequest>> tcs = new TaskCompletionSource<>();
        db.collection(COL)
                .whereEqualTo("status", status.name())
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<RegRequest> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        RegRequest r = RegRequest.fromMap(d.getData(), d.getId());
                        out.add(r);
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    @Override
    public Task<RegRequest> details(String requestId) {
        TaskCompletionSource<RegRequest> tcs = new TaskCompletionSource<>();
        db.collection(COL).document(requestId).get()
                .addOnSuccessListener(d -> tcs.setResult(RegRequest.fromMap(d.getData(), d.getId())))
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    // ---------- Actions

    @Override
    public Task<Void> approve(String requestId, String adminUid) {
        DocumentReference ref = db.collection(COL).document(requestId);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        db.runTransaction(tx -> {
                    DocumentSnapshot d = tx.get(ref);
                    String cur = d.getString("status");
                    if (RequestStatus.APPROVED.name().equals(cur)) {
                        // Already approved → no-op
                        return null;
                    }
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("status", RequestStatus.APPROVED.name());
                    upd.put("decidedBy", adminUid);
                    upd.put("decidedAt", System.currentTimeMillis());
                    upd.put("reason", null);
                    tx.update(ref, upd);
                    return null;
                }).addOnSuccessListener(x -> tcs.setResult(null))
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    @Override
    public Task<Void> reject(String requestId, String adminUid, String reason) {
        DocumentReference ref = db.collection(COL).document(requestId);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        db.runTransaction(tx -> {
                    DocumentSnapshot d = tx.get(ref);
                    String cur = d.getString("status");
                    if (RequestStatus.APPROVED.name().equals(cur)) {
                        throw new IllegalStateException("Approved requests cannot be reversed.");
                    }
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("status", RequestStatus.REJECTED.name());
                    upd.put("decidedBy", adminUid);
                    upd.put("decidedAt", System.currentTimeMillis());
                    upd.put("reason", reason);
                    tx.update(ref, upd);
                    return null;
                }).addOnSuccessListener(x -> tcs.setResult(null))
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    @Override
    public Task<String> createFromRegistration(RegRequest request) {
        // We use userId as the document id so there's one request per user.
        String docId = request.userId;
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        db.collection(COL).document(docId).set(request.toMap(), SetOptions.merge())
                .addOnSuccessListener(v -> tcs.setResult(docId))
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }
}
