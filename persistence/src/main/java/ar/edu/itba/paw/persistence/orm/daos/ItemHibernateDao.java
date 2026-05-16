package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.orm.projections.MyBoatsRowOrm;
import ar.edu.itba.paw.persistence.nuevo.ItemDao;
import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingStatusEnumOrm;
import ar.edu.itba.paw.models.entity.ImageOrm;
import ar.edu.itba.paw.models.entity.ItemOrm;
import ar.edu.itba.paw.models.entity.ItemStatusEnumOrm;
import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.MediaOrm;
import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class ItemHibernateDao implements ItemDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId, final int page, final int pageSize) {
        final String hql = baseMyBoatsQuery() + " WHERE i.host.id = :ownerId ORDER BY i.createdAt DESC, i.id DESC";
        final List<MyBoatsRowOrm> rows = entityManager
                .createQuery(hql, MyBoatsRowOrm.class)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnumOrm.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnumOrm.CANCELLED)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return toDomainItems(rows);
    }

    @Override
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        final Number count = (Number) entityManager
                .createQuery("SELECT COUNT(i) FROM ItemOrm i WHERE i.host.id = :ownerId")
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        final String hql = baseMyBoatsQuery() + " WHERE i.id = :itemId AND i.host.id = :ownerId";
        final List<MyBoatsRowOrm> rows = entityManager
                .createQuery(hql, MyBoatsRowOrm.class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnumOrm.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnumOrm.CANCELLED)
                .setMaxResults(1)
                .getResultList();
        final List<MyBoatsItem> items = toDomainItems(rows);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public boolean updateMyBoatsItem(final int itemId, final int ownerId,
            final String title, final String description,
            final int pricePerHour, final Integer difficultyLevel, final int locationOptionId) {
        return createPublicationVersion(itemId, ownerId, title, description,
                pricePerHour, difficultyLevel, locationOptionId) >= 0;
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
    public int createPublicationVersion(final int itemId, final int ownerId,
            final String title, final String description,
            final int pricePerHour, final Integer difficultyLevel, final int locationOptionId) {
        final VersionOrm current = findCurrentVersion(itemId);
        if (current == null) {
            return -1;
        }

        final boolean hasBookings = hasBookingReferences(current.getId());
        if (!hasBookings) {
            current.setTitle(title);
            current.setDescription(description);
            current.setPrice(BigDecimal.valueOf(pricePerHour));
            current.setDifficulty(
                    difficultyLevel != null ? difficultyLevel : current.getDifficulty());
            current.setLocation(entityManager.getReference(LocationOrm.class, locationOptionId));
            current.setCreatedAt(LocalDateTime.now());
            entityManager.flush();
            return current.getId();
        }

        final VersionOrm next = new VersionOrm();
        next.setItem(current.getItem());
        next.setType(current.getType());
        next.setTitle(title);
        next.setDescription(description);
        next.setPrice(BigDecimal.valueOf(pricePerHour));
        next.setCapacity(current.getCapacity());
        next.setWeight(current.getWeight());
        next.setDifficulty(difficultyLevel != null ? difficultyLevel : current.getDifficulty());
        next.setLocation(entityManager.getReference(LocationOrm.class, locationOptionId));
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

        final ItemOrm item = ItemOrm.builder()
                .host(entityManager.getReference(UsersOrm.class, ownerId))
                .status(ItemStatusEnumOrm.ACTIVE)
                .createdAt(now)
                .build();
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
        return "SELECT NEW ar.edu.itba.paw.persistence.orm.projections.MyBoatsRowOrm("
                + "i.id, v.id, v.title, v.description, v.price, v.difficulty, v.location.id,"
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
                + " AND b.end > CURRENT_TIMESTAMP))"
                + " FROM ItemOrm i"
                + " JOIN VersionOrm v ON v.item = i"
                + " AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i)";
    }

    static List<MyBoatsItem> toDomainItems(final List<MyBoatsRowOrm> projections) {
        final List<MyBoatsItem> items = new ArrayList<>(projections.size());
        for (final MyBoatsRowOrm p : projections) {
            final MyBoatsItem item = new MyBoatsItem();
            item.setId(p.getItemId());
            item.setVersionId(p.getVersionId());
            item.setTitle(p.getTitle());
            item.setDescription(p.getDescription());
            final BigDecimal price = p.getPrice();
            item.setPrice(price == null ? null : price.intValue());
            item.setDifficulty(p.getDifficulty());
            item.setLocationId(p.getLocationId());
            item.setCapacity(p.getCapacity());
            item.setLocation(p.getLocationName());
            item.setActive(ItemStatusEnumOrm.ACTIVE.equals(p.getStatus()));
            item.setCoverImageId(p.getCoverImageId());
            final boolean active = Boolean.TRUE.equals(item.getActive());
            item.setDeleteDeactivates(active && Boolean.TRUE.equals(p.getHasBlockingBookings()));
            item.setDeleteDisabled(!active && Boolean.TRUE.equals(p.getHasFutureBlockingBookings()));
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

    @Override
    public Optional<byte[]> findImageDataById(final int imageId) {
        return Optional.ofNullable(entityManager.find(ImageOrm.class, imageId))
                .map(ImageOrm::getData);
    }

    @Override
    public List<Integer> listImageIds(final int itemId) {
        final Integer versionId = findCurrentVersionId(itemId);
        if (versionId == null) {
            return List.of();
        }
        return entityManager
                .createQuery("SELECT m.image.id FROM MediaOrm m WHERE m.version.id = :versionId ORDER BY m.id.index", Integer.class)
                .setParameter("versionId", versionId)
                .getResultList();
    }

    @Override
    public Optional<Integer> uploadGalleryImage(final int itemId, final byte[] imageData) {
        final Integer versionId = findCurrentVersionId(itemId);
        if (versionId == null) {
            return Optional.empty();
        }
        final long count = (Long) entityManager
                .createQuery("SELECT COUNT(m) FROM MediaOrm m WHERE m.version.id = :versionId")
                .setParameter("versionId", versionId)
                .getSingleResult();
        if (count >= MAX_GALLERY_IMAGES) {
            return Optional.empty();
        }
        final ImageOrm image = new ImageOrm();
        image.setData(imageData);
        entityManager.persist(image);
        entityManager.flush();

        final MediaOrm media = new MediaOrm();
        media.setId(new MediaId(versionId, (int) count));
        media.setVersion(entityManager.getReference(VersionOrm.class, versionId));
        media.setImage(image);
        entityManager.persist(media);

        return Optional.of(image.getId());
    }

    @Override
    public boolean deleteImageFromGallery(final int imageId) {
        final int deleted = entityManager
                .createQuery("DELETE FROM MediaOrm m WHERE m.image.id = :imageId")
                .setParameter("imageId", imageId)
                .executeUpdate();
        if (deleted > 0) {
            entityManager
                    .createQuery("DELETE FROM ImageOrm i WHERE i.id = :imageId")
                    .setParameter("imageId", imageId)
                    .executeUpdate();
            return true;
        }
        return false;
    }

    @Override
    public boolean reorderGallery(final int itemId, final List<Integer> imageIdsInOrder) {
        final Integer versionId = findCurrentVersionId(itemId);
        if (versionId == null) {
            return false;
        }
        for (int i = 0; i < imageIdsInOrder.size(); i++) {
            entityManager
                    .createQuery("UPDATE MediaOrm m SET m.id.index = :index WHERE m.version.id = :versionId AND m.image.id = :imageId")
                    .setParameter("index", i)
                    .setParameter("versionId", versionId)
                    .setParameter("imageId", imageIdsInOrder.get(i))
                    .executeUpdate();
        }
        return true;
    }

    private Integer findCurrentVersionId(final int itemId) {
        final List<Integer> result = entityManager
                .createQuery("SELECT MAX(v.id) FROM VersionOrm v WHERE v.item.id = :itemId", Integer.class)
                .setParameter("itemId", itemId)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    private static final int MAX_GALLERY_IMAGES = 5;
}
