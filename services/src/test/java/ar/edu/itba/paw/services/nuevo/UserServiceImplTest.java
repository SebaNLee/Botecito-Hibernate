package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.MailService;
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

        final UserService.RegistrationResult result = userService.register(
                registerUser("A", "B", " A@A.com ", "   ", PreferredLanguageModel.EN), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.SUCCESS, result);
        Mockito.verify(userDao).createUser("A", "B", "a@a.com", "hashed-password", null, "en");
    }

    @Test
    public void testRegisterClaimsLegacyUser() {
        final User existingUser = new User();
        existingUser.setId(3);
        existingUser.setEmail("legacy@a.com");
        existingUser.setPasswordHash(null);

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.claimUser("A", "B", "legacy@a.com", "hashed-password", "mi.alias", "en"))
                .thenReturn(Optional.of(existingUser));

        final UserService.RegistrationResult result = userService.register(
                registerUser("A", "B", " legacy@a.com ", " mi.alias ", PreferredLanguageModel.EN), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.SUCCESS, result);
    }

    @Test
    public void testRegisterRejectsDuplicateEmail() {
        final User existingUser = new User();
        existingUser.setEmail("a@a.com");
        existingUser.setPasswordHash("already-hashed");

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("a@a.com")).thenReturn(Optional.of(existingUser));

        final UserService.RegistrationResult result =
                userService.register(registerUser("A", "B", "a@a.com", null, PreferredLanguageModel.ES), "password123");

        Assertions.assertEquals(UserService.RegistrationResult.EMAIL_ALREADY_EXISTS, result);
        Mockito.verify(userDao, Mockito.never())
                .createUser(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.anyString());
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
        final User oldUser = oldUser(5, "Ada", "Lovelace", "ada@example.com");
        oldUser.setPreferredLanguage(PreferredLanguage.EN);
        Mockito.when(userDao.findByEmail("ada@example.com")).thenReturn(Optional.of(oldUser));

        final Optional<UserModel> result = userService.findByEmail(" Ada@Example.COM ");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get().getId());
        Assertions.assertEquals("Ada Lovelace", result.get().getName());
        Assertions.assertEquals(PreferredLanguageModel.EN, result.get().getPreferredLanguage());
    }

    @Test
    public void testRequestPasswordRecoveryGeneratesTokenAndSendsEmail() {
        final User existingUser = oldUser(5, "Ada", "Lovelace", "recover@a.com");
        existingUser.setPasswordHash("stored-hash");
        Mockito.when(userDao.findByEmail("recover@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.updatePasswordRecoveryToken(Mockito.eq(5), Mockito.anyString()))
                .thenAnswer(invocation -> {
                    final User updatedUser = oldUser(5, "Ada", "Lovelace", "recover@a.com");
                    updatedUser.setPasswordHash("stored-hash");
                    updatedUser.setPasswordRecoveryToken(invocation.getArgument(1));
                    return Optional.of(updatedUser);
                });

        final Optional<UserModel> result = userService.requestPasswordRecovery(recoveryUser(" recover@a.com "));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get().getPasswordRecoveryToken());
        Assertions.assertFalse(result.get().getPasswordRecoveryToken().isBlank());
        Mockito.verify(mailService)
                .sendPasswordRecoveryEmail(
                        Mockito.eq("recover@a.com"), Mockito.eq("Ada Lovelace"), Mockito.anyString());
    }

    @Test
    public void testPasswordRecoveryIgnoresLegacyUserWithoutCredentials() {
        final User existingUser = oldUser(5, "Legacy", "User", "legacy@a.com");
        existingUser.setPasswordHash(null);
        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));

        final Optional<UserModel> result = userService.requestPasswordRecovery(recoveryUser("legacy@a.com"));

        Assertions.assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(mailService);
    }

    @Test
    public void testResetPasswordConsumesToken() {
        Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        Mockito.when(userDao.resetPasswordByRecoveryToken(
                        Mockito.eq("token-1"), Mockito.eq("new-password-hash"), Mockito.any()))
                .thenReturn(true);

        final UserService.PasswordRecoveryResult result =
                userService.resetPassword(resetUser(" token-1 "), "new-password");

        Assertions.assertEquals(UserService.PasswordRecoveryResult.SUCCESS, result);
    }

    @Test
    public void testFindUsedRecoveryTokenReturnsEmpty() {
        final User user = oldUser(5, "Ada", "Lovelace", "ada@example.com");
        user.setPasswordRecoveryToken("token-2");
        user.setPasswordRecoveryUsedAt(OffsetDateTime.now());
        Mockito.when(userDao.findByPasswordRecoveryToken("token-2")).thenReturn(Optional.of(user));

        final Optional<UserModel> result = userService.findByPasswordRecoveryToken(" token-2 ");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateProfileNormalizesOptionalFieldsAndPreferredLanguage() {
        final User currentOwner = oldUser(5, "Ada", "Lovelace", "new@example.com");
        final User updatedUser = oldUser(5, "Ada", "Lovelace", "new@example.com");
        updatedUser.setPhone(null);
        updatedUser.setPaymentAlias("pay.alias");
        updatedUser.setPreferredLanguage(PreferredLanguage.ES);

        Mockito.when(userDao.findByEmail("new@example.com")).thenReturn(Optional.of(currentOwner));
        Mockito.when(userDao.updateProfile(5, "Ada", "Lovelace", "new@example.com", null, "pay.alias", "es"))
                .thenReturn(Optional.of(updatedUser));

        final Optional<UserModel> result = userService.updateProfile(profileUser(
                5, " Ada ", " Lovelace ", " New@Example.COM ", "   ", " pay.alias ", PreferredLanguageModel.ES));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("new@example.com", result.get().getEmail());
        Assertions.assertNull(result.get().getPhone());
        Assertions.assertEquals("pay.alias", result.get().getPaymentAlias());
        Assertions.assertEquals(PreferredLanguageModel.ES, result.get().getPreferredLanguage());
    }

    @Test
    public void testUpdateProfileReturnsEmptyWhenEmailBelongsToAnotherUser() {
        final User otherUser = oldUser(9, "Other", "User", "taken@example.com");
        Mockito.when(userDao.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        final Optional<UserModel> result = userService.updateProfile(
                profileUser(5, "Ada", "Lovelace", "taken@example.com", null, null, PreferredLanguageModel.EN));

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(userDao, Mockito.never())
                .updateProfile(
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyString());
    }

    @Test
    public void testResetPasswordReturnsInvalidTokenWhenTokenIsBlank() {
        final UserService.PasswordRecoveryResult result = userService.resetPassword(resetUser("   "), "new-password");

        Assertions.assertEquals(UserService.PasswordRecoveryResult.INVALID_TOKEN, result);
    }

    @Test
    public void testResetPasswordPassesTrimmedToken() {
        Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        Mockito.when(userDao.resetPasswordByRecoveryToken(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(true);

        userService.resetPassword(resetUser(" token-3 "), "new-password");

        final ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userDao)
                .resetPasswordByRecoveryToken(tokenCaptor.capture(), Mockito.eq("new-password-hash"), Mockito.any());
        Assertions.assertEquals("token-3", tokenCaptor.getValue());
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

    private static User oldUser(final Integer id, final String givenName, final String lastName, final String email) {
        final User user = new User();
        user.setId(id);
        user.setGivenName(givenName);
        user.setLastName(lastName);
        user.setEmail(email);
        return user;
    }
}
