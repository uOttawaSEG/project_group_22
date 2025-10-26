package com.example.seg2105_project_1_tutor_registration_form.data;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.RequestStatus;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Contract the UI can call for admin review flows. */
public interface RegistrationRepository {

    /** List requests by status (PENDING, REJECTED, APPROVED). */
    Task<List<RegRequest>> listByStatus(RequestStatus status);

    /** Load full details for a specific request (row → details card). */
    Task<RegRequest> details(String requestId);

    /**
     * Approve a request.
     * - PENDING → APPROVED
     * - REJECTED → APPROVED
     * - APPROVED stays APPROVED (no-op)
     */
    Task<Void> approve(String requestId, String adminUid);

    /**
     * Reject a request.
     * - PENDING → REJECTED
     * - REJECTED stays REJECTED (updates reason/audit)
     * - APPROVED cannot be reversed (fails)
     */
    Task<Void> reject(String requestId, String adminUid, String reason);

    /** Create a PENDING request when a user registers (1 doc per user). */
    Task<String> createFromRegistration(RegRequest request);
}
