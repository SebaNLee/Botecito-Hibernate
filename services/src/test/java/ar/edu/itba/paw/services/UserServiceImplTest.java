package ar.edu.itba.paw.services;

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
    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "password123";
    private static final String HASH = "hashedPassword";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String TOKEN = "recovery-token";

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
        Users user = new Users();
        user.setId(USER_ID);
        when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.findById(USER_ID);

        assertTrue(result.isPresent());
        assertEquals(USER_ID, result.get().getId());
    }

    @Test
    public void findByEmailReturnsUser() {
        Users user = new Users();
        user.setEmail(EMAIL);
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.findByEmail(EMAIL);

        assertTrue(result.isPresent());
    }

    @Test
    public void registerCreatesNewUser() {
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userDao.createUser(any())).thenReturn(new Users());

        assertDoesNotThrow(() -> userService.register(FIRST_NAME, LAST_NAME, EMAIL, null, "ES", PASSWORD));
    }

    @Test
    public void registerThrowsWhenEmailExists() {
        Users existing = new Users();
        existing.setPasswordHash(HASH);
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.register(FIRST_NAME, LAST_NAME, EMAIL, null, "ES", PASSWORD));
    }

    @Test
    public void updateProfileReturnsUser() {
        Users user = new Users();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setVerified(true);
        when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Users> result = userService.updateProfile(USER_ID, FIRST_NAME, LAST_NAME, EMAIL, null, null, "ES");

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
}
