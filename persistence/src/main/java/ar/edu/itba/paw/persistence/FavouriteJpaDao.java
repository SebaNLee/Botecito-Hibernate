package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Favourite;
import ar.edu.itba.paw.models.entity.FavouriteId;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class FavouriteJpaDao implements FavouriteDao {

    private static final String ITEM_FETCH_JPQL = "SELECT i FROM Item i "
            + "JOIN FETCH i.host "
            + "JOIN FETCH i.latestVersion lv "
            + "JOIN FETCH lv.location JOIN FETCH lv.type "
            + "LEFT JOIN FETCH lv.media m LEFT JOIN FETCH m.image "
            + "INNER JOIN Favourite f ON f.item = i AND f.user.id = :userId "
            + "WHERE i.id IN :ids";

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
    public PageModel<Item> listFavourites(final FavouritesQueryModel query) {
        final long totalCount = countFavourites(query);

        // Phase 1 (native SQL): filter, sort, and paginate in the DB; fetch only item IDs.
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        String sql = "SELECT i.id FROM version v ";
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

        // Phase 2 (JPQL): load items with associations needed by listing cards (latest version via @JoinFormula).
        final String jpqlOrderBy = jpqlOrderBy(query);
        final TypedQuery<Item> itemQuery;
        if (jpqlOrderBy != null) {
            itemQuery = em.createQuery(ITEM_FETCH_JPQL + " ORDER BY " + jpqlOrderBy, Item.class);
        } else {
            itemQuery = em.createQuery(ITEM_FETCH_JPQL + " ORDER BY f.createdAt DESC, i.id DESC", Item.class);
        }
        itemQuery.setParameter("userId", query.getUserId());
        itemQuery.setParameter("ids", idList);

        return new PageModel<>(
                new ArrayList<>(new LinkedHashSet<>(itemQuery.getResultList())),
                query.getPage(),
                query.getPageSize(),
                totalCount);
    }

    @Override
    public long countFavourites(final FavouritesQueryModel query) {
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        String sql = "SELECT COUNT(i.id) FROM version v ";
        sql = getSqlForFilter(query, parameters, whereClauses, sql);

        final Query countQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private static String getSqlForFilter(
            final FavouritesQueryModel query,
            final Map<String, Object> parameters,
            final List<String> whereClauses,
            String sql) {

        sql += "INNER JOIN item i ON v.item_id = i.id ";
        sql += "INNER JOIN favourite f ON f.item_id = i.id AND f.user_id = :userId ";
        parameters.put("userId", query.getUserId());

        // One row per item: only its latest version (filtering done in SQL, not in Java).
        whereClauses.add("v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)");
        whereClauses.add("i.status = CAST(:active AS item_status_enum)");
        parameters.put("active", ItemStatusEnum.ACTIVE.name());

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }

        if (!whereClauses.isEmpty()) {
            sql += "WHERE " + String.join(" AND ", whereClauses);
        }
        return sql;
    }

    // Native ORDER BY for phase 1 (IDs). JPQL re-applies the same sort in phase 2 (IN clause is unordered).
    private static String nativeOrderBy(final FavouritesQueryModel query) {
        if (resolveSortBy(query) != null) {
            return " ORDER BY " + nativeOrderByClause(query) + " ";
        }
        return " ORDER BY f.created_at DESC, i.id DESC";
    }

    private static String nativeOrderByClause(final FavouritesQueryModel query) {
        final String sortBy = resolveSortBy(query);
        return switch (sortBy) {
            case "oldest" -> "f.created_at ASC, i.id ASC";
            case "nameAsc" -> "v.title ASC, i.id ASC";
            case "nameDesc" -> "v.title DESC, i.id DESC";
            case "newest" -> "f.created_at DESC, i.id DESC";
            default -> "f.created_at DESC, i.id DESC";
        };
    }

    private static String jpqlOrderBy(final FavouritesQueryModel query) {
        final String sortBy = resolveSortBy(query);
        if (sortBy == null) {
            return null;
        }
        return switch (sortBy) {
            case "oldest" -> "f.createdAt ASC, i.id ASC";
            case "nameAsc" -> "lv.title ASC, i.id ASC";
            case "nameDesc" -> "lv.title DESC, i.id DESC";
            case "newest" -> "f.createdAt DESC, i.id DESC";
            default -> null;
        };
    }

    private static String resolveSortBy(final FavouritesQueryModel query) {
        if (query == null || query.getSortBy() == null) {
            return null;
        }
        return query.getSortBy();
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
}
