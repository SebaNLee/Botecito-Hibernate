package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class UserJdbcDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    public void testCreateUserStoresPasswordHash() {
        final User created = userDao.createUser("A", "B", "a@a.com", "hash-value");

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("hash-value", created.getPasswordHash());
    }

    @Test
    public void testClaimUserWhenPasswordHashIsNull() {
        userDao.createUser("A", "B", "legacy@a.com", null);

        final User claimed =
                userDao.claimUser("Legacy", "User", "legacy@a.com", "new-hash").orElse(null);

        Assertions.assertNotNull(claimed);
        Assertions.assertEquals("Legacy", claimed.getGivenName());
        Assertions.assertEquals("new-hash", claimed.getPasswordHash());
    }
}
