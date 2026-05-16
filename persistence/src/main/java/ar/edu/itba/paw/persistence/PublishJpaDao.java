package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.ImageOrm;
import ar.edu.itba.paw.models.entity.ItemOrm;
import ar.edu.itba.paw.models.entity.ItemStatusEnumOrm;
import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.MediaOrm;
import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import ar.edu.itba.paw.models.entity.WeekdayEnumOrm;
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

    private static final String HQL_FIND_LATEST_VERSION = "SELECT v FROM VersionOrm v"
            + " JOIN FETCH v.item i"
            + " JOIN FETCH v.type"
            + " JOIN FETCH v.location"
            + " LEFT JOIN FETCH v.media m LEFT JOIN FETCH m.image"
            + " WHERE i.id = :itemId"
            + " AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i)";

    private static final String HQL_AVAILABILITIES =
            "SELECT a FROM AvailabilityOrm a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private static final String HQL_LATEST_VERSION_ID = "SELECT MAX(v.id) FROM VersionOrm v WHERE v.item.id = :itemId";

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
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final String timezone,
            final String status,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final LocalDateTime now = LocalDateTime.now();

        final ItemOrm item = ItemOrm.builder()
                .host(entityManager.getReference(UsersOrm.class, ownerId))
                .status(ItemStatusEnumOrm.valueOf(status))
                .createdAt(now)
                .build();
        entityManager.persist(item);

        final VersionOrm version = VersionOrm.builder()
                .item(item)
                .type(entityManager.getReference(ItemTypeOrm.class, typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(maxWeightKg.intValue())
                .difficulty(difficultyLevel)
                .location(entityManager.getReference(LocationOrm.class, locationOptionId))
                .timezone(timezone)
                .createdAt(now)
                .build();
        entityManager.persist(version);

        for (final AvailabilityWindow window : availabilities) {
            final AvailabilityOrm a = new AvailabilityOrm();
            a.setVersion(version);
            a.setWeekday(WeekdayEnumOrm.valueOf(window.getWeekday().name()));
            a.setStartTime(window.getStartTime());
            a.setEndTime(window.getEndTime());
            entityManager.persist(a);
        }

        for (int idx = 0; idx < images.size(); idx++) {
            final ImageUpload upload = images.get(idx);
            final ImageOrm image = new ImageOrm();
            image.setData(upload.getData());
            entityManager.persist(image);

            final MediaOrm media = new MediaOrm();
            media.setId(new MediaId(version.getId(), idx));
            media.setVersion(version);
            media.setImage(image);
            entityManager.persist(media);
        }

        return item.getId();
    }

    @Override
    public Optional<VersionOrm> findById(final int itemId) {
        final TypedQuery<VersionOrm> query = entityManager.createQuery(HQL_FIND_LATEST_VERSION, VersionOrm.class);
        query.setParameter("itemId", itemId);
        query.setMaxResults(1);
        final List<VersionOrm> result = query.getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<AvailabilityOrm> listAvailabilities(final int itemId) {
        final Integer versionId = findLatestVersionId(itemId);
        if (versionId == null) {
            return List.of();
        }
        final TypedQuery<AvailabilityOrm> q = entityManager.createQuery(HQL_AVAILABILITIES, AvailabilityOrm.class);
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
