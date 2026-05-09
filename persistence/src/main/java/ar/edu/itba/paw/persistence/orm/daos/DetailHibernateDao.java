package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.persistence.nuevo.DetailDao;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.projections.ItemListingRowOrm;
import ar.edu.itba.paw.persistence.orm.queries.ItemListingHql;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DetailHibernateDao implements DetailDao {

    private static final String GALLERY_IMAGE_IDS = "SELECT m.image.id FROM MediaOrm m WHERE m.version.id = "
            + "(SELECT MAX(v.id) FROM VersionOrm v WHERE v.item.id = :itemId) "
            + "ORDER BY m.id.index";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ItemModel> getItemById(final int itemId) {
        final TypedQuery<ItemListingRowOrm> query =
                entityManager.createQuery(ItemListingHql.ITEM_DETAIL_BY_ID, ItemListingRowOrm.class);
        query.setParameter("itemId", itemId);
        query.setParameter(ItemListingHql.ITEM_TARGET_PARAM, TargetEnumOrm.ITEM);
        final List<ItemListingRowOrm> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        final ItemModel item = rows.get(0).toItemModel();
        item.setImages(resolveGalleryPaths(itemId));
        return Optional.of(item);
    }

    private List<String> resolveGalleryPaths(final int itemId) {
        final TypedQuery<Integer> gallery = entityManager.createQuery(GALLERY_IMAGE_IDS, Integer.class);
        gallery.setParameter("itemId", itemId);
        final List<Integer> ids = gallery.getResultList();
        if (ids.isEmpty()) {
            return List.of("/css/boat-placeholder.svg");
        }
        final List<String> paths = new ArrayList<>(ids.size());
        for (final Integer imageId : ids) {
            paths.add("/image/" + imageId);
        }
        return paths;
    }
}
