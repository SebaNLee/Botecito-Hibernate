package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

// TODO: Check there is no item in the production dump without a version, and then remove all checks that do: `if
// (version == null)`.

@Repository
public class EditJpaDao implements EditDao {

    private static final String SQL_LATEST_VERSION_ID_FOR_ITEM = "SELECT v.id FROM version v "
            + "WHERE v.item_id = :itemId "
            + "AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean itemHasBookings(final int itemId) {
        final Integer versionId = resolveLatestVersionIdForItem(itemId);
        if (versionId == null) {
            return false;
        }
        final Long count = entityManager
                .createQuery("SELECT COUNT(b) FROM Booking b WHERE b.version.id = :versionId", Long.class)
                .setParameter("versionId", versionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public Optional<Version> findVersionById(final int versionId) {
        return Optional.ofNullable(entityManager.find(Version.class, versionId));
    }

    @Override
    public void removeVersionChildren(final Version version) {
        Hibernate.initialize(version.getAvailabilities());
        Hibernate.initialize(version.getMedia());

        if (version.getAvailabilities() != null) {
            new ArrayList<>(version.getAvailabilities()).forEach(entityManager::remove);
            version.getAvailabilities().clear();
        }
        if (version.getMedia() != null) {
            new ArrayList<>(version.getMedia()).forEach(entityManager::remove);
            version.getMedia().clear();
        }
    }

    private Integer resolveLatestVersionIdForItem(final int itemId) {
        @SuppressWarnings("unchecked")
        final List<Number> ids = entityManager
                .createNativeQuery(SQL_LATEST_VERSION_ID_FOR_ITEM)
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .getResultList();
        if (ids.isEmpty()) {
            return null;
        }
        return ids.get(0).intValue();
    }
}
