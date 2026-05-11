package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.ItemDetail;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import ar.edu.itba.paw.persistence.nuevo.DetailDao;
import ar.edu.itba.paw.persistence.orm.entities.AvailabilityOrm;
import ar.edu.itba.paw.persistence.orm.entities.ReviewOrm;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.projections.ItemListingRowOrm;
import ar.edu.itba.paw.persistence.orm.queries.ItemListingHql;
import java.time.DayOfWeek;
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

    private static final String HQL_VERSION_TIMEZONE = "SELECT v.timezone FROM VersionOrm v WHERE v.id = :versionId";

    /**
     * Item-scoped reviews (matches {@link ar.edu.itba.paw.persistence.orm.queries.ItemListingHql} aggregate
     * subqueries on {@code r.booking.version.item}), so detail ratings and listed reviews stay consistent when a
     * booking was made on an older listing version.
     */
    private static final String HQL_REVIEWS_FOR_ITEM =
            "SELECT r FROM ReviewOrm r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender "
                    + "WHERE b.version.item.id = :itemId AND r.targetType = :itemTarget ORDER BY r.createdAt DESC";

    private static final String HQL_AVAILABILITIES_FOR_VERSION =
            "SELECT a FROM AvailabilityOrm a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private final BookingDao bookingDao;

    @PersistenceContext
    private EntityManager entityManager;

    public DetailHibernateDao(final BookingDao bookingDao) {
        this.bookingDao = bookingDao;
    }

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
        final int itemId = Objects.requireNonNull(rows.get(0).getItemId(), "itemId");
        final List<ReviewModel> itemReviews = List.copyOf(loadReviewsForItem(itemId));
        final List<ItemDetail.ItemModelVersion> versions = new ArrayList<>(rows.size());
        for (final ItemListingRowOrm row : rows) {
            final Integer versionId = row.getVersionId();
            if (versionId == null) {
                continue;
            }
            final ItemModel item = row.toItemModel();
            item.setImages(resolveGalleryPathsForVersion(versionId));
            final String timezone = resolveVersionTimezone(versionId);
            final List<Booking> bookings = List.copyOf(bookingDao.getBookingsForVersion(versionId));
            final List<AvailabilityWindow> availabilityWindows =
                    List.copyOf(loadAvailabilityWindowsForVersion(versionId));
            versions.add(new ItemDetail.ItemModelVersion(
                    item, bookings, itemReviews, versionId.longValue(), timezone, availabilityWindows));
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

    private String resolveVersionTimezone(final int versionId) {
        final TypedQuery<String> q = entityManager.createQuery(HQL_VERSION_TIMEZONE, String.class);
        q.setParameter("versionId", versionId);
        final String tz = q.getSingleResult();
        return tz == null ? "" : tz.trim();
    }

    private List<AvailabilityWindow> loadAvailabilityWindowsForVersion(final int versionId) {
        final TypedQuery<AvailabilityOrm> q =
                entityManager.createQuery(HQL_AVAILABILITIES_FOR_VERSION, AvailabilityOrm.class);
        q.setParameter("versionId", versionId);
        final List<AvailabilityOrm> rows = q.getResultList();
        final List<AvailabilityWindow> out = new ArrayList<>(rows.size());
        for (final AvailabilityOrm orm : rows) {
            out.add(toAvailabilityWindow(orm));
        }
        return out;
    }

    private static AvailabilityWindow toAvailabilityWindow(final AvailabilityOrm orm) {
        final AvailabilityWindow w = new AvailabilityWindow();
        w.setWeekday(
                orm.getWeekday() == null
                        ? null
                        : DayOfWeek.valueOf(orm.getWeekday().name()));
        w.setStartTime(orm.getStartTime());
        w.setEndTime(orm.getEndTime());
        return w;
    }

    private List<ReviewModel> loadReviewsForItem(final int itemId) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(HQL_REVIEWS_FOR_ITEM, ReviewOrm.class);
        q.setParameter("itemId", itemId);
        q.setParameter("itemTarget", TargetEnumOrm.ITEM);
        final List<ReviewOrm> rows = q.getResultList();
        final List<ReviewModel> out = new ArrayList<>(rows.size());
        for (final ReviewOrm orm : rows) {
            out.add(toReviewModel(orm));
        }
        return out;
    }

    private static ReviewModel toReviewModel(final ReviewOrm orm) {
        final ReviewModel m = new ReviewModel();
        m.setId(orm.getId());
        m.setBookingId(orm.getBooking().getId());
        m.setSenderId(orm.getSender() != null ? orm.getSender().getId() : 0);
        m.setTargetType(TargetType.valueOf(orm.getTargetType().name()));
        m.setRating(orm.getRating() == null ? 0d : orm.getRating().doubleValue());
        m.setComment(orm.getComment());
        m.setCreatedAt(orm.getCreatedAt());
        return m;
    }
}
