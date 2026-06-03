package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
@Transactional
public class UserJpaDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    public void testCreateAndFindUser() {
        final Users user = new Users();
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setLanguage("EN");
        user.setVerified(false);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());

        final Users created = userDao.createUser(user);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());

        final Optional<Users> found = userDao.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Ada", found.get().getFirstName());
        assertEquals("Lovelace", found.get().getLastName());
        assertEquals("ada@example.com", found.get().getEmail());
    }
}
