package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

//import the following
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.Student;

import java.util.List;
/** * TutorRepository—contract for tutor data operations (Firestore impl: FirestoreTutorRepository) *

 * Scope * - Add, remove, and list available slots. Students submit requests, and tutors approve or reject them. * * Callbacks * - All methods are async; return via callbacks * Helpers: fetch student, single slot, sessions (past/upcoming). - onError(String): a brief message for the user
 * Customs * - Time "HH:mm" (:00 or :30); Date "yyyy-MM-dd" Status of the request: PENDING, APPROVED, REJECTED Sessions are divided by start time into upcoming and past categories.
 * UI/Threading: Firestore operations are asynchronous; only callbacks are used for updating the user interface. */

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

    // 🔹 Sessions list for the Tutor “Sessions” tab
    void getTutorSessions(String tutorId, SessionsListCallback cb);
}
