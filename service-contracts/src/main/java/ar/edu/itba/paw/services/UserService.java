package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.UsersOrm;
import java.util.Optional;

public interface UserService {

    void register(String firstName, String lastName, String email, String alias, String language, String rawPassword);

    Optional<UsersOrm> findByEmail(String email);

    Optional<UsersOrm> findById(int id);

    Optional<UsersOrm> updateProfile(
            int userId, String firstName, String lastName, String email, String phone, String alias, String language);

    Optional<UsersOrm> requestPasswordRecovery(String email);

    Optional<UsersOrm> findByPasswordRecoveryToken(String token);

    boolean resetPassword(String token, String rawPassword);

    Optional<UsersOrm> verifyEmail(String token);
}
