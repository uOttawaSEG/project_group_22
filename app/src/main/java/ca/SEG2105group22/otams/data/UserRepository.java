package ca.SEG2105group22.otams.data;

import ca.SEG2105group22.otams.model.User;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    CompletableFuture<Void> save(User user);
    CompletableFuture<User> findByUid(String uid);
    CompletableFuture<User> findByEmail(String email);
}
