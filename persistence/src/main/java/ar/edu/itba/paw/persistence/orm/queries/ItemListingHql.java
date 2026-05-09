package ar.edu.itba.paw.persistence.orm.queries;

/**
 * Shared JPQL for listing and loading items with their current version, optional
 * availability slice, reviews, and cover image projection.
 */
public final class ItemListingHql {

    public static final String ITEM_TARGET_PARAM = "itemTargetType";

    private static final String ITEM_WITH_CURRENT_VERSION = "FROM ItemOrm i "
            + "JOIN VersionOrm v ON v.item = i "
            + "  AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i) ";

    private static final String AVAILABILITY_SLICE = "LEFT JOIN AvailabilityOrm a ON a.version = v "
            + "  AND a.id = (SELECT MIN(a2.id) FROM AvailabilityOrm a2 WHERE a2.version = v)";

    private static final String ITEM_LISTING_ROW_CONSTRUCTOR =
            "SELECT new ar.edu.itba.paw.persistence.orm.projections.ItemListingRowOrm("
                    + "i.id, i.host.id, i.status, "
                    + "v.title, v.description, v.price, v.capacity, v.weight, v.difficulty, v.location.id, "
                    + "v.location.name, v.type.name, "
                    + "a.weekday, a.startTime, a.endTime, "
                    + "(SELECT MIN(m.image.id) FROM MediaOrm m "
                    + " WHERE m.version = v AND m.id.index = ("
                    + "   SELECT MIN(m2.id.index) FROM MediaOrm m2 WHERE m2.version = v"
                    + " )), "
                    + "(SELECT COALESCE(AVG(r.rating), 0) FROM ReviewOrm r "
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i), "
                    + "(SELECT COUNT(r) FROM ReviewOrm r "
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i)"
                    + ") ";

    /** Full row query up to (but not including) any {@code WHERE} / {@code ORDER BY}. */
    public static final String ITEM_LISTING_SELECT =
            ITEM_LISTING_ROW_CONSTRUCTOR + ITEM_WITH_CURRENT_VERSION + AVAILABILITY_SLICE;

    /** Count items that have a current version (matches listing join shape without availability). */
    public static final String ITEM_LISTING_COUNT = "SELECT COUNT(i) " + ITEM_WITH_CURRENT_VERSION;

    /** Single item by primary key, same projection as marketplace rows. */
    public static final String ITEM_DETAIL_BY_ID = ITEM_LISTING_SELECT + " WHERE i.id = :itemId";

    private ItemListingHql() {}
}
