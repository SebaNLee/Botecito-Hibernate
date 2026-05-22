package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
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
    public Optional<Image> findImageWithDataById(final int imageId) {
        return Optional.ofNullable(entityManager.find(Image.class, imageId));
    }
}
