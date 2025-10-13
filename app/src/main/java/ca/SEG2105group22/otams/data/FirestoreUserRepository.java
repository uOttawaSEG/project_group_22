package ca.SEG2105group22.otams.data;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CompletableFuture;

import ca.SEG2105group22.otams.model.User;

public class FirestoreUserRepository implements UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public CompletableFuture<Void> save(User user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        db.collection("users")
                .document(user.uid)
                .set(user)
                .addOnSuccessListener(unused -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    @Override
    public CompletableFuture<User> findByUid(String uid) {
        CompletableFuture<User> future = new CompletableFuture<>();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        future.complete(user);
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    @Override
    public CompletableFuture<User> findByEmail(String email) {
        CompletableFuture<User> future = new CompletableFuture<>();
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        User user = query.getDocuments().get(0).toObject(User.class);
                        future.complete(user);
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }
}