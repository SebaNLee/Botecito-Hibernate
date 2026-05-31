package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Favourite;
import ar.edu.itba.paw.models.entity.FavouriteId;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class FavouriteJpaDao implements FavouriteDao {

    private static final String VERSION_FETCH_JPQL = "SELECT DISTINCT v FROM Version v "
            + "JOIN FETCH v.item i JOIN FETCH i.host JOIN FETCH v.location JOIN FETCH v.type "
            + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
            + "WHERE v.id IN :ids";

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean create(final int userId, final int itemId) {
        final FavouriteId id = new FavouriteId(userId, itemId);
        if (em.find(Favourite.class, id) != null) {
            return false;
        }
        final Favourite favourite = new Favourite();
        favourite.setId(id);
        favourite.setUser(em.getReference(Users.class, userId));
        favourite.setItem(em.getReference(Item.class, itemId));
        favourite.setCreatedAt(LocalDateTime.now());
        em.persist(favourite);
        return true;
    }

    @Override
    public boolean delete(final int userId, final int itemId) {
        final Favourite favourite = em.find(Favourite.class, new FavouriteId(userId, itemId));
        if (favourite == null) {
            return false;
        }
        em.remove(favourite);
        return true;
    }

    @Override
    public boolean exists(final int userId, final int itemId) {
        return em.find(Favourite.class, new FavouriteId(userId, itemId)) != null;
    }

    @Override
    public Set<Integer> findFavouriteItemIds(final int userId, final Collection<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery(
                        "SELECT f.item.id FROM Favourite f WHERE f.user.id = :userId AND f.item.id IN :itemIds",
                        Integer.class)
                .setParameter("userId", userId)
                .setParameter("itemIds", itemIds)
                .getResultList());
    }

    @Override
    public List<Item> listFavourites(final int userId, final int page, final int pageSize) {
        final Query versionIdsQuery = em.createNativeQuery("SELECT v.id FROM favourite f "
                + "INNER JOIN item i ON f.item_id = i.id "
                + "INNER JOIN version v ON v.id = (SELECT v2.id FROM version v2 "
                + "WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1) "
                + "WHERE f.user_id = :userId AND i.status <> CAST(:deleted AS item_status_enum) "
                + "ORDER BY f.created_at DESC, f.item_id DESC");
        versionIdsQuery.setParameter("userId", userId);
        versionIdsQuery.setParameter("deleted", ItemStatusEnum.DELETED.name());
        Paging.apply(versionIdsQuery, page, pageSize);

        final List<Integer> versionIds = Paging.toIntegerIds(versionIdsQuery.getResultList());
        if (versionIds.isEmpty()) {
            return List.of();
        }

        final TypedQuery<Version> versionQuery = em.createQuery(VERSION_FETCH_JPQL, Version.class);
        versionQuery.setParameter("ids", versionIds);

        final Map<Integer, Version> versionById = new LinkedHashMap<>();
        for (final Version version : versionQuery.getResultList()) {
            versionById.put(version.getId(), version);
        }

        final List<Item> items = new ArrayList<>(versionIds.size());
        for (final Integer versionId : versionIds) {
            final Version version = versionById.get(versionId);
            if (version == null) {
                continue;
            }
            final Item item = version.getItem();
            item.setLatestVersion(version);
            items.add(item);
        }
        populateReviewTransients(items);
        return items;
    }

    @Override
    public int countFavourites(final int userId) {
        return em.createQuery(
                        "SELECT COUNT(f) FROM Favourite f" + " WHERE f.user.id = :userId AND f.item.status <> :deleted",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("deleted", ItemStatusEnum.DELETED)
                .getSingleResult()
                .intValue();
    }

    private void populateReviewTransients(final List<Item> items) {
        for (final Item item : items) {
            final long totalReviews = countReviewsForItem(item.getId());
            item.setTotalReviews(totalReviews);
            if (totalReviews > 0) {
                item.setAverageRating(averageRatingForItem(item.getId()));
            }
        }
    }

    private long countReviewsForItem(final int itemId) {
        final Query query = em.createNativeQuery("SELECT COUNT(r.id) FROM review r "
                + "INNER JOIN booking b ON r.booking_id = b.id "
                + "INNER JOIN version v2 ON b.version_id = v2.id "
                + "WHERE r.target_type = CAST(:target AS target_enum) AND v2.item_id = :itemId");
        query.setParameter("target", TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        return ((Number) query.getSingleResult()).longValue();
    }

    private double averageRatingForItem(final int itemId) {
        final Query query = em.createNativeQuery("SELECT COALESCE(AVG(r.rating), 0) FROM review r "
                + "INNER JOIN booking b ON r.booking_id = b.id "
                + "INNER JOIN version v2 ON b.version_id = v2.id "
                + "WHERE r.target_type = CAST(:target AS target_enum) AND v2.item_id = :itemId");
        query.setParameter("target", TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        return ((Number) query.getSingleResult()).doubleValue();
    }
}
