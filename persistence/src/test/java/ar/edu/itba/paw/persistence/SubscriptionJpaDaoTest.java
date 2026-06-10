package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.*;
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
        em.flush();
        em.clear();

        assertNotNull(em.find(Subscription.class, new SubscriptionId(subscriber.getId(), publisher.getId())));
    }

    @Test
    public void testDelete() {
        insertSubscription(em, subscriber, publisher);
        em.flush();

        boolean deleted = subscriptionDao.delete(subscriber.getId(), publisher.getId());

        assertTrue(deleted);
        em.flush();
        em.clear();
        
        assertNull(em.find(Subscription.class, new SubscriptionId(subscriber.getId(), publisher.getId())));
    }

    @Test
    public void testListSubscriptions() {
        Users publisher2 = insertUser(em, "Botecito", "Admin", "botecito.dev2@gmail.com");
        em.flush();
        insertSubscription(em, subscriber, publisher);
        insertSubscription(em, subscriber, publisher2);
        em.flush();

        List<Users> subscriptions = subscriptionDao.listSubscriptions(subscriber.getId(), 1, 12);

        assertEquals(2, subscriptions.size());
    }

    @Test
    public void testCountFollowers() {
        Users subscriber2 = insertUser(em, "Botecito", "User", "botecito.user2@gmail.com");
        em.flush();
        insertSubscription(em, subscriber, publisher);
        insertSubscription(em, subscriber2, publisher);
        em.flush();

        int count = subscriptionDao.countFollowers(publisher.getId());

        assertEquals(2, count);
    }

    @Test
    public void testCountSubscriptions() {
        Users publisher2 = insertUser(em, "Botecito", "Admin", "botecito.dev2@gmail.com");
        em.flush();
        insertSubscription(em, subscriber, publisher);
        insertSubscription(em, subscriber, publisher2);
        em.flush();

        int count = subscriptionDao.countSubscriptions(subscriber.getId());

        assertEquals(2, count);
    }

    @Test
    public void testListFollowers() {
        Users subscriber2 = insertUser(em, "Botecito", "User", "botecito.user2@gmail.com");
        em.flush();
        insertSubscription(em, subscriber, publisher);
        insertSubscription(em, subscriber2, publisher);
        em.flush();

        PageModel<Users> followers = subscriptionDao.listFollowers(publisher.getId(), 1, 12);

        assertEquals(2, followers.getContent().size());
    }
}
