package ar.edu.itba.paw.persistence.utils;

import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.TargetEnum;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;

public final class ItemReviewSummaries {

    private static final String TARGET_PARAM = "target";

    private static final String REVIEW_JOIN = "FROM review r INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:" + TARGET_PARAM + " AS target_enum) ";

    private static final String BATCH_AGG_SQL = "SELECT v2.item_id, COUNT(r.id), COALESCE(AVG(r.rating), 0) "
            + REVIEW_JOIN
            + "AND v2.item_id IN :itemIds GROUP BY v2.item_id";

    private static final String SINGLE_AGG_SQL =
            "SELECT COUNT(r.id), COALESCE(AVG(r.rating), 0) " + REVIEW_JOIN + "AND v2.item_id = :itemId";

    public static final String MARKETPLACE_REVIEW_AVERAGES_JOIN =
            "LEFT JOIN (SELECT v2.item_id AS item_id, COALESCE(AVG(r.rating), 0) AS average_rating "
                    + "FROM review r INNER JOIN booking b ON r.booking_id = b.id "
                    + "INNER JOIN version v2 ON b.version_id = v2.id "
                    + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) "
                    + "GROUP BY v2.item_id) item_reviews ON item_reviews.item_id = v.item_id ";

    private static final ReviewSummary EMPTY_SUMMARY = new ReviewSummary(0L, 0.0);

    private ItemReviewSummaries() {}

    public static void populateReviewSummaries(final EntityManager em, final List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        final List<Integer> itemIds = items.stream().map(Item::getId).toList();
        final Map<Integer, ReviewSummary> summaryByItemId = reviewSummariesByItemIds(em, itemIds);
        for (final Item item : items) {
            item.setReviewSummary(summaryByItemId.getOrDefault(item.getId(), EMPTY_SUMMARY));
        }
    }

    public static ReviewSummary reviewSummaryForItem(final EntityManager em, final int itemId) {
        final Query query = em.createNativeQuery(SINGLE_AGG_SQL);
        query.setParameter(TARGET_PARAM, TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        final Object[] row = (Object[]) query.getSingleResult();
        return new ReviewSummary(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
    }

    private static Map<Integer, ReviewSummary> reviewSummariesByItemIds(
            final EntityManager em, final Collection<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        final Query query = em.createNativeQuery(BATCH_AGG_SQL);
        query.setParameter(TARGET_PARAM, TargetEnum.ITEM.name());
        query.setParameter("itemIds", itemIds);
        final Map<Integer, ReviewSummary> result = new HashMap<>();
        for (final Object rowObj : query.getResultList()) {
            final Object[] row = (Object[]) rowObj;
            result.put(
                    ((Number) row[0]).intValue(),
                    new ReviewSummary(((Number) row[1]).longValue(), ((Number) row[2]).doubleValue()));
        }
        return result;
    }
}
