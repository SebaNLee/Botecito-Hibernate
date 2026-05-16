package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<Users> findById(int id);

    Optional<Users> findByEmail(String email);

    Users createUser(Users user);

    Optional<Users> findByPasswordRecoveryToken(String token);

    boolean resetPasswordByRecoveryToken(String token, String passwordHash, LocalDateTime usedAt);

    Optional<Users> findByEmailVerificationToken(String token);

    List<Users> findUsersByIds(Collection<Integer> userIds);
}
