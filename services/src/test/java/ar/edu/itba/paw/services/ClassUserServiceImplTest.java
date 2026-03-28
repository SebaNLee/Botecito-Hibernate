package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ClassUserDao;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClassUserServiceImplTest {

    @InjectMocks
    private ClassUserServiceImpl userService;

    @Mock
    private ClassUserDao userDao;

    @Test
    public void testFindByIdWhenUserExists() {
        // 1. Arrange
        final User user = new User(1L, "test", "test", "test");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(user));

        // 2. Excercise
        final Optional<User> result = userService.findById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1L, result.get().getId());
    }

    @Test
    public void testFindByIdwhenUserNotExists() {
        // 1. Arrange
        Mockito.when(userDao.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Excercise
        final Optional<User> result = userService.findById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        // 1. Arrange
        final User user = new User(1L, "test", "test", "test");
        Mockito.when(userDao.createUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(user);

        // 2. Excercise
        final User result = userService.createUser("test", "test", "test");

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
    }
}
