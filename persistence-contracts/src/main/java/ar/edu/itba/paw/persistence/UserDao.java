package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.UsersOrm;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<UsersOrm> findById(int id);

    Optional<UsersOrm> findByEmail(String email);

    UsersOrm createUser(UsersOrm user);

    Optional<UsersOrm> claimUser(UsersOrm user);

    Optional<UsersOrm> updateProfile(UsersOrm user);

    Optional<UsersOrm> updatePasswordRecoveryToken(int userId, String mailToken);

    Optional<UsersOrm> findByPasswordRecoveryToken(String token);

    boolean resetPasswordByRecoveryToken(String token, String passwordHash, LocalDateTime usedAt);

    Optional<UsersOrm> findByEmailVerificationToken(String token);

    Optional<UsersOrm> verifyEmailByToken(String token);

    List<UsersOrm> findUsersByIds(Collection<Integer> userIds);
}
