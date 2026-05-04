package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.utils.UserNameRules;
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
    private final MailService mailService;

    @Override
    public RegistrationResult register(
            final String givenName,
            final String lastName,
            final String email,
            final String rawPassword,
            final String paymentAlias,
            final String preferredLanguage) {
        UserNameRules.requireBothLegalNames(givenName, lastName);
        final String normalizedEmail = email.trim().toLowerCase();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        final String normalizedPaymentAlias = normalizePaymentAlias(paymentAlias);
        final String normalizedPreferredLanguage =
                PreferredLanguage.fromInput(preferredLanguage).getPersistenceCode();
        final Optional<User> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            LOGGER.info("Registering new user with email {}", normalizedEmail);
            userDao.createUser(
                    givenName,
                    lastName,
                    normalizedEmail,
                    passwordHash,
                    normalizedPaymentAlias,
                    normalizedPreferredLanguage);
            return RegistrationResult.SUCCESS;
        }

        if (existingUser.get().getPasswordHash() == null) {
            LOGGER.info("Claiming account for user with email {}", normalizedEmail);
            userDao.claimUser(
                            givenName,
                            lastName,
                            normalizedEmail,
                            passwordHash,
                            normalizedPaymentAlias,
                            normalizedPreferredLanguage)
                    .orElseThrow(() -> new IllegalStateException("Could not claim account for " + normalizedEmail));
            return RegistrationResult.SUCCESS;
        }

        LOGGER.warn("Attempted registration with existing email: {}", normalizedEmail);
        return RegistrationResult.EMAIL_ALREADY_EXISTS;
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

        UserNameRules.requireBothLegalNames(givenName, lastName);

        LOGGER.info("Updating profile for user {}", userId);
        return userDao.updateProfile(
                userId,
                givenName.trim(),
                lastName.trim(),
                normalizedEmail,
                normalizeNullable(phone),
                normalizePaymentAlias(paymentAlias),
                PreferredLanguage.fromInput(preferredLanguage).getPersistenceCode());
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
        final Optional<User> updatedUser = userDao.updatePasswordRecoveryToken(
                user.get().getId(), UUID.randomUUID().toString());
        updatedUser.ifPresent(this::sendPasswordRecoveryEmail);
        return updatedUser;
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

    private static String normalizeNullable(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private void sendPasswordRecoveryEmail(final User user) {
        try {
            mailService.sendPasswordRecoveryEmail(
                    user.getEmail(),
                    user.getName().isBlank() ? user.getEmail() : user.getName(),
                    user.getPasswordRecoveryToken());
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger password recovery email for user {}.", user.getId(), e);
        }
    }
}
