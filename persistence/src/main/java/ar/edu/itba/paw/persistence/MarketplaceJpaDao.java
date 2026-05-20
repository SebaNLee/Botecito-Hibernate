package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceJpaDao implements MarketplaceDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public MarketplaceSearchResult searchMarketplace(final MarketplaceQueryModel query) {
        final String marketplaceFilter = marketplaceFilter(query);
        final TypedQuery<Long> countQuery =
                entityManager.createQuery("SELECT COUNT(v) FROM Version v" + marketplaceFilter, Long.class);
        setMarketplaceParams(countQuery, query);
        final long totalCount = countQuery.getSingleResult();

        final TypedQuery<Version> queryResult = entityManager.createQuery(
                "SELECT v FROM Version v" + marketplaceFilter + orderBy(query), Version.class);
        setMarketplaceParams(queryResult, query);

        final int pageSize = resolvePageSize(query);
        final int offset = (resolvePage(query) - 1) * pageSize;
        queryResult.setFirstResult(offset);
        queryResult.setMaxResults(pageSize);

        final List<Version> results = queryResult.getResultList();

        return new MarketplaceSearchResult(results, totalCount);
    }

    private static String marketplaceFilter(final MarketplaceQueryModel query) {
        final StringBuilder jpql = new StringBuilder(
                " WHERE v.createdAt = (SELECT MAX(v2.createdAt) FROM Version v2 WHERE v2.item = v.item)"
                        + " AND v.item.status = :active");

        if (!isEmpty(query.getSearchQuery())) {
            jpql.append(" AND LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
        }
        if (query.getCapacity() != null) {
            jpql.append(" AND v.capacity >= :capacity");
        }
        if (query.getWeight() != null) {
            jpql.append(" AND v.weight >= :weight");
        }
        if (query.getDifficulty() != null) {
            jpql.append(" AND v.difficulty = :difficulty");
        }
        if (query.getMinAvgRating() != null) {
            jpql.append(" AND (SELECT COALESCE(AVG(r.rating), 0) FROM Review r"
                    + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = v.item) >= :minAvgRating");
        }
        if (query.getLocationSlug() != null) {
            jpql.append(" AND v.location.slug = :location");
        }
        if (query.getItemTypeSlug() != null) {
            jpql.append(" AND v.type.slug = :type");
        }
        if (queryFiltersDay(query)) {
            jpql.append(" AND EXISTS (SELECT a.id FROM Availability a"
                    + " WHERE a.version = v AND a.weekday = :weekday"
                    + " AND a.startTime <= :requestedStart AND a.endTime >= :requestedEnd)");
        }
        return jpql.toString();
    }

    private static void setMarketplaceParams(final Query queryResult, final MarketplaceQueryModel query) {
        queryResult.setParameter("active", ItemStatusEnum.ACTIVE);

        if (!isEmpty(query.getSearchQuery())) {
            queryResult.setParameter("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (query.getCapacity() != null) {
            queryResult.setParameter("capacity", query.getCapacity());
        }
        if (query.getWeight() != null) {
            queryResult.setParameter("weight", query.getWeight());
        }
        if (query.getDifficulty() != null) {
            queryResult.setParameter("difficulty", query.getDifficulty());
        }
        if (query.getMinAvgRating() != null) {
            queryResult.setParameter("itemTargetType", TargetEnum.ITEM);
            queryResult.setParameter("minAvgRating", query.getMinAvgRating());
        }
        if (query.getLocationSlug() != null) {
            queryResult.setParameter("location", query.getLocationSlug());
        }
        if (query.getItemTypeSlug() != null) {
            queryResult.setParameter("type", query.getItemTypeSlug());
        }
        if (queryFiltersDay(query)) {
            queryResult.setParameter("weekday", query.getWeekday());
            queryResult.setParameter("requestedStart", query.getStartTime());
            queryResult.setParameter("requestedEnd", query.getEndTime());
        }
    }

    private static boolean queryFiltersDay(final MarketplaceQueryModel query) {
        return query.getWeekday() != null && query.getStartTime() != null && query.getEndTime() != null;
    }

    private static String orderBy(final MarketplaceQueryModel query) {
        String defaultOrder = " ORDER BY v.item.createdAt DESC, v.id DESC";
        if (query == null || query.getSortBy() == null) {
            return defaultOrder;
        }
        return switch (query.getSortBy()) {
            case "oldest" -> " ORDER BY v.item.createdAt ASC, v.id ASC";
            case "price_asc" -> " ORDER BY v.price ASC, v.id ASC";
            case "price_desc" -> " ORDER BY v.price DESC, v.id ASC";
            case "newest" -> " ORDER BY v.item.createdAt DESC, v.id DESC";
            default -> defaultOrder;
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

    private static boolean isEmpty(final String value) {
        return value == null || value.isBlank();
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
}
