package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemDetail;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.persistence.nuevo.DetailDao;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.projections.ItemListingRowOrm;
import ar.edu.itba.paw.persistence.orm.queries.ItemListingHql;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DetailHibernateDao implements DetailDao {

    private static final String GALLERY_IMAGE_IDS_FOR_VERSION =
            "SELECT m.image.id FROM MediaOrm m WHERE m.version.id = :versionId ORDER BY m.id.index";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ItemDetail> getItemDetailById(final int itemId, final boolean allVersions) {
        final String hql =
                allVersions ? ItemListingHql.ITEM_DETAIL_VERSIONS_BY_ITEM_ID : ItemListingHql.ITEM_DETAIL_BY_ID;
        final TypedQuery<ItemListingRowOrm> query = entityManager.createQuery(hql, ItemListingRowOrm.class);
        query.setParameter("itemId", itemId);
        query.setParameter(ItemListingHql.ITEM_TARGET_PARAM, TargetEnumOrm.ITEM);
        return toItemDetail(query.getResultList());
    }

    @Override
    public List<Integer> findVersionIdsFromGuestBookingsForItem(final int itemId, final int guestUserId) {
        final TypedQuery<Integer> query =
                entityManager.createQuery(ItemListingHql.VERSION_IDS_FOR_GUEST_BOOKINGS_ON_ITEM, Integer.class);
        query.setParameter("itemId", itemId);
        query.setParameter("guestId", guestUserId);
        return List.copyOf(query.getResultList());
    }

    @Override
    public Optional<ItemDetail> getItemDetailForVersionIds(final int itemId, final List<Integer> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Optional.empty();
        }
        final TypedQuery<ItemListingRowOrm> query = entityManager.createQuery(
                ItemListingHql.ITEM_DETAIL_VERSIONS_BY_ITEM_ID_IN_VERSION_IDS, ItemListingRowOrm.class);
        query.setParameter("itemId", itemId);
        query.setParameter("versionIds", versionIds);
        query.setParameter(ItemListingHql.ITEM_TARGET_PARAM, TargetEnumOrm.ITEM);
        return toItemDetail(query.getResultList());
    }

    private Optional<ItemDetail> toItemDetail(final List<ItemListingRowOrm> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        final ItemDetail detail = new ItemDetail();
        final List<ItemDetail.ItemModelVersion> versions = new ArrayList<>(rows.size());
        for (final ItemListingRowOrm row : rows) {
            final Integer versionId = row.getVersionId();
            if (versionId == null) {
                continue;
            }
            final ItemModel item = row.toItemModel();
            item.setImages(resolveGalleryPathsForVersion(versionId));
            versions.add(new ItemDetail.ItemModelVersion(item, versionId.longValue()));
        }
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        detail.setVersions(versions);
        return Optional.of(detail);
    }

    private List<String> resolveGalleryPathsForVersion(final int versionId) {
        final TypedQuery<Integer> gallery = entityManager.createQuery(GALLERY_IMAGE_IDS_FOR_VERSION, Integer.class);
        gallery.setParameter("versionId", versionId);
        final List<Integer> ids = gallery.getResultList();
        if (ids.isEmpty()) {
            return List.of("/css/boat-placeholder.svg");
        }
        final List<String> paths = new ArrayList<>(ids.size());
        for (final Integer imageId : ids) {
            paths.add("/image/" + Objects.requireNonNull(imageId));
        }
        return paths;
    }
}
