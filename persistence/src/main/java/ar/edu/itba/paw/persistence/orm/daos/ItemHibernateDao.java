package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.nuevo.ItemDao;
import ar.edu.itba.paw.persistence.orm.entities.AvailabilityOrm;
import ar.edu.itba.paw.persistence.orm.entities.BookingStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ImageOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemTypeOrm;
import ar.edu.itba.paw.persistence.orm.entities.LocationOrm;
import ar.edu.itba.paw.persistence.orm.entities.MediaId;
import ar.edu.itba.paw.persistence.orm.entities.MediaOrm;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import ar.edu.itba.paw.persistence.orm.entities.VersionOrm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Transactional
public class ItemHibernateDao implements ItemDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId) {
        final String hql = baseMyBoatsQuery() + " WHERE i.host.id = :ownerId ORDER BY i.createdAt DESC, i.id DESC";
        final List<Object[]> rows = entityManager
                .createQuery(hql, Object[].class)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnumOrm.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnumOrm.CANCELLED)
                .getResultList();
        return mapMyBoatsRows(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        final String hql = baseMyBoatsQuery() + " WHERE i.id = :itemId AND i.host.id = :ownerId";
        final List<Object[]> rows = entityManager
                .createQuery(hql, Object[].class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnumOrm.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnumOrm.CANCELLED)
                .setMaxResults(1)
                .getResultList();
        final List<MyBoatsItem> items = mapMyBoatsRows(rows);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public Optional<MyBoatsItem> createMyBoatsItem(final ItemCreateModel createModel) {
        if (createModel == null) {
            return Optional.empty();
        }
        final Integer itemId = insertItem(
                createModel.getOwnerId(),
                createModel.getTypeId(),
                createModel.getTitle(),
                createModel.getDescription(),
                createModel.getPricePerHour(),
                createModel.getCapacityPeople(),
                createModel.getMaxWeightKg().intValue(),
                createModel.getDifficultyLevel(),
                createModel.getLocationOptionId());
        if (itemId == null) {
            return Optional.empty();
        }
        return findMyBoatsItemByIdForOwner(itemId, createModel.getOwnerId());
    }

    @Override
    public boolean updateMyBoatsItem(final int itemId, final int ownerId, final ItemUpdateModel updateModel) {
        if (updateModel == null) {
            return false;
        }
        return createPublicationVersion(itemId, ownerId, updateModel) >= 0;
    }

    @Override
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        final Optional<ItemOrm> item = findItemOrm(itemId, ownerId);
        if (item.isEmpty()) {
            return false;
        }

        if (hasBookingsBlockingHardDelete(itemId)) {
            if (item.get().getStatus() == ItemStatusEnumOrm.ACTIVE) {
                item.get().setStatus(ItemStatusEnumOrm.INACTIVE);
                entityManager.flush();
                return true;
            }
            return false;
        }

        entityManager.remove(item.get());
        entityManager.flush();
        return true;
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        final int updated = entityManager
                .createQuery("UPDATE ItemOrm i SET i.status = :status WHERE i.id = :itemId AND i.host.id = :ownerId")
                .setParameter("status", active ? ItemStatusEnumOrm.ACTIVE : ItemStatusEnumOrm.INACTIVE)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
        entityManager.flush();
        return updated > 0;
    }

    @Override
    public int createPublicationVersion(final int itemId, final int ownerId, final ItemUpdateModel update) {
        final VersionOrm current = findCurrentVersion(itemId);
        if (current == null) {
            return -1;
        }

        final boolean hasBookings = hasBookingReferences(current.getId());
        if (!hasBookings) {
            current.setTitle(update.getTitle());
            current.setDescription(update.getDescription());
            current.setPrice(BigDecimal.valueOf(update.getPricePerHour()));
            current.setDifficulty(
                    update.getDifficultyLevel() != null ? update.getDifficultyLevel() : current.getDifficulty());
            current.setLocation(entityManager.getReference(LocationOrm.class, update.getLocationOptionId()));
            current.setCreatedAt(LocalDateTime.now());
            entityManager.flush();
            return current.getId();
        }

        final VersionOrm next = new VersionOrm();
        next.setItem(current.getItem());
        next.setType(current.getType());
        next.setTitle(update.getTitle());
        next.setDescription(update.getDescription());
        next.setPrice(BigDecimal.valueOf(update.getPricePerHour()));
        next.setCapacity(current.getCapacity());
        next.setWeight(current.getWeight());
        next.setDifficulty(update.getDifficultyLevel() != null ? update.getDifficultyLevel() : current.getDifficulty());
        next.setLocation(entityManager.getReference(LocationOrm.class, update.getLocationOptionId()));
        next.setTimezone(current.getTimezone());
        next.setCreatedAt(LocalDateTime.now());
        entityManager.persist(next);
        entityManager.flush();

        copyAvailabilities(current.getId(), next);
        copyMedia(current.getId(), next);

        entityManager.flush();
        return next.getId();
    }

    @Override
    public boolean replaceVersionPrimaryImage(final int versionId, final byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return false;
        }
        final ImageOrm image = new ImageOrm();
        image.setData(imageData);
        entityManager.persist(image);
        entityManager.flush();

        entityManager
                .createQuery("DELETE FROM MediaOrm m WHERE m.version.id = :versionId AND m.id.index = 0")
                .setParameter("versionId", versionId)
                .executeUpdate();

        final MediaOrm media = new MediaOrm();
        media.setId(new MediaId(versionId, 0));
        media.setVersion(entityManager.getReference(VersionOrm.class, versionId));
        media.setImage(image);
        entityManager.persist(media);

        entityManager.flush();
        return true;
    }

    public Integer insertItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final int difficulty,
            final int locationOptionId) {
        final LocalDateTime now = LocalDateTime.now();

        final ItemOrm item = new ItemOrm();
        item.setHost(entityManager.getReference(UsersOrm.class, ownerId));
        item.setStatus(ItemStatusEnumOrm.ACTIVE);
        item.setCreatedAt(now);
        entityManager.persist(item);

        final VersionOrm version = new VersionOrm();
        version.setItem(item);
        version.setType(entityManager.getReference(ItemTypeOrm.class, typeId));
        version.setTitle(title);
        version.setDescription(description);
        version.setPrice(BigDecimal.valueOf(pricePerHour));
        version.setCapacity(capacityPeople);
        version.setWeight(weight);
        version.setDifficulty(difficulty);
        version.setLocation(entityManager.getReference(LocationOrm.class, locationOptionId));
        version.setTimezone("America/Argentina/Buenos_Aires");
        version.setCreatedAt(now);
        entityManager.persist(version);
        entityManager.flush();

        return item.getId();
    }

    private VersionOrm findCurrentVersion(final int itemId) {
        final List<VersionOrm> rows = entityManager
                .createQuery("FROM VersionOrm v WHERE v.item.id = :itemId ORDER BY v.id DESC", VersionOrm.class)
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean hasBookingReferences(final int versionId) {
        final Long count = entityManager
                .createQuery("SELECT COUNT(b) FROM BookingOrm b WHERE b.version.id = :versionId", Long.class)
                .setParameter("versionId", versionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    private void copyAvailabilities(final int sourceVersionId, final VersionOrm targetVersion) {
        final List<AvailabilityOrm> availabilities = entityManager
                .createQuery("FROM AvailabilityOrm a WHERE a.version.id = :versionId", AvailabilityOrm.class)
                .setParameter("versionId", sourceVersionId)
                .getResultList();
        for (final AvailabilityOrm a : availabilities) {
            final AvailabilityOrm copy = new AvailabilityOrm();
            copy.setVersion(targetVersion);
            copy.setWeekday(a.getWeekday());
            copy.setStartTime(a.getStartTime());
            copy.setEndTime(a.getEndTime());
            entityManager.persist(copy);
        }
    }

    private void copyMedia(final int sourceVersionId, final VersionOrm targetVersion) {
        final List<MediaOrm> mediaList = entityManager
                .createQuery("FROM MediaOrm m WHERE m.version.id = :versionId", MediaOrm.class)
                .setParameter("versionId", sourceVersionId)
                .getResultList();
        for (final MediaOrm m : mediaList) {
            final MediaOrm copy = new MediaOrm();
            copy.setId(new MediaId(targetVersion.getId(), m.getId().getIndex()));
            copy.setVersion(targetVersion);
            copy.setImage(m.getImage());
            entityManager.persist(copy);
        }
    }

    private static String baseMyBoatsQuery() {
        return "SELECT i.id, v.id, v.title, v.description, v.price, v.difficulty, v.location.id,"
                + " v.capacity, v.location.name, i.status,"
                + " (SELECT m.image.id FROM MediaOrm m"
                + " WHERE m.version = v"
                + " AND m.id.index = (SELECT MIN(m2.id.index) FROM MediaOrm m2 WHERE m2.version = v)),"
                + " EXISTS (SELECT 1 FROM BookingOrm b"
                + " WHERE b.version.item = i"
                + " AND b.status NOT IN (:rejectedStatus, :cancelledStatus)"
                + " AND b.guest.id <> i.host.id),"
                + " EXISTS (SELECT 1 FROM BookingOrm b"
                + " WHERE b.version.item = i"
                + " AND b.status NOT IN (:rejectedStatus, :cancelledStatus)"
                + " AND b.guest.id <> i.host.id"
                + " AND b.end > CURRENT_TIMESTAMP)"
                + " FROM ItemOrm i"
                + " JOIN VersionOrm v ON v.item = i"
                + " AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i)";
    }

    private static List<MyBoatsItem> mapMyBoatsRows(final List<Object[]> rows) {
        final List<MyBoatsItem> items = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final MyBoatsItem item = new MyBoatsItem();
            item.setId((Integer) row[0]);
            item.setVersionId((Integer) row[1]);
            item.setTitle((String) row[2]);
            item.setDescription((String) row[3]);
            final BigDecimal price = (BigDecimal) row[4];
            item.setPricePerHour(price == null ? null : price.intValue());
            item.setDifficultyLevel((Integer) row[5]);
            item.setLocationOptionId((Integer) row[6]);
            item.setCapacityPeople((Integer) row[7]);
            item.setLocation((String) row[8]);
            item.setActive(ItemStatusEnumOrm.ACTIVE.equals(row[9]));
            item.setCoverImageId((Integer) row[10]);
            final boolean hasBlocking = Boolean.TRUE.equals(row[11]);
            final boolean hasFutureBlocking = Boolean.TRUE.equals(row[12]);
            item.setDeleteDeactivates(Boolean.TRUE.equals(item.getActive()) && hasBlocking);
            item.setDeleteDisabled(!Boolean.TRUE.equals(item.getActive()) && hasFutureBlocking);
            items.add(item);
        }
        return items;
    }

    private Optional<ItemOrm> findItemOrm(final int itemId, final Integer ownerId) {
        final String hql = ownerId == null
                ? "FROM ItemOrm i WHERE i.id = :itemId"
                : "FROM ItemOrm i WHERE i.id = :itemId AND i.host.id = :ownerId";
        final var query = entityManager.createQuery(hql, ItemOrm.class).setParameter("itemId", itemId);
        if (ownerId != null) {
            query.setParameter("ownerId", ownerId);
        }
        final List<ItemOrm> rows = query.setMaxResults(1).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private boolean hasBookingsBlockingHardDelete(final int itemId) {
        final Number count = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM booking b "
                        + "JOIN version v ON v.id = b.version_id "
                        + "JOIN item i ON i.id = v.item_id "
                        + "WHERE i.id = :itemId "
                        + "AND b.status NOT IN ('REJECTED', 'CANCELLED') "
                        + "AND b.\"end\" > CURRENT_TIMESTAMP "
                        + "AND b.guest_id <> i.host_id")
                .setParameter("itemId", itemId)
                .getSingleResult();
        return count != null && count.intValue() > 0;
    }
}
