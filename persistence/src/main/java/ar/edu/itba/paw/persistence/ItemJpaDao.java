package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
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
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJpaDao implements ItemDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public SearchResult<Item> listOwnerItems(MyBoatsQueryModel query) {
        final long totalCount = countOwnerItems(query);

        // Phase 1: native SQL for IDs with dynamic filters + ordering + pagination
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        whereClauses.add("i.host_id = :ownerId");
        parameters.put("ownerId", query.getOwnerId());

        whereClauses.add("i.status <> CAST(:deleted AS item_status_enum)");
        parameters.put("deleted", ItemStatusEnum.DELETED.name());

        String sql =
                "SELECT v.id FROM item i JOIN version v ON v.id = (SELECT v2.id FROM version v2 WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1)";

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (!isEmpty(query.getStatus())) {
            whereClauses.add("i.status = CAST(:status AS item_status_enum)");
            parameters.put("status", query.getStatus());
        }

        sql += " WHERE " + String.join(" AND ", whereClauses);
        sql += " ORDER BY " + nativeOrderBy(query);

        final Query nativeQuery = em.createNativeQuery(sql);
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        Paging.apply(nativeQuery, query.getPage(), query.getPageSize());
        final List<Integer> versionIds = Paging.toIntegerIds(nativeQuery.getResultList());

        if (versionIds.isEmpty()) {
            return new SearchResult<>(List.of(), totalCount);
        }

        final String jpql = "SELECT DISTINCT v FROM Version v "
                + "JOIN FETCH v.item JOIN FETCH v.location JOIN FETCH v.type "
                + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
                + "WHERE v.id IN :ids";
        final TypedQuery<Version> versionQuery = em.createQuery(jpql, Version.class);
        versionQuery.setParameter("ids", versionIds);
        final List<Version> versions = versionQuery.getResultList();

        final Map<Integer, Version> versionById =
                versions.stream().collect(Collectors.toMap(Version::getId, Function.identity()));

        final List<Item> sortedItems = new ArrayList<>(versionIds.size());
        for (final Integer versionId : versionIds) {
            final Version v = versionById.get(versionId);
            if (v == null) continue;
            final Item item = v.getItem();
            item.setLatestVersion(v);
            sortedItems.add(item);
        }

        return new SearchResult<>(sortedItems, totalCount);
    }

    private long countOwnerItems(MyBoatsQueryModel query) {
        final Map<String, Object> parameters = new HashMap<>();
        final List<String> whereClauses = new ArrayList<>();

        whereClauses.add("i.host_id = :ownerId");
        parameters.put("ownerId", query.getOwnerId());

        whereClauses.add("i.status <> CAST(:deleted AS item_status_enum)");
        parameters.put("deleted", ItemStatusEnum.DELETED.name());

        String sql = "SELECT COUNT(DISTINCT i.id) FROM item i";
        boolean needsVersionJoin = !isEmpty(query.getSearchQuery()) || isNameSort(query.getSortBy());

        if (needsVersionJoin) {
            sql +=
                    " JOIN version v ON v.id = (SELECT v2.id FROM version v2 WHERE v2.item_id = i.id ORDER BY v2.created_at DESC LIMIT 1)";
        }

        if (!isEmpty(query.getSearchQuery())) {
            whereClauses.add("LOWER(v.title) LIKE LOWER(:searchQuery) ESCAPE '!'");
            parameters.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }
        if (!isEmpty(query.getStatus())) {
            whereClauses.add("i.status = CAST(:status AS item_status_enum)");
            parameters.put("status", query.getStatus());
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

    @Override
    public void deleteItem(final Item item) {
        em.remove(item);
    }
}
