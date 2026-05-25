package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJpaDao implements ItemDao {

    @PersistenceContext
    private EntityManager em;

    private long countOwnerItems(int ownerId) {
        var query = em.createNativeQuery(
                        "SELECT COUNT(DISTINCT i.id) FROM item i WHERE i.host_id = :ownerId AND i.status <> CAST(:deleted AS item_status_enum)")
                .setParameter("ownerId", ownerId)
                .setParameter("deleted", ItemStatusEnum.DELETED.name());

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public ItemSearchResult listOwnerItems(int ownerId, int page, int pageSize) {
        final long totalCount = countOwnerItems(ownerId);

        // Get item IDs for this page

        var nativeQuery = em.createNativeQuery(
                        "SELECT i.id FROM item i WHERE i.host_id = :ownerId AND i.status <> CAST(:deleted AS item_status_enum) "
                                + "ORDER BY i.created_at DESC, i.id DESC")
                .setParameter("ownerId", ownerId)
                .setParameter("deleted", ItemStatusEnum.DELETED.name());

        Paging.apply(nativeQuery, page, pageSize);
        final List<Integer> ids = Paging.toIntegerIds(nativeQuery.getResultList());

        // Empty page failsafe
        if (ids.isEmpty()) {
            return new ItemSearchResult(List.of(), totalCount);
        }

        // Fetch item entities

        List<Item> items = em.createQuery(
                        "SELECT DISTINCT i FROM Item i WHERE i.id IN :ids ORDER BY i.createdAt DESC, i.id DESC",
                        Item.class)
                .setParameter("ids", ids)
                .getResultList();

        return new ItemSearchResult(items, totalCount);
    }

    @Override
    public Optional<Item> findItemById(int id) {
        return Optional.ofNullable(em.find(Item.class, id));
    }

    @Override
    public Optional<Image> findImageById(int id) {
        return Optional.ofNullable(em.find(Image.class, id));
    }
}
