package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.persistence.nuevo.UserDao;
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

        final Optional<UserModel> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            LOGGER.info("Registering new user with email {}", normalizedEmail);
            final UserModel userToCreate = new UserModel();
            userToCreate.setGivenName(user.getGivenName());
            userToCreate.setLastName(user.getLastName());
            userToCreate.setEmail(normalizedEmail);
            userToCreate.setPasswordHash(passwordHash);
            userToCreate.setPaymentAlias(normalizedPaymentAlias);
            userToCreate.setPreferredLanguage(PreferredLanguageModel.fromPersistence(normalizedPreferredLanguage));
            userDao.createUser(userToCreate);
            return RegistrationResult.SUCCESS;
        }

        if (existingUser.get().getPasswordHash() == null) {
            LOGGER.info("Claiming account for user with email {}", normalizedEmail);
            final UserModel userToClaim = new UserModel();
            userToClaim.setGivenName(user.getGivenName());
            userToClaim.setLastName(user.getLastName());
            userToClaim.setEmail(normalizedEmail);
            userToClaim.setPasswordHash(passwordHash);
            userToClaim.setPaymentAlias(normalizedPaymentAlias);
            userToClaim.setPreferredLanguage(PreferredLanguageModel.fromPersistence(normalizedPreferredLanguage));
            userDao.claimUser(userToClaim)
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
        return userDao.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public Optional<UserModel> findById(final int id) {
        return userDao.findById(id);
    }

    @Override
    public Optional<UserModel> updateProfile(final UserModel user) {
        if (user.getId() == null) {
            return Optional.empty();
        }

        final String normalizedEmail =
                user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        final Optional<UserModel> emailOwner = userDao.findByEmail(normalizedEmail);
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
        final UserModel profileUpdate = new UserModel();
        profileUpdate.setId(user.getId());
        profileUpdate.setGivenName(user.getGivenName().trim());
        profileUpdate.setLastName(user.getLastName().trim());
        profileUpdate.setEmail(normalizedEmail);
        profileUpdate.setPhone(normalizeNullable(user.getPhone()));
        profileUpdate.setPaymentAlias(normalizePaymentAlias(user.getPaymentAlias()));
        profileUpdate.setPreferredLanguage(user.getPreferredLanguage());
        return userDao.updateProfile(profileUpdate);
    }

    @Override
    public Optional<UserModel> requestPasswordRecovery(final UserModel user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Optional.empty();
        }

        final Optional<UserModel> existingUser =
                userDao.findByEmail(user.getEmail().trim().toLowerCase());
        if (existingUser.isEmpty() || existingUser.get().getPasswordHash() == null) {
            LOGGER.warn("Password recovery requested for non-existent or unclaimable user: {}", user.getEmail());
            return Optional.empty();
        }

        LOGGER.info(
                "Password recovery requested for user {}", existingUser.get().getId());
        final UserModel recoveryUpdate = new UserModel();
        recoveryUpdate.setId(existingUser.get().getId());
        recoveryUpdate.setPasswordRecoveryToken(UUID.randomUUID().toString());
        final Optional<UserModel> updatedUser = userDao.updatePasswordRecoveryToken(recoveryUpdate);
        updatedUser.ifPresent(this::sendPasswordRecoveryEmail);
        return updatedUser;
    }

    @Override
    public Optional<UserModel> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        final Optional<UserModel> user = userDao.findByPasswordRecoveryToken(token.trim());
        if (user.isEmpty() || user.get().getPasswordRecoveryUsedAt() != null) {
            return Optional.empty();
        }
        return user;
    }

    @Override
    public PasswordRecoveryResult resetPassword(final UserModel user, final String rawPassword) {
        final String token = user.getPasswordRecoveryToken();
        if (token == null || token.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return PasswordRecoveryResult.INVALID_TOKEN;
        }

        final UserModel passwordUpdate = new UserModel();
        passwordUpdate.setPasswordRecoveryToken(token.trim());
        passwordUpdate.setPasswordHash(passwordEncoder.encode(rawPassword));
        passwordUpdate.setPasswordRecoveryUsedAt(OffsetDateTime.now());
        final boolean updated = userDao.resetPasswordByRecoveryToken(passwordUpdate);
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

    private void sendPasswordRecoveryEmail(final UserModel user) {
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
