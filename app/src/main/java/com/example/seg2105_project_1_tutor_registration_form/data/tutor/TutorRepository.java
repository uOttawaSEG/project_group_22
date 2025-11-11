package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import androidx.annotation.NonNull;

import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.TutorSummary;

import java.util.List;

public interface TutorRepository {

    // ---- Callbacks ----
    interface SimpleCallback { void onSuccess(); void onError(@NonNull String msg); }

    interface SingleSlotCallback { void onSuccess(@NonNull AvailabilitySlot slot); void onError(@NonNull String msg); }

    interface SlotsListCallback { void onSuccess(@NonNull List<AvailabilitySlot> slots); void onError(@NonNull String msg); }

    interface RequestsListCallback { void onSuccess(@NonNull List<SessionRequest> requests); void onError(@NonNull String msg); }

    interface SessionsListCallback { void onSuccess(@NonNull List<Session> sessions); void onError(@NonNull String msg); }

    interface SingleSessionCallback { void onSuccess(@NonNull Session session); void onError(@NonNull String msg); }

    interface StudentCallback { void onSuccess(@NonNull Student student); void onError(@NonNull String msg); }

    interface TutorsListCallback {
        void onSuccess(java.util.List<TutorSummary> tutors);
        void onError(String msg);
    }

    // ---- Availability ----
    void createAvailabilitySlot(@NonNull String tutorId, @NonNull AvailabilitySlot slot, @NonNull SimpleCallback cb);

    void getAvailabilitySlots(@NonNull String tutorId, @NonNull SlotsListCallback cb);

    void getSlotById(@NonNull String tutorId, @NonNull String slotId, @NonNull SingleSlotCallback cb);

    void deleteAvailabilitySlot(@NonNull String tutorId, @NonNull String slotId, @NonNull SimpleCallback cb);

    // ---- Requests ----
    void getPendingRequests(@NonNull String tutorId, @NonNull RequestsListCallback cb);

    void approveRequest(@NonNull String tutorId, @NonNull String requestId, @NonNull SimpleCallback cb);

    void rejectRequest(@NonNull String tutorId, @NonNull String requestId, @NonNull SimpleCallback cb);

    // ---- Sessions ----
    void getTutorSessions(@NonNull String tutorId, @NonNull SessionsListCallback cb);

    void getSessionById(@NonNull String tutorId, @NonNull String sessionId, @NonNull SingleSessionCallback cb);

    // ---- Users ----
    void getStudent(@NonNull String studentId, @NonNull StudentCallback cb);

    void requestBooking(String tutorId, String slotId, String studentId, SimpleCallback cb);

    void getOpenSlots(@NonNull String tutorId, @NonNull SlotsListCallback cb);

    void requestSession(
            @NonNull String tutorId,
            @NonNull String studentId,
            @NonNull String slotId,
            boolean requiresApproval,
            @NonNull SimpleCallback cb
    );

    /** Tutors that have ≥1 future open slot (startMillis > now). */
    void listTutorsWithOpenSlots(long nowEpochMillis, TutorsListCallback cb);

}

