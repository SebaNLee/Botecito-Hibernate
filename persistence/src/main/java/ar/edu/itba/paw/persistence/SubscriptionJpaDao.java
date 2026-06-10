package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Subscription;
import ar.edu.itba.paw.models.entity.SubscriptionId;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionJpaDao implements SubscriptionDao {

    private static final String SUBSCRIPTIONS_FETCH_JPQL = "SELECT u FROM Users u WHERE u.id IN :ids"
            + " ORDER BY LOWER(u.firstName) ASC, LOWER(u.lastName) ASC, LOWER(u.email) ASC";

    private static final String FOLLOWERS_FETCH_JPQL = "SELECT u FROM Subscription s JOIN s.subscriber u"
            + " WHERE s.subscribedTo.id = :userId AND u.id IN :ids"
            + " ORDER BY s.createdAt ASC";

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean create(final int subscriberId, final int subscribedToId) {
        final SubscriptionId id = new SubscriptionId(subscriberId, subscribedToId);
        if (em.find(Subscription.class, id) != null) {
            return false;
        }
        final Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setSubscriber(em.getReference(Users.class, subscriberId));
        subscription.setSubscribedTo(em.getReference(Users.class, subscribedToId));
        subscription.setCreatedAt(LocalDateTime.now());
        em.persist(subscription);
        return true;
    }

    @Override
    public boolean delete(final int subscriberId, final int subscribedToId) {
        final Subscription subscription = em.find(Subscription.class, new SubscriptionId(subscriberId, subscribedToId));
        if (subscription == null) {
            return false;
        }
        em.remove(subscription);
        return true;
    }

    @Override
    public boolean exists(final int subscriberId, final int subscribedToId) {
        return em.find(Subscription.class, new SubscriptionId(subscriberId, subscribedToId)) != null;
    }

    @Override
    public PageModel<Users> listSubscriptions(final int subscriberId, final int page, final int pageSize) {
        final long totalCount = countSubscriptions(subscriberId);

        final String sql = "SELECT s.subscribed_to_id FROM subscriptons s"
                + " INNER JOIN users u ON u.id = s.subscribed_to_id"
                + " WHERE s.subscriber_id = :subscriberId"
                + " ORDER BY LOWER(u.first_name) ASC, LOWER(u.last_name) ASC, LOWER(u.email) ASC";

        final Query nativeQuery = em.createNativeQuery(sql);
        nativeQuery.setParameter("subscriberId", subscriberId);
        Paging.apply(nativeQuery, page, pageSize);
        final List<Integer> userIds = Paging.toIntegerIds(nativeQuery.getResultList());

        if (userIds.isEmpty()) {
            return new PageModel<>(List.of(), page, pageSize, totalCount);
        }

        return new PageModel<>(
                em.createQuery(SUBSCRIPTIONS_FETCH_JPQL, Users.class)
                        .setParameter("ids", userIds)
                        .getResultList(),
                page,
                pageSize,
                totalCount);
    }

    @Override
    public int countSubscriptions(final int subscriberId) {
        return ((Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM subscriptons s WHERE s.subscriber_id = :subscriberId")
                        .setParameter("subscriberId", subscriberId)
                        .getSingleResult())
                .intValue();
    }

    @Override
    public int countFollowers(final int userId) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM subscriptons s WHERE s.subscribed_to_id = :userId")
                        .setParameter("userId", userId)
                        .getSingleResult())
                .intValue();
    }

    @Override
    public PageModel<Users> listFollowers(final int userId, final int page, final int pageSize) {
        final long totalCount = countFollowers(userId);

        final String sql = "SELECT s.subscriber_id FROM subscriptons s"
                + " WHERE s.subscribed_to_id = :userId"
                + " ORDER BY s.created_at ASC";

        final Query nativeQuery = em.createNativeQuery(sql);
        nativeQuery.setParameter("userId", userId);
        Paging.apply(nativeQuery, page, pageSize);
        final List<Integer> userIds = Paging.toIntegerIds(nativeQuery.getResultList());

        if (userIds.isEmpty()) {
            return new PageModel<>(List.of(), page, pageSize, totalCount);
        }

        return new PageModel<>(
                em.createQuery(FOLLOWERS_FETCH_JPQL, Users.class)
                        .setParameter("userId", userId)
                        .setParameter("ids", userIds)
                        .getResultList(),
                page,
                pageSize,
                totalCount);
    }
}
