package ar.edu.itba.paw.persistence.utils;

public final class ItemReviewSql {

    public static final String MARKETPLACE_REVIEW_AVERAGES_JOIN =
            "LEFT JOIN (SELECT v2.item_id AS item_id, COALESCE(AVG(r.rating), 0) AS average_rating "
                    + "FROM review r INNER JOIN booking b ON r.booking_id = b.id "
                    + "INNER JOIN version v2 ON b.version_id = v2.id "
                    + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) "
                    + "GROUP BY v2.item_id) item_reviews ON item_reviews.item_id = v.item_id ";

    private ItemReviewSql() {}
}
