package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.MissingUserNamesException;
import ar.edu.itba.paw.models.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("nuevoUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    @Transactional
    public void register(
            final String firstName,
            final String lastName,
            final String email,
            final String alias,
            final String language,
            final String rawPassword) {
        requireBothLegalNames(firstName, lastName);
        final String normalizedEmail = email.trim().toLowerCase();
        final String passwordHash = passwordEncoder.encode(rawPassword);
        final String verificationToken = UUID.randomUUID().toString();

        final Optional<Users> existingUser = userDao.findByEmail(normalizedEmail);
        if (existingUser.isEmpty()) {
            LOGGER.info("Registering new user with email {}", normalizedEmail);
            final Users userToCreate = new Users();
            userToCreate.setFirstName(firstName);
            userToCreate.setLastName(lastName);
            userToCreate.setEmail(normalizedEmail);
            userToCreate.setPasswordHash(passwordHash);
            userToCreate.setAlias(normalizeNullable(alias));
            userToCreate.setLanguage(language != null ? language : "ES");
            userToCreate.setVerified(false);
            userToCreate.setMailToken(verificationToken);
            userToCreate.setCreatedAt(LocalDateTime.now());
            sendEmailVerificationEmail(userDao.createUser(userToCreate));
            return;
        }

        if (existingUser.get().getPasswordHash() == null) {
            LOGGER.info("Claiming account for user with email {}", normalizedEmail);
            final Users userToClaim = new Users();
            userToClaim.setFirstName(firstName);
            userToClaim.setLastName(lastName);
            userToClaim.setEmail(normalizedEmail);
            userToClaim.setPasswordHash(passwordHash);
            userToClaim.setAlias(normalizeNullable(alias));
            userToClaim.setLanguage(language != null ? language : "ES");
            userToClaim.setVerified(false);
            userToClaim.setMailToken(verificationToken);
            userToClaim.setCreatedAt(LocalDateTime.now());
            sendEmailVerificationEmail(userDao.claimUser(userToClaim)
                    .orElseThrow(() -> new IllegalStateException("Could not claim account for " + normalizedEmail)));
            return;
        }

        LOGGER.warn("Attempted registration with existing email: {}", normalizedEmail);
        throw new EmailAlreadyExistsException();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Users> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Users> findById(final int id) {
        return userDao.findById(id);
    }

    @Override
    @Transactional
    public Optional<Users> updateProfile(
            final int userId,
            final String firstName,
            final String lastName,
            final String email,
            final String phone,
            final String alias,
            final String language) {

        final Optional<Users> currentUser = userDao.findById(userId);
        if (currentUser.isEmpty()) {
            return Optional.empty();
        }

        final String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        final Optional<Users> emailOwner = userDao.findByEmail(normalizedEmail);
        if (normalizedEmail.isBlank()
                || emailOwner
                        .filter(owner -> owner.getId() == null || !owner.getId().equals(userId))
                        .isPresent()) {
            LOGGER.warn(
                    "Attempt to update profile with email {} by user {} failed: email in use or invalid",
                    normalizedEmail,
                    userId);
            return Optional.empty();
        }

        requireBothLegalNames(firstName, lastName);
        final boolean emailChanged =
                !normalizedEmail.equalsIgnoreCase(currentUser.get().getEmail());

        LOGGER.info("Updating profile for user {}", userId);
        final Users profileUpdate = new Users();
        profileUpdate.setId(userId);
        profileUpdate.setFirstName(firstName.trim());
        profileUpdate.setLastName(lastName.trim());
        profileUpdate.setEmail(normalizedEmail);
        profileUpdate.setPhone(normalizeNullable(phone));
        profileUpdate.setAlias(normalizeNullable(alias));
        profileUpdate.setLanguage(language);
        if (emailChanged) {
            profileUpdate.setVerified(false);
            profileUpdate.setMailToken(UUID.randomUUID().toString());
        } else {
            profileUpdate.setVerified(
                    currentUser.get().getVerified() != null && currentUser.get().getVerified());
            profileUpdate.setMailToken(currentUser.get().getMailToken());
            profileUpdate.setMailTokenEmittedAt(currentUser.get().getMailTokenEmittedAt());
        }
        final Optional<Users> updatedUser = userDao.updateProfile(profileUpdate);
        if (emailChanged) {
            updatedUser.ifPresent(this::sendEmailVerificationEmail);
        }
        return updatedUser;
    }

    @Override
    @Transactional
    public Optional<Users> requestPasswordRecovery(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        final Optional<Users> existingUser = userDao.findByEmail(email.trim().toLowerCase());
        if (existingUser.isEmpty()
                || existingUser.get().getPasswordHash() == null
                || !Boolean.TRUE.equals(existingUser.get().getVerified())) {
            LOGGER.warn("Password recovery requested for non-existent or unclaimable user: {}", email);
            return Optional.empty();
        }

        final Users existing = existingUser.get();
        LOGGER.info("Password recovery requested for user {}", existing.getId());
        final String recoveryToken = UUID.randomUUID().toString();
        final Optional<Users> updatedUser = userDao.updatePasswordRecoveryToken(existing.getId(), recoveryToken);
        updatedUser.ifPresent(this::sendPasswordRecoveryEmail);
        return updatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Users> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        final Optional<Users> user = userDao.findByPasswordRecoveryToken(token.trim());
        if (user.isEmpty() || user.get().getMailTokenEmittedAt() != null) {
            return Optional.empty();
        }
        return user;
    }

    @Override
    @Transactional
    public boolean resetPassword(final String token, final String rawPassword) {
        if (token == null || token.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        final String passwordHash = passwordEncoder.encode(rawPassword);
        final boolean updated = userDao.resetPasswordByRecoveryToken(token.trim(), passwordHash, LocalDateTime.now());
        if (!updated) {
            LOGGER.warn("Failed password reset attempt with invalid token");
            return false;
        }

        LOGGER.info("Password successfully reset using recovery token");
        return true;
    }

    @Override
    @Transactional
    public Optional<Users> verifyEmail(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        final String normalizedToken = token.trim();
        if (userDao.findByEmailVerificationToken(normalizedToken).isEmpty()) {
            return Optional.empty();
        }
        return userDao.verifyEmailByToken(normalizedToken);
    }

    private static void requireBothLegalNames(final String givenName, final String lastName) {
        if (givenName == null || givenName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new MissingUserNamesException();
        }
    }

    private static String normalizeNullable(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private void sendPasswordRecoveryEmail(final Users user) {
        try {
            final PasswordRecoveryMailModel mail = new PasswordRecoveryMailModel();
            mail.setRecipient(MailRecipientModel.fromUser(user));
            mail.setRecoveryToken(user.getMailToken());
            mailService.sendPasswordRecoveryEmail(mail);
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger password recovery email for user {}.", user.getId(), e);
        }
    }

    private void sendEmailVerificationEmail(final Users user) {
        try {
            final EmailVerificationMailModel mail = new EmailVerificationMailModel();
            mail.setRecipient(MailRecipientModel.fromUser(user));
            mail.setVerificationToken(user.getMailToken());
            mailService.sendEmailVerificationEmail(mail);
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger email verification for user {}.", user.getId(), e);
        }
    }
}
