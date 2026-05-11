package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.ItemJdbcDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.ItemTypeOrm;
import ar.edu.itba.paw.persistence.orm.entities.LocationOrm;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import ar.edu.itba.paw.persistence.orm.entities.VersionOrm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Transactional
public class ItemHibernateDao implements ItemDao, ar.edu.itba.paw.persistence.nuevo.ItemDao {

    private static final int DEFAULT_WEIGHT = 2000;
    private static final int DEFAULT_DIFFICULTY = 1;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ItemJdbcDao itemJdbcDao;

    @Override
    @Transactional(readOnly = true)
    public List<Item> listItems() {
        return itemJdbcDao.listItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> listItems(final ItemSearchCriteria criteria, final int limit, final int offset) {
        return itemJdbcDao.listItems(criteria, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countItems(final ItemSearchCriteria criteria) {
        return itemJdbcDao.countItems(criteria);
    }

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
        final Item created = createItem(
                createModel.getOwnerId(),
                createModel.getTypeId(),
                createModel.getTitle(),
                createModel.getDescription(),
                createModel.getPricePerHour(),
                createModel.getCapacityPeople(),
                createModel.getMaxWeightKg(),
                createModel.getDifficultyLevel(),
                createModel.getLocationOptionId(),
                createModel.getOwnerDeleteToken());
        if (created == null || created.getId() == null) {
            return Optional.empty();
        }
        return findMyBoatsItemByIdForOwner(created.getId(), createModel.getOwnerId());
    }

    @Override
    public boolean updateMyBoatsItem(final int itemId, final int ownerId, final ItemUpdateModel updateModel) {
        if (updateModel == null) {
            return false;
        }
        return updatePublicationForOwner(
                itemId,
                ownerId,
                updateModel.getTitle(),
                updateModel.getDescription(),
                updateModel.getPricePerHour(),
                updateModel.getDifficultyLevel(),
                updateModel.getLocationOptionId());
    }

    @Override
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        return deleteItemByIdForOwner(itemId, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationOption> listLocationOptions() {
        return itemJdbcDao.listLocationOptions();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> findItemById(final int id) {
        return itemJdbcDao.findItemById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> findItemByIdForOwner(final int id, final int ownerId) {
        return itemJdbcDao.findItemByIdForOwner(id, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> findAnyItemById(final int id) {
        return itemJdbcDao.findAnyItemById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemType> findItemTypeById(final int id) {
        return itemJdbcDao.findItemTypeById(id);
    }

    @Override
    public boolean updatePublication(
            final int itemId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        return itemJdbcDao.updatePublication(
                itemId, title, description, pricePerHour, difficultyLevel, locationOptionId);
    }

    @Override
    public boolean updatePublicationForOwner(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        return itemJdbcDao.updatePublicationForOwner(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId);
    }

    @Override
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        return itemJdbcDao.hasBlockingBookingsForEdition(itemId);
    }

    @Override
    public boolean deleteItemById(final int itemId) {
        return deleteItemWithOwnershipScope(itemId, null);
    }

    @Override
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        return deleteItemWithOwnershipScope(itemId, ownerId);
    }

    @Override
    public Item createItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final String ownerDeleteToken) {
        final Integer itemId = insertItem(
                ownerId,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                maxWeightKg,
                difficultyLevel,
                locationOptionId);
        if (itemId == null) {
            throw new IllegalStateException("Could not create item for owner " + ownerId);
        }
        return itemJdbcDao
                .findAnyItemById(itemId)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted item " + itemId));
    }

    @Override
    public boolean snapshotBookingsForPublicationEdit(final int itemId) {
        return itemJdbcDao.snapshotBookingsForPublicationEdit(itemId);
    }

    @Override
    public boolean setItemActive(final int itemId, final boolean active) {
        return itemJdbcDao.setItemActive(itemId, active);
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return itemJdbcDao.setItemActiveForOwner(itemId, ownerId, active);
    }

    @Override
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
        version.setTimezone("America/Argentina/Buenos_Aires"); // TODO dynamic IANA timezones
        // https://timeapi.io/documentation/iana-timezones
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
        final List<MyBoatsItem> items = new java.util.ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final MyBoatsItem item = new MyBoatsItem();
            item.setId((Integer) row[0]);
            item.setTitle((String) row[1]);
            final java.math.BigDecimal price = (java.math.BigDecimal) row[2];
            item.setPricePerHour(price == null ? null : price.intValue());
            item.setCapacityPeople((Integer) row[3]);
            item.setLocation((String) row[4]);
            item.setActive(ItemStatusEnumOrm.ACTIVE.equals(row[5]));
            item.setCoverImageId((Integer) row[6]);
            final boolean hasBlocking = Boolean.TRUE.equals(row[7]);
            final boolean hasFutureBlocking = Boolean.TRUE.equals(row[8]);
            item.setDeleteDeactivates(Boolean.TRUE.equals(item.getActive()) && hasBlocking);
            item.setDeleteDisabled(!Boolean.TRUE.equals(item.getActive()) && hasFutureBlocking);
            items.add(item);
        }
        return items;
    }

    private boolean deleteItemWithOwnershipScope(final int itemId, final Integer ownerId) {
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
