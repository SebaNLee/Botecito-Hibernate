package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.entity.Subscription;
import ar.edu.itba.paw.models.entity.SubscriptionId;
import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class, SubscriptionJpaDaoTest.TestConfig.class})
@Transactional
public class SubscriptionJpaDaoTest {

    @Configuration
    static class TestConfig {
        @Bean
        public List<String> entityClassNames() {
            return List.of(Users.class.getName(), Subscription.class.getName(), SubscriptionId.class.getName());
        }
    }

    @Autowired
    private SubscriptionDao subscriptionDao;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testCreateListAndDeleteSubscription() {
        final Users subscriber = persistUser("sub@example.com", true);
        final Users publisher = persistUser("pub@example.com", true);
        entityManager.flush();

        assertTrue(subscriptionDao.create(subscriber.getId(), publisher.getId()));
        assertTrue(subscriptionDao.exists(subscriber.getId(), publisher.getId()));

        final List<Users> subscriptions = subscriptionDao.listSubscriptions(subscriber.getId(), 1, 10);
        assertEquals(1, subscriptions.size());
        assertEquals(publisher.getId(), subscriptions.get(0).getId());
        assertEquals(1, subscriptionDao.countSubscriptions(subscriber.getId()));
        assertEquals(
                publisher.getId(),
                subscriptionDao
                        .listSubscriptions(subscriber.getId(), 1, 1)
                        .get(0)
                        .getId());

        final List<Users> subscribers = subscriptionDao.listVerifiedSubscribersForPublisher(publisher.getId());
        assertEquals(1, subscribers.size());
        assertEquals(subscriber.getId(), subscribers.get(0).getId());

        assertTrue(subscriptionDao.delete(subscriber.getId(), publisher.getId()));
        assertFalse(subscriptionDao.exists(subscriber.getId(), publisher.getId()));
    }

    @Test
    public void testListVerifiedSubscribersExcludesUnverifiedUsers() {
        final Users subscriber = persistUser("sub@example.com", false);
        final Users publisher = persistUser("pub@example.com", true);
        entityManager.flush();

        subscriptionDao.create(subscriber.getId(), publisher.getId());

        assertTrue(subscriptionDao
                .listVerifiedSubscribersForPublisher(publisher.getId())
                .isEmpty());
    }

    private Users persistUser(final String email, final boolean verified) {
        final Users user = new Users();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setLanguage("EN");
        user.setVerified(verified);
        user.setCreatedAt(LocalDateTime.now());
        entityManager.persist(user);
        return user;
    }
}
