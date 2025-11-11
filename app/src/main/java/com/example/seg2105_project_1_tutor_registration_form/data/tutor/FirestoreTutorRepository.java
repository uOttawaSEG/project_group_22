package com.example.seg2105_project_1_tutor_registration_form.data.tutor;

import com.example.seg2105_project_1_tutor_registration_form.model.tutor.AvailabilitySlot;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Session;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.SessionRequest;
import com.example.seg2105_project_1_tutor_registration_form.model.tutor.Student;
import java.util.Collections;
import java.util.List;

public class FirestoreTutorRepository implements TutorRepository {

    @Override public void createAvailabilitySlot(String tutorId, String date, String startTime, boolean requiresApproval, SlotCallback cb) {
        cb.onError("TODO Firestore createAvailabilitySlot");
    }
    @Override public void getAvailabilitySlots(String tutorId, SlotsListCallback cb) {
        cb.onSuccess(Collections.<AvailabilitySlot>emptyList());
    }
    @Override public void deleteAvailabilitySlot(String tutorId, String slotId, SimpleCallback cb) {
        cb.onError("TODO Firestore deleteAvailabilitySlot");
    }

    @Override public void getPendingRequests(String tutorId, RequestsListCallback cb) {
        cb.onSuccess(Collections.<SessionRequest>emptyList());
    }
    @Override public void approveRequest(String tutorId, String requestId, SimpleCallback cb) {
        cb.onError("TODO Firestore approveRequest");
    }
    @Override public void rejectRequest(String tutorId, String requestId, SimpleCallback cb) {
        cb.onError("TODO Firestore rejectRequest");
    }

    @Override public void getTutorSessions(String tutorId, SessionsListCallback cb) {
        cb.onSuccess(Collections.<Session>emptyList(), Collections.<Session>emptyList());
    }
    @Override public void cancelSession(String tutorId, String sessionId, SimpleCallback cb) {
        cb.onError("TODO Firestore cancelSession");
    }

    @Override public void getStudent(String studentId, StudentCallback cb) { cb.onError("TODO Firestore getStudent"); }
    @Override public void submitSessionRequest(String tutorId, String studentId, String slotId, RequestCreateCallback cb) { cb.onError("TODO Firestore submitSessionRequest"); }
    @Override public void getRequestById(String tutorId, String requestId, SingleRequestCallback cb) { cb.onError("TODO Firestore getRequestById"); }
    @Override public void getSlotById(String tutorId, String slotId, SingleSlotCallback cb) { cb.onError("TODO Firestore getSlotById"); }
    @Override public void getSessionById(String tutorId, String sessionId, SingleSessionCallback cb) { cb.onError("TODO Firestore getSessionById"); }
}
