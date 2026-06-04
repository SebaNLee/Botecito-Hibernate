package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Version;
import java.util.ArrayList;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

// TODO: Check there is no item in the production dump without a version, and then remove all checks that do: `if
// (version == null)`.

@Repository
public class EditJpaDao implements EditDao {

    @PersistenceContext
    private EntityManager entityManager;

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
}
