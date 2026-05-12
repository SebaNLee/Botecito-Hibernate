package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchModel;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingOrm;
import ar.edu.itba.paw.persistence.orm.entities.BookingStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.PaymentProofOrm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class BookingHibernateDao implements BookingDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    /**
     * Minimum clear time between adjacent blocking bookings on the same version
     * (minutes).
     */
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

    /** Booking states a host can actively set an incoming booking to */
    private static final EnumSet<BookingStatus> HOST_STATUS_CHANGES =
            EnumSet.of(BookingStatus.ACCEPTED, BookingStatus.REJECTED, BookingStatus.REFUSED, BookingStatus.CONFIRMED);

    /** Booking states a guest can actively set an outgoing booking to */
    private static final EnumSet<BookingStatus> GUEST_STATUS_CHANGES =
            EnumSet.of(BookingStatus.PAID, BookingStatus.CANCELLED);

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
            // Minimum bookable duration is 120 minutes (aligned with timeRangePicker
            // default and JS rules).
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
    public BookingSearchResult searchOutcomingBookings(final OutcomingSearch outcoming) {
        if (outcoming == null) {
            return new BookingSearchResult(List.of(), 0L);
        }
        final BookingSearchModel criteria =
                outcoming.getSearch() != null ? outcoming.getSearch() : new BookingSearchModel();
        final Map<String, Object> params = new HashMap<>();
        final int pageSize = resolvePageSize(criteria);
        final int offset = (resolvePage(criteria) - 1) * pageSize;

        final StringBuilder hql =
                new StringBuilder("SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest INNER JOIN FETCH b.version ");
        appendOutcomingGuestFilters(hql, outcoming.getGuestId(), criteria, params);
        hql.append(orderByBookings(criteria));

        final TypedQuery<BookingOrm> query = entityManager.createQuery(hql.toString(), BookingOrm.class);
        bindParams(query, params);
        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        final List<BookingOrm> rows = query.getResultList();
        final long total = countMatchingOutcoming(outcoming.getGuestId(), criteria);
        if (!rows.isEmpty()) {
            return new BookingSearchResult(
                    rows.stream().map(BookingHibernateDao::toBooking).toList(), total);
        }
        return new BookingSearchResult(List.of(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSearchResult searchIncomingBookings(final IncomingSearch incoming) {
        if (incoming == null) {
            return new BookingSearchResult(List.of(), 0L);
        }
        final BookingSearchModel criteria =
                incoming.getSearch() != null ? incoming.getSearch() : new BookingSearchModel();
        final Map<String, Object> params = new HashMap<>();
        final int pageSize = resolvePageSize(criteria);
        final int offset = (resolvePage(criteria) - 1) * pageSize;

        final StringBuilder hql =
                new StringBuilder("SELECT b FROM BookingOrm b LEFT JOIN FETCH b.guest INNER JOIN FETCH b.version ");
        appendIncomingHostFilters(hql, incoming.getHostId(), criteria, params);
        hql.append(orderByBookings(criteria));

        final TypedQuery<BookingOrm> query = entityManager.createQuery(hql.toString(), BookingOrm.class);
        bindParams(query, params);
        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        final List<BookingOrm> rows = query.getResultList();
        final long total = countMatchingIncoming(incoming.getHostId(), criteria);
        if (!rows.isEmpty()) {
            return new BookingSearchResult(
                    rows.stream().map(BookingHibernateDao::toBooking).toList(), total);
        }
        return new BookingSearchResult(List.of(), total);
    }

    // TODO: throw custom exception when these fail
    @Override
    public void updateStatusIncoming(int id, int callerId, BookingStatus status) {
        if (status == null) return;
        if (!HOST_STATUS_CHANGES.contains(status)) return;
        entityManager
                .createQuery(
                        "UPDATE BookingOrm b SET b.status = :newStatus WHERE b.id = :bookingId AND b.version.item.host.id = :caller")
                .setParameter("newStatus", BookingStatusEnumOrm.valueOf(status.name()))
                .setParameter("bookingId", id)
                .setParameter("caller", callerId)
                .executeUpdate();
    }

    @Override
    public void updateStatusOutgoing(int id, int callerId, BookingStatus status) {
        if (status == null) return;
        if (!GUEST_STATUS_CHANGES.contains(status)) return;
        entityManager
                .createQuery(
                        "UPDATE BookingOrm b SET b.status = :newStatus WHERE b.id = :bookingId AND b.guest.id = :caller")
                .setParameter("newStatus", BookingStatusEnumOrm.valueOf(status.name()))
                .setParameter("bookingId", id)
                .setParameter("caller", callerId)
                .executeUpdate();
    }

    @Override
    public void uploadPayment(PaymentProof p) {
        if (p == null) return;

        final BookingOrm booking = entityManager.getReference(BookingOrm.class, p.getBookingId());

        final PaymentProofOrm payment = PaymentProofOrm.builder()
                .booking(booking)
                .filename(p.getFileName())
                .contentType(p.getContentType())
                .fileData(p.getFileData())
                .createdAt(p.getCreatedAt())
                .refuseMsg(p.getRefuseMsg())
                .refusedAt(p.getRefusedAt())
                .replyMsg(p.getReplyMsg())
                .repliedAt(p.getRepliedAt())
                .build();

        entityManager.merge(payment);
    }

    @Override
    public void refusePayment(int bookingId, String message, LocalDateTime refuseTime) {
        entityManager
                .createQuery(
                        "UPDATE PaymentProofOrm p SET p.refuseMsg = :message,  p.refusedAt = :time WHERE p.booking.id = :id")
                .setParameter("message", message)
                .setParameter("time", refuseTime)
                .setParameter("id", bookingId)
                .executeUpdate();
    }

    @Override
    public void finalizeBookingsBefore(LocalDateTime maxEndTime) {
        entityManager
                .createQuery("UPDATE BookingOrm b SET b.status = :status WHERE b.end < :endTime")
                .setParameter("status", BookingStatus.FINISHED)
                .setParameter("endTime", maxEndTime)
                .executeUpdate();
    }

    @Override
    public void expireBookingsAfter(LocalDateTime minStartTime) {
        entityManager
                .createQuery("UPDATE BookingOrm b SET b.status = :status WHERE b.start > :startTime")
                .setParameter("status", BookingStatus.CANCELLED)
                .setParameter("startTime", minStartTime)
                .executeUpdate();
    }

    private long countMatchingOutcoming(final int guestId, final BookingSearchModel criteria) {
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder hql = new StringBuilder("SELECT COUNT(b) FROM BookingOrm b INNER JOIN b.version ");
        appendOutcomingGuestFilters(hql, guestId, criteria, params);
        final TypedQuery<Long> countQuery = entityManager.createQuery(hql.toString(), Long.class);
        bindParams(countQuery, params);
        return toLong(countQuery.getSingleResult());
    }

    private long countMatchingIncoming(final int hostId, final BookingSearchModel criteria) {
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder hql = new StringBuilder("SELECT COUNT(b) FROM BookingOrm b INNER JOIN b.version ");
        appendIncomingHostFilters(hql, hostId, criteria, params);
        final TypedQuery<Long> countQuery = entityManager.createQuery(hql.toString(), Long.class);
        bindParams(countQuery, params);
        return toLong(countQuery.getSingleResult());
    }

    private static void appendOutcomingGuestFilters(
            final StringBuilder hql,
            final int guestId,
            final BookingSearchModel search,
            final Map<String, Object> params) {
        hql.append("WHERE b.guest IS NOT NULL AND b.guest.id = :guestId");
        params.put("guestId", guestId);
        appendSharedBookingSearchFilters(hql, search, params);
    }

    private static void appendIncomingHostFilters(
            final StringBuilder hql,
            final int hostId,
            final BookingSearchModel search,
            final Map<String, Object> params) {
        hql.append("WHERE b.version.item.host.id = :hostId");
        params.put("hostId", hostId);
        appendSharedBookingSearchFilters(hql, search, params);
    }

    private static void appendSharedBookingSearchFilters(
            final StringBuilder hql, final BookingSearchModel search, final Map<String, Object> params) {
        if (hasText(search.getSearchQuery())) {
            hql.append(" AND LOWER(b.version.title) LIKE :searchQuery ESCAPE '!'");
            params.put("searchQuery", setupSearchQuery(search.getSearchQuery()));
        }
        if (search.getStatus() != null) {
            hql.append(" AND b.status = :status");
            params.put("status", BookingStatusEnumOrm.valueOf(search.getStatus().name()));
        }
        if (search.getDate() != null) {
            final LocalDate day = search.getDate();
            hql.append(" AND b.start < :dayEnd AND b.end > :dayStart");
            params.put("dayStart", day.atStartOfDay(ZoneOffset.UTC).toLocalDateTime());
            params.put("dayEnd", day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toLocalDateTime());
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

    private static String orderByBookings(final BookingSearchModel search) {
        if (search.getSortBy() == null) {
            return " ORDER BY b.createdAt DESC, b.id DESC";
        }
        return switch (search.getSortBy()) {
            case "oldest" -> " ORDER BY b.createdAt ASC, b.id ASC";
            case "start_asc" -> " ORDER BY b.start ASC, b.id ASC";
            case "start_desc" -> " ORDER BY b.start DESC, b.id DESC";
            case "newest" -> " ORDER BY b.createdAt DESC, b.id DESC";
            default -> " ORDER BY b.createdAt DESC, b.id DESC";
        };
    }

    private static int resolvePage(final BookingSearchModel search) {
        if (search.getPage() == null || search.getPage() < 1) {
            return DEFAULT_PAGE;
        }
        return search.getPage();
    }

    private static int resolvePageSize(final BookingSearchModel search) {
        if (search.getPageSize() == null) {
            return DEFAULT_PAGE_SIZE;
        }
        final int pageSize = search.getPageSize();
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

    private static Booking toBooking(final BookingOrm orm) {
        final Booking b = new Booking();
        b.setId(orm.getId());
        b.setVersionId(orm.getVersion().getId());
        b.setVersionTitle(orm.getVersion().getTitle());
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
