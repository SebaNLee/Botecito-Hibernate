package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class ManageItemJpaDao implements ManageItemDao {

    private static final String SQL_LATEST_VERSION_ID_FOR_ITEM = "SELECT v.id FROM version v "
            + "WHERE v.item_id = :itemId "
            + "AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Item> findItemById(final int itemId) {
        return Optional.ofNullable(entityManager.find(Item.class, itemId));
    }

    @Override
    public int countVersionsByItemId(final int itemId) {
        final Long count = entityManager
                .createQuery("SELECT COUNT(v) FROM Version v WHERE v.item.id = :itemId", Long.class)
                .setParameter("itemId", itemId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void deleteItem(final Item item) {
        entityManager.remove(item);
    }

    @Override
    public Optional<Integer> findLatestVersionIdByItemId(final int itemId) {
        @SuppressWarnings("unchecked")
        final List<Number> ids = entityManager
                .createNativeQuery(SQL_LATEST_VERSION_ID_FOR_ITEM)
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .getResultList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.get(0).intValue());
    }

    @Override
    public void deleteVersion(final int versionId) {
        final Version version = entityManager.find(Version.class, versionId);
        if (version != null) {
            entityManager.remove(version);
        }
    }
}
