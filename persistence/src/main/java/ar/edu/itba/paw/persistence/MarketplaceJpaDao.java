package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceJpaDao implements MarketplaceDao {

    private static final String BOOKING_BLOCKING_STATUSES =
            "CAST('PENDING' AS booking_status_enum), CAST('ACCEPTED' AS booking_status_enum), "
                    + "CAST('PAID' AS booking_status_enum), CAST('CONFIRMED' AS booking_status_enum)";

    private static final String VERSION_FETCH_JPQL = "SELECT DISTINCT v FROM Version v "
            + "JOIN FETCH v.item JOIN FETCH v.location JOIN FETCH v.type "
            + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
            + "WHERE v.id IN :ids";

    private static final String MIN_AVG_RATING_FILTER = "v.item_id IN (SELECT v2.item_id FROM review r "
            + "INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) "
            + "GROUP BY v2.item_id "
            + "HAVING COALESCE(AVG(r.rating), 0) >= :minAvgRating)";

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageModel<Item> searchMarketplace(final MarketplaceQueryModel query) {
        // Total matching rows (ignoring page limits) for pagination UI.
        final long totalCount = countMarketplaceResults(query);

        // Phase 1 (native SQL): filter, sort, and paginate in the DB; fetch only version IDs.
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        String sql = "SELECT v.id FROM version v ";
        sql = getSqlForFilter(query, parameters, whereClauses, sql);
        sql += nativeOrderBy(query);

        final Query nativeQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        Paging.apply(nativeQuery, query.getPage(), query.getPageSize());

        final List<Integer> idList = Paging.toIntegerIds(nativeQuery.getResultList());

        if (idList.isEmpty()) {
            return new PageModel<>(List.of(), query.getPage(), query.getPageSize(), totalCount);
        }

        // Phase 2 (JPQL): load full entities for this page only (one row per item, already latest).
        final String jpqlOrderBy = jpqlOrderBy(query);
        final TypedQuery<Version> versionQuery;
        if (jpqlOrderBy != null) {
            versionQuery = em.createQuery(VERSION_FETCH_JPQL + " ORDER BY " + jpqlOrderBy, Version.class);
        } else {
            versionQuery =
                    em.createQuery(VERSION_FETCH_JPQL + " ORDER BY v.item.createdAt DESC, v.id DESC", Version.class);
        }
        versionQuery.setParameter("ids", idList);

        final List<Version> versions = versionQuery.getResultList();

        // Map page rows to Item; each version is already the latest for its item (see getSqlForFilter).
        // Loop size = page size, not total versions in the DB.
        final List<Item> items = new ArrayList<>(versions.size());
        for (final Version version : versions) {
            final Item item = version.getItem();
            item.setLatestVersion(version);
            items.add(item);
        }

        return new PageModel<>(items, query.getPage(), query.getPageSize(), totalCount);
    }

    private long countMarketplaceResults(final MarketplaceQueryModel query) {
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        String sql = "SELECT COUNT(v.id) FROM version v ";
        sql = getSqlForFilter(query, parameters, whereClauses, sql);

        final Query countQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private static String getSqlForFilter(
            final MarketplaceQueryModel query,
            final Map<String, Object> parameters,
            final List<String> whereClauses,
            String sql) {

        sql += "INNER JOIN item i ON v.item_id = i.id ";

        // One row per item: only its latest version (filtering done in SQL, not in Java).
        whereClauses.add("v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)");
        whereClauses.add("i.status = CAST(:active AS item_status_enum)");
        parameters.put("active", ItemStatusEnum.ACTIVE.name());

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (query.getCapacity() != null) {
            whereClauses.add("v.capacity >= :capacity");
            parameters.put("capacity", query.getCapacity());
        }
        if (query.getWeight() != null) {
            whereClauses.add("v.weight >= :weight");
            parameters.put("weight", query.getWeight());
        }
        if (query.getDifficulty() != null) {
            whereClauses.add("v.difficulty = :difficulty");
            parameters.put("difficulty", query.getDifficulty());
        }
        if (query.getMinAvgRating() != null) {
            whereClauses.add(MIN_AVG_RATING_FILTER);
            parameters.put("itemTargetType", TargetEnum.ITEM.name());
            parameters.put("minAvgRating", query.getMinAvgRating());
        }
        if (query.getLocationSlug() != null) {
            whereClauses.add("EXISTS (SELECT 1 FROM location l WHERE l.id = v.location_id AND l.slug = :location)");
            parameters.put("location", query.getLocationSlug());
        }
        if (query.getItemTypeSlug() != null) {
            whereClauses.add("EXISTS (SELECT 1 FROM item_type t WHERE t.id = v.type_id AND t.slug = :type)");
            parameters.put("type", query.getItemTypeSlug());
        }
        if (query.getWeekday() != null) {
            final StringBuilder availabilityClause =
                    new StringBuilder("EXISTS (SELECT 1 FROM availability a WHERE a.version_id = v.id"
                            + " AND a.weekday = CAST(:weekday AS weekday_enum)");
            parameters.put("weekday", query.getWeekday().name());
            if (query.getStartTime() != null && query.getEndTime() != null) {
                availabilityClause.append(" AND a.start_time <= :requestedStart AND a.end_time >= :requestedEnd");
                parameters.put("requestedStart", query.getStartTime());
                parameters.put("requestedEnd", query.getEndTime());
            }
            whereClauses.add(availabilityClause.append(')').toString());
        }

        if (hasRequestedBookingRange(query)) {
            whereClauses.add("NOT EXISTS (SELECT 1 FROM booking b"
                    + " INNER JOIN version bv ON bv.id = b.version_id"
                    + " WHERE bv.item_id = v.item_id"
                    + " AND b.status IN (" + BOOKING_BLOCKING_STATUSES + ")"
                    + " AND b.start <= ((CAST(:requestedDate AS date) + CAST(:requestedEnd AS time))"
                    + " AT TIME ZONE COALESCE(NULLIF(TRIM(v.timezone), ''), 'UTC')) AT TIME ZONE 'UTC'"
                    + " AND ((CAST(:requestedDate AS date) + CAST(:requestedStart AS time))"
                    + " AT TIME ZONE COALESCE(NULLIF(TRIM(v.timezone), ''), 'UTC')) AT TIME ZONE 'UTC' <= b.\"end\")");
            parameters.put("requestedDate", query.getRequestedDate());
            parameters.put("requestedStart", query.getStartTime());
            parameters.put("requestedEnd", query.getEndTime());
        }

        if (!whereClauses.isEmpty()) {
            sql += "WHERE " + String.join(" AND ", whereClauses);
        }
        return sql;
    }

    // Native ORDER BY for phase 1 (IDs). JPQL re-applies the same sort in phase 2 (IN clause is unordered).
    private static String nativeOrderBy(final MarketplaceQueryModel query) {
        if (resolveSortBy(query) != null) {
            return " ORDER BY " + nativeOrderByClause(query) + " ";
        }
        return " ORDER BY i.created_at DESC, v.id DESC";
    }

    private static String nativeOrderByClause(final MarketplaceQueryModel query) {
        return switch (resolveSortBy(query)) {
            case "oldest" -> "i.created_at ASC, v.id ASC";
            case "priceAsc" -> "v.price ASC, v.id ASC";
            case "priceDesc" -> "v.price DESC, v.id ASC";
            case "newest" -> "i.created_at DESC, v.id DESC";
            default -> "i.created_at DESC, v.id DESC";
        };
    }

    private static String jpqlOrderBy(final MarketplaceQueryModel query) {
        return switch (resolveSortBy(query)) {
            case "oldest" -> "v.item.createdAt ASC, v.id ASC";
            case "priceAsc" -> "v.price ASC, v.id ASC";
            case "priceDesc" -> "v.price DESC, v.id ASC";
            case "newest" -> "v.item.createdAt DESC, v.id DESC";
            default -> null;
        };
    }

    private static String resolveSortBy(final MarketplaceQueryModel query) {
        if (query == null || query.getSortBy() == null) {
            return null;
        }
        return query.getSortBy();
    }

    // Escape LIKE wildcards and turn spaces into % so "foo bar" matches titles containing both words.
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

    private static boolean isEmpty(final String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasRequestedBookingRange(final MarketplaceQueryModel query) {
        return query.getRequestedDate() != null && query.getStartTime() != null && query.getEndTime() != null;
    }
}
