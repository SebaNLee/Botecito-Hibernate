package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.UserModel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<UserModel> findById(int id);

    Optional<UserModel> findByEmail(String email);

    UserModel createUser(UserModel user);

    Optional<UserModel> claimUser(UserModel user);

    Optional<UserModel> updateProfile(UserModel user);

    Optional<UserModel> updatePasswordRecoveryToken(UserModel user);

    Optional<UserModel> findByPasswordRecoveryToken(String token);

    boolean resetPasswordByRecoveryToken(UserModel user);

    List<UserModel> findUsersByIds(Collection<Integer> userIds);
}
