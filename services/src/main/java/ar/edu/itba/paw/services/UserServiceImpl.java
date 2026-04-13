package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(final UserDao userDao, final PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(final String givenName, final String lastName, final String email, final String rawPassword) {
        final String normalizedEmail = email.trim().toLowerCase();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        final Optional<User> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            return userDao.createUser(givenName, lastName, normalizedEmail, passwordHash);
        }

        if (existingUser.get().getPasswordHash() == null) {
            return userDao.claimUser(givenName, lastName, normalizedEmail, passwordHash)
                    .orElseThrow(() -> new IllegalStateException("Could not claim account for " + normalizedEmail));
        }

        throw new IllegalArgumentException("User already exists with email " + normalizedEmail);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public Optional<User> findById(final int id) {
        return userDao.findById(id);
    }
}
