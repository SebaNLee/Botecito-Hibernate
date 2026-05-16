package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.BookingStatusEnumOrm;
import ar.edu.itba.paw.models.entity.PaymentProofOrm;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class BookingHibernateDao implements BookingDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    /**
     * Minimum clear time between adjacent blocking bookings on the same version
     * (minutes).
     */
    private static final int MIN_CLEARANCE_BETWEEN_BOOKINGS_MINUTES = 30;

    private static final String HQL_BOOKINGS_FOR_VERSION =
            "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof WHERE b.version.id = :versionId ORDER BY b.start ASC";

    private static final String INSERT_WITH_OVERLAP_CHECK =
            "INSERT INTO booking (version_id, guest_id, start, \"end\", status, msg, created_at, updated_at) "
                    + "SELECT :versionId, :guestId, :utcStart, :utcEnd, "
                    + "CAST(:status AS booking_status_enum), :msg, :now, :now "
                    + "WHERE NOT EXISTS ( "
                    + "  SELECT 1 FROM booking b "
                    + "  WHERE b.version_id = :versionId "
                    + "    AND b.status IN ( "
                    + "      CAST('PENDING' AS booking_status_enum), "
                    + "      CAST('ACCEPTED' AS booking_status_enum), "
                    + "      CAST('PAID' AS booking_status_enum), "
                    + "      CAST('CONFIRMED' AS booking_status_enum) "
                    + "    ) "
                    + "    AND NOT ( "
                    + "      CAST(:utcEnd AS timestamp) <= b.start - (:gapMinutes * INTERVAL '1 minute') "
                    + "      OR CAST(:utcStart AS timestamp) >= b.\"end\" + (:gapMinutes * INTERVAL '1 minute') "
                    + "    ) "
                    + ") "
                    + "RETURNING id";

    private static final String HQL_VERSION_TIMEZONE =
            "SELECT v.timezone FROM VersionOrm v WHERE v.id = :versionId";

    private static final String HQL_VERSION_OWNER_ID =
            "SELECT v.item.host.id FROM VersionOrm v WHERE v.id = :versionId";

    private static final String HQL_AVAILABILITIES_FOR_VERSION =
            "SELECT a FROM AvailabilityOrm a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private static final EnumSet<BookingStatusEnumOrm> NON_AUTO_CANCEL_STATES = EnumSet.of(
            BookingStatusEnumOrm.CONFIRMED,
            BookingStatusEnumOrm.CANCELLED,
            BookingStatusEnumOrm.FINISHED,
            BookingStatusEnumOrm.REJECTED);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Integer> insertBooking(
            final int versionId, final int guestId,
            final LocalDateTime utcStart, final LocalDateTime utcEnd,
            final BookingStatusEnumOrm status, final String msg) {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        final Query query = entityManager.createNativeQuery(INSERT_WITH_OVERLAP_CHECK);
        query.setParameter("versionId", versionId);
        query.setParameter("guestId", guestId);
        query.setParameter("utcStart", utcStart);
        query.setParameter("utcEnd", utcEnd);
        query.setParameter("status", status.name());
        query.setParameter("msg", msg);
        query.setParameter("now", now);
        query.setParameter("gapMinutes", MIN_CLEARANCE_BETWEEN_BOOKINGS_MINUTES);

        @SuppressWarnings("unchecked")
        final List<Number> ids = query.getResultList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.get(0).intValue());
    }

    @Override
    public Optional<String> findVersionTimezone(final int versionId) {
        try {
            return Optional.ofNullable(entityManager
                    .createQuery(HQL_VERSION_TIMEZONE, String.class)
                    .setParameter("versionId", versionId)
                    .getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> findOwnerIdForVersion(final int versionId) {
        try {
            return Optional.ofNullable(entityManager
                    .createQuery(HQL_VERSION_OWNER_ID, Integer.class)
                    .setParameter("versionId", versionId)
                    .getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<AvailabilityOrm> listAvailabilitiesForVersion(final int versionId) {
        return entityManager
                .createQuery(HQL_AVAILABILITIES_FOR_VERSION, AvailabilityOrm.class)
                .setParameter("versionId", versionId)
                .getResultList();
    }

    @Override
    public boolean deleteOwnerSelfBlock(final int bookingId, final int ownerId) {
        return entityManager
                .createQuery("DELETE FROM BookingOrm b WHERE b.id = :id AND b.guest.id = :ownerId "
                        + "AND b.status IN :statuses")
                .setParameter("id", bookingId)
                .setParameter("ownerId", ownerId)
                .setParameter("statuses", EnumSet.of(
                        BookingStatusEnumOrm.CONFIRMED, BookingStatusEnumOrm.ACCEPTED))
                .executeUpdate() > 0;
    }

    @Override
    public List<BookingOrm> getBookingsForVersion(final int versionId) {
        return entityManager
                .createQuery(HQL_BOOKINGS_FOR_VERSION, BookingOrm.class)
                .setParameter("versionId", versionId)
                .getResultList();
    }

    @Override
    public Optional<BookingOrm> findById(final int bookingId) {
        return Optional.ofNullable(entityManager.find(BookingOrm.class, bookingId));
    }

    @Override

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

    @Override
    public BookingSearchResult searchBookings(final int userId, final boolean asHost,
            final String searchQuery, final LocalDate date, final BookingStatusEnumOrm status,
            final Integer page, final Integer pageSize, final String sortBy) {
        final Map<String, Object> params = new HashMap<>();
        final int resolvedPageSize = resolvePageSize(pageSize);
        final int offset = (resolvePage(page) - 1) * resolvedPageSize;

        final StringBuilder hql = new StringBuilder(
                "SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof INNER JOIN FETCH b.version v "
                        + "INNER JOIN FETCH v.item i LEFT JOIN FETCH i.host h ");
        appendUserFilters(hql, userId, asHost, searchQuery, date, status, params);
        hql.append(orderByBookings(sortBy));

        final TypedQuery<BookingOrm> query = entityManager.createQuery(hql.toString(), BookingOrm.class);
        bindParams(query, params);
        query.setFirstResult(offset);
        query.setMaxResults(resolvedPageSize);

        final List<BookingOrm> rows = query.getResultList();
        final long total = countMatching(userId, asHost, searchQuery, date, status);
        return new BookingSearchResult(rows, total);
    }

    @Override
    public boolean startsAfter(int bookingId, LocalDateTime requestedStart) {
        return entityManager
                .createQuery(
                        "SELECT COUNT(b) > 0 FROM BookingOrm b WHERE b.id = :id AND b.start > :requested",
                        Boolean.class)
                .setParameter("id", bookingId)
                .setParameter("requested", requestedStart)
                .getSingleResult();
    }

    @Override
    public Optional<BookingOrm> updateStatusIncoming(int id, int callerId, BookingStatusEnumOrm status) {
        try {
            int rowsUpdated = entityManager
                    .createQuery("UPDATE BookingOrm b SET b.status = :newStatus WHERE b.id IN ("
                            + "SELECT b2.id FROM BookingOrm b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
                            + "WHERE b2.id = :bookingId AND h.id = :caller)")
                    .setParameter("newStatus", status)
                    .setParameter("bookingId", id)
                    .setParameter("caller", callerId)
                    .executeUpdate();
            if (rowsUpdated <= 0) {
                return Optional.empty();
            }
            return findById(id);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BookingOrm> updateStatusOutgoing(int id, int callerId, BookingStatusEnumOrm status) {
        try {
            int rowsUpdated = entityManager
                    .createQuery(
                            "UPDATE BookingOrm b SET b.status = :newStatus WHERE b.id IN ("
                                    + "SELECT b2.id FROM BookingOrm b2 INNER JOIN b2.guest g WHERE b2.id = :bookingId AND g.id = :caller)")
                    .setParameter("newStatus", status)
                    .setParameter("bookingId", id)
                    .setParameter("caller", callerId)
                    .executeUpdate();
            if (rowsUpdated <= 0) {
                return Optional.empty();
            }
            return findById(id);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PaymentProofOrm> uploadPayment(final PaymentProofOrm proof) {
        if (proof == null) return Optional.empty();

        try {
            entityManager.merge(proof);
            return Optional.of(proof);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PaymentProofOrm> findPaymentProofForParticipant(final int bookingId, final int userId) {
        final TypedQuery<PaymentProofOrm> query = entityManager.createQuery(
                "SELECT p FROM PaymentProofOrm p INNER JOIN p.booking b LEFT JOIN b.guest g "
                        + "INNER JOIN b.version v INNER JOIN v.item i LEFT JOIN i.host h "
                        + "WHERE b.id = :bookingId AND (g.id = :userId OR h.id = :userId)",
                PaymentProofOrm.class);
        query.setParameter("bookingId", bookingId);
        query.setParameter("userId", userId);
        final List<PaymentProofOrm> rows = query.getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<BookingOrm> refusePayment(int bookingId, String message, LocalDateTime refuseTime) {
        try {
            final int rowsUpdated = entityManager
                    .createNativeQuery(
                            "UPDATE payment_proof SET refuse_msg = :message, refused_at = :time WHERE booking_id = :id")
                    .setParameter("message", message)
                    .setParameter("time", refuseTime)
                    .setParameter("id", bookingId)
                    .executeUpdate();
            if (rowsUpdated <= 0) {
                return Optional.empty();
            }
            return findById(bookingId);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override

    public void finalizeBookingsBefore(LocalDateTime maxEndTime) {
        entityManager
                .createQuery("UPDATE BookingOrm b SET b.status = :status WHERE b.id IN ("
                        + "SELECT b2.id FROM BookingOrm b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
                        + "WHERE b2.end < :endTime AND b2.status = :confirmed AND h.id <> b2.guest.id)")
                .setParameter("status", BookingStatusEnumOrm.FINISHED)
                .setParameter("endTime", maxEndTime)
                .setParameter("confirmed", BookingStatusEnumOrm.CONFIRMED)
                .executeUpdate();
    }

    @Override

    public void expireBookingsBefore(LocalDateTime minStartTime) {
        entityManager
                .createQuery(
                        "UPDATE BookingOrm b SET b.status = :status WHERE b.start < :startTime AND b.status NOT IN :excluded")
                .setParameter("status", BookingStatusEnumOrm.CANCELLED)
                .setParameter("startTime", minStartTime)
                .setParameter("excluded", NON_AUTO_CANCEL_STATES)
                .executeUpdate();
    }

    private long countMatching(final int userId, final boolean isHost,
            final String searchQuery, final LocalDate date, final BookingStatusEnumOrm status) {
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder hql = new StringBuilder("SELECT COUNT(b) FROM BookingOrm b INNER JOIN b.version ");
        appendUserFilters(hql, userId, isHost, searchQuery, date, status, params);
        final TypedQuery<Long> countQuery = entityManager.createQuery(hql.toString(), Long.class);
        bindParams(countQuery, params);
        return toLong(countQuery.getSingleResult());
    }

    private static void appendUserFilters(
            final StringBuilder hql,
            final int userId,
            final boolean isHost,
            final String searchQuery,
            final LocalDate date,
            final BookingStatusEnumOrm status,
            final Map<String, Object> params) {
        if (isHost) {
            hql.append("WHERE b.version.item.host.id = :userId AND b.guest IS NOT NULL AND b.guest.id <> :userId");
        } else {
            hql.append("WHERE b.guest IS NOT NULL AND b.guest.id = :userId AND b.version.item.host.id <> :userId");
        }
        params.put("userId", userId);
        appendSharedBookingSearchFilters(hql, searchQuery, date, status, params);
    }

    private static void appendSharedBookingSearchFilters(
            final StringBuilder hql, final String searchQuery,
            final LocalDate date, final BookingStatusEnumOrm status,
            final Map<String, Object> params) {
        if (hasText(searchQuery)) {
            hql.append(" AND LOWER(b.version.title) LIKE :searchQuery ESCAPE '!'");
            params.put("searchQuery", setupSearchQuery(searchQuery));
        }
        if (status != null) {
            hql.append(" AND b.status = :status");
            params.put("status", status);
        }
        if (date != null) {
            hql.append(" AND b.start < :dayEnd AND b.end > :dayStart");
            params.put("dayStart", date.atStartOfDay(ZoneOffset.UTC).toLocalDateTime());
            params.put("dayEnd", date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toLocalDateTime());
        }
    }

    private static long toLong(final Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static void bindParams(final javax.persistence.Query query, final Map<String, Object> params) {
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static String orderByBookings(final String sortBy) {
        if (sortBy == null) {
            return " ORDER BY b.createdAt DESC, b.id DESC";
        }
        return switch (sortBy) {
            case "oldest" -> " ORDER BY b.createdAt ASC, b.id ASC";
            case "start_asc" -> " ORDER BY b.start ASC, b.id ASC";
            case "start_desc" -> " ORDER BY b.start DESC, b.id DESC";
            case "newest" -> " ORDER BY b.createdAt DESC, b.id DESC";
            default -> " ORDER BY b.createdAt DESC, b.id DESC";
        };
    }

    private static int resolvePage(final Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int resolvePageSize(final Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize == 6 || pageSize == 12 || pageSize == 18) {
            return pageSize;
        }
        return DEFAULT_PAGE_SIZE;
    }

    private static String setupSearchQuery(final String searchQuery) {
        final String queryWithWildcards = searchQuery
                .trim()
                .toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replaceAll("\\s+", "%");
        return "%" + queryWithWildcards + "%";
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
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
