package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class PublishJpaDao implements PublishDao {

    private static final String HQL_FIND_LATEST_VERSION = "SELECT v FROM Version v"
            + " JOIN FETCH v.item i"
            + " JOIN FETCH v.type"
            + " JOIN FETCH v.location"
            + " LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image"
            + " WHERE i.id = :itemId"
            + " AND v.id = (SELECT MAX(v2.id) FROM Version v2 WHERE v2.item = i)";

    private static final String HQL_AVAILABILITIES =
            "SELECT a FROM Availability a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private static final String HQL_LATEST_VERSION_ID = "SELECT MAX(v.id) FROM Version v WHERE v.item.id = :itemId";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int create(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal weight,
            final Integer difficulty,
            final int locationOptionId,
            final String timezone,
            final String status,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final LocalDateTime now = LocalDateTime.now();

        final Item item = Item.builder()
                .host(entityManager.getReference(Users.class, ownerId))
                .status(ItemStatusEnum.valueOf(status))
                .createdAt(now)
                .build();
        entityManager.persist(item);

        final Version version = Version.builder()
                .item(item)
                .type(entityManager.getReference(ItemType.class, typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(weight.intValue())
                .difficulty(difficulty)
                .location(entityManager.getReference(Location.class, locationOptionId))
                .timezone(timezone)
                .createdAt(now)
                .build();
        entityManager.persist(version);

        for (final AvailabilityWindow window : availabilities) {
            final Availability a = new Availability();
            a.setVersion(version);
            a.setWeekday(WeekdayEnum.valueOf(window.getWeekday().name()));
            a.setStartTime(window.getStartTime());
            a.setEndTime(window.getEndTime());
            entityManager.persist(a);
        }

        for (int idx = 0; idx < images.size(); idx++) {
            final ImageUpload upload = images.get(idx);
            final Image image = new Image();
            image.setData(upload.getData());
            entityManager.persist(image);

            final Media media = new Media();
            media.setId(new MediaId(version.getId(), idx));
            media.setVersion(version);
            media.setImage(image);
            entityManager.persist(media);
        }

        return item.getId();
    }

    @Override
    public Optional<Version> findById(final int itemId) {
        final TypedQuery<Version> query = entityManager.createQuery(HQL_FIND_LATEST_VERSION, Version.class);
        query.setParameter("itemId", itemId);
        query.setMaxResults(1);
        final List<Version> result = query.getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Availability> listAvailabilities(final int itemId) {
        final Integer versionId = findLatestVersionId(itemId);
        if (versionId == null) {
            return List.of();
        }
        final TypedQuery<Availability> q = entityManager.createQuery(HQL_AVAILABILITIES, Availability.class);
        q.setParameter("versionId", versionId);
        return q.getResultList();
    }

    private Integer findLatestVersionId(final int itemId) {
        final TypedQuery<Integer> q = entityManager.createQuery(HQL_LATEST_VERSION_ID, Integer.class);
        q.setParameter("itemId", itemId);
        final List<Integer> result = q.setMaxResults(1).getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
