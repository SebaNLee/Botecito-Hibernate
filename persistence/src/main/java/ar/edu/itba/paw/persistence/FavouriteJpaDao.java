package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Favourite;
import ar.edu.itba.paw.models.entity.FavouriteId;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class FavouriteJpaDao implements FavouriteDao {

    private static final String VERSION_FETCH_JPQL = "SELECT DISTINCT v FROM Version v "
            + "JOIN FETCH v.item i JOIN FETCH i.host JOIN FETCH v.location JOIN FETCH v.type "
            + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
            + "WHERE v.id IN :ids";
    private static final int DEFAULT_PAGE_SIZE = 12;

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean create(final int userId, final int itemId) {
        final FavouriteId id = new FavouriteId(userId, itemId);
        if (em.find(Favourite.class, id) != null) {
            return false;
        }
        final Favourite favourite = new Favourite();
        favourite.setId(id);
        favourite.setUser(em.getReference(Users.class, userId));
        favourite.setItem(em.getReference(Item.class, itemId));
        favourite.setCreatedAt(LocalDateTime.now());
        em.persist(favourite);
        return true;
    }

    @Override
    public boolean delete(final int userId, final int itemId) {
        final Favourite favourite = em.find(Favourite.class, new FavouriteId(userId, itemId));
        if (favourite == null) {
            return false;
        }
        em.remove(favourite);
        return true;
    }

    @Override
    public boolean exists(final int userId, final int itemId) {
        return em.find(Favourite.class, new FavouriteId(userId, itemId)) != null;
    }

    @Override
    public Set<Integer> findFavouriteItemIds(final int userId, final Collection<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery(
                        "SELECT f.item.id FROM Favourite f WHERE f.user.id = :userId AND f.item.id IN :itemIds",
                        Integer.class)
                .setParameter("userId", userId)
                .setParameter("itemIds", itemIds)
                .getResultList());
    }

    @Override
    public SearchResult<Item> listFavourites(final FavouritesQueryModel query) {
        final long totalCount = countFavourites(query);

        final Map<String, Object> parameters = new LinkedHashMap<>();
        final List<String> whereClauses = new ArrayList<>();
        String sql = "SELECT v.id FROM favourite f "
                + "INNER JOIN item i ON f.item_id = i.id "
                + "INNER JOIN version v ON v.id = (SELECT v2.id FROM version v2 "
                + "WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1) ";
        sql = getSqlForFilter(query, parameters, whereClauses, sql);
        sql += " ORDER BY " + nativeOrderBy(query);

        final Query versionIdsQuery = em.createNativeQuery(sql);
        setParameters(versionIdsQuery, parameters);
        Paging.apply(versionIdsQuery, resolvePage(query), resolvePageSize(query));

        final List<Integer> versionIds = Paging.toIntegerIds(versionIdsQuery.getResultList());
        if (versionIds.isEmpty()) {
            return new SearchResult<>(List.of(), totalCount);
        }

        final TypedQuery<Version> versionQuery = em.createQuery(VERSION_FETCH_JPQL, Version.class);
        versionQuery.setParameter("ids", versionIds);

        final Map<Integer, Version> versionById = new LinkedHashMap<>();
        for (final Version version : versionQuery.getResultList()) {
            versionById.put(version.getId(), version);
        }

        final List<Item> items = new ArrayList<>(versionIds.size());
        for (final Integer versionId : versionIds) {
            final Version version = versionById.get(versionId);
            if (version == null) {
                continue;
            }
            final Item item = version.getItem();
            item.setLatestVersion(version);
            items.add(item);
        }
        populateReviewTransients(items);
        return new SearchResult<>(items, totalCount);
    }

    @Override
    public int countFavourites(final FavouritesQueryModel query) {
        final Map<String, Object> parameters = new LinkedHashMap<>();
        final List<String> whereClauses = new ArrayList<>();
        String sql = "SELECT COUNT(i.id) FROM favourite f "
                + "INNER JOIN item i ON f.item_id = i.id "
                + "INNER JOIN version v ON v.id = (SELECT v2.id FROM version v2 "
                + "WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1) ";
        sql = getSqlForFilter(query, parameters, whereClauses, sql);
        final Query countQuery = em.createNativeQuery(sql);
        setParameters(countQuery, parameters);
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    private static String getSqlForFilter(
            final FavouritesQueryModel query,
            final Map<String, Object> parameters,
            final List<String> whereClauses,
            String sql) {
        whereClauses.add("f.user_id = :userId");
        parameters.put("userId", query.getUserId());
        whereClauses.add("i.status = CAST(:active AS item_status_enum)");
        parameters.put("active", ItemStatusEnum.ACTIVE.name());

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }

        sql += "WHERE " + String.join(" AND ", whereClauses);
        return sql;
    }

    private static void setParameters(final Query query, final Map<String, Object> parameters) {
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static String nativeOrderBy(final FavouritesQueryModel query) {
        final String sortBy = query.getSortBy();
        if (sortBy == null || sortBy.isBlank()) {
            return "f.created_at DESC, f.item_id DESC";
        }
        return switch (sortBy) {
            case "oldest" -> "f.created_at ASC, f.item_id ASC";
            case "nameAsc" -> "v.title ASC, i.id ASC";
            case "nameDesc" -> "v.title DESC, i.id DESC";
            default -> "f.created_at DESC, f.item_id DESC";
        };
    }

    private static int resolvePage(final FavouritesQueryModel query) {
        return query == null ? Paging.DEFAULT_PAGE : Paging.resolvePage(query.getPage());
    }

    private static int resolvePageSize(final FavouritesQueryModel query) {
        return query == null
                ? DEFAULT_PAGE_SIZE
                : Paging.resolvePageSize(query.getPageSize(), DEFAULT_PAGE_SIZE, 6, 12, 18);
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

    private void populateReviewTransients(final List<Item> items) {
        for (final Item item : items) {
            final long totalReviews = countReviewsForItem(item.getId());
            item.setTotalReviews(totalReviews);
            if (totalReviews > 0) {
                item.setAverageRating(averageRatingForItem(item.getId()));
            }
        }
    }

    private long countReviewsForItem(final int itemId) {
        final Query query = em.createNativeQuery("SELECT COUNT(r.id) FROM review r "
                + "INNER JOIN booking b ON r.booking_id = b.id "
                + "INNER JOIN version v2 ON b.version_id = v2.id "
                + "WHERE r.target_type = CAST(:target AS target_enum) AND v2.item_id = :itemId");
        query.setParameter("target", TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        return ((Number) query.getSingleResult()).longValue();
    }

    private double averageRatingForItem(final int itemId) {
        final Query query = em.createNativeQuery("SELECT COALESCE(AVG(r.rating), 0) FROM review r "
                + "INNER JOIN booking b ON r.booking_id = b.id "
                + "INNER JOIN version v2 ON b.version_id = v2.id "
                + "WHERE r.target_type = CAST(:target AS target_enum) AND v2.item_id = :itemId");
        query.setParameter("target", TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        return ((Number) query.getSingleResult()).doubleValue();
    }
}
