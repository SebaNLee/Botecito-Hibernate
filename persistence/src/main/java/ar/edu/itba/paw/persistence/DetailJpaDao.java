package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

@Repository
public class DetailJpaDao implements DetailDao {

    private static final String SQL_LATEST_VERSION_ID_FOR_ITEM = "SELECT v.id FROM version v "
            + "INNER JOIN item i ON v.item_id = i.id "
            + "WHERE i.id = :itemId "
            + "AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)";

    private static final String VERSION_FETCH_JPQL = "SELECT DISTINCT v FROM Version v "
            + "JOIN FETCH v.item i JOIN FETCH i.host "
            + "JOIN FETCH v.location JOIN FETCH v.type "
            + "LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image "
            + "WHERE v.id IN :ids";

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Item> getItemDetail(final int itemId) {
        final Integer versionId = resolveLatestVersionIdForItem(itemId);
        if (versionId == null) {
            return Optional.empty();
        }

        return Optional.of(loadItemViaVersionFetch(versionId));
    }

    private Integer resolveLatestVersionIdForItem(final int itemId) {
        final Query nativeQuery = em.createNativeQuery(SQL_LATEST_VERSION_ID_FOR_ITEM);
        nativeQuery.setParameter("itemId", itemId);
        nativeQuery.setMaxResults(1);

        final List<Integer> idList = Paging.toIntegerIds(nativeQuery.getResultList());

        return idList.isEmpty() ? null : idList.get(0);
    }

    private Item loadItemViaVersionFetch(final int versionId) {
        final TypedQuery<Version> versionQuery = em.createQuery(VERSION_FETCH_JPQL, Version.class);
        versionQuery.setParameter("ids", List.of(versionId));

        final List<Version> versions = versionQuery.getResultList();
        if (versions.isEmpty()) {
            throw new IllegalStateException("Version not found: " + versionId);
        }
        final Version version = versions.get(0);
        // Init while the persistence context is open (detail uses this collection after the service returns).
        Hibernate.initialize(version.getAvailabilities());
        final Item item = version.getItem();
        item.setLatestVersion(version);
        return item;
    }
}
