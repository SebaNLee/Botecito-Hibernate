package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
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
public class BookingJpaDao implements BookingDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    /**
     * Minimum clear time between adjacent blocking bookings on the same version
     * (minutes).
     */
    private static final int MIN_CLEARANCE_BETWEEN_BOOKINGS_MINUTES = 30;

    private static final String HQL_BOOKINGS_FOR_VERSION =
            "SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof WHERE b.version.id = :versionId ORDER BY b.start ASC";

    private static final String HQL_BOOKING_MAIL_FETCH =
            "SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof "
                    + "INNER JOIN FETCH b.version v INNER JOIN FETCH v.item i LEFT JOIN FETCH i.host ";

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

    private static final String HQL_VERSION_TIMEZONE = "SELECT v.timezone FROM Version v WHERE v.id = :versionId";

    private static final String HQL_VERSION_OWNER_ID = "SELECT v.item.host.id FROM Version v WHERE v.id = :versionId";

    private static final String HQL_AVAILABILITIES_FOR_VERSION =
            "SELECT a FROM Availability a WHERE a.version.id = :versionId ORDER BY a.weekday, a.startTime, a.id";

    private static final EnumSet<BookingStatusEnum> NON_AUTO_CANCEL_STATES = EnumSet.of(
            BookingStatusEnum.CONFIRMED,
            BookingStatusEnum.CANCELLED,
            BookingStatusEnum.FINISHED,
            BookingStatusEnum.REJECTED);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Integer> insertBooking(
            final int versionId,
            final int guestId,
            final LocalDateTime utcStart,
            final LocalDateTime utcEnd,
            final BookingStatusEnum status,
            final String msg) {
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
    public List<Availability> listAvailabilitiesForVersion(final int versionId) {
        return entityManager
                .createQuery(HQL_AVAILABILITIES_FOR_VERSION, Availability.class)
                .setParameter("versionId", versionId)
                .getResultList();
    }

    @Override
    public boolean deleteOwnerSelfBlock(final int bookingId, final int ownerId) {
        return entityManager
                        .createQuery("DELETE FROM Booking b WHERE b.id = :id AND b.guest.id = :ownerId "
                                + "AND b.status IN :statuses")
                        .setParameter("id", bookingId)
                        .setParameter("ownerId", ownerId)
                        .setParameter("statuses", EnumSet.of(BookingStatusEnum.CONFIRMED, BookingStatusEnum.ACCEPTED))
                        .executeUpdate()
                > 0;
    }

    @Override
    public List<Booking> getBookingsForVersion(final int versionId) {
        return entityManager
                .createQuery(HQL_BOOKINGS_FOR_VERSION, Booking.class)
                .setParameter("versionId", versionId)
                .getResultList();
    }

    @Override
    public Optional<Booking> findById(final int bookingId) {
        final List<Booking> rows = entityManager
                .createQuery(HQL_BOOKING_MAIL_FETCH + "WHERE b.id = :bookingId", Booking.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Integer> findOwnerIdForBookingId(final int bookingId) {
        try {
            final Integer ownerId = entityManager
                    .createQuery("SELECT b.version.item.host.id FROM Booking b WHERE b.id = :id", Integer.class)
                    .setParameter("id", bookingId)
                    .getSingleResult();
            return Optional.ofNullable(ownerId);
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public BookingSearchResult searchBookings(
            final int userId,
            final boolean asHost,
            final String searchQuery,
            final LocalDate date,
            final BookingStatusEnum status,
            final Integer page,
            final Integer pageSize,
            final String sortBy) {
        final Map<String, Object> params = new HashMap<>();
        final int resolvedPageSize = resolvePageSize(pageSize);
        final int offset = (resolvePage(page) - 1) * resolvedPageSize;

        final StringBuilder hql = new StringBuilder(
                "SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof INNER JOIN FETCH b.version v "
                        + "INNER JOIN FETCH v.item i LEFT JOIN FETCH i.host h ");
        appendUserFilters(hql, userId, asHost, searchQuery, date, status, params);
        hql.append(orderByBookings(sortBy));

        final TypedQuery<Booking> query = entityManager.createQuery(hql.toString(), Booking.class);
        bindParams(query, params);
        query.setFirstResult(offset);
        query.setMaxResults(resolvedPageSize);

        final List<Booking> rows = query.getResultList();
        final long total = countMatching(userId, asHost, searchQuery, date, status);
        return new BookingSearchResult(rows, total);
    }

    @Override
    public boolean startsAfter(int bookingId, LocalDateTime requestedStart) {
        return entityManager
                .createQuery(
                        "SELECT COUNT(b) > 0 FROM Booking b WHERE b.id = :id AND b.start > :requested", Boolean.class)
                .setParameter("id", bookingId)
                .setParameter("requested", requestedStart)
                .getSingleResult();
    }

    @Override
    public Optional<Booking> updateStatusIncoming(int id, int callerId, BookingStatusEnum status) {
        try {
            int rowsUpdated = entityManager
                    .createQuery("UPDATE Booking b SET b.status = :newStatus WHERE b.id IN ("
                            + "SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
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
    public Optional<Booking> updateStatusOutgoing(int id, int callerId, BookingStatusEnum status) {
        try {
            int rowsUpdated = entityManager
                    .createQuery(
                            "UPDATE Booking b SET b.status = :newStatus WHERE b.id IN ("
                                    + "SELECT b2.id FROM Booking b2 INNER JOIN b2.guest g WHERE b2.id = :bookingId AND g.id = :caller)")
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
    public Optional<PaymentProof> uploadPayment(final PaymentProof proof) {
        if (proof == null) return Optional.empty();

        try {
            entityManager.merge(proof);
            return Optional.of(proof);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PaymentProof> findPaymentProofForParticipant(final int bookingId, final int userId) {
        final TypedQuery<PaymentProof> query = entityManager.createQuery(
                "SELECT p FROM PaymentProof p INNER JOIN p.booking b LEFT JOIN b.guest g "
                        + "INNER JOIN b.version v INNER JOIN v.item i LEFT JOIN i.host h "
                        + "WHERE b.id = :bookingId AND (g.id = :userId OR h.id = :userId)",
                PaymentProof.class);
        query.setParameter("bookingId", bookingId);
        query.setParameter("userId", userId);
        final List<PaymentProof> rows = query.getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<Booking> refusePayment(int bookingId, String message, LocalDateTime refuseTime) {
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
                .createQuery("UPDATE Booking b SET b.status = :status WHERE b.id IN ("
                        + "SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
                        + "WHERE b2.end < :endTime AND b2.status = :confirmed AND h.id <> b2.guest.id)")
                .setParameter("status", BookingStatusEnum.FINISHED)
                .setParameter("endTime", maxEndTime)
                .setParameter("confirmed", BookingStatusEnum.CONFIRMED)
                .executeUpdate();
    }

    @Override
    public void expireBookingsBefore(LocalDateTime minStartTime) {
        entityManager
                .createQuery(
                        "UPDATE Booking b SET b.status = :status WHERE b.start < :startTime AND b.status NOT IN :excluded")
                .setParameter("status", BookingStatusEnum.CANCELLED)
                .setParameter("startTime", minStartTime)
                .setParameter("excluded", NON_AUTO_CANCEL_STATES)
                .executeUpdate();
    }

    @Override
    public List<Booking> findBookingsToFinalizeBefore(final LocalDateTime maxEndTime) {
        return entityManager
                .createQuery(
                        HQL_BOOKING_MAIL_FETCH
                                + "WHERE b.end < :endTime AND b.status = :confirmed AND i.host.id <> b.guest.id",
                        Booking.class)
                .setParameter("endTime", maxEndTime)
                .setParameter("confirmed", BookingStatusEnum.CONFIRMED)
                .getResultList();
    }

    @Override
    public List<Booking> findBookingsToExpireBefore(final LocalDateTime minStartTime) {
        return entityManager
                .createQuery(
                        HQL_BOOKING_MAIL_FETCH + "WHERE b.start < :startTime AND b.status NOT IN :excluded",
                        Booking.class)
                .setParameter("startTime", minStartTime)
                .setParameter("excluded", NON_AUTO_CANCEL_STATES)
                .getResultList();
    }

    private long countMatching(
            final int userId,
            final boolean isHost,
            final String searchQuery,
            final LocalDate date,
            final BookingStatusEnum status) {
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder hql = new StringBuilder("SELECT COUNT(b) FROM Booking b INNER JOIN b.version ");
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
            final BookingStatusEnum status,
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
            final StringBuilder hql,
            final String searchQuery,
            final LocalDate date,
            final BookingStatusEnum status,
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
}
