package com.example.seg2105_project_1_tutor_registration_form.auth;

import com.google.firebase.auth.FirebaseAuth;

/** One place to read the current user's UID. */
public final class AuthIdProvider {
    private AuthIdProvider() {}

    public static String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    /** Throws if not signed in. */
    public static String requireCurrentUserId() {
        String id = getCurrentUserId();
        if (id == null) throw new IllegalStateException("Not signed in");
        return id;
    }
}
