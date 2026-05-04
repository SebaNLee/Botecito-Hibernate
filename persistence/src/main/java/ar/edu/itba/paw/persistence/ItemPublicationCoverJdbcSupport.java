package ar.edu.itba.paw.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

final class ItemPublicationCoverJdbcSupport {

    private ItemPublicationCoverJdbcSupport() {}

    static byte[] findCurrentCoverImageData(final JdbcTemplate jdbcTemplate, final int itemId) {
        return jdbcTemplate.query(
                "SELECT image_data FROM item_media WHERE item_id = ? ORDER BY display_order ASC, id ASC LIMIT 1",
                rs -> rs.next() ? rs.getBytes("image_data") : null,
                itemId);
    }

    static void syncCurrentPublicationVersionCoverImage(final JdbcTemplate jdbcTemplate, final int itemId) {
        jdbcTemplate.update(
                "UPDATE item_publication_version SET cover_image_data = ? WHERE id = (SELECT version_id FROM item WHERE id = ?)",
                findCurrentCoverImageData(jdbcTemplate, itemId),
                itemId);
    }
}
