package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PublishJpaDao implements PublishDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Item persistItem(final Item item) {
        entityManager.persist(item);
        return item;
    }

    @Override
    public Version persistVersion(final Version version) {
        entityManager.persist(version);
        return version;
    }

    @Override
    public Availability persistAvailability(final Availability availability) {
        entityManager.persist(availability);
        return availability;
    }

    @Override
    public Image persistImage(final Image image) {
        entityManager.persist(image);
        return image;
    }

    @Override
    public Media persistMedia(final Media media) {
        entityManager.persist(media);
        return media;
    }

    @Override
    public Optional<Version> findVersionById(final int versionId) {
        return Optional.ofNullable(entityManager.find(Version.class, versionId));
    }

    @Override
    public void removeVersionChildren(final Version version) {
        entityManager
                .createQuery("DELETE FROM Availability a WHERE a.version.id = :vid")
                .setParameter("vid", version.getId())
                .executeUpdate();
        if (version.getAvailabilities() != null) {
            version.getAvailabilities().clear();
        }

        if (version.getMedia() != null && !version.getMedia().isEmpty()) {
            for (final Media media : List.copyOf(version.getMedia())) {
                entityManager.remove(media);
            }
            version.getMedia().clear();
        } else {
            entityManager
                    .createQuery("DELETE FROM Media m WHERE m.version.id = :vid")
                    .setParameter("vid", version.getId())
                    .executeUpdate();
        }
        entityManager.flush();
    }
}
