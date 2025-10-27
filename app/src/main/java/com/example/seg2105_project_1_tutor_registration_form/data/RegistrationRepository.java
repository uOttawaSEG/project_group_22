package com.example.seg2105_project_1_tutor_registration_form.data;

import com.example.seg2105_project_1_tutor_registration_form.model.RegRequest;
import com.google.android.gms.tasks.Task;
import java.util.List;

public interface RegistrationRepository {
    Task<List<RegRequest>> listByStatus(RequestStatus status);
    Task<Void> approve(String requestId, String adminUid);
    Task<Void> reject(String requestId, String adminUid, String reasonNullable);
}
