package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingOrm;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class BookingHibernateDao implements BookingDao {

    /** Minimum clear time between adjacent blocking bookings on the same version (minutes). */
    private static final int MIN_CLEARANCE_BETWEEN_BOOKINGS_MINUTES = 30;

    private static final String SELECT_TIMEZONE_AND_AVAILABILITY_COVER = "SELECT v.timezone, "
            + "EXISTS ( "
            + "  SELECT 1 FROM availability a "
            + "  WHERE a.version_id = v.id "
            + "    AND a.weekday = CAST(:weekday AS weekday_enum) "
            + "    AND a.start_time <= CAST(:localStart AS time) "
            + "    AND a.end_time >= CAST(:localEnd AS time) "
            + ") "
            + "FROM version v "
            + "WHERE v.id = :versionId";

    private static final String HQL_BOOKINGS_FOR_VERSION =
            "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest WHERE b.version.id = :versionId ORDER BY b.start ASC";

    private static final String INSERT_IF_SLOT_FREE =
            "INSERT INTO booking (version_id, guest_id, start, \"end\", status, msg, created_at, updated_at) "
                    + "SELECT :versionId, :guestId, :utcStart, :utcEnd, CAST('PENDING' AS booking_status_enum), :msg, :now, :now "
                    + "WHERE EXISTS (SELECT 1 FROM version v WHERE v.id = :versionId) "
                    + "  AND EXISTS (SELECT 1 FROM users u WHERE u.id = :guestId) "
                    + "  AND NOT EXISTS ( "
                    + "    SELECT 1 FROM booking b "
                    + "    WHERE b.version_id = :versionId "
                    + "      AND b.status IN ( "
                    + "        CAST('PENDING' AS booking_status_enum), "
                    + "        CAST('ACCEPTED' AS booking_status_enum), "
                    + "        CAST('PAID' AS booking_status_enum), "
                    + "        CAST('CONFIRMED' AS booking_status_enum) "
                    + "      ) "
                    + "      AND NOT ( "
                    + "        CAST(:utcEnd AS timestamp) <= b.start - (:gapMinutes * INTERVAL '1 minute') "
                    + "        OR CAST(:utcStart AS timestamp) >= b.\"end\" + (:gapMinutes * INTERVAL '1 minute') "
                    + "      ) "
                    + "  ) "
                    + "RETURNING id";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int createBooking(final PreBookingReq preBookingReq) {
        try {
            if (!guestExists(preBookingReq.getGuestId())) {
                return RESULT_UNEXPECTED_ERROR;
            }

            final LocalDateTime localStart = LocalDateTime.of(preBookingReq.getDate(), preBookingReq.getStartTime());
            final LocalDateTime localEnd = LocalDateTime.of(preBookingReq.getDate(), preBookingReq.getEndTime());
            // Minimum bookable duration is 120 minutes (aligned with timeRangePicker default and JS rules).
            if (!localEnd.isAfter(localStart) || localEnd.isBefore(localStart.plusMinutes(120))) {
                return RESULT_UNEXPECTED_ERROR;
            }

            final Query coverQuery = entityManager.createNativeQuery(SELECT_TIMEZONE_AND_AVAILABILITY_COVER);
            coverQuery.setParameter("versionId", preBookingReq.getVersionId());
            coverQuery.setParameter("weekday", localStart.getDayOfWeek().name());
            coverQuery.setParameter("localStart", preBookingReq.getStartTime());
            coverQuery.setParameter("localEnd", preBookingReq.getEndTime());

            @SuppressWarnings("unchecked")
            final List<Object[]> coverRows = coverQuery.getResultList();
            if (coverRows.isEmpty()) {
                return RESULT_UNEXPECTED_ERROR;
            }

            final Object[] coverRow = coverRows.get(0);
            final ZoneId zoneId = ZoneId.of(((String) coverRow[0]).trim());
            final ZonedDateTime zonedStart = localStart.atZone(zoneId);
            final ZonedDateTime zonedEnd = localEnd.atZone(zoneId);

            final boolean covers = toBoolean(coverRow[1]);
            if (!covers) {
                return RESULT_OUTSIDE_AVAILABILITY;
            }

            final LocalDateTime utcStart = LocalDateTime.ofInstant(zonedStart.toInstant(), ZoneOffset.UTC);
            final LocalDateTime utcEnd = LocalDateTime.ofInstant(zonedEnd.toInstant(), ZoneOffset.UTC);
            final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

            final Query insertQuery = entityManager.createNativeQuery(INSERT_IF_SLOT_FREE);
            insertQuery.setParameter("versionId", preBookingReq.getVersionId());
            insertQuery.setParameter("guestId", preBookingReq.getGuestId());
            insertQuery.setParameter("utcStart", utcStart);
            insertQuery.setParameter("utcEnd", utcEnd);
            insertQuery.setParameter("msg", preBookingReq.getMessage());
            insertQuery.setParameter("now", now);
            insertQuery.setParameter("gapMinutes", MIN_CLEARANCE_BETWEEN_BOOKINGS_MINUTES);

            @SuppressWarnings("unchecked")
            final List<Number> ids = insertQuery.getResultList();
            if (ids.isEmpty()) {
                return RESULT_BOOKING_COLLISION;
            }
            return ids.get(0).intValue();
        } catch (final RuntimeException e) {
            return RESULT_UNEXPECTED_ERROR;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForVersion(final int versionId) {
        final TypedQuery<BookingOrm> query = entityManager.createQuery(HQL_BOOKINGS_FOR_VERSION, BookingOrm.class);
        query.setParameter("versionId", versionId);
        return query.getResultList().stream()
                .map(BookingHibernateDao::toBooking)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(final int bookingId) {
        return Optional.ofNullable(entityManager.find(BookingOrm.class, bookingId))
                .map(BookingHibernateDao::toBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> listBookingsByGuestId(final int guestId) {
        return entityManager
                .createQuery(
                        "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest "
                                + "WHERE b.guest.id = :guestId ORDER BY b.start ASC",
                        BookingOrm.class)
                .setParameter("guestId", guestId)
                .getResultList()
                .stream()
                .map(BookingHibernateDao::toBooking)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> listBookingsByOwnerId(final int ownerId) {
        return entityManager
                .createQuery(
                        "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest "
                                + "WHERE b.version.item.host.id = :ownerId ORDER BY b.start ASC",
                        BookingOrm.class)
                .setParameter("ownerId", ownerId)
                .getResultList()
                .stream()
                .map(BookingHibernateDao::toBooking)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> findItemIdForBookingId(final int bookingId) {
        try {
            final Integer itemId = entityManager
                    .createQuery("SELECT b.version.item.id FROM BookingOrm b WHERE b.id = :id", Integer.class)
                    .setParameter("id", bookingId)
                    .getSingleResult();
            return Optional.ofNullable(itemId);
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> findOwnerIdForBookingId(final int bookingId) {
        try {
            final Integer ownerId = entityManager
                    .createQuery("SELECT b.version.item.host.id FROM BookingOrm b WHERE b.id = :id", Integer.class)
                    .setParameter("id", bookingId)
                    .getSingleResult();
            return Optional.ofNullable(ownerId);
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    private static Booking toBooking(final BookingOrm orm) {
        final Booking b = new Booking();
        b.setId(orm.getId());
        b.setVersionId(orm.getVersion().getId());
        b.setGuestId(orm.getGuest() != null ? orm.getGuest().getId() : 0);
        b.setStart(orm.getStart());
        b.setEnd(orm.getEnd());
        b.setStatus(BookingStatus.valueOf(orm.getStatus().name()));
        b.setMsg(orm.getMsg());
        b.setCreatedAt(orm.getCreatedAt());
        b.setUpdatedAt(orm.getUpdatedAt());
        return b;
    }

    private boolean guestExists(final int guestId) {
        final Object count = entityManager
                .createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :guestId")
                .setParameter("guestId", guestId)
                .getSingleResult();
        return ((Number) count).intValue() > 0;
    }

    private static boolean toBoolean(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return false;
    }
}
