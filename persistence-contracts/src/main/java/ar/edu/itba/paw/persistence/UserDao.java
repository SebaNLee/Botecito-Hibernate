package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(int id);

    Optional<User> findByEmail(String email);

    User createUser(
            String givenName,
            String lastName,
            String email,
            String passwordHash,
            String paymentAlias,
            String preferredLanguage);

    Optional<User> claimUser(
            String givenName,
            String lastName,
            String email,
            String passwordHash,
            String paymentAlias,
            String preferredLanguage);

    Optional<User> updateProfile(
            int userId,
            String givenName,
            String lastName,
            String email,
            String phone,
            String paymentAlias,
            String preferredLanguage);

    Optional<User> updatePasswordRecoveryToken(int userId, String token);

    Optional<User> findByPasswordRecoveryToken(String token);

    boolean resetPasswordByRecoveryToken(String token, String passwordHash, OffsetDateTime usedAt);

    /** Inserts a row with no password (e.g. guest or booking-created profile). */
    User createUserWithoutCredentials(String givenName, String lastName, String email, String preferredLanguage);

    boolean updateBasicProfileNamesAndLanguage(int userId, String givenName, String lastName, String preferredLanguage);

    List<User> findUsersByIds(Collection<Integer> userIds);
}
