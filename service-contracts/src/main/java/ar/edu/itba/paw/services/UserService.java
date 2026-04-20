package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface UserService {
    User register(String givenName, String lastName, String email, String rawPassword);

    Optional<User> findByEmail(String email);

    Optional<User> findById(int id);
}
