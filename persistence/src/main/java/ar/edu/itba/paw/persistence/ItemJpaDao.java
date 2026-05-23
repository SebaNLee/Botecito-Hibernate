package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
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
        final String hql = baseMyBoatsQuery()
                + " WHERE i.host.id = :ownerId AND i.status <> :deletedStatus"
                + " ORDER BY i.createdAt DESC, i.id DESC";
        final List<MyBoatsRow> rows = entityManager
                .createQuery(hql, MyBoatsRow.class)
                .setParameter("ownerId", ownerId)
                .setParameter("deletedStatus", ItemStatusEnum.DELETED)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return toDomainItems(rows);
    }

    @Override
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        final Number count = (Number) entityManager
                .createQuery("SELECT COUNT(i) FROM Item i WHERE i.host.id = :ownerId AND i.status <> :deletedStatus")
                .setParameter("ownerId", ownerId)
                .setParameter("deletedStatus", ItemStatusEnum.DELETED)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        final String hql =
                baseMyBoatsQuery() + " WHERE i.id = :itemId AND i.host.id = :ownerId AND i.status <> :deletedStatus";
        final List<MyBoatsRow> rows = entityManager
                .createQuery(hql, MyBoatsRow.class)
                .setParameter("itemId", itemId)
                .setParameter("ownerId", ownerId)
                .setParameter("deletedStatus", ItemStatusEnum.DELETED)
                .setMaxResults(1)
                .getResultList();
        final List<MyBoatsItem> items = toDomainItems(rows);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    private static String baseMyBoatsQuery() {
        return "SELECT NEW ar.edu.itba.paw.persistence.projections.MyBoatsRow("
                + "i.id, v.id, v.title, v.description, v.price, v.difficulty, v.location.id,"
                + " v.capacity, v.location.name, i.status,"
                + " (SELECT m.image.id FROM Media m"
                + " WHERE m.version = v"
                + " AND m.id.index = (SELECT MIN(m2.id.index) FROM Media m2 WHERE m2.version = v)))"
                + " FROM Item i"
                + " JOIN Version v ON v.item = i"
                + " AND v.createdAt = (SELECT MAX(v2.createdAt) FROM Version v2 WHERE v2.item = i)";
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
            items.add(item);
        }
        return items;
    }

    @Override
    public Optional<Image> findImageWithDataById(final int imageId) {
        return Optional.ofNullable(entityManager.find(Image.class, imageId));
    }
}
