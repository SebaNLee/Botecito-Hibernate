package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.models.nuevo.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.persistence.nuevo.UserDao;
import ar.edu.itba.paw.services.MissingUserNamesException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Test
    public void testRegisterCreatesUser() {
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("a@a.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser(Mockito.any(UserModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final UserService.RegistrationResult result = userService.register(
                registerUser("A", "B", " A@A.com ", "   ", PreferredLanguageModel.EN), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.SUCCESS, result);
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userDao).createUser(userCaptor.capture());
        Assertions.assertEquals("A", userCaptor.getValue().getGivenName());
        Assertions.assertEquals("B", userCaptor.getValue().getLastName());
        Assertions.assertEquals("a@a.com", userCaptor.getValue().getEmail());
        Assertions.assertEquals("hashed-password", userCaptor.getValue().getPasswordHash());
        Assertions.assertNull(userCaptor.getValue().getPaymentAlias());
        Assertions.assertEquals(PreferredLanguageModel.EN, userCaptor.getValue().getPreferredLanguage());
        Assertions.assertFalse(userCaptor.getValue().isVerified());
        Assertions.assertNotNull(userCaptor.getValue().getPasswordRecoveryToken());
        final ArgumentCaptor<EmailVerificationMailModel> mailCaptor =
                ArgumentCaptor.forClass(EmailVerificationMailModel.class);
        Mockito.verify(mailService).sendEmailVerificationEmail(mailCaptor.capture());
        Assertions.assertEquals("a@a.com", mailCaptor.getValue().getRecipient().getEmail());
        Assertions.assertNotNull(mailCaptor.getValue().getVerificationToken());
    }

    @Test
    public void testRegisterClaimsLegacyUser() {
        final UserModel existingUser = new UserModel();
        existingUser.setId(3);
        existingUser.setEmail("legacy@a.com");
        existingUser.setPasswordHash(null);

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.claimUser(Mockito.any(UserModel.class)))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        final UserService.RegistrationResult result = userService.register(
                registerUser("A", "B", " legacy@a.com ", " mi.alias ", PreferredLanguageModel.EN), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.SUCCESS, result);
        Mockito.verify(mailService).sendEmailVerificationEmail(Mockito.any(EmailVerificationMailModel.class));
    }

    @Test
    public void testRegisterRejectsDuplicateEmail() {
        final UserModel existingUser = new UserModel();
        existingUser.setEmail("a@a.com");
        existingUser.setPasswordHash("already-hashed");

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("a@a.com")).thenReturn(Optional.of(existingUser));

        final UserService.RegistrationResult result =
                userService.register(registerUser("A", "B", "a@a.com", null, PreferredLanguageModel.ES), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.EMAIL_ALREADY_EXISTS, result);
        Mockito.verify(userDao, Mockito.never()).createUser(Mockito.any(UserModel.class));
    }

    @Test
    public void testRegisterThrowsWhenGivenNameIsBlank() {
        Assertions.assertThrows(
                MissingUserNamesException.class,
                () -> userService.register(
                        registerUser(" ", "B", "a@a.com", null, PreferredLanguageModel.ES), "password123"));
    }

    @Test
    public void testFindByEmailNormalizesAndMapsResult() {
        final UserModel oldUser = oldUser(5, "Ada", "Lovelace", "ada@example.com");
        oldUser.setPreferredLanguage(PreferredLanguageModel.EN);
        Mockito.when(userDao.findByEmail("ada@example.com")).thenReturn(Optional.of(oldUser));

        final Optional<UserModel> result = userService.findByEmail(" Ada@Example.COM ");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get().getId());
        Assertions.assertEquals("Ada Lovelace", result.get().getName());
        Assertions.assertEquals(PreferredLanguageModel.EN, result.get().getPreferredLanguage());
    }

    @Test
    public void testRequestPasswordRecoveryGeneratesTokenAndSendsEmail() {
        final UserModel existingUser = oldUser(5, "Ada", "Lovelace", "recover@a.com");
        existingUser.setPasswordHash("stored-hash");
        existingUser.setVerified(true);
        Mockito.when(userDao.findByEmail("recover@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.updatePasswordRecoveryToken(Mockito.any(UserModel.class)))
                .thenAnswer(invocation -> {
                    final UserModel updatedUser = oldUser(5, "Ada", "Lovelace", "recover@a.com");
                    updatedUser.setPasswordHash("stored-hash");
                    updatedUser.setPasswordRecoveryToken(
                            invocation.<UserModel>getArgument(0).getPasswordRecoveryToken());
                    return Optional.of(updatedUser);
                });

        final Optional<UserModel> result = userService.requestPasswordRecovery(recoveryUser(" recover@a.com "));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get().getPasswordRecoveryToken());
        Assertions.assertFalse(result.get().getPasswordRecoveryToken().isBlank());
        final ArgumentCaptor<PasswordRecoveryMailModel> mailCaptor =
                ArgumentCaptor.forClass(PasswordRecoveryMailModel.class);
        Mockito.verify(mailService).sendPasswordRecoveryEmail(mailCaptor.capture());
        Assertions.assertEquals(
                "recover@a.com", mailCaptor.getValue().getRecipient().getEmail());
        Assertions.assertEquals(
                "Ada Lovelace", mailCaptor.getValue().getRecipient().getDisplayName());
        Assertions.assertNotNull(mailCaptor.getValue().getRecoveryToken());
        Assertions.assertFalse(mailCaptor.getValue().getRecoveryToken().isBlank());
    }

    @Test
    public void testPasswordRecoveryIgnoresLegacyUserWithoutCredentials() {
        final UserModel existingUser = oldUser(5, "Legacy", "User", "legacy@a.com");
        existingUser.setPasswordHash(null);
        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));

        final Optional<UserModel> result = userService.requestPasswordRecovery(recoveryUser("legacy@a.com"));

        Assertions.assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(mailService);
    }

    @Test
    public void testPasswordRecoveryIgnoresUnverifiedUser() {
        final UserModel existingUser = oldUser(5, "Unverified", "User", "unverified@a.com");
        existingUser.setPasswordHash("stored-hash");
        existingUser.setVerified(false);
        Mockito.when(userDao.findByEmail("unverified@a.com")).thenReturn(Optional.of(existingUser));

        final Optional<UserModel> result = userService.requestPasswordRecovery(recoveryUser("unverified@a.com"));

        Assertions.assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(mailService);
    }

    @Test
    public void testResetPasswordConsumesToken() {
        Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        Mockito.when(userDao.resetPasswordByRecoveryToken(Mockito.any(UserModel.class)))
                .thenReturn(true);

        final UserService.PasswordRecoveryResult result =
                userService.resetPassword(resetUser(" token-1 "), "new-password");

        Assertions.assertEquals(UserService.PasswordRecoveryResult.SUCCESS, result);
    }

    @Test
    public void testFindUsedRecoveryTokenReturnsEmpty() {
        final UserModel user = oldUser(5, "Ada", "Lovelace", "ada@example.com");
        user.setPasswordRecoveryToken("token-2");
        user.setPasswordRecoveryUsedAt(OffsetDateTime.now());
        Mockito.when(userDao.findByPasswordRecoveryToken("token-2")).thenReturn(Optional.of(user));

        final Optional<UserModel> result = userService.findByPasswordRecoveryToken(" token-2 ");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateProfileNormalizesOptionalFieldsAndPreferredLanguage() {
        final UserModel currentOwner = oldUser(5, "Ada", "Lovelace", "new@example.com");
        currentOwner.setVerified(true);
        final UserModel updatedUser = oldUser(5, "Ada", "Lovelace", "new@example.com");
        updatedUser.setPhone(null);
        updatedUser.setPaymentAlias("pay.alias");
        updatedUser.setPreferredLanguage(PreferredLanguageModel.ES);
        updatedUser.setVerified(true);

        Mockito.when(userDao.findById(5)).thenReturn(Optional.of(currentOwner));
        Mockito.when(userDao.findByEmail("new@example.com")).thenReturn(Optional.of(currentOwner));
        Mockito.when(userDao.updateProfile(Mockito.any(UserModel.class))).thenReturn(Optional.of(updatedUser));

        final Optional<UserModel> result = userService.updateProfile(profileUser(
                5, " Ada ", " Lovelace ", " New@Example.COM ", "   ", " pay.alias ", PreferredLanguageModel.ES));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("new@example.com", result.get().getEmail());
        Assertions.assertNull(result.get().getPhone());
        Assertions.assertEquals("pay.alias", result.get().getPaymentAlias());
        Assertions.assertEquals(PreferredLanguageModel.ES, result.get().getPreferredLanguage());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userDao).updateProfile(userCaptor.capture());
        Assertions.assertTrue(userCaptor.getValue().isVerified());
        Mockito.verify(mailService, Mockito.never()).sendEmailVerificationEmail(Mockito.any());
    }

    @Test
    public void testUpdateProfileReturnsEmptyWhenEmailBelongsToAnotherUser() {
        final UserModel otherUser = oldUser(9, "Other", "User", "taken@example.com");
        Mockito.when(userDao.findById(5)).thenReturn(Optional.of(oldUser(5, "Ada", "Lovelace", "ada@example.com")));
        Mockito.when(userDao.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        final Optional<UserModel> result = userService.updateProfile(
                profileUser(5, "Ada", "Lovelace", "taken@example.com", null, null, PreferredLanguageModel.EN));

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(userDao, Mockito.never()).updateProfile(Mockito.any(UserModel.class));
    }

    @Test
    public void testUpdateProfileChangingEmailResetsVerificationAndSendsEmail() {
        final UserModel currentOwner = oldUser(5, "Ada", "Lovelace", "old@example.com");
        currentOwner.setVerified(true);
        final UserModel updatedUser = oldUser(5, "Ada", "Lovelace", "new@example.com");
        updatedUser.setVerified(false);
        updatedUser.setPasswordRecoveryToken("verification-token");

        Mockito.when(userDao.findById(5)).thenReturn(Optional.of(currentOwner));
        Mockito.when(userDao.findByEmail("new@example.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.updateProfile(Mockito.any(UserModel.class))).thenReturn(Optional.of(updatedUser));

        final Optional<UserModel> result = userService.updateProfile(
                profileUser(5, "Ada", "Lovelace", "new@example.com", null, null, PreferredLanguageModel.EN));

        Assertions.assertTrue(result.isPresent());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userDao).updateProfile(userCaptor.capture());
        Assertions.assertFalse(userCaptor.getValue().isVerified());
        Assertions.assertNotNull(userCaptor.getValue().getPasswordRecoveryToken());
        Mockito.verify(mailService).sendEmailVerificationEmail(Mockito.any(EmailVerificationMailModel.class));
    }

    @Test
    public void testVerifyEmailConsumesVerificationToken() {
        final UserModel user = oldUser(5, "Ada", "Lovelace", "ada@example.com");
        user.setPasswordRecoveryToken("verification-token");
        Mockito.when(userDao.findByEmailVerificationToken("verification-token")).thenReturn(Optional.of(user));
        Mockito.when(userDao.verifyEmailByToken("verification-token")).thenReturn(Optional.of(user));

        final Optional<UserModel> result = userService.verifyEmail(" verification-token ");

        Assertions.assertTrue(result.isPresent());
        Mockito.verify(userDao).verifyEmailByToken("verification-token");
    }

    @Test
    public void testResetPasswordReturnsInvalidTokenWhenTokenIsBlank() {
        final UserService.PasswordRecoveryResult result = userService.resetPassword(resetUser("   "), "new-password");

        Assertions.assertEquals(UserService.PasswordRecoveryResult.INVALID_TOKEN, result);
    }

    @Test
    public void testResetPasswordPassesTrimmedToken() {
        Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        Mockito.when(userDao.resetPasswordByRecoveryToken(Mockito.any(UserModel.class)))
                .thenReturn(true);

        userService.resetPassword(resetUser(" token-3 "), "new-password");

        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userDao).resetPasswordByRecoveryToken(userCaptor.capture());
        Assertions.assertEquals("token-3", userCaptor.getValue().getPasswordRecoveryToken());
        Assertions.assertEquals("new-password-hash", userCaptor.getValue().getPasswordHash());
    }

    private static UserModel registerUser(
            final String givenName,
            final String lastName,
            final String email,
            final String paymentAlias,
            final PreferredLanguageModel preferredLanguage) {
        final UserModel user = new UserModel();
        user.setGivenName(givenName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPaymentAlias(paymentAlias);
        user.setPreferredLanguage(preferredLanguage);
        return user;
    }

    private static UserModel recoveryUser(final String email) {
        final UserModel user = new UserModel();
        user.setEmail(email);
        return user;
    }

    private static UserModel resetUser(final String token) {
        final UserModel user = new UserModel();
        user.setPasswordRecoveryToken(token);
        return user;
    }

    private static UserModel profileUser(
            final Integer id,
            final String givenName,
            final String lastName,
            final String email,
            final String phone,
            final String paymentAlias,
            final PreferredLanguageModel preferredLanguage) {
        final UserModel user = registerUser(givenName, lastName, email, paymentAlias, preferredLanguage);
        user.setId(id);
        user.setPhone(phone);
        return user;
    }

    private static UserModel oldUser(
            final Integer id, final String givenName, final String lastName, final String email) {
        final UserModel user = new UserModel();
        user.setId(id);
        user.setGivenName(givenName);
        user.setLastName(lastName);
        user.setEmail(email);
        return user;
    }
}
