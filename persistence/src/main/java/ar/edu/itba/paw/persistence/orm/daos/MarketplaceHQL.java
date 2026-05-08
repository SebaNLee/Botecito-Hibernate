package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.ItemStatus;
import ar.edu.itba.paw.models.nuevo.MarketplaceQueryModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import ar.edu.itba.paw.persistence.nuevo.MarketplaceDao;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.WeekdayEnumOrm;
import ar.edu.itba.paw.persistence.orm.projections.MarketplaceSearchRowOrm;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class MarketplaceHQL implements MarketplaceDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final String ITEM_TARGET_PARAM = "itemTargetType";

    private static final String ITEM_SELECT =
            "SELECT new ar.edu.itba.paw.persistence.orm.projections.MarketplaceSearchRowOrm("
                    + "i.id, i.host.id, i.status, "
                    + "v.title, v.description, v.price, v.capacity, v.weight, v.difficulty, v.location.id, "
                    + "v.location.name, "
                    + "a.weekday, a.startTime, a.endTime, "
                    + "(SELECT MIN(m.image.id) FROM MediaOrm m "
                    + " WHERE m.version = v AND m.id.index = ("
                    + "   SELECT MIN(m2.id.index) FROM MediaOrm m2 WHERE m2.version = v"
                    + " )), "
                    + "(SELECT COALESCE(AVG(r.rating), 0) FROM ReviewOrm r "
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i), "
                    + "(SELECT COUNT(r) FROM ReviewOrm r "
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i)"
                    + ") "
                    + "FROM ItemOrm i "
                    + "JOIN VersionOrm v ON v.item = i "
                    + "  AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i) "
                    + "LEFT JOIN AvailabilityOrm a ON a.version = v "
                    + "  AND a.id = (SELECT MIN(a2.id) FROM AvailabilityOrm a2 WHERE a2.version = v)";

    private static final String ITEM_COUNT = "SELECT COUNT(i) "
            + "FROM ItemOrm i "
            + "JOIN VersionOrm v ON v.item = i "
            + "  AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public MarketplaceSearchResult searchMarketplace(final MarketplaceQueryModel query) {
        final Map<String, Object> params = new HashMap<>();
        final int pageSize = resolvePageSize(query);
        final int offset = (resolvePage(query) - 1) * pageSize;
        params.put(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM);

        final String hql = ITEM_SELECT + whereClause(query, params) + orderBy(query);
        final TypedQuery<MarketplaceSearchRowOrm> queryResult =
                entityManager.createQuery(hql, MarketplaceSearchRowOrm.class);
        bindParams(queryResult, params);
        queryResult.setFirstResult(offset);
        queryResult.setMaxResults(pageSize);

        final List<MarketplaceSearchRowOrm> rows = queryResult.getResultList();
        if (!rows.isEmpty()) {
            return new MarketplaceSearchResult(mapRows(rows), countMatching(query));
        }
        return new MarketplaceSearchResult(List.of(), countMatching(query));
    }

    private long countMatching(final MarketplaceQueryModel query) {
        final Map<String, Object> params = new HashMap<>();
        final String countHql = ITEM_COUNT + whereClause(query, params);
        final TypedQuery<Long> countQuery = entityManager.createQuery(countHql, Long.class);
        bindParams(countQuery, params);
        return toLong(countQuery.getSingleResult());
    }

    private static List<ItemModel> mapRows(final List<MarketplaceSearchRowOrm> rows) {
        final List<ItemModel> items = new ArrayList<>(rows.size());
        for (final MarketplaceSearchRowOrm row : rows) {
            items.add(mapRow(row));
        }
        return items;
    }

    private static ItemModel mapRow(final MarketplaceSearchRowOrm row) {
        final ItemModel item = new ItemModel();
        item.setId(row.getItemId());
        final Integer hostId = row.getHostId();
        item.setHostId(hostId == null ? null : hostId.toString());
        final ItemStatusEnumOrm statusEnum = row.getStatus();
        item.setStatus(statusEnum == null ? null : ItemStatus.valueOf(statusEnum.name()));
        item.setTitle(row.getTitle());
        item.setDescription(row.getDescription());
        item.setPrice(row.getPrice());
        item.setCapacity(row.getCapacity());
        item.setWeight(row.getWeight());
        item.setDifficulty(row.getDifficulty());
        item.setLocationId(row.getLocationId());
        item.setLocation(row.getLocationName());

        final Integer coverImageId = row.getCoverImageId();
        if (coverImageId != null) {
            item.setImages(List.of("/image/" + coverImageId));
        } else {
            item.setImages(List.of("/css/boat-placeholder.svg"));
        }

        item.setAverageRating(row.getAverageRating() == null ? 0D : row.getAverageRating());
        item.setTotalReviews(
                row.getTotalReviews() == null ? 0 : row.getTotalReviews().intValue());

        final WeekdayEnumOrm weekday = row.getWeekday();
        item.setWeekday(weekday == null ? null : DayOfWeek.valueOf(weekday.name()));
        item.setStartTime(row.getStartTime());
        item.setEndTime(row.getEndTime());

        return item;
    }

    private static void bindParams(final javax.persistence.Query query, final Map<String, Object> params) {
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static String whereClause(final MarketplaceQueryModel query, final Map<String, Object> params) {
        final StringBuilder sql = new StringBuilder(" WHERE i.status = :activeStatus");
        params.put("activeStatus", ItemStatusEnumOrm.ACTIVE);
        if (query == null) {
            return sql.toString();
        }
        if (hasText(query.getSearchQuery())) {
            sql.append(" AND LOWER(v.title) LIKE :searchQuery ESCAPE '!'");
            params.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (hasText(query.getLocationSlug())) {
            sql.append(" AND v.location.slug = :locationSlug");
            params.put("locationSlug", query.getLocationSlug().trim());
        }
        if (query.getCapacity() != null) {
            sql.append(" AND v.capacity >= :capacity");
            params.put("capacity", query.getCapacity());
        }
        if (query.getWeight() != null) {
            sql.append(" AND v.weight >= :weight");
            params.put("weight", query.getWeight());
        }
        if (query.getDifficulty() != null) {
            sql.append(" AND v.difficulty = :difficulty");
            params.put("difficulty", query.getDifficulty());
        }
        appendAvailabilityFilter(query, sql, params);
        return sql.toString();
    }

    private static void appendAvailabilityFilter(
            final MarketplaceQueryModel query, final StringBuilder sql, final Map<String, Object> params) {
        if (query.getWeekday() == null && query.getStartTime() == null && query.getEndTime() == null) {
            return;
        }
        sql.append(" AND EXISTS (SELECT 1 FROM AvailabilityOrm af WHERE af.version = v");
        if (query.getWeekday() != null) {
            sql.append(" AND af.weekday = :weekday");
            params.put("weekday", WeekdayEnumOrm.valueOf(query.getWeekday().name()));
        }
        if (query.getStartTime() != null) {
            sql.append(" AND af.startTime <= :startTime");
            params.put("startTime", query.getStartTime());
        }
        if (query.getEndTime() != null) {
            sql.append(" AND af.endTime >= :endTime");
            params.put("endTime", query.getEndTime());
        }
        sql.append(")");
    }

    private static String orderBy(final MarketplaceQueryModel query) {
        if (query == null || query.getSortBy() == null) {
            return " ORDER BY i.createdAt DESC, i.id DESC";
        }
        return switch (query.getSortBy()) {
            case "oldest" -> " ORDER BY i.createdAt ASC, i.id ASC";
            case "price_asc" -> " ORDER BY v.price ASC, i.id ASC";
            case "price_desc" -> " ORDER BY v.price DESC, i.id ASC";
            case "newest" -> " ORDER BY i.createdAt DESC, i.id DESC";
            default -> " ORDER BY i.createdAt DESC, i.id DESC";
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

    private static long toLong(final Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
