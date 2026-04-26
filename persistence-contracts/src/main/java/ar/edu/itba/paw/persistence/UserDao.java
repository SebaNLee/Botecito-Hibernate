package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(int id);

    Optional<User> findByEmail(String email);

    User createUser(String givenName, String lastName, String email, String passwordHash, String paymentAlias);

    Optional<User> claimUser(String givenName, String lastName, String email, String passwordHash, String paymentAlias);

    Optional<User> updatePasswordRecoveryToken(int userId, String token);

    Optional<User> findByPasswordRecoveryToken(String token);

    boolean resetPasswordByRecoveryToken(String token, String passwordHash, OffsetDateTime usedAt);
}
