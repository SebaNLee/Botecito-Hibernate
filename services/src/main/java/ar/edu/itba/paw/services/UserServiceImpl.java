package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(
            final String givenName,
            final String lastName,
            final String email,
            final String rawPassword,
            final String paymentAlias) {
        final String normalizedEmail = email.trim().toLowerCase();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        final String normalizedPaymentAlias = normalizePaymentAlias(paymentAlias);
        final Optional<User> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            LOGGER.info("Registering new user with email {}", normalizedEmail);
            return userDao.createUser(givenName, lastName, normalizedEmail, passwordHash, normalizedPaymentAlias);
        }

        if (existingUser.get().getPasswordHash() == null) {
            LOGGER.info("Claiming account for user with email {}", normalizedEmail);
            return userDao.claimUser(givenName, lastName, normalizedEmail, passwordHash, normalizedPaymentAlias)
                    .orElseThrow(() -> new IllegalStateException("Could not claim account for " + normalizedEmail));
        }

        LOGGER.warn("Attempted registration with existing email: {}", normalizedEmail);
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

    @Override
    public Optional<User> updateProfile(
            final int userId,
            final String givenName,
            final String lastName,
            final String email,
            final String phone,
            final String paymentAlias,
            final String preferredLanguage) {
        final String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        final Optional<User> emailOwner = userDao.findByEmail(normalizedEmail);
        if (normalizedEmail.isBlank()
                || emailOwner
                        .filter(user -> user.getId() == null || user.getId() != userId)
                        .isPresent()) {
            LOGGER.warn(
                    "Attempt to update profile with email {} by user {} failed: email in use or invalid",
                    normalizedEmail,
                    userId);
            return Optional.empty();
        }

        LOGGER.info("Updating profile for user {}", userId);
        return userDao.updateProfile(
                userId,
                safeTrim(givenName),
                safeTrim(lastName),
                normalizedEmail,
                normalizeNullable(phone),
                normalizePaymentAlias(paymentAlias),
                normalizePreferredLanguage(preferredLanguage));
    }

    @Override
    public Optional<User> requestPasswordRecovery(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        final Optional<User> user = userDao.findByEmail(email.trim().toLowerCase());
        if (user.isEmpty() || user.get().getPasswordHash() == null) {
            LOGGER.warn("Password recovery requested for non-existent or unclaimable user: {}", email);
            return Optional.empty();
        }

        LOGGER.info("Password recovery requested for user {}", user.get().getId());
        return userDao.updatePasswordRecoveryToken(
                user.get().getId(), UUID.randomUUID().toString());
    }

    @Override
    public Optional<User> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        final Optional<User> user = userDao.findByPasswordRecoveryToken(token.trim());
        if (user.isEmpty() || user.get().getPasswordRecoveryUsedAt() != null) {
            return Optional.empty();
        }
        return user;
    }

    @Override
    public PasswordRecoveryResult resetPassword(final String token, final String rawPassword) {
        if (token == null || token.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return PasswordRecoveryResult.INVALID_TOKEN;
        }

        final String passwordHash = passwordEncoder.encode(rawPassword);
        final boolean updated = userDao.resetPasswordByRecoveryToken(token.trim(), passwordHash, OffsetDateTime.now());

        if (!updated) {
            LOGGER.warn("Failed password reset attempt with invalid token");
            return PasswordRecoveryResult.INVALID_TOKEN;
        }

        LOGGER.info("Password successfully reset using recovery token");

        return PasswordRecoveryResult.SUCCESS;
    }

    private static String normalizePaymentAlias(final String paymentAlias) {
        return normalizeNullable(paymentAlias);
    }

    private static String normalizePreferredLanguage(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return "en";
        }
        return "es";
    }

    private static String safeTrim(final String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullable(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
