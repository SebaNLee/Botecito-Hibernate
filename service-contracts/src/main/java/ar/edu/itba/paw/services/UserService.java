package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Users;
import java.util.Optional;

public interface UserService {

    void register(String firstName, String lastName, String email, String alias, String language, String rawPassword);

    Optional<Users> findByEmail(String email);

    Optional<Users> findById(int id);

    Optional<Users> updateProfile(
            int userId, String firstName, String lastName, String email, String phone, String alias, String language);

    Optional<Users> requestPasswordRecovery(String email);

    Optional<Users> findByPasswordRecoveryToken(String token);

    boolean resetPassword(String token, String rawPassword);

    Optional<Users> verifyEmail(String token);
}
