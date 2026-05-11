package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.persistence.ItemJdbcDao;
import ar.edu.itba.paw.persistence.ItemDao;
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

    private final ItemJdbcDao itemJdbcDao;

    public ItemHibernateDao(final ItemJdbcDao itemJdbcDao) {
        this.itemJdbcDao = itemJdbcDao;
    }

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
    public List<Item> listItemsByOwnerId(final int ownerId) {
        return itemJdbcDao.listItemsByOwnerId(ownerId);
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
        return itemJdbcDao.updatePublication(itemId, title, description, pricePerHour, difficultyLevel, locationOptionId);
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
        version.setDifficulty(difficultyLevel == null ? DEFAULT_DIFFICULTY : difficultyLevel);
        version.setLocation(entityManager.getReference(LocationOrm.class, locationOptionId));
        version.setTimezone("America/Argentina/Buenos_Aires"); // TODO dynamic IANA timezones https://timeapi.io/documentation/iana-timezones
        version.setCreatedAt(now);
        entityManager.persist(version);
        entityManager.flush();

        return item.getId();
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
                .createNativeQuery(
                        "SELECT COUNT(*) FROM booking b "
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
