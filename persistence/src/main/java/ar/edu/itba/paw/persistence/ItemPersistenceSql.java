package ar.edu.itba.paw.persistence;

/**
 * Shared SQL fragments for item-related persistence (publication, bookings tied to items, reviews in marketplace filter).
 */
final class ItemPersistenceSql {

    static final String EDIT_CONFLICT_BOOKING_STATES =
            "'BOOKING_PENDING', 'BOOKING_CONFIRMED', 'BOOKING_PAYMENT_SUBMITTED', 'BOOKING_PAID'";
    static final String PUBLICATION_EDIT_SNAPSHOT_STATES = EDIT_CONFLICT_BOOKING_STATES + ", 'BOOKING_COMPLETED'";

    static final String SNAPSHOT_SELECT = "SELECT v.*, b.item_id";
    static final String SNAPSHOT_BOOKING_JOIN =
            " FROM item_booking b JOIN item_publication_version v ON v.id = b.item_version_id WHERE b.id = ? AND ";
    static final String SNAPSHOT_LIST_BASE = "SELECT DISTINCT v.*, b.item_id"
            + " FROM item_publication_version v"
            + " JOIN item_booking b ON b.item_version_id = v.id"
            + " WHERE b.item_id = ? AND ";

    static final String BOOKING_BY_OWNER_BASE = "SELECT b.*"
            + " FROM item_booking b"
            + " JOIN item i ON i.id = b.item_id"
            + " JOIN item_publication_version v ON v.id = i.version_id"
            + " WHERE v.owner_id = ?";

    static final String BOOKING_UPDATE_BY_OWNER_SUBMITTED = "UPDATE item_booking b"
            + " SET state = ?::booking_state, updated_at = CURRENT_TIMESTAMP"
            + " FROM item i"
            + " JOIN item_publication_version v ON v.id = i.version_id"
            + " WHERE b.item_id = i.id"
            + " AND b.id = ?"
            + " AND v.owner_id = ?"
            + " AND b.state = 'BOOKING_PAYMENT_SUBMITTED'";

    static final String REVIEW_TARGET_TYPE_EQUALS = "CAST(target_type AS VARCHAR(16)) = ?";
    static final String REVIEW_R_TARGET_TYPE_EQUALS = "CAST(r.target_type AS VARCHAR(16)) = ?";

    /** Join booking row to the item's current publication version. */
    static final String ITEM_BOOKING_VERSION_JOIN = " FROM item_booking b"
            + " JOIN item i ON i.id = b.item_id"
            + " JOIN item_publication_version v ON v.id = i.version_id"
            + " WHERE b.item_id = ?";

    /**
     * Bookings that block hard-deleting an item: from someone other than the owner, not rejected/cancelled, and not
     * fully in the past (by {@code end_time}).
     */
    static final String ITEM_BOOKING_HARD_DELETE_BLOCKERS =
            " AND b.state NOT IN ('BOOKING_REJECTED', 'BOOKING_CANCELLED')"
                    + " AND b.end_time > CURRENT_TIMESTAMP"
                    + " AND b.guest_id <> v.owner_id";

    private ItemPersistenceSql() {}
}
