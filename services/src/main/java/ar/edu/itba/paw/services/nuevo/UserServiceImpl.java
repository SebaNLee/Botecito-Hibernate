package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.utils.UserNameRules;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("nuevoUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    public RegistrationResult register(final UserModel user, final String rawPassword) {
        UserNameRules.requireBothLegalNames(user.getGivenName(), user.getLastName());
        final String normalizedEmail = user.getEmail().trim().toLowerCase();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        final String normalizedPaymentAlias = normalizePaymentAlias(user.getPaymentAlias());
        final String normalizedPreferredLanguage = user.getPreferredLanguage().getPersistenceCode();

        final Optional<ar.edu.itba.paw.models.User> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            LOGGER.info("Registering new user with email {}", normalizedEmail);
            userDao.createUser(
                    user.getGivenName(),
                    user.getLastName(),
                    normalizedEmail,
                    passwordHash,
                    normalizedPaymentAlias,
                    normalizedPreferredLanguage);
            return RegistrationResult.SUCCESS;
        }

        if (existingUser.get().getPasswordHash() == null) {
            LOGGER.info("Claiming account for user with email {}", normalizedEmail);
            userDao.claimUser(
                            user.getGivenName(),
                            user.getLastName(),
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
    public Optional<UserModel> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByEmail(email.trim().toLowerCase()).map(UserServiceImpl::toUserModel);
    }

    @Override
    public Optional<UserModel> findById(final int id) {
        return userDao.findById(id).map(UserServiceImpl::toUserModel);
    }

    @Override
    public Optional<UserModel> updateProfile(final UserModel user) {
        if (user.getId() == null) {
            return Optional.empty();
        }

        final String normalizedEmail =
                user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        final Optional<ar.edu.itba.paw.models.User> emailOwner = userDao.findByEmail(normalizedEmail);
        if (normalizedEmail.isBlank()
                || emailOwner
                        .filter(owner -> owner.getId() == null || !owner.getId().equals(user.getId()))
                        .isPresent()) {
            LOGGER.warn(
                    "Attempt to update profile with email {} by user {} failed: email in use or invalid",
                    normalizedEmail,
                    user.getId());
            return Optional.empty();
        }

        UserNameRules.requireBothLegalNames(user.getGivenName(), user.getLastName());

        LOGGER.info("Updating profile for user {}", user.getId());
        return userDao.updateProfile(
                        user.getId(),
                        user.getGivenName().trim(),
                        user.getLastName().trim(),
                        normalizedEmail,
                        normalizeNullable(user.getPhone()),
                        normalizePaymentAlias(user.getPaymentAlias()),
                        user.getPreferredLanguage().getPersistenceCode())
                .map(UserServiceImpl::toUserModel);
    }

    @Override
    public Optional<UserModel> requestPasswordRecovery(final UserModel user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Optional.empty();
        }

        final Optional<ar.edu.itba.paw.models.User> existingUser =
                userDao.findByEmail(user.getEmail().trim().toLowerCase());
        if (existingUser.isEmpty() || existingUser.get().getPasswordHash() == null) {
            LOGGER.warn("Password recovery requested for non-existent or unclaimable user: {}", user.getEmail());
            return Optional.empty();
        }

        LOGGER.info(
                "Password recovery requested for user {}", existingUser.get().getId());
        final Optional<ar.edu.itba.paw.models.User> updatedUser = userDao.updatePasswordRecoveryToken(
                existingUser.get().getId(), UUID.randomUUID().toString());
        updatedUser.ifPresent(this::sendPasswordRecoveryEmail);
        return updatedUser.map(UserServiceImpl::toUserModel);
    }

    @Override
    public Optional<UserModel> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        final Optional<ar.edu.itba.paw.models.User> user = userDao.findByPasswordRecoveryToken(token.trim());
        if (user.isEmpty() || user.get().getPasswordRecoveryUsedAt() != null) {
            return Optional.empty();
        }
        return user.map(UserServiceImpl::toUserModel);
    }

    @Override
    public PasswordRecoveryResult resetPassword(final UserModel user, final String rawPassword) {
        final String token = user.getPasswordRecoveryToken();
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

    private static UserModel toUserModel(final ar.edu.itba.paw.models.User user) {
        final UserModel userModel = new UserModel();
        userModel.setId(user.getId());
        userModel.setCreatedAt(user.getCreatedAt());
        userModel.setGivenName(user.getGivenName());
        userModel.setLastName(user.getLastName());
        userModel.setEmail(user.getEmail());
        userModel.setPhone(user.getPhone());
        userModel.setPaymentAlias(user.getPaymentAlias());
        if (user.getPreferredLanguage() != null) {
            userModel.setPreferredLanguage(PreferredLanguageModel.fromPersistence(
                    user.getPreferredLanguage().getPersistenceCode()));
        }
        userModel.setPasswordHash(user.getPasswordHash());
        userModel.setPasswordRecoveryToken(user.getPasswordRecoveryToken());
        userModel.setPasswordRecoveryUsedAt(user.getPasswordRecoveryUsedAt());
        return userModel;
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

    private void sendPasswordRecoveryEmail(final ar.edu.itba.paw.models.User user) {
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
