package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ItemDetail;
import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.ReviewOrm;
import ar.edu.itba.paw.models.entity.TargetEnumOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import ar.edu.itba.paw.persistence.orm.projections.DetailRowOrm;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class DetailJpaDao implements DetailDao {

    private static final String GALLERY_IMAGE_IDS_FOR_VERSION =
            "SELECT m.image.id FROM MediaOrm m WHERE m.version.id = :versionId ORDER BY m.id.index";

    private static final String HQL_VERSION_TIMEZONE = "SELECT v.timezone FROM VersionOrm v WHERE v.id = :versionId";

    /**
     * Item-scoped reviews (matches aggregate subqueries on {@code r.booking.version.item}),
     * so detail ratings and listed reviews stay consistent when a booking was made on an older
     * listing version.
     */
    private static final String HQL_REVIEWS_FOR_ITEM =
            "SELECT r FROM ReviewOrm r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender "
                    + "WHERE b.version.item.id = :itemId AND r.targetType = :itemTarget ORDER BY r.createdAt DESC";

    private static final String HQL_AVAILABILITIES_FOR_VERSION =
            "SELECT a FROM AvailabilityOrm a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private static final String HQL_BOOKINGS_FOR_VERSION =
            "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof "
                    + "WHERE b.version.id = :versionId ORDER BY b.start ASC";

    private static final String ITEM_WITH_CURRENT_VERSION = "FROM ItemOrm i "
            + "JOIN VersionOrm v ON v.item = i "
            + "  AND v.id = (SELECT MAX(v2.id) FROM VersionOrm v2 WHERE v2.item = i) ";

    private static final String ITEM_WITH_VERSION_JOIN = "FROM ItemOrm i JOIN VersionOrm v ON v.item = i ";

    private static final String COVER_IMAGE_SUBQUERY = "(SELECT MIN(m.image.id) FROM MediaOrm m"
            + " WHERE m.version = v AND m.id.index = ("
            + "   SELECT MIN(m2.id.index) FROM MediaOrm m2 WHERE m2.version = v"
            + " ))";

    private static final String RATING_SUBQUERY = "(SELECT COALESCE(AVG(r.rating), 0) FROM ReviewOrm r"
            + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i)";

    private static final String REVIEW_COUNT_SUBQUERY = "(SELECT COUNT(r) FROM ReviewOrm r"
            + " WHERE r.targetType = :itemTargetType AND r.booking.version.item = i)";

    private static final String DETAIL_ROW_CONSTRUCTOR =
            "SELECT NEW ar.edu.itba.paw.persistence.orm.projections.DetailRowOrm("
                    + "i.id, i.host.id, i.status,"
                    + " v.title, v.description, v.price, v.capacity, v.weight, v.difficulty,"
                    + " v.location.id, v.location.name, v.type.name, "
                    + COVER_IMAGE_SUBQUERY + ", " + RATING_SUBQUERY + ", " + REVIEW_COUNT_SUBQUERY + ","
                    + " v.id"
                    + ") ";

    private static final String ITEM_DETAIL_BY_ID =
            DETAIL_ROW_CONSTRUCTOR + ITEM_WITH_CURRENT_VERSION + " WHERE i.id = :itemId";

    private static final String ITEM_DETAIL_VERSIONS_BY_ITEM_ID =
            DETAIL_ROW_CONSTRUCTOR + ITEM_WITH_VERSION_JOIN + " WHERE i.id = :itemId ORDER BY v.id DESC";

    private static final String ITEM_DETAIL_VERSIONS_BY_ITEM_ID_IN_VERSION_IDS = DETAIL_ROW_CONSTRUCTOR
            + ITEM_WITH_VERSION_JOIN + " WHERE i.id = :itemId AND v.id IN :versionIds ORDER BY v.id DESC";

    private static final String VERSION_IDS_FOR_GUEST_BOOKINGS_ON_ITEM =
            "SELECT DISTINCT b.version.id FROM BookingOrm b WHERE b.guest IS NOT NULL"
                    + " AND b.guest.id = :guestId AND b.version.item.id = :itemId";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ItemDetail> getItemDetailById(final int itemId, final boolean allVersions) {
        final String hql = allVersions ? ITEM_DETAIL_VERSIONS_BY_ITEM_ID : ITEM_DETAIL_BY_ID;
        final TypedQuery<DetailRowOrm> query = entityManager.createQuery(hql, DetailRowOrm.class);
        query.setParameter("itemId", itemId);
        query.setParameter("itemTargetType", TargetEnumOrm.ITEM);
        return toItemDetail(query.getResultList());
    }

    @Override
    public List<Integer> findVersionIdsFromGuestBookingsForItem(final int itemId, final int guestUserId) {
        final TypedQuery<Integer> query =
                entityManager.createQuery(VERSION_IDS_FOR_GUEST_BOOKINGS_ON_ITEM, Integer.class);
        query.setParameter("itemId", itemId);
        query.setParameter("guestId", guestUserId);
        return List.copyOf(query.getResultList());
    }

    @Override
    public Optional<ItemDetail> getItemDetailForVersionIds(final int itemId, final List<Integer> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Optional.empty();
        }
        final TypedQuery<DetailRowOrm> query =
                entityManager.createQuery(ITEM_DETAIL_VERSIONS_BY_ITEM_ID_IN_VERSION_IDS, DetailRowOrm.class);
        query.setParameter("itemId", itemId);
        query.setParameter("versionIds", versionIds);
        query.setParameter("itemTargetType", TargetEnumOrm.ITEM);
        return toItemDetail(query.getResultList());
    }

    @Override
    public Optional<ItemDetail> getItemDetailCurrent(final int itemId) {
        return getItemDetailById(itemId, false);
    }

    private Optional<ItemDetail> toItemDetail(final List<DetailRowOrm> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        final ItemDetail detail = new ItemDetail();
        final int itemId = Objects.requireNonNull(rows.get(0).getItemId(), "itemId");
        final List<ReviewOrm> itemReviews = List.copyOf(loadReviewsForItem(itemId));
        final List<ItemDetail.VersionDetail> versions = new ArrayList<>(rows.size());
        for (final DetailRowOrm row : rows) {
            final Integer versionId = row.getVersionId();
            if (versionId == null) {
                continue;
            }
            final String timezone = resolveVersionTimezone(versionId);
            final List<BookingOrm> bookings = List.copyOf(loadBookingsForVersion(versionId));
            final List<AvailabilityOrm> availabilityWindows = List.copyOf(loadAvailabilityWindowsForVersion(versionId));
            final VersionOrm versionEntity = entityManager.find(VersionOrm.class, versionId);
            versions.add(ItemDetail.VersionDetail.builder()
                    .itemId(itemId)
                    .hostId(row.getHostId() != null ? row.getHostId() : 0)
                    .status(row.getStatus())
                    .versionId(versionId)
                    .title(row.getTitle())
                    .description(row.getDescription())
                    .price(row.getPrice())
                    .capacity(Objects.requireNonNull(row.getCapacity(), "capacity"))
                    .weight(Objects.requireNonNull(row.getWeight(), "weight"))
                    .difficulty(Objects.requireNonNull(row.getDifficulty(), "difficulty"))
                    .locationId(Objects.requireNonNull(row.getLocationId(), "locationId"))
                    .location(row.getLocationName())
                    .itemTypeName(row.getItemTypeName())
                    .averageRating(row.getAverageRating() == null ? 0D : row.getAverageRating())
                    .totalReviews(
                            row.getTotalReviews() == null
                                    ? 0
                                    : row.getTotalReviews().intValue())
                    .images(resolveGalleryPathsForVersion(versionId))
                    .bookings(bookings)
                    .reviews(itemReviews)
                    .versionTimezone(timezone)
                    .availabilityWindows(availabilityWindows)
                    .version(versionEntity)
                    .build());
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

    private List<BookingOrm> loadBookingsForVersion(final int versionId) {
        final TypedQuery<BookingOrm> q = entityManager.createQuery(HQL_BOOKINGS_FOR_VERSION, BookingOrm.class);
        q.setParameter("versionId", versionId);
        return q.getResultList();
    }

    private List<AvailabilityOrm> loadAvailabilityWindowsForVersion(final int versionId) {
        final TypedQuery<AvailabilityOrm> q =
                entityManager.createQuery(HQL_AVAILABILITIES_FOR_VERSION, AvailabilityOrm.class);
        q.setParameter("versionId", versionId);
        return q.getResultList();
    }

    private List<ReviewOrm> loadReviewsForItem(final int itemId) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(HQL_REVIEWS_FOR_ITEM, ReviewOrm.class);
        q.setParameter("itemId", itemId);
        q.setParameter("itemTarget", TargetEnumOrm.ITEM);
        return q.getResultList();
    }
}
