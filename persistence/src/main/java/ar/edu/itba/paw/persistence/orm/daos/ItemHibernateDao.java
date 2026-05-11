package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.nuevo.ItemDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemTypeOrm;
import ar.edu.itba.paw.persistence.orm.entities.LocationOrm;
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

    private static final int DEFAULT_WEIGHT = 2000;
    private static final int DEFAULT_DIFFICULTY = 1;

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
                createModel.getMaxWeightKg(),
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
        final String hql = "SELECT v FROM VersionOrm v"
                + " WHERE v.item.id = :itemId AND v.item.host.id = :ownerId"
                + " AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item.id = :itemId)";
        final List<VersionOrm> versions = entityManager
                .createQuery(hql, VersionOrm.class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setMaxResults(1)
                .getResultList();
        if (versions.isEmpty()) {
            return false;
        }
        final VersionOrm latest = versions.get(0);
        final VersionOrm version = new VersionOrm();
        version.setItem(latest.getItem());
        version.setType(latest.getType());
        version.setTitle(updateModel.getTitle());
        version.setDescription(updateModel.getDescription());
        version.setPrice(BigDecimal.valueOf(updateModel.getPricePerHour()));
        version.setCapacity(latest.getCapacity());
        version.setWeight(latest.getWeight());
        version.setDifficulty(
                updateModel.getDifficultyLevel() == null ? DEFAULT_DIFFICULTY : updateModel.getDifficultyLevel());
        version.setLocation(entityManager.getReference(LocationOrm.class, updateModel.getLocationOptionId()));
        version.setTimezone(latest.getTimezone());
        version.setCreatedAt(LocalDateTime.now());
        entityManager.persist(version);
        entityManager.flush();
        return true;
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
                .createQuery(
                        "UPDATE ItemOrm i SET i.status = :status WHERE i.id = :itemId AND i.host.id = :ownerId")
                .setParameter("status", active ? ItemStatusEnumOrm.ACTIVE : ItemStatusEnumOrm.INACTIVE)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
        entityManager.flush();
        return updated > 0;
    }

    public Integer insertItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
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
        version.setWeight(maxWeightKg == null ? DEFAULT_WEIGHT : maxWeightKg.intValue());
        final int difficulty = difficultyLevel == null ? DEFAULT_DIFFICULTY : difficultyLevel.intValue();
        version.setDifficulty(difficulty);
        version.setLocation(entityManager.getReference(LocationOrm.class, locationOptionId));
        version.setTimezone("America/Argentina/Buenos_Aires");
        version.setCreatedAt(now);
        entityManager.persist(version);
        entityManager.flush();

        return item.getId();
    }

    private static String baseMyBoatsQuery() {
        return "SELECT i.id, v.title, v.price, v.capacity, v.location.name, i.status,"
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
            final Integer id = (Integer) row[0];
            final String title = (String) row[1];
            final BigDecimal price = (BigDecimal) row[2];
            final Integer pricePerHour = price == null ? null : price.intValue();
            final Integer capacityPeople = (Integer) row[3];
            final String location = (String) row[4];
            final Boolean active = ItemStatusEnumOrm.ACTIVE.equals(row[5]);
            final Integer coverImageId = (Integer) row[6];
            final boolean hasBlocking = Boolean.TRUE.equals(row[7]);
            final boolean hasFutureBlocking = Boolean.TRUE.equals(row[8]);

            items.add(new MyBoatsItem(
                    id,
                    title,
                    pricePerHour,
                    capacityPeople,
                    location,
                    active,
                    coverImageId,
                    active && hasBlocking,
                    !active && hasFutureBlocking));
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
