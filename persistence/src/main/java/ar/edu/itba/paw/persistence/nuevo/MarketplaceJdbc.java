package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceQueryModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import ar.edu.itba.paw.persistence.JdbcDialect;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Repository;

// @Repository
public final class MarketplaceJdbc implements MarketplaceDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    private static final String ITEM_FROM = " FROM item i"
            + " JOIN version v ON v.id = (SELECT MAX(v2.id) FROM version v2 WHERE v2.item_id = i.id)"
            + " JOIN item_type it ON it.id = v.type_id"
            + " JOIN location lo ON lo.id = v.location_id"
            + " LEFT JOIN availability a ON a.id = ("
            + "     SELECT MIN(a2.id) FROM availability a2 WHERE a2.version_id = v.id"
            + " )";

    /**
     * Total matches over the filtered set (before LIMIT). {@code COUNT(*) OVER ()}
     * is used because PostgreSQL
     * does not support {@code COUNT(DISTINCT i.id) OVER ()}; this query’s joins
     * yield at most one row per item.
     */
    private static final String ITEM_SELECT = "SELECT i.id, i.host_id, v.id AS version_id, i.status,"
            + " v.title, v.description, v.price, v.capacity, v.weight, v.difficulty, v.location_id,"
            + " lo.name AS location_name,"
            + " a.weekday, a.start_time, a.end_time,"
            + " (SELECT m.image_id FROM media m WHERE m.version_id = v.id ORDER BY m.index ASC, m.image_id ASC LIMIT 1)"
            + " AS cover_image_id,"
            + " (SELECT COALESCE(AVG(r.rating), 0) FROM review r JOIN booking b ON b.id = r.booking_id"
            + " JOIN version vr ON vr.id = b.version_id WHERE CAST(r.target_type AS VARCHAR(16)) = 'ITEM'"
            + " AND vr.item_id = i.id) AS item_avg_rating,"
            + " (SELECT COUNT(*) FROM review r JOIN booking b ON b.id = r.booking_id"
            + " JOIN version vr ON vr.id = b.version_id WHERE CAST(r.target_type AS VARCHAR(16)) = 'ITEM'"
            + " AND vr.item_id = i.id) AS item_total_reviews,"
            + " COUNT(*) OVER () AS total_row_count"
            + ITEM_FROM;

    private final @NonNull JdbcTemplate jdbcTemplate;

    private static final @NonNull ResultSetExtractor<MarketplaceSearchResult> PAGED_RESULT_EXTRACTOR = rs -> {
        final List<ItemModel> items = new ArrayList<>();
        long total = 0L;
        boolean first = true;
        while (rs.next()) {
            if (first) {
                total = rs.getLong("total_row_count");
                first = false;
            }
            items.add(MarketplaceRowMapper.mapRow(rs));
        }
        return new MarketplaceSearchResult(items, total);
    };

    @Autowired
    public MarketplaceJdbc(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public MarketplaceSearchResult searchMarketplace(final MarketplaceQueryModel query) {
        final List<Object> args = new ArrayList<>();
        final int pageSize = resolvePageSize(query);
        final int offset = (resolvePage(query) - 1) * pageSize;
        final String sql = ITEM_SELECT + whereClause(query, args) + orderBy(query) + " LIMIT ? OFFSET ?";
        args.add(pageSize);
        args.add(offset);
        final MarketplaceSearchResult paged =
                Objects.requireNonNull(jdbcTemplate.query(sql, PAGED_RESULT_EXTRACTOR, args.toArray()));
        if (!paged.getItems().isEmpty()) {
            return paged;
        }
        return new MarketplaceSearchResult(List.of(), countMatching(query));
    }

    private long countMatching(final MarketplaceQueryModel query) {
        final List<Object> countArgs = new ArrayList<>();
        final String countSql = "SELECT COUNT(DISTINCT i.id)" + ITEM_FROM + whereClause(query, countArgs);
        final Long count = jdbcTemplate.queryForObject(countSql, Long.class, countArgs.toArray());
        return count == null ? 0L : count;
    }

    private static String whereClause(final MarketplaceQueryModel query, final List<Object> args) {
        final StringBuilder sql = new StringBuilder(" WHERE i.status = 'ACTIVE'");
        if (query == null) {
            return sql.toString();
        }
        if (hasText(query.getSearchQuery())) {
            sql.append(" AND LOWER(v.title) LIKE ? ESCAPE '!'");
            args.add(setupSearchQuery(query.getSearchQuery()));
        }
        if (hasText(query.getLocationSlug())) {
            sql.append(" AND lo.slug = ?");
            args.add(query.getLocationSlug().trim());
        }
        if (hasText(query.getItemTypeSlug())) {
            sql.append(" AND it.slug = ?");
            args.add(query.getItemTypeSlug().trim());
        }
        if (query.getCapacity() != null) {
            sql.append(" AND v.capacity >= ?");
            args.add(query.getCapacity());
        }
        if (query.getWeight() != null) {
            sql.append(" AND v.weight >= ?");
            args.add(query.getWeight());
        }
        if (query.getDifficulty() != null) {
            sql.append(" AND v.difficulty = ?");
            args.add(query.getDifficulty());
        }
        if (query.getMinAvgRating() != null) {
            sql.append(" AND (SELECT COALESCE(AVG(r.rating), 0) FROM review r JOIN booking b ON b.id = r.booking_id"
                    + " JOIN version vr ON vr.id = b.version_id WHERE CAST(r.target_type AS VARCHAR(16)) = 'ITEM'"
                    + " AND vr.item_id = i.id) >= ?");
            args.add(query.getMinAvgRating());
        }
        appendAvailabilityFilter(query, sql, args);
        return sql.toString();
    }

    private static void appendAvailabilityFilter(
            final MarketplaceQueryModel query, final StringBuilder sql, final List<Object> args) {
        if (query.getWeekday() == null && query.getStartTime() == null && query.getEndTime() == null) {
            return;
        }
        sql.append(" AND EXISTS (SELECT 1 FROM availability af WHERE af.version_id = v.id");
        if (query.getWeekday() != null) {
            sql.append(" AND af.weekday = ?");
            args.add(query.getWeekday().name());
        }
        if (query.getStartTime() != null) {
            sql.append(" AND af.start_time <= ?");
            args.add(Time.valueOf(query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            sql.append(" AND af.end_time >= ?");
            args.add(Time.valueOf(query.getEndTime()));
        }
        sql.append(")");
    }

    private static String orderBy(final MarketplaceQueryModel query) {
        if (query == null || query.getSortBy() == null) {
            return " ORDER BY i.created_at DESC, i.id DESC";
        }
        return switch (query.getSortBy()) {
            case "oldest" -> " ORDER BY i.created_at ASC, i.id ASC";
            case "price_asc" -> " ORDER BY v.price ASC, i.id ASC";
            case "price_desc" -> " ORDER BY v.price DESC, i.id ASC";
            case "newest" -> " ORDER BY i.created_at DESC, i.id DESC";
            default -> " ORDER BY i.created_at DESC, i.id DESC";
        };
    }

    private static int resolvePage(final MarketplaceQueryModel query) {
        if (query == null || query.getPage() == null || query.getPage() < 1) {
            return DEFAULT_PAGE;
        }
        return query.getPage();
    }

    private static int resolvePageSize(final MarketplaceQueryModel query) {
        if (query == null || query.getPageSize() == null) {
            return DEFAULT_PAGE_SIZE;
        }
        final int pageSize = query.getPageSize();
        if (pageSize == 6 || pageSize == 12 || pageSize == 18) {
            return pageSize;
        }
        return DEFAULT_PAGE_SIZE;
    }

    private static String setupSearchQuery(final String searchQuery) {
        final String queryWithWildcards = searchQuery
                .trim()
                .toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replaceAll("\\s+", "%");
        return "%" + queryWithWildcards + "%";
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
