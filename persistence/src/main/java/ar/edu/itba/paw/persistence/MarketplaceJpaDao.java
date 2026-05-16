package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceCardItem;
import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import ar.edu.itba.paw.persistence.projections.MarketplaceRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceJpaDao implements MarketplaceDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    private static final String MARKETPLACE_LISTING =
            "SELECT NEW ar.edu.itba.paw.persistence.projections.MarketplaceRow("
                    + "i.id, i.host.id, v.id,"
                    + " v.title, v.description, v.price, v.capacity, v.weight, v.difficulty,"
                    + " v.location.id, v.location.name, v.type.name, "
                    + "(SELECT MIN(m.image.id) FROM Media m"
                    + " WHERE m.version = v AND m.id.index = ("
                    + "   SELECT MIN(m2.id.index) FROM Media m2 WHERE m2.version = v"
                    + " )),"
                    + "(SELECT COALESCE(AVG(r.rating), 0) FROM Review r"
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i),"
                    + "(SELECT COUNT(r) FROM Review r"
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i)"
                    + ") "
                    + "FROM Item i JOIN Version v ON v.item = i"
                    + "  AND v.id = (SELECT MAX(v2.id) FROM Version v2 WHERE v2.item = i)";

    private static final String ITEM_LISTING_COUNT = "SELECT COUNT(i) FROM Item i"
            + " JOIN Version v ON v.item = i"
            + "  AND v.id = (SELECT MAX(v2.id) FROM Version v2 WHERE v2.item = i)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public MarketplaceSearchResult searchMarketplace(final MarketplaceQueryModel query) {
        final Map<String, Object> params = new HashMap<>();
        final int pageSize = resolvePageSize(query);
        final int offset = (resolvePage(query) - 1) * pageSize;
        params.put("itemTargetType", TargetEnum.ITEM);

        final String hql = MARKETPLACE_LISTING + whereClause(query, params) + orderBy(query);
        final TypedQuery<MarketplaceRow> queryResult = entityManager.createQuery(hql, MarketplaceRow.class);
        bindParams(queryResult, params);
        queryResult.setFirstResult(offset);
        queryResult.setMaxResults(pageSize);

        final List<MarketplaceCardItem> items = queryResult.getResultList().stream()
                .map(MarketplaceRow::toCardItem)
                .toList();
        return new MarketplaceSearchResult(items, countMatching(query));
    }

    private long countMatching(final MarketplaceQueryModel query) {
        final Map<String, Object> params = new HashMap<>();
        final String countHql = ITEM_LISTING_COUNT + whereClause(query, params);
        final TypedQuery<Long> countQuery = entityManager.createQuery(countHql, Long.class);
        bindParams(countQuery, params);
        return toLong(countQuery.getSingleResult());
    }

    private static void bindParams(final javax.persistence.Query query, final Map<String, Object> params) {
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static String whereClause(final MarketplaceQueryModel query, final Map<String, Object> params) {
        final StringBuilder sql = new StringBuilder(" WHERE i.status = :activeStatus");
        params.put("activeStatus", ItemStatusEnum.ACTIVE);
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
        if (hasText(query.getItemTypeSlug())) {
            sql.append(" AND v.type.slug = :itemTypeSlug");
            params.put("itemTypeSlug", query.getItemTypeSlug().trim());
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
        if (query.getMinAvgRating() != null) {
            sql.append(" AND (SELECT COALESCE(AVG(rm.rating), 0) FROM Review rm WHERE rm.targetType = :itemTargetType"
                    + " AND rm.booking.version.item = i) >= :minAvgRating");
            params.put("itemTargetType", TargetEnum.ITEM);
            params.put("minAvgRating", query.getMinAvgRating());
        }
        appendAvailabilityFilter(query, sql, params);
        return sql.toString();
    }

    private static void appendAvailabilityFilter(
            final MarketplaceQueryModel query, final StringBuilder sql, final Map<String, Object> params) {
        if (query.getWeekday() == null && query.getStartTime() == null && query.getEndTime() == null) {
            return;
        }
        sql.append(" AND EXISTS (SELECT 1 FROM Availability af WHERE af.version = v");
        if (query.getWeekday() != null) {
            sql.append(" AND af.weekday = :weekday");
            params.put("weekday", WeekdayEnum.valueOf(query.getWeekday().name()));
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

    @Override
    public List<Availability> getAllAvailabilities() {
        return entityManager
                .createQuery("FROM Availability", Availability.class)
                .getResultList();
    }

    @Override
    public List<Booking> getAllBlockingBookings() {
        return entityManager
                .createQuery(
                        "SELECT b FROM Booking b WHERE b.guest IS NOT NULL AND b.guest.id <> b.version.item.host.id",
                        Booking.class)
                .getResultList();
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
