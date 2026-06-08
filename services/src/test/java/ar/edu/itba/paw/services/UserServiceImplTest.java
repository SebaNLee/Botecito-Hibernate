package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.persistence.UserDao;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    private static final int USER_ID = 1;
    private static final String EMAIL = "botecito.dev@gmail.com";
    private static final String PASSWORD = "password";
    private static final String HASH = "hashedPassword";
    private static final String FIRST_NAME = "Botecito";
    private static final String LAST_NAME = "Dev";
    private static final String TOKEN = "recoveryToken";

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void findByIdReturnsUser() {
        when(userDao.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));

        Optional<Users> result = userService.findById(USER_ID);

        assertTrue(result.isPresent());
        assertEquals(USER_ID, result.get().getId());
    }

    @Test
    public void findByEmailReturnsUser() {
        Users user = user(0);
        user.setEmail(EMAIL);
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.findByEmail(EMAIL);

        assertTrue(result.isPresent());
    }

    @Test
    public void registerCreatesNewUser() {
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userDao.createUser(any())).thenReturn(user(0));

        assertDoesNotThrow(() -> userService.register(FIRST_NAME, LAST_NAME, EMAIL, null, "es", PASSWORD));
    }

    @Test
    public void registerThrowsWhenEmailExists() {
        Users existing = new Users();
        existing.setPasswordHash(HASH);
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.register(FIRST_NAME, LAST_NAME, EMAIL, null, "es", PASSWORD));
    }

    @Test
    public void updateProfileReturnsUser() {
        Users user = user(USER_ID);
        user.setEmail(EMAIL);
        user.setVerified(true);
        when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.updateProfile(USER_ID, FIRST_NAME, LAST_NAME, EMAIL, null, null, "es");

        assertTrue(result.isPresent());
    }

    @Test
    public void requestPasswordRecoveryReturnsUser() {
        Users user = new Users();
        user.setEmail(EMAIL);
        user.setPasswordHash(HASH);
        user.setVerified(true);
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.requestPasswordRecovery(EMAIL);

        assertTrue(result.isPresent());
    }

    @Test
    public void resetPasswordReturnsTrue() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userDao.resetPasswordByRecoveryToken(eq(TOKEN), eq(HASH), any())).thenReturn(true);

        assertTrue(userService.resetPassword(TOKEN, PASSWORD));
    }

    @Test
    public void verifyEmailReturnsUser() {
        Users user = new Users();
        user.setVerified(false);
        when(userDao.findByEmailVerificationToken(TOKEN)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.verifyEmail(TOKEN);

        assertTrue(result.isPresent());
        assertTrue(result.get().getVerified());
    }

    @Test
    public void findByPasswordRecoveryTokenReturnsUser() {
        Users user = new Users();
        user.setMailTokenEmittedAt(null);
        when(userDao.findByPasswordRecoveryToken(TOKEN)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.findByPasswordRecoveryToken(TOKEN);

        assertTrue(result.isPresent());
    }
}
