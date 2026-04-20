package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(int id);

    Optional<User> findByEmail(String email);

    User createUser(String givenName, String lastName, String email, String passwordHash);

    Optional<User> claimUser(String givenName, String lastName, String email, String passwordHash);
}
