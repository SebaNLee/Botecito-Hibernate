package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;
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
    public void flush() {
        entityManager.flush();
    }
}
