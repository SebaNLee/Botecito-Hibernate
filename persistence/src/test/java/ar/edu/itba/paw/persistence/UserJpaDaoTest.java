package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.UsersOrm;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class, UserJpaDaoTest.TestConfig.class})
@Transactional
public class UserJpaDaoTest {

    @Configuration
    static class TestConfig {
        @Bean
        public List<String> entityClassNames() {
            return List.of("ar.edu.itba.paw.models.entity.UsersOrm");
        }
    }

    @Autowired
    private UserDao userDao;

    @Test
    public void testCreateAndFindUser() {
        final UsersOrm user = new UsersOrm();
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setLanguage("EN");
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        final UsersOrm created = userDao.createUser(user);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());

        final Optional<UsersOrm> found = userDao.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Ada", found.get().getFirstName());
        assertEquals("Lovelace", found.get().getLastName());
        assertEquals("ada@example.com", found.get().getEmail());
    }
}
