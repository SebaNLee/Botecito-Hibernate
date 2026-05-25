package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Subscription;
import ar.edu.itba.paw.models.entity.SubscriptionId;
import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionJpaDao implements SubscriptionDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean create(final int subscriberId, final int subscribedToId) {
        final SubscriptionId id = new SubscriptionId(subscriberId, subscribedToId);
        if (entityManager.find(Subscription.class, id) != null) {
            return false;
        }
        final Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setSubscriber(entityManager.getReference(Users.class, subscriberId));
        subscription.setSubscribedTo(entityManager.getReference(Users.class, subscribedToId));
        subscription.setCreatedAt(LocalDateTime.now());
        entityManager.persist(subscription);
        return true;
    }

    @Override
    public boolean delete(final int subscriberId, final int subscribedToId) {
        final Subscription subscription =
                entityManager.find(Subscription.class, new SubscriptionId(subscriberId, subscribedToId));
        if (subscription == null) {
            return false;
        }
        entityManager.remove(subscription);
        return true;
    }

    @Override
    public boolean exists(final int subscriberId, final int subscribedToId) {
        return entityManager.find(Subscription.class, new SubscriptionId(subscriberId, subscribedToId)) != null;
    }

    @Override
    public List<Users> listSubscriptions(final int subscriberId, final int page, final int pageSize) {
        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        return entityManager
                .createQuery(
                        "SELECT s.subscribedTo FROM Subscription s"
                                + " WHERE s.subscriber.id = :subscriberId"
                                + " ORDER BY LOWER(s.subscribedTo.firstName), LOWER(s.subscribedTo.lastName), LOWER(s.subscribedTo.email)",
                        Users.class)
                .setParameter("subscriberId", subscriberId)
                .setFirstResult((safePage - 1) * safePageSize)
                .setMaxResults(safePageSize)
                .getResultList();
    }

    @Override
    public int countSubscriptions(final int subscriberId) {
        return entityManager
                .createQuery("SELECT COUNT(s) FROM Subscription s WHERE s.subscriber.id = :subscriberId", Long.class)
                .setParameter("subscriberId", subscriberId)
                .getSingleResult()
                .intValue();
    }

    @Override
    public int countFollowers(final int userId) {
        return entityManager
                .createQuery("SELECT COUNT(s) FROM Subscription s WHERE s.subscribedTo.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult()
                .intValue();
    }

    @Override
    public List<Users> listVerifiedSubscribersForPublisher(final int publisherId) {
        return entityManager
                .createQuery(
                        "SELECT s.subscriber FROM Subscription s"
                                + " WHERE s.subscribedTo.id = :publisherId"
                                + " AND s.subscriber.verified = true"
                                + " AND s.subscriber.email IS NOT NULL"
                                + " ORDER BY s.createdAt ASC",
                        Users.class)
                .setParameter("publisherId", publisherId)
                .getResultList();
    }
}
