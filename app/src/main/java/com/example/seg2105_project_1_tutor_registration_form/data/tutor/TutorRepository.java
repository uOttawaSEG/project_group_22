package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import java.util.List;

public interface TutorRepository {
    interface SimpleCallback { void onSuccess(); void onError(String msg); }
    interface SlotCallback { void onSuccess(AvailabilitySlot s); void onError(String msg); }
    interface SlotsListCallback { void onSuccess(List<AvailabilitySlot> slots); void onError(String msg); }
    interface RequestsListCallback { void onSuccess(List<SessionRequest> reqs); void onError(String msg); }
    interface RequestCreateCallback { void onSuccess(String requestId); void onError(String msg); }
    interface SingleRequestCallback { void onSuccess(SessionRequest r); void onError(String msg); }
    interface SingleSlotCallback { void onSuccess(AvailabilitySlot s); void onError(String msg); }
    interface SessionsListCallback { void onSuccess(List<Session> upcoming, List<Session> past); void onError(String msg); }
    interface SingleSessionCallback { void onSuccess(Session s); void onError(String msg); }
    interface StudentCallback { void onSuccess(Student s); void onError(String msg); }

    void createAvailabilitySlot(String tutorId, String date, String startTime, boolean requiresApproval, SlotCallback cb);
    void getAvailabilitySlots(String tutorId, SlotsListCallback cb);
    void deleteAvailabilitySlot(String tutorId, String slotId, SimpleCallback cb);

    void getPendingRequests(String tutorId, RequestsListCallback cb);
    void approveRequest(String tutorId, String requestId, SimpleCallback cb);
    void rejectRequest(String tutorId, String requestId, SimpleCallback cb);

    void getTutorSessions(String tutorId, SessionsListCallback cb);
    void cancelSession(String tutorId, String sessionId, SimpleCallback cb);

    void getStudent(String studentId, StudentCallback cb);
    void submitSessionRequest(String tutorId, String studentId, String slotId, RequestCreateCallback cb);

    void getRequestById(String tutorId, String requestId, SingleRequestCallback cb);
    void getSlotById(String tutorId, String slotId, SingleSlotCallback cb);
    void getSessionById(String tutorId, String sessionId, SingleSessionCallback cb);
}
