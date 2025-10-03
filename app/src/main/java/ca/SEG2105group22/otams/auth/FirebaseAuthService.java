package ca.SEG2105group22.otams.auth;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ca.SEG2105group22.otams.data.FirestoreUserRepository;
import ca.SEG2105group22.otams.data.UserRepository;
import ca.SEG2105group22.otams.model.User;

public class FirebaseAuthService implements AuthService {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final UserRepository users = new FirestoreUserRepository();

    @Override
    public CompletableFuture<User> signUpStudent(String email, String password,
                                                 String firstName, String lastName,
                                                 String phone, String programOfStudy) {

        CompletableFuture<User> future = new CompletableFuture<>();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser fu = result.getUser();
                    if (fu == null) {
                        future.completeExceptionally(new IllegalStateException("Auth user is null"));
                        return;
                    }
                    User u = User.student(fu.getUid(), email, firstName, lastName, phone, programOfStudy);
                    users.save(u)
                            .thenRun(() -> future.complete(u))
                            .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    @Override
    public CompletableFuture<User> signUpTutor(String email, String password,
                                               String firstName, String lastName,
                                               String phone, String highestDegree,
                                               List<String> courses) {

        CompletableFuture<User> future = new CompletableFuture<>();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser fu = result.getUser();
                    if (fu == null) {
                        future.completeExceptionally(new IllegalStateException("Auth user is null"));
                        return;
                    }
                    User u = User.tutor(fu.getUid(), email, firstName, lastName, phone, highestDegree, courses);
                    users.save(u)
                            .thenRun(() -> future.complete(u))
                            .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    @Override
    public CompletableFuture<User> signIn(String email, String password) {
        CompletableFuture<User> future = new CompletableFuture<>();
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser fu = result.getUser();
                    if (fu == null) {
                        future.completeExceptionally(new IllegalStateException("Auth user is null"));
                        return;
                    }
                    users.findByUid(fu.getUid())
                            .thenAccept(future::complete)
                            .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                })
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    @Override
    public void signOut() {
        try {
            auth.signOut();
        } catch (Exception e) {
            Log.w("Auth", "signOut: " + e.getMessage());
        }
    }
}