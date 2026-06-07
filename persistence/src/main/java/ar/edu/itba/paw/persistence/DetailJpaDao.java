package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

@Repository
public class DetailJpaDao implements DetailDao {

    private static final int REVIEW_PAGE_SIZE = 5;

    private static final String SQL_LATEST_VERSION_ID_FOR_ITEM = "SELECT v.id FROM version v "
            + "INNER JOIN item i ON v.item_id = i.id "
            + "WHERE i.id = :itemId "
            + "AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)";

    private static final String VERSION_FETCH_JPQL = "SELECT DISTINCT v FROM Version v "
            + "JOIN FETCH v.item i JOIN FETCH i.host "
            + "JOIN FETCH v.location JOIN FETCH v.type "
            + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
            + "WHERE v.id IN :ids";

    private static final String SQL_REVIEW_COUNT_FOR_ITEM = "SELECT COUNT(r.id) FROM review r "
            + "INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) AND v2.item_id = :itemId";

    private static final String SQL_REVIEW_AVG_FOR_ITEM = "SELECT COALESCE(AVG(r.rating), 0) FROM review r "
            + "INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) AND v2.item_id = :itemId";

    private static final String SQL_REVIEW_IDS_FOR_ITEM = "SELECT r.id FROM review r "
            + "INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) AND v2.item_id = :itemId "
            + "ORDER BY r.created_at DESC";

    private static final String REVIEW_FETCH_JPQL =
            "FROM Review r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender WHERE r.id IN :ids "
                    + "ORDER BY r.createdAt DESC";

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Item> getItemDetail(final int itemId, final int reviewPage) {
        final Integer versionId = resolveLatestVersionIdForItem(itemId);
        if (versionId == null) {
            return Optional.empty();
        }

        final Item item = loadItemViaVersionFetch(versionId);
        populateReviewTransients(item, itemId, reviewPage);
        return Optional.of(item);
    }

    private Integer resolveLatestVersionIdForItem(final int itemId) {
        final Query nativeQuery = em.createNativeQuery(SQL_LATEST_VERSION_ID_FOR_ITEM);
        nativeQuery.setParameter("itemId", itemId);
        nativeQuery.setMaxResults(1);

        final List<Integer> idList = Paging.toIntegerIds(nativeQuery.getResultList());

        return idList.isEmpty() ? null : idList.get(0);
    }

    private Item loadItemViaVersionFetch(final int versionId) {
        final TypedQuery<Version> versionQuery = em.createQuery(VERSION_FETCH_JPQL, Version.class);
        versionQuery.setParameter("ids", List.of(versionId));

        final List<Version> versions = versionQuery.getResultList();
        if (versions.isEmpty()) {
            throw new IllegalStateException("Version not found: " + versionId);
        }
        final Version version = versions.get(0);
        // Init while the persistence context is open (detail uses this collection after the service returns).
        Hibernate.initialize(version.getAvailabilities());
        final Item item = version.getItem();
        item.setLatestVersion(version);
        return item;
    }

    private void populateReviewTransients(final Item item, final int itemId, final int reviewPage) {
        final long totalReviews = countReviewsForItem(itemId);
        final double averageRating = averageRatingForItem(itemId);
        final List<Review> reviews = loadReviewsPage(itemId, reviewPage);

        item.setTotalReviews(totalReviews);
        item.setAverageRating(averageRating);
        item.setReviewPage(new PageModel<>(reviews, reviewPage, REVIEW_PAGE_SIZE, (int) totalReviews));
    }

    private long countReviewsForItem(final int itemId) {
        final Query countQuery = em.createNativeQuery(SQL_REVIEW_COUNT_FOR_ITEM);
        countQuery.setParameter("itemId", itemId);
        countQuery.setParameter("itemTargetType", TargetEnum.ITEM.name());
        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private double averageRatingForItem(final int itemId) {
        final Query avgQuery = em.createNativeQuery(SQL_REVIEW_AVG_FOR_ITEM);
        avgQuery.setParameter("itemId", itemId);
        avgQuery.setParameter("itemTargetType", TargetEnum.ITEM.name());
        return ((Number) avgQuery.getSingleResult()).doubleValue();
    }

    private List<Review> loadReviewsPage(final int itemId, final int page) {
        final Query reviewIdsQuery = em.createNativeQuery(SQL_REVIEW_IDS_FOR_ITEM);
        reviewIdsQuery.setParameter("itemId", itemId);
        reviewIdsQuery.setParameter("itemTargetType", TargetEnum.ITEM.name());
        Paging.apply(reviewIdsQuery, page, REVIEW_PAGE_SIZE);

        final List<Integer> reviewIds = Paging.toIntegerIds(reviewIdsQuery.getResultList());

        if (reviewIds.isEmpty()) {
            return List.of();
        }

        return em.createQuery(REVIEW_FETCH_JPQL, Review.class)
                .setParameter("ids", reviewIds)
                .getResultList();
    }
}
