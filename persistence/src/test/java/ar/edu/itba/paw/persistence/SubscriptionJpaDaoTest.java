package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;
import java.util.List;
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
public class SubscriptionJpaDaoTest {

    @Autowired
    private SubscriptionDao subscriptionDao;

    @PersistenceContext
    private EntityManager em;

    private Users subscriber;
    private Users publisher;

    @BeforeEach
    public void setup() {
        subscriber = insertUser(em, "Botecito", "User", "botecito.user@gmail.com");
        publisher = insertUser(em, "Botecito", "Admin", "botecito.dev@gmail.com");
        em.flush();
    }

    @Test
    public void testCreate() {
        assertTrue(subscriptionDao.create(subscriber.getId(), publisher.getId()));
        assertTrue(subscriptionDao.exists(subscriber.getId(), publisher.getId()));
    }

    @Test
    public void testDelete() {
        subscriptionDao.create(subscriber.getId(), publisher.getId());

        boolean deleted = subscriptionDao.delete(subscriber.getId(), publisher.getId());

        assertTrue(deleted);
        assertFalse(subscriptionDao.exists(subscriber.getId(), publisher.getId()));
    }

    @Test
    public void testListSubscriptions() {
        Users publisher2 = insertUser(em, "Botecito", "Admin", "botecito.dev2@gmail.com");
        em.flush();
        subscriptionDao.create(subscriber.getId(), publisher.getId());
        subscriptionDao.create(subscriber.getId(), publisher2.getId());

        List<Users> subscriptions = subscriptionDao.listSubscriptions(subscriber.getId(), 1, 12);

        assertEquals(2, subscriptions.size());
    }

    @Test
    public void testCountFollowers() {
        Users subscriber2 = insertUser(em, "Botecito", "User", "botecito.user2@gmail.com");
        em.flush();
        subscriptionDao.create(subscriber.getId(), publisher.getId());
        subscriptionDao.create(subscriber2.getId(), publisher.getId());

        int count = subscriptionDao.countFollowers(publisher.getId());

        assertEquals(2, count);
    }

    @Test
    public void testCountSubscriptions() {
        Users publisher2 = insertUser(em, "Botecito", "Admin", "botecito.dev2@gmail.com");
        em.flush();
        subscriptionDao.create(subscriber.getId(), publisher.getId());
        subscriptionDao.create(subscriber.getId(), publisher2.getId());

        int count = subscriptionDao.countSubscriptions(subscriber.getId());

        assertEquals(2, count);
    }

    @Test
    public void testListVerifiedSubscribersForPublisher() {
        Users verified = insertUser(em, "Verified", "User", "botecito.verified@gmail.com");
        Users unverified = new Users();
        unverified.setFirstName("Unverified");
        unverified.setLastName("User");
        unverified.setEmail("botecito.unverified@gmail.com");
        unverified.setLanguage("en");
        unverified.setVerified(false);
        unverified.setAdmin(false);
        unverified.setCreatedAt(LocalDateTime.now());
        em.persist(unverified);
        em.flush();
        subscriptionDao.create(verified.getId(), publisher.getId());
        subscriptionDao.create(unverified.getId(), publisher.getId());

        List<Users> result = subscriptionDao.listVerifiedSubscribersForPublisher(publisher.getId());

        assertEquals(1, result.size());
        assertEquals(verified.getId(), result.get(0).getId());
    }
}
