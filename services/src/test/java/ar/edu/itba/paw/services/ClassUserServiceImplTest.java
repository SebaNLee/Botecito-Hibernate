package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ClassUser;
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
    private ClassUserServiceImpl classUserService;

    @Mock
    private ClassUserDao classUserDao;

    @Test
    public void testFindByIdWhenUserExists() {
        // 1. Arrange
        final ClassUser classUser = new ClassUser(1L, "test", "test", "test");
        Mockito.when(classUserDao.findClassUserById(1L)).thenReturn(Optional.of(classUser));

        // 2. Excercise
        final Optional<ClassUser> result = classUserService.findClassUserById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1L, result.get().getId());
    }

    @Test
    public void testFindByIdwhenUserNotExists() {
        // 1. Arrange
        Mockito.when(classUserDao.findClassUserById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Excercise
        final Optional<ClassUser> result = classUserService.findClassUserById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        // 1. Arrange
        final ClassUser classUser = new ClassUser(1L, "test", "test", "test");
        Mockito.when(classUserDao.createClassUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(classUser);

        // 2. Excercise
        final ClassUser result = classUserService.createClassUser("test", "test", "test");

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
    }
}
