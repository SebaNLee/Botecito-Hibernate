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
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId, final int page, final int pageSize) {
        final String hql = baseMyBoatsQuery() + " WHERE i.host.id = :ownerId ORDER BY i.createdAt DESC, i.id DESC";
        final List<Object[]> rows = entityManager
                .createQuery(hql, Object[].class)
                .setParameter("ownerId", ownerId)
                .setParameter("rejectedStatus", BookingStatusEnumOrm.REJECTED)
                .setParameter("cancelledStatus", BookingStatusEnumOrm.CANCELLED)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return mapMyBoatsRows(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        final Number count = (Number) entityManager
                .createQuery("SELECT COUNT(i) FROM ItemOrm i WHERE i.host.id = :ownerId")
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
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
        // TODO hardcode redirect for now
        throw new UnsupportedOperationException("Not yet implemented");
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
                .weight(maxWeightKg == null ? DEFAULT_WEIGHT : maxWeightKg.intValue())
                .difficulty(difficultyLevel == null ? Integer.valueOf(DEFAULT_DIFFICULTY) : difficultyLevel)
                .location(entityManager.getReference(LocationOrm.class, locationOptionId))
                .timezone("America/Argentina/Buenos_Aires")
                .createdAt(now)
                .build();
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
                + " AND b.end > CURRENT_TIMESTAMP),"
                + " v.description, v.difficulty, v.location.id"
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
            final String description = row[9] == null ? "" : (String) row[9];
            final Integer difficultyLevel = (Integer) row[10];
            final Integer locationOptionId = (Integer) row[11];

            items.add(new MyBoatsItem(
                    id,
                    title,
                    pricePerHour,
                    capacityPeople,
                    location,
                    active,
                    coverImageId,
                    active && hasBlocking,
                    !active && hasFutureBlocking,
                    description,
                    difficultyLevel,
                    locationOptionId));
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
