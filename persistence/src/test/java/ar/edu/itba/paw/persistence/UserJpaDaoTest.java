package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
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

    @PersistenceContext
    private EntityManager em;

    private Users user;

    @BeforeEach
    public void setup() {
        user = new Users();
        user.setFirstName("Botecito");
        user.setLastName("Dev");
        user.setEmail("botecito.dev@gmail.com");
        user.setLanguage("en");
        user.setVerified(false);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        em.persist(user);
        em.flush();
    }

    @Test
    public void testCreateUser() {
        Users user = new Users();
        user.setFirstName("Botecito");
        user.setLastName("User");
        user.setEmail("botecito.user@gmail.com");
        user.setLanguage("en");
        user.setVerified(false);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());

        Users created = userDao.createUser(user);

        assertNotNull(created.getId());
    }

    @Test
    public void testFindById() {
        Optional<Users> found = userDao.findById(user.getId());

        assertTrue(found.isPresent());
        assertEquals("botecito.dev@gmail.com", found.get().getEmail());
    }

    @Test
    public void testFindByIdNotFound() {
        assertFalse(userDao.findById(-1).isPresent());
    }

    @Test
    public void testFindByEmail() {
        Optional<Users> found = userDao.findByEmail("botecito.dev@gmail.com");

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByEmailNotFound() {
        assertFalse(userDao.findByEmail("lmao@gmail.com").isPresent());
    }

    @Test
    public void testFindByPasswordRecoveryToken() {
        Users user = new Users();
        user.setFirstName("Botecito");
        user.setLastName("User");
        user.setEmail("botecito.user@gmail.com");
        user.setLanguage("en");
        user.setVerified(true);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        String token = UUID.randomUUID().toString();
        user.setMailToken(token);
        em.persist(user);
        em.flush();

        Optional<Users> found = userDao.findByPasswordRecoveryToken(token);

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByEmailVerificationToken() {
        String token = UUID.randomUUID().toString();
        user.setMailToken(token);
        em.flush();

        Optional<Users> found = userDao.findByEmailVerificationToken(token);

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindUsersByIds() {
        Users user = new Users();
        user.setFirstName("Botecito");
        user.setLastName("User");
        user.setEmail("botecito.user@gmail.com");
        user.setLanguage("en");
        user.setVerified(false);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        em.persist(user);
        em.flush();

        List<Users> found = userDao.findUsersByIds(List.of(user.getId(), user.getId()));

        assertEquals(2, found.size());
    }
}
