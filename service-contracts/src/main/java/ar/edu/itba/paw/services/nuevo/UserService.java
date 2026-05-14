package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.UserModel;
import java.util.Optional;

public interface UserService {
    // TODO: Idealmente en vez de enums, no hacer nada si sale bien, tirar excepcion
    // si sale mal
    enum PasswordRecoveryResult {
        SUCCESS,
        INVALID_TOKEN
    }

    enum RegistrationResult {
        SUCCESS,
        EMAIL_ALREADY_EXISTS
    }

    RegistrationResult register(UserModel user, String rawPassword);

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findById(int id);

    Optional<UserModel> updateProfile(UserModel user);

    Optional<UserModel> requestPasswordRecovery(UserModel user);

    Optional<UserModel> findByPasswordRecoveryToken(String token);

    PasswordRecoveryResult resetPassword(UserModel user, String rawPassword);
}
