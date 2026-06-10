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
        user = TestUtils.insertUnverifiedUser(em, "Botecito", "Dev", "botecito.dev@gmail.com");
        em.flush();
    }

    @Test
    public void testCreateUser() {
        Users newUser = new Users();
        newUser.setFirstName("Botecito");
        newUser.setLastName("User");
        newUser.setEmail("botecito.user@gmail.com");
        newUser.setLanguage("en");
        newUser.setVerified(false);
        newUser.setAdmin(false);
        newUser.setCreatedAt(LocalDateTime.now());

        Users created = userDao.createUser(newUser);
        assertNotNull(created.getId());
        assertEquals("botecito.user@gmail.com", created.getEmail());

        em.flush();
        em.clear();
        Users found = em.find(Users.class, created.getId());
        assertEquals("botecito.user@gmail.com", found.getEmail());
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
        Users recoveryUser = TestUtils.insertUser(em, "Botecito", "User", "botecito.user@gmail.com");
        String token = UUID.randomUUID().toString();
        recoveryUser.setMailToken(token);
        em.flush();

        Optional<Users> found = userDao.findByPasswordRecoveryToken(token);

        assertTrue(found.isPresent());
        assertEquals(recoveryUser.getId(), found.get().getId());
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
        Users user1 = TestUtils.insertUser(em, "Botecito", "User", "botecito.user@gmail.com");
        Users user2 = TestUtils.insertUser(em, "Botecito", "User2", "botecito.user2@gmail.com");
        em.flush();

        List<Users> found = userDao.findUsersByIds(List.of(user1.getId(), user2.getId()));

        assertEquals(2, found.size());
    }

    @Test
    public void testResetPasswordByRecoveryToken() {
        String token = UUID.randomUUID().toString();
        Users pwUser = TestUtils.insertUser(em, "Botecito", "User", "botecito.user@gmail.com");
        pwUser.setMailToken(token);
        em.flush();

        boolean result = userDao.resetPasswordByRecoveryToken(token, "newhash", LocalDateTime.now());

        assertTrue(result);
        em.clear();
        Users updated = em.find(Users.class, pwUser.getId());
        assertEquals("newhash", updated.getPasswordHash());
    }
}
