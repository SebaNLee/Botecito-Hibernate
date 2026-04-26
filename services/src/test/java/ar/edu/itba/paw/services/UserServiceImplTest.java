package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    public void testRegisterCreatesUserWhenEmailDoesNotExist() {
        final User createdUser = new User();
        createdUser.setId(1);
        createdUser.setEmail("a@a.com");

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("a@a.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser("A", "B", "a@a.com", "hashed-password", null))
                .thenReturn(createdUser);

        final User result = userService.register("A", "B", "a@a.com", "password123", "   ");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getId());
        Mockito.verify(userDao).createUser("A", "B", "a@a.com", "hashed-password", null);
        Mockito.verify(userDao, Mockito.never())
                .claimUser(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void testRegisterClaimsUserWhenEmailExistsWithoutPassword() {
        final User existingUser = new User();
        existingUser.setId(3);
        existingUser.setEmail("legacy@a.com");
        existingUser.setPasswordHash(null);

        final User claimedUser = new User();
        claimedUser.setId(3);
        claimedUser.setEmail("legacy@a.com");
        claimedUser.setPasswordHash("hashed-password");

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.claimUser("A", "B", "legacy@a.com", "hashed-password", "mi.alias"))
                .thenReturn(Optional.of(claimedUser));

        final User result = userService.register("A", "B", "legacy@a.com", "password123", " mi.alias ");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("hashed-password", result.getPasswordHash());
        Mockito.verify(userDao).claimUser("A", "B", "legacy@a.com", "hashed-password", "mi.alias");
        Mockito.verify(userDao, Mockito.never())
                .createUser(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void testRegisterFailsWhenEmailExistsWithPassword() {
        final User existingUser = new User();
        existingUser.setEmail("a@a.com");
        existingUser.setPasswordHash("already-hashed");

        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        Mockito.when(userDao.findByEmail("a@a.com")).thenReturn(Optional.of(existingUser));

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> userService.register("A", "B", "a@a.com", "password123", null));
        Mockito.verify(userDao, Mockito.never())
                .createUser(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());
        Mockito.verify(userDao, Mockito.never())
                .claimUser(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void testRequestPasswordRecoveryGeneratesNewTokenForExistingUser() {
        final User existingUser = new User();
        existingUser.setId(5);
        existingUser.setEmail("recover@a.com");
        existingUser.setPasswordHash("stored-hash");

        final User updatedUser = new User();
        updatedUser.setId(5);
        updatedUser.setEmail("recover@a.com");
        updatedUser.setPasswordHash("stored-hash");
        updatedUser.setPasswordRecoveryToken("generated-token");

        Mockito.when(userDao.findByEmail("recover@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.updatePasswordRecoveryToken(Mockito.eq(5), Mockito.anyString()))
                .thenReturn(Optional.of(updatedUser));

        final Optional<User> result = userService.requestPasswordRecovery(" recover@a.com ");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("generated-token", result.get().getPasswordRecoveryToken());
        Mockito.verify(userDao).updatePasswordRecoveryToken(Mockito.eq(5), Mockito.anyString());
    }

    @Test
    public void testRequestPasswordRecoverySkipsLegacyUserWithoutPassword() {
        final User existingUser = new User();
        existingUser.setId(5);
        existingUser.setEmail("legacy@a.com");
        existingUser.setPasswordHash(null);

        Mockito.when(userDao.findByEmail("legacy@a.com")).thenReturn(Optional.of(existingUser));

        final Optional<User> result = userService.requestPasswordRecovery("legacy@a.com");

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(userDao, Mockito.never()).updatePasswordRecoveryToken(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testResetPasswordConsumesRecoveryToken() {
        Mockito.when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        Mockito.when(userDao.resetPasswordByRecoveryToken(
                        Mockito.eq("token-1"), Mockito.eq("new-password-hash"), Mockito.any()))
                .thenReturn(true);

        final UserService.PasswordRecoveryResult result = userService.resetPassword("token-1", "new-password");

        Assertions.assertEquals(UserService.PasswordRecoveryResult.SUCCESS, result);
        Mockito.verify(userDao)
                .resetPasswordByRecoveryToken(Mockito.eq("token-1"), Mockito.eq("new-password-hash"), Mockito.any());
    }

    @Test
    public void testFindByPasswordRecoveryTokenReturnsEmptyWhenAlreadyUsed() {
        final User user = new User();
        user.setPasswordRecoveryToken("token-2");
        user.setPasswordRecoveryUsedAt(java.time.OffsetDateTime.now());
        Mockito.when(userDao.findByPasswordRecoveryToken("token-2")).thenReturn(Optional.of(user));

        final Optional<User> result = userService.findByPasswordRecoveryToken("token-2");

        Assertions.assertTrue(result.isEmpty());
    }
}
