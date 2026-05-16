package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.projections.MyBoatsRow;
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
public class ItemJpaDao implements ItemDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId, final int page, final int pageSize) {
        final String hql = baseMyBoatsQuery() + " WHERE i.host.id = :ownerId ORDER BY i.createdAt DESC, i.id DESC";
        final List<MyBoatsRow> rows = entityManager
                .createQuery(hql, MyBoatsRow.class)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnum.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnum.CANCELLED)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return toDomainItems(rows);
    }

    @Override
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        final Number count = (Number) entityManager
                .createQuery("SELECT COUNT(i) FROM Item i WHERE i.host.id = :ownerId")
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        final String hql = baseMyBoatsQuery() + " WHERE i.id = :itemId AND i.host.id = :ownerId";
        final List<MyBoatsRow> rows = entityManager
                .createQuery(hql, MyBoatsRow.class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnum.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnum.CANCELLED)
                .setMaxResults(1)
                .getResultList();
        final List<MyBoatsItem> items = toDomainItems(rows);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        final Optional<Item> item = findItemOrm(itemId, ownerId);
        if (item.isEmpty()) {
            return false;
        }

        if (hasBookingsBlockingHardDelete(itemId)) {
            if (item.get().getStatus() == ItemStatusEnum.ACTIVE) {
                item.get().setStatus(ItemStatusEnum.INACTIVE);
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
                .createQuery("UPDATE Item i SET i.status = :status WHERE i.id = :itemId AND i.host.id = :ownerId")
                .setParameter("status", active ? ItemStatusEnum.ACTIVE : ItemStatusEnum.INACTIVE)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
        entityManager.flush();
        return updated > 0;
    }

    @Override
    public int createPublicationVersion(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final Version current = findCurrentVersion(itemId);
        if (current == null) {
            return -1;
        }

        final boolean hasBookings = hasBookingReferences(current.getId());
        if (!hasBookings) {
            current.setTitle(title);
            current.setDescription(description);
            current.setPrice(BigDecimal.valueOf(pricePerHour));
            current.setDifficulty(difficultyLevel != null ? difficultyLevel : current.getDifficulty());
            current.setLocation(entityManager.getReference(Location.class, locationOptionId));
            current.setCreatedAt(LocalDateTime.now());
            entityManager.flush();
            return current.getId();
        }

        final Version next = new Version();
        next.setItem(current.getItem());
        next.setType(current.getType());
        next.setTitle(title);
        next.setDescription(description);
        next.setPrice(BigDecimal.valueOf(pricePerHour));
        next.setCapacity(current.getCapacity());
        next.setWeight(current.getWeight());
        next.setDifficulty(difficultyLevel != null ? difficultyLevel : current.getDifficulty());
        next.setLocation(entityManager.getReference(Location.class, locationOptionId));
        next.setTimezone(current.getTimezone());
        next.setCreatedAt(LocalDateTime.now());
        entityManager.persist(next);
        entityManager.flush();

        copyAvailabilities(current.getId(), next);
        copyMedia(current.getId(), next);

        entityManager.flush();
        return next.getId();
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

        final Item item = Item.builder()
                .host(entityManager.getReference(Users.class, ownerId))
                .status(ItemStatusEnum.ACTIVE)
                .createdAt(now)
                .build();
        entityManager.persist(item);

        final Version version = new Version();
        version.setItem(item);
        version.setType(entityManager.getReference(ItemType.class, typeId));
        version.setTitle(title);
        version.setDescription(description);
        version.setPrice(BigDecimal.valueOf(pricePerHour));
        version.setCapacity(capacityPeople);
        version.setWeight(weight);
        version.setDifficulty(difficulty);
        version.setLocation(entityManager.getReference(Location.class, locationOptionId));
        version.setTimezone("America/Argentina/Buenos_Aires");
        version.setCreatedAt(now);
        entityManager.persist(version);
        entityManager.flush();

        return item.getId();
    }

    private Version findCurrentVersion(final int itemId) {
        final List<Version> rows = entityManager
                .createQuery("FROM Version v WHERE v.item.id = :itemId ORDER BY v.id DESC", Version.class)
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean hasBookingReferences(final int versionId) {
        final Long count = entityManager
                .createQuery("SELECT COUNT(b) FROM Booking b WHERE b.version.id = :versionId", Long.class)
                .setParameter("versionId", versionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    private void copyAvailabilities(final int sourceVersionId, final Version targetVersion) {
        final List<Availability> availabilities = entityManager
                .createQuery("FROM Availability a WHERE a.version.id = :versionId", Availability.class)
                .setParameter("versionId", sourceVersionId)
                .getResultList();
        for (final Availability a : availabilities) {
            final Availability copy = new Availability();
            copy.setVersion(targetVersion);
            copy.setWeekday(a.getWeekday());
            copy.setStartTime(a.getStartTime());
            copy.setEndTime(a.getEndTime());
            entityManager.persist(copy);
        }
    }

    private void copyMedia(final int sourceVersionId, final Version targetVersion) {
        final List<Media> mediaList = entityManager
                .createQuery("FROM Media m WHERE m.version.id = :versionId", Media.class)
                .setParameter("versionId", sourceVersionId)
                .getResultList();
        for (final Media m : mediaList) {
            final Media copy = new Media();
            copy.setId(new MediaId(targetVersion.getId(), m.getId().getIndex()));
            copy.setVersion(targetVersion);
            copy.setImage(m.getImage());
            entityManager.persist(copy);
        }
    }

    private static String baseMyBoatsQuery() {
        return "SELECT NEW ar.edu.itba.paw.persistence.projections.MyBoatsRow("
                + "i.id, v.id, v.title, v.description, v.price, v.difficulty, v.location.id,"
                + " v.capacity, v.location.name, i.status,"
                + " (SELECT m.image.id FROM Media m"
                + " WHERE m.version = v"
                + " AND m.id.index = (SELECT MIN(m2.id.index) FROM Media m2 WHERE m2.version = v)),"
                + " EXISTS (SELECT 1 FROM Booking b"
                + " WHERE b.version.item = i"
                + " AND b.status NOT IN (:rejectedStatus, :cancelledStatus)"
                + " AND b.guest.id <> i.host.id),"
                + " EXISTS (SELECT 1 FROM Booking b"
                + " WHERE b.version.item = i"
                + " AND b.status NOT IN (:rejectedStatus, :cancelledStatus)"
                + " AND b.guest.id <> i.host.id"
                + " AND b.end > CURRENT_TIMESTAMP))"
                + " FROM Item i"
                + " JOIN Version v ON v.item = i"
                + " AND v.id = (SELECT MAX(v2.id) FROM Version v2 WHERE v2.item = i)";
    }

    static List<MyBoatsItem> toDomainItems(final List<MyBoatsRow> projections) {
        final List<MyBoatsItem> items = new ArrayList<>(projections.size());
        for (final MyBoatsRow p : projections) {
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
            item.setActive(ItemStatusEnum.ACTIVE.equals(p.getStatus()));
            item.setCoverImageId(p.getCoverImageId());
            final boolean active = Boolean.TRUE.equals(item.getActive());
            item.setDeleteDeactivates(active && Boolean.TRUE.equals(p.getHasBlockingBookings()));
            item.setDeleteDisabled(!active && Boolean.TRUE.equals(p.getHasFutureBlockingBookings()));
            items.add(item);
        }
        return items;
    }

    private Optional<Item> findItemOrm(final int itemId, final Integer ownerId) {
        final String hql = ownerId == null
                ? "FROM Item i WHERE i.id = :itemId"
                : "FROM Item i WHERE i.id = :itemId AND i.host.id = :ownerId";
        final var query = entityManager.createQuery(hql, Item.class).setParameter("itemId", itemId);
        if (ownerId != null) {
            query.setParameter("ownerId", ownerId);
        }
        final List<Item> rows = query.setMaxResults(1).getResultList();
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
        return Optional.ofNullable(entityManager.find(Image.class, imageId)).map(Image::getData);
    }

    @Override
    public List<Integer> listImageIds(final int itemId) {
        final Integer versionId = findCurrentVersionId(itemId);
        if (versionId == null) {
            return List.of();
        }
        return entityManager
                .createQuery(
                        "SELECT m.image.id FROM Media m WHERE m.version.id = :versionId ORDER BY m.id.index",
                        Integer.class)
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
                .createQuery("SELECT COUNT(m) FROM Media m WHERE m.version.id = :versionId")
                .setParameter("versionId", versionId)
                .getSingleResult();
        if (count >= MAX_GALLERY_IMAGES) {
            return Optional.empty();
        }
        final Image image = new Image();
        image.setData(imageData);
        entityManager.persist(image);
        entityManager.flush();

        final Media media = new Media();
        media.setId(new MediaId(versionId, (int) count));
        media.setVersion(entityManager.getReference(Version.class, versionId));
        media.setImage(image);
        entityManager.persist(media);

        return Optional.of(image.getId());
    }

    @Override
    public boolean deleteImageFromGallery(final int imageId) {
        final int deleted = entityManager
                .createQuery("DELETE FROM Media m WHERE m.image.id = :imageId")
                .setParameter("imageId", imageId)
                .executeUpdate();
        if (deleted > 0) {
            entityManager
                    .createQuery("DELETE FROM Image i WHERE i.id = :imageId")
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
                    .createQuery(
                            "UPDATE Media m SET m.id.index = :index WHERE m.version.id = :versionId AND m.image.id = :imageId")
                    .setParameter("index", i)
                    .setParameter("versionId", versionId)
                    .setParameter("imageId", imageIdsInOrder.get(i))
                    .executeUpdate();
        }
        return true;
    }

    private Integer findCurrentVersionId(final int itemId) {
        final List<Integer> result = entityManager
                .createQuery("SELECT MAX(v.id) FROM Version v WHERE v.item.id = :itemId", Integer.class)
                .setParameter("itemId", itemId)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    private static final int MAX_GALLERY_IMAGES = 5;
}
