package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJpaDao implements ItemDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ItemSearchResult listOwnerItems(MyBoatsQueryModel query) {
        final long totalCount = countOwnerItems(query);

        // Phase 1: native SQL for IDs with dynamic filters + ordering + pagination
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        whereClauses.add("i.host_id = :ownerId");
        parameters.put("ownerId", query.getOwnerId());

        whereClauses.add("i.status <> CAST(:deleted AS item_status_enum)");
        parameters.put("deleted", ItemStatusEnum.DELETED.name());

        String sql = "SELECT i.id FROM item i";
        sql += " JOIN version v ON v.id = (SELECT v2.id FROM version v2 WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1)";

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (!isEmpty(query.getStatus())) {
            whereClauses.add("i.status = CAST(:status AS item_status_enum)");
            parameters.put("status", query.getStatus());
        }
        if (!isEmpty(query.getLocationSlug())) {
            whereClauses.add("EXISTS (SELECT 1 FROM location l WHERE l.id = v.location_id AND l.slug = :location)");
            parameters.put("location", query.getLocationSlug());
        }

        sql += " WHERE " + String.join(" AND ", whereClauses);
        sql += " ORDER BY " + nativeOrderBy(query);

        final Query nativeQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        Paging.apply(nativeQuery, query.getPage(), query.getPageSize());
        final List<Integer> ids = Paging.toIntegerIds(nativeQuery.getResultList());

        if (ids.isEmpty()) {
            return new ItemSearchResult(List.of(), totalCount);
        }

        // Phase 2: load full entities (no ORDER BY needed — reorder by Phase 1 ID order)
        List<Item> items = em.createQuery(
                        "SELECT DISTINCT i FROM Item i WHERE i.id IN :ids", Item.class)
                .setParameter("ids", ids)
                .getResultList();

        final Map<Integer, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        final List<Item> sortedItems = ids.stream()
                .map(itemMap::get)
                .collect(Collectors.toList());

        return new ItemSearchResult(sortedItems, totalCount);
    }

    private long countOwnerItems(MyBoatsQueryModel query) {
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        whereClauses.add("i.host_id = :ownerId");
        parameters.put("ownerId", query.getOwnerId());

        whereClauses.add("i.status <> CAST(:deleted AS item_status_enum)");
        parameters.put("deleted", ItemStatusEnum.DELETED.name());

        String sql = "SELECT COUNT(DISTINCT i.id) FROM item i";
        boolean needsVersionJoin = !isEmpty(query.getSearchQuery())
                || !isEmpty(query.getLocationSlug())
                || isNameSort(query.getSortBy());

        if (needsVersionJoin) {
            sql += " JOIN version v ON v.id = (SELECT v2.id FROM version v2 WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1)";
        }

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (!isEmpty(query.getStatus())) {
            whereClauses.add("i.status = CAST(:status AS item_status_enum)");
            parameters.put("status", query.getStatus());
        }
        if (!isEmpty(query.getLocationSlug())) {
            whereClauses.add("EXISTS (SELECT 1 FROM location l WHERE l.id = v.location_id AND l.slug = :location)");
            parameters.put("location", query.getLocationSlug());
        }

        sql += " WHERE " + String.join(" AND ", whereClauses);

        final Query countQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private static String nativeOrderBy(final MyBoatsQueryModel query) {
        final String sortBy = query.getSortBy();
        if (sortBy == null || sortBy.isBlank()) {
            return "i.created_at DESC, i.id DESC";
        }
        return switch (sortBy) {
            case "oldest" -> "i.created_at ASC, i.id ASC";
            case "nameAsc" -> "v.title ASC, i.id ASC";
            case "nameDesc" -> "v.title DESC, i.id DESC";
            default -> "i.created_at DESC, i.id DESC";
        };
    }

    private static boolean isNameSort(final String sortBy) {
        return "nameAsc".equals(sortBy) || "nameDesc".equals(sortBy);
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
    public Optional<Item> findItemById(int id) {
        return Optional.ofNullable(em.find(Item.class, id));
    }

    @Override
    public Optional<Image> findImageById(int id) {
        return Optional.ofNullable(em.find(Image.class, id));
    }
}
