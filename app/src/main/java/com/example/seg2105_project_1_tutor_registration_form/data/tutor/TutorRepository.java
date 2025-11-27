package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

//import the following
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;
import com.example.seg2105_project_1_tutor_registration_form.model.Tutor;

import java.util.List;
/** * * TutorRepository – handles tutor data in Firestore:
 *
 * Basically, this lets you list, add, and remove available time slots. Tutors can then accept or reject student requests. All methods are async, using callbacks to return data.
 *
 * Helpers: we can get student info, a single time slot, and past or upcoming sessions.
 * - onError(String): quickly tells the user if something went wrong.
 *
 * Customs:
 * - Dates are in yyyy-MM-dd format. Times are HH:mm (:00 or :30).
 * - Request statuses: PENDING, APPROVED, REJECTED
 * - Sessions are split into upcoming and past, based on start time.
 *
 * UI/Threading: Callbacks are the only way to update the UI, since Firestore is async.
 */

public interface TutorRepository {

    /* ===== callbacks ===== */
    interface SimpleCallback { void onSuccess(); void onError(String msg); }

    interface SlotCallback { void onSuccess(AvailabilitySlot s); void onError(String msg); }

    interface SlotsListCallback { void onSuccess(List<AvailabilitySlot> slots); void onError(String msg); }

    interface RequestsListCallback { void onSuccess(List<SessionRequest> reqs); void onError(String msg); }

    interface RequestCreateCallback { void onSuccess(String requestId); void onError(String msg); }

    interface SingleRequestCallback { void onSuccess(SessionRequest r); void onError(String msg); }

    interface SingleSlotCallback { void onSuccess(AvailabilitySlot s); void onError(String msg); }

    interface StudentCallback { void onSuccess(Student s); void onError(String msg); }

    interface SessionsListCallback { void onSuccess(List<Session> upcoming, List<Session> past); void onError(String msg); }

    interface TutorCallback { void onSuccess(Tutor t); void onError(String msg); }
    /* ===== D3 operations ===== */

    // Create a new 30-min availability slot for a tutor
    void createAvailabilitySlot(String tutorId,
                                String date,        // "yyyy-MM-dd"
                                String startTime,   // "HH:mm" (:00 or :30)
                                boolean requiresApproval,
                                SlotCallback cb);

    // Student flow (request a slot)
    void submitSessionRequest(String tutorId, String studentId, String slotId, RequestCreateCallback cb);

    // Tutor actions on requests
    void approveRequest(String tutorId, String requestId, SimpleCallback cb);
    void rejectRequest(String tutorId, String requestId, SimpleCallback cb);

    // Slots
    void getAvailabilitySlots(String tutorId, SlotsListCallback cb);
    void deleteAvailabilitySlot(String tutorId, String slotId, SimpleCallback cb);

    // Requests / helpers
    void getPendingRequests(String tutorId, RequestsListCallback cb);
    void getSlotById(String tutorId, String slotId, SingleSlotCallback cb);
    void getStudent(String studentId, StudentCallback cb);

    void getTutorSessions(String tutorId, SessionsListCallback cb);

    void rateTutor(String tutorId, String sessionId, int stars, SimpleCallback cb);
}
