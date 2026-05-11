package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ImageUpload;
import ar.edu.itba.paw.models.nuevo.PublishItem;
import ar.edu.itba.paw.persistence.nuevo.PublishDao;
import ar.edu.itba.paw.persistence.orm.entities.AvailabilityOrm;
import ar.edu.itba.paw.persistence.orm.entities.ImageOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemTypeOrm;
import ar.edu.itba.paw.persistence.orm.entities.LocationOrm;
import ar.edu.itba.paw.persistence.orm.entities.MediaId;
import ar.edu.itba.paw.persistence.orm.entities.MediaOrm;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import ar.edu.itba.paw.persistence.orm.entities.VersionOrm;
import ar.edu.itba.paw.persistence.orm.entities.WeekdayEnumOrm;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PublishHibernateDao implements PublishDao {

    private static final String HQL_FIND_BY_ID =
            "SELECT i.id, i.host.id, v.type.id, v.title, v.description, v.price, v.capacity, v.weight, v.difficulty, v.location.name,"
                    + " (SELECT m.image.id FROM MediaOrm m WHERE m.version = v AND m.id.index = 0)"
                    + " FROM ItemOrm i"
                    + " JOIN VersionOrm v ON v.item = i"
                    + " AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i)"
                    + " WHERE i.id = :itemId";

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
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final LocalDateTime now = LocalDateTime.now();

        final ItemOrm item = ItemOrm.builder()
                .host(entityManager.getReference(UsersOrm.class, ownerId))
                .status(ItemStatusEnumOrm.ACTIVE)
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
                .timezone("America/Argentina/Buenos_Aires")
                .createdAt(now)
                .build();
        entityManager.persist(version);

        if (availabilities != null) {
            for (final AvailabilityWindow window : availabilities) {
                if (window == null
                        || window.getWeekday() == null
                        || window.getStartTime() == null
                        || window.getEndTime() == null) {
                    continue;
                }
                final AvailabilityOrm a = new AvailabilityOrm();
                a.setVersion(version);
                a.setWeekday(WeekdayEnumOrm.valueOf(window.getWeekday().name()));
                a.setStartTime(window.getStartTime());
                a.setEndTime(window.getEndTime());
                entityManager.persist(a);
            }
        }

        if (images != null) {
            for (int idx = 0; idx < images.size(); idx++) {
                final ImageUpload upload = images.get(idx);
                if (upload == null || upload.getData() == null || upload.getData().length == 0) {
                    continue;
                }
                final ImageOrm image = new ImageOrm();
                image.setData(upload.getData());
                entityManager.persist(image);

                final MediaOrm media = new MediaOrm();
                media.setId(new MediaId(version.getId(), idx));
                media.setVersion(version);
                media.setImage(image);
                entityManager.persist(media);
            }
        }

        entityManager.flush();
        return item.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublishItem> findById(final int itemId) {
        final TypedQuery<Object[]> query = entityManager.createQuery(HQL_FIND_BY_ID, Object[].class);
        query.setParameter("itemId", itemId);
        query.setMaxResults(1);
        final List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toPublishItem(rows.get(0)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityWindow> listAvailabilities(final int itemId) {
        final Integer versionId = findLatestVersionId(itemId);
        if (versionId == null) {
            return List.of();
        }
        final TypedQuery<AvailabilityOrm> q = entityManager.createQuery(HQL_AVAILABILITIES, AvailabilityOrm.class);
        q.setParameter("versionId", versionId);
        final List<AvailabilityOrm> rows = q.getResultList();
        final List<AvailabilityWindow> out = new ArrayList<>(rows.size());
        for (final AvailabilityOrm orm : rows) {
            out.add(toAvailabilityWindow(orm));
        }
        return out;
    }

    private Integer findLatestVersionId(final int itemId) {
        final TypedQuery<Integer> q = entityManager.createQuery(HQL_LATEST_VERSION_ID, Integer.class);
        q.setParameter("itemId", itemId);
        final List<Integer> result = q.setMaxResults(1).getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    private static PublishItem toPublishItem(final Object[] row) {
        final Integer id = (Integer) row[0];
        final Integer ownerId = (Integer) row[1];
        final Integer typeId = (Integer) row[2];
        final String title = (String) row[3];
        final String description = (String) row[4];
        final BigDecimal price = (BigDecimal) row[5];
        final Integer capacityPeople = (Integer) row[6];
        final Integer weight = (Integer) row[7];
        final Integer difficulty = (Integer) row[8];
        final String locationName = (String) row[9];
        final Integer coverImageId = (Integer) row[10];

        return new PublishItem(
                Objects.requireNonNull(id),
                Objects.requireNonNull(ownerId),
                Objects.requireNonNull(typeId),
                title,
                description == null ? "" : description,
                price == null ? 0 : price.intValue(),
                Objects.requireNonNull(capacityPeople),
                BigDecimal.valueOf(weight == null ? 0 : weight),
                difficulty,
                locationName == null ? "" : locationName,
                coverImageId);
    }

    private static AvailabilityWindow toAvailabilityWindow(final AvailabilityOrm orm) {
        final AvailabilityWindow w = new AvailabilityWindow();
        w.setWeekday(
                orm.getWeekday() == null
                        ? null
                        : DayOfWeek.valueOf(orm.getWeekday().name()));
        w.setStartTime(orm.getStartTime());
        w.setEndTime(orm.getEndTime());
        return w;
    }
}
