package com.example.seg2105_project_1_tutor_registration_form.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FirebaseRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Auth
    public Task<AuthResult> signUp(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }
    public Task<AuthResult> signIn(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }
    public String uid() { return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid(); }

    // Profiles
    public Task<Void> saveUserProfile(String uid, Map<String,Object> data) {
        return db.collection("users").document(uid).set(data);
    }
}
