package com.example.seg2105_project_1_tutor_registration_form.model;


import java.util.HashMap;
import java.util.Map;

/**
 * A registration request that an Admin reviews.
 * One document per user under collection "registrationRequests".
 */
public class RegRequest {
    // Firestore doc id (not stored inside the doc; we set it after reads)
    public String id;

    // Who/what
    public String userId;         // Firebase Auth UID
    public String role;           // "Student" | "Tutor" | "Administrator" (string to match what you save now)
    public String firstName;
    public String lastName;
    public String email;
    public String phone;          // optional

    // Status + audit trail
    public String status;         // "PENDING" | "APPROVED" | "REJECTED"
    public long   submittedAt;    // epoch millis
    public String decidedBy;      // admin UID (nullable)
    public Long   decidedAt;      // epoch millis (nullable)
    public String reason;         // rejection reason (nullable)

    // Required by Firestore
    public RegRequest() {}

    public static RegRequest pending(String userId,
                                     String role,
                                     String firstName,
                                     String lastName,
                                     String email,
                                     String phone,
                                     long nowMillis) {
        RegRequest r = new RegRequest();
        r.userId = userId;
        r.role = role;
        r.firstName = firstName;
        r.lastName  = lastName;
        r.email     = email;
        r.phone     = phone;
        r.status    = RequestStatus.PENDING.name();
        r.submittedAt = nowMillis;
        return r;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("role", role);
        m.put("firstName", firstName);
        m.put("lastName", lastName);
        m.put("email", email);
        m.put("phone", phone);
        m.put("status", status);
        m.put("submittedAt", submittedAt);
        m.put("decidedBy", decidedBy);
        m.put("decidedAt", decidedAt);
        m.put("reason", reason);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static RegRequest fromMap(Map<String, Object> m, String id) {
        if (m == null) return null;
        RegRequest r = new RegRequest();
        r.id         = id;
        r.userId     = (String) m.get("userId");
        r.role       = (String) m.get("role");
        r.firstName  = (String) m.get("firstName");
        r.lastName   = (String) m.get("lastName");
        r.email      = (String) m.get("email");
        r.phone      = (String) m.get("phone");
        r.status     = (String) m.get("status");
        Object subAt = m.get("submittedAt");
        r.submittedAt= subAt instanceof Number ? ((Number) subAt).longValue() : 0L;
        r.decidedBy  = (String) m.get("decidedBy");
        Object decAt = m.get("decidedAt");
        r.decidedAt  = (decAt instanceof Number) ? ((Number) decAt).longValue() : null;
        r.reason     = (String) m.get("reason");
        return r;
    }
}
