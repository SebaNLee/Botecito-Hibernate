package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public Version edit(
            final int itemId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final Version current = requireLatestVersionForItem(itemId);
        final LocalDateTime now = LocalDateTime.now();

        final Version version = Version.builder()
                .item(current.getItem())
                .type(entityManager.getReference(ItemType.class, typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(weight)
                .difficulty(difficulty)
                .location(entityManager.getReference(Location.class, locationOptionId))
                .timezone(current.getTimezone())
                .createdAt(now)
                .build();
        entityManager.persist(version);
        entityManager.flush();

        persistAvailabilities(version, availabilities);
        persistMedia(version, images, allowedExistingImageIds(current));
        return version;
    }

    @Override
    public Version overwrite(
            final int versionId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final Version version = entityManager.find(Version.class, versionId);
        if (version == null) {
            throw new IllegalStateException("Version not found: " + versionId);
        }

        version.setType(entityManager.getReference(ItemType.class, typeId));
        version.setTitle(title);
        version.setDescription(description);
        version.setPrice(BigDecimal.valueOf(pricePerHour));
        version.setCapacity(capacityPeople);
        version.setWeight(weight);
        version.setDifficulty(difficulty);
        version.setLocation(entityManager.getReference(Location.class, locationOptionId));

        removeVersionChildren(version);
        entityManager.flush();

        persistAvailabilities(version, availabilities);
        persistMedia(version, images, Set.of());
        return version;
    }

    private Version requireLatestVersionForItem(final int itemId) {
        final Integer versionId = resolveLatestVersionIdForItem(itemId);
        if (versionId == null) {
            throw new IllegalStateException("No version found for item: " + itemId);
        }
        final Version version = entityManager.find(Version.class, versionId);
        if (version == null) {
            throw new IllegalStateException("Version not found: " + versionId);
        }
        return version;
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

    private static Set<Integer> allowedExistingImageIds(final Version version) {
        final Set<Integer> allowed = new HashSet<>();
        if (version.getMedia() == null) {
            return allowed;
        }
        for (final Media media : version.getMedia()) {
            if (media.getImage() != null && media.getImage().getId() != null) {
                allowed.add(media.getImage().getId());
            }
        }
        return allowed;
    }

    private void persistAvailabilities(final Version version, final List<AvailabilityWindow> availabilities) {
        for (final AvailabilityWindow window : availabilities) {
            final Availability availability = new Availability();
            availability.setVersion(version);
            availability.setWeekday(WeekdayEnum.valueOf(window.getWeekday().name()));
            availability.setStartTime(window.getStartTime());
            availability.setEndTime(window.getEndTime());
            entityManager.persist(availability);
        }
    }

    private void persistMedia(
            final Version version, final List<ImageUpload> images, final Set<Integer> allowedExistingImageIds) {
        for (int idx = 0; idx < images.size(); idx++) {
            final ImageUpload upload = images.get(idx);
            final Image image;
            if (upload.isExisting()) {
                if (!allowedExistingImageIds.isEmpty()
                        && !allowedExistingImageIds.contains(upload.getExistingImageId())) {
                    continue;
                }
                image = entityManager.getReference(Image.class, upload.getExistingImageId());
            } else {
                image = new Image();
                image.setData(upload.getData());
                entityManager.persist(image);
            }

            final Media media = new Media();
            media.setId(new MediaId(version.getId(), idx));
            media.setVersion(version);
            media.setImage(image);
            entityManager.persist(media);
        }
    }

    /**
     * Removes availability and media through the persistence context so Hibernate
     * stays in sync.
     * Bulk JPQL deletes leave managed children attached to the version when it was
     * loaded earlier
     * in the same session (e.g. via DetailService), which breaks subsequent inserts
     * on flush.
     */
    private void removeVersionChildren(final Version version) {
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
