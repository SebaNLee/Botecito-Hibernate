package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.persistence.orm.daos.UserHibernateDao;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class UserHibernateDaoTest {

    private EntityManager entityManager;

    private UserHibernateDao userDao;
    private UsersOrm persistedUser;
    private boolean flushed;
    private final Map<String, Object> queryParams = new HashMap<>();

    @BeforeEach
    public void setUp() {
        userDao = new UserHibernateDao();
        entityManager = entityManagerProxy();
        ReflectionTestUtils.setField(userDao, "entityManager", entityManager);
    }

    @Test
    public void testCreateUserPersistsMappedUser() {
        final UserModel user = new UserModel();
        user.setGivenName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setPhone("123");
        user.setPaymentAlias("pay.alias");
        user.setPasswordHash("hash");
        user.setPreferredLanguage(PreferredLanguageModel.EN);

        final UserModel createdUser = userDao.createUser(user);

        Assertions.assertTrue(flushed);
        Assertions.assertNotNull(persistedUser);
        Assertions.assertEquals("Ada", persistedUser.getFirstName());
        Assertions.assertEquals("Lovelace", persistedUser.getLastName());
        Assertions.assertEquals("ada@example.com", persistedUser.getEmail());
        Assertions.assertEquals("123", persistedUser.getPhone());
        Assertions.assertEquals("pay.alias", persistedUser.getAlias());
        Assertions.assertEquals("hash", persistedUser.getPasswordHash());
        Assertions.assertEquals("en", persistedUser.getLanguage());
        Assertions.assertEquals(false, persistedUser.getVerified());
        Assertions.assertNotNull(persistedUser.getCreatedAt());
        Assertions.assertEquals("ada@example.com", createdUser.getEmail());
        Assertions.assertEquals(PreferredLanguageModel.EN, createdUser.getPreferredLanguage());
    }

    @Test
    public void testResetPasswordByRecoveryTokenBindsMappedFields() {
        final UserModel user = new UserModel();
        user.setPasswordRecoveryToken("token");
        user.setPasswordHash("new-hash");
        user.setPasswordRecoveryUsedAt(OffsetDateTime.parse("2026-01-02T12:00:00Z"));

        final boolean result = userDao.resetPasswordByRecoveryToken(user);

        Assertions.assertTrue(result);
        Assertions.assertEquals("new-hash", queryParams.get("passwordHash"));
        Assertions.assertEquals("token", queryParams.get("token"));
        Assertions.assertNotNull(queryParams.get("usedAt"));
    }

    private EntityManager entityManagerProxy() {
        return (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[] {EntityManager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "persist" -> {
                        persistedUser = (UsersOrm) args[0];
                        yield null;
                    }
                    case "flush" -> {
                        flushed = true;
                        yield null;
                    }
                    case "createQuery" -> queryProxy();
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Query queryProxy() {
        return (Query) Proxy.newProxyInstance(
                Query.class.getClassLoader(),
                new Class<?>[] {Query.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setParameter" -> {
                        queryParams.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "executeUpdate" -> 1;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(final Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        return null;
    }
}
