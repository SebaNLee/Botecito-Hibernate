package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.time.OffsetDateTime;
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
        final User created = userDao.createUser("A", "B", "a@a.com", "hash-value", "alias.cbun", "en");

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("hash-value", created.getPasswordHash());
        Assertions.assertEquals("alias.cbun", created.getPaymentAlias());
        Assertions.assertEquals("en", created.getPreferredLanguage().getPersistenceCode());
    }

    @Test
    public void testClaimUserWhenPasswordHashIsNull() {
        userDao.createUser("A", "B", "legacy@a.com", null, null, "es");

        final User claimed = userDao.claimUser("Legacy", "User", "legacy@a.com", "new-hash", "legacy.alias", "en")
                .orElse(null);

        Assertions.assertNotNull(claimed);
        Assertions.assertEquals("Legacy", claimed.getGivenName());
        Assertions.assertEquals("new-hash", claimed.getPasswordHash());
        Assertions.assertEquals("legacy.alias", claimed.getPaymentAlias());
        Assertions.assertEquals("en", claimed.getPreferredLanguage().getPersistenceCode());
    }

    @Test
    public void testCreateUserStoresNullAliasWhenBlankValueIsNotProvided() {
        final User created = userDao.createUser("No", "Alias", "noalias@a.com", "hash-value", null, "es");

        Assertions.assertNotNull(created.getId());
        Assertions.assertNull(created.getPaymentAlias());
    }

    @Test
    public void testUpdatePasswordRecoveryTokenReplacesPreviousValueAndClearsUsedAt() {
        final User created = userDao.createUser("Recover", "User", "recover@a.com", "hash-value", null, "es");
        userDao.updatePasswordRecoveryToken(created.getId(), "token-a");
        final boolean consumed = userDao.resetPasswordByRecoveryToken("token-a", "hash-2", OffsetDateTime.now());
        Assertions.assertTrue(consumed);

        final User updated =
                userDao.updatePasswordRecoveryToken(created.getId(), "token-b").orElse(null);

        Assertions.assertNotNull(updated);
        Assertions.assertEquals("token-b", updated.getPasswordRecoveryToken());
        Assertions.assertNull(updated.getPasswordRecoveryUsedAt());
    }

    @Test
    public void testFindByPasswordRecoveryTokenReturnsStoredUser() {
        final User created = userDao.createUser("Recover", "Token", "token@a.com", "hash-value", null, "es");
        userDao.updatePasswordRecoveryToken(created.getId(), "token-c");

        final User found = userDao.findByPasswordRecoveryToken("token-c").orElse(null);

        Assertions.assertNotNull(found);
        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals("token-c", found.getPasswordRecoveryToken());
    }

    @Test
    public void testResetPasswordByRecoveryTokenCanOnlyBeUsedOnce() {
        final User created = userDao.createUser("Recover", "Once", "once@a.com", "hash-value", null, "es");
        userDao.updatePasswordRecoveryToken(created.getId(), "token-d");

        final boolean firstUse = userDao.resetPasswordByRecoveryToken("token-d", "hash-updated", OffsetDateTime.now());
        final boolean secondUse =
                userDao.resetPasswordByRecoveryToken("token-d", "hash-updated-2", OffsetDateTime.now());
        final User updated = userDao.findById(created.getId()).orElse(null);

        Assertions.assertTrue(firstUse);
        Assertions.assertFalse(secondUse);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals("hash-updated", updated.getPasswordHash());
        Assertions.assertEquals("token-d", updated.getPasswordRecoveryToken());
        Assertions.assertNotNull(updated.getPasswordRecoveryUsedAt());
    }
}
