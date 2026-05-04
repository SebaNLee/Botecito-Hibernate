package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface UserService {
    enum PasswordRecoveryResult {
        SUCCESS,
        INVALID_TOKEN
    }

    enum RegistrationResult {
        SUCCESS,
        EMAIL_ALREADY_EXISTS
    }

    RegistrationResult register(
            String givenName,
            String lastName,
            String email,
            String rawPassword,
            String paymentAlias,
            String preferredLanguage);

    Optional<User> findByEmail(String email);

    Optional<User> findById(int id);

    Optional<User> updateProfile(
            int userId,
            String givenName,
            String lastName,
            String email,
            String phone,
            String paymentAlias,
            String preferredLanguage);

    Optional<User> requestPasswordRecovery(String email);

    Optional<User> findByPasswordRecoveryToken(String token);

    PasswordRecoveryResult resetPassword(String token, String rawPassword);
}
