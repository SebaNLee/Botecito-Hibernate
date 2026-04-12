package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public User register(final String givenName, final String lastName, final String email, final String rawPassword) {
        // TODO: implement with UserDao + PasswordEncoder
        throw new UnsupportedOperationException("UserDao implementation pending");
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        // TODO: implement with UserDao
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(final int id) {
        // TODO: implement with UserDao
        return Optional.empty();
    }
}
