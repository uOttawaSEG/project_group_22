package com.example.seg2105_project_1_tutor_registration_form.data;

import androidx.annotation.Nullable;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;


import java.util.ArrayList;
import java.util.List;

public class FirestoreRegistrationRepository implements RegistrationRepository {

    private static final String COL = "registrationRequests";
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();

    @Override
    public Task<List<RegRequest>> listByStatus(RequestStatus status) {
        return fs.collection(COL)
                .whereEqualTo("status", status.name())
                .get()
                .continueWith(t -> {
                    List<RegRequest> out = new ArrayList<>();
                    if (!t.isSuccessful() || t.getResult() == null) return out;
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        RegRequest r = d.toObject(RegRequest.class);
                        if (r == null) continue;
                        r.setId(d.getId());   // ✅ keep Firestore doc id
                        out.add(r);
                    }
                    return out;
                });
    }

    @Override
    public Task<Void> approve(String requestId, String adminUid) {
        return fs.collection(COL).document(requestId)
                .update(
                        "status", RequestStatus.APPROVED.name(),
                        "decidedAt", FieldValue.serverTimestamp(),
                        "decidedBy", adminUid,
                        "reason", FieldValue.delete()
                );
    }

    @Override
    public Task<Void> reject(String requestId, String adminUid, @Nullable String reasonNullable) {
        Object reasonField = (reasonNullable == null || reasonNullable.isBlank())
                ? FieldValue.delete()
                : reasonNullable;

        return fs.collection(COL).document(requestId)
                .update(
                        "status", RequestStatus.REJECTED.name(),
                        "decidedAt", FieldValue.serverTimestamp(),
                        "decidedBy", adminUid,
                        "reason", reasonField
                );
    }
}
