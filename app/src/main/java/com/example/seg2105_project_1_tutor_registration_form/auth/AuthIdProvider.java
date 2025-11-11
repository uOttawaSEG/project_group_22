package com.example.seg2105_project_1_tutor_registration_form.auth;

import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Central source of truth for retrieving the currently logged-in user's UID.
 * Use requireCurrentUserId() if you need to guarantee a valid session.
 */
public final class AuthIdProvider {

    private AuthIdProvider() {}

    /** Returns UID or null if not logged in (useful when UI wants to redirect). */
    @Nullable
    public static String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    /** Returns UID or throws if the user is not logged in. */
    public static String requireCurrentUserId() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            throw new IllegalStateException("No logged-in user. Redirect to login.");
        }
        return uid;
    }
}