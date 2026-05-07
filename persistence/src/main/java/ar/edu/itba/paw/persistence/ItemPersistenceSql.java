package ar.edu.itba.paw.persistence;

/**
 * Shared SQL fragments for item-related persistence (publication, bookings tied to items, reviews in marketplace filter).
 */
final class ItemPersistenceSql {

    static final String EDIT_CONFLICT_BOOKING_STATES = "'PENDING', 'ACCEPTED', 'PAID', 'CONFIRMED'";
    static final String PUBLICATION_EDIT_SNAPSHOT_STATES = EDIT_CONFLICT_BOOKING_STATES + ", 'FINISHED'";

    static final String SNAPSHOT_SELECT =
            "SELECT v.*, i.id AS item_id, i.host_id AS owner_id, l.name AS location, img.data AS cover_image_data";
    static final String SNAPSHOT_BOOKING_JOIN = " FROM booking b JOIN version v ON v.id = b.version_id"
            + " JOIN item i ON i.id = v.item_id"
            + " JOIN location l ON l.id = v.location_id"
            + " LEFT JOIN media m ON m.version_id = v.id AND m.index = 0"
            + " LEFT JOIN image img ON img.id = m.image_id"
            + " WHERE b.id = ? AND ";
    static final String SNAPSHOT_LIST_BASE =
            "SELECT DISTINCT v.*, i.id AS item_id, i.host_id AS owner_id, l.name AS location, img.data AS cover_image_data"
                    + " FROM version v"
                    + " JOIN booking b ON b.version_id = v.id"
                    + " JOIN item i ON i.id = v.item_id"
                    + " JOIN location l ON l.id = v.location_id"
                    + " LEFT JOIN media m ON m.version_id = v.id AND m.index = 0"
                    + " LEFT JOIN image img ON img.id = m.image_id"
                    + " WHERE i.id = ? AND ";

    static final String BOOKING_BY_OWNER_BASE = "SELECT b.*, v.item_id AS item_id"
            + " FROM booking b"
            + " JOIN version v ON v.id = b.version_id"
            + " JOIN item i ON i.id = v.item_id"
            + " WHERE i.host_id = ?"
            + " AND b.guest_id <> i.host_id";

    static final String BOOKING_BY_GUEST_BASE = "SELECT b.*, v.item_id AS item_id"
            + " FROM booking b"
            + " JOIN version v ON v.id = b.version_id"
            + " JOIN item i ON i.id = v.item_id"
            + " WHERE b.guest_id = ?"
            + " AND b.guest_id <> i.host_id";

    static final String BOOKING_UPDATE_BY_OWNER_SUBMITTED = "UPDATE booking b"
            + " SET status = ?::booking_status_enum, updated_at = CURRENT_TIMESTAMP"
            + " FROM version v, item i"
            + " WHERE b.version_id = v.id"
            + " AND v.item_id = i.id"
            + " AND b.id = ?"
            + " AND i.host_id = ?"
            + " AND b.status = 'PAID'";

    static final String REVIEW_TARGET_TYPE_EQUALS = "CAST(target_type AS VARCHAR(16)) = ?";
    static final String REVIEW_R_TARGET_TYPE_EQUALS = "CAST(r.target_type AS VARCHAR(16)) = ?";

    /** Join booking row to item ownership. */
    static final String ITEM_BOOKING_VERSION_JOIN = " FROM booking b"
            + " JOIN version v ON v.id = b.version_id"
            + " JOIN item i ON i.id = v.item_id"
            + " WHERE i.id = ?";

    /**
     * Bookings that block hard-deleting an item: from someone other than the owner, not rejected/cancelled, and not
     * fully in the past (by {@code end_time}).
     */
    static final String ITEM_BOOKING_HARD_DELETE_BLOCKERS = " AND b.status NOT IN ('REJECTED', 'CANCELLED')"
            + " AND b.\"end\" > CURRENT_TIMESTAMP"
            + " AND b.guest_id <> i.host_id";

    private ItemPersistenceSql() {}
}
