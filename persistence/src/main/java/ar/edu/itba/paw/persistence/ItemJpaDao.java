package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.projections.MyBoatsRow;
import java.math.BigDecimal;
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
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        final int updated = entityManager
                .createQuery("UPDATE Item i SET i.status = :status WHERE i.id = :itemId AND i.host.id = :ownerId")
                .setParameter("status", active ? ItemStatusEnum.ACTIVE : ItemStatusEnum.INACTIVE)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
        return updated > 0;
    }

    @Override
    public Optional<Item> findItemByIdAndOwner(final int itemId, final int ownerId) {
        final List<Item> rows = entityManager
                .createQuery("FROM Item i WHERE i.id = :itemId AND i.host.id = :ownerId", Item.class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public boolean hasActiveOrFutureBookings(final int itemId) {
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
    public void deleteItem(final Item item) {
        entityManager.remove(item);
    }

    @Override
    public Optional<Version> findCurrentVersionByItemId(final int itemId) {
        final List<Version> rows = entityManager
                .createQuery("FROM Version v WHERE v.item.id = :itemId ORDER BY v.id DESC", Version.class)
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public boolean hasBookingReferencesByVersionId(final int versionId) {
        final Long count = entityManager
                .createQuery("SELECT COUNT(b) FROM Booking b WHERE b.version.id = :versionId", Long.class)
                .setParameter("versionId", versionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public void persistVersion(final Version version) {
        entityManager.persist(version);
    }

    @Override
    public void copyVersionContent(final int sourceVersionId, final Version targetVersion) {
        copyAvailabilities(sourceVersionId, targetVersion);
        copyMedia(sourceVersionId, targetVersion);
    }

    @Override
    public Location getLocationReference(final int locationId) {
        return entityManager.getReference(Location.class, locationId);
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
