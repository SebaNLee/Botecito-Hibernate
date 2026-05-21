package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
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
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class NewBookingJpaDao {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;

    // Mejor pedir por parametro el complemento de este set
    private static final EnumSet<BookingStatusEnum> NON_AUTO_CANCEL_STATES = EnumSet.of(
            BookingStatusEnum.CONFIRMED,
            BookingStatusEnum.CANCELLED,
            BookingStatusEnum.FINISHED,
            BookingStatusEnum.REJECTED);

    @PersistenceContext
    private EntityManager em;

    public void insertBooking(final Booking booking) {
        em.persist(booking);
    }

    public boolean deleteBooking(final int id) {
        final Booking booking = em.find(Booking.class, id);
        if (booking == null) return false;
        em.remove(booking);
        return true;
    }

    public Optional<Booking> findById(final int bookingId) {
        return Optional.ofNullable(em.find(Booking.class, bookingId));
    }

    public void uploadPayment(final PaymentProof proof) {
        em.persist(proof);
    }

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

        final TypedQuery<Booking> query = em.createQuery(hql.toString(), Booking.class);
        bindParams(query, params);
        query.setFirstResult(offset);
        query.setMaxResults(resolvedPageSize);

        final List<Booking> rows = query.getResultList();
        final long total = countMatching(userId, asHost, searchQuery, date, status);
        return new BookingSearchResult(rows, total);
    }

    public Optional<PaymentProof> findPaymentProofForParticipant(final int bookingId, final int userId) {
        final TypedQuery<PaymentProof> query = em.createQuery(
                "SELECT p FROM PaymentProof p INNER JOIN p.booking b LEFT JOIN b.guest g "
                        + "INNER JOIN b.version v INNER JOIN v.item i LEFT JOIN i.host h "
                        + "WHERE b.id = :bookingId AND (g.id = :userId OR h.id = :userId)",
                PaymentProof.class);
        query.setParameter("bookingId", bookingId);
        query.setParameter("userId", userId);
        final List<PaymentProof> rows = query.getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void finalizeBookingsBefore(LocalDateTime maxEndTime) {
        em.createQuery("UPDATE Booking b SET b.status = :status WHERE b.id IN ("
                        + "SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
                        + "WHERE b2.end < :endTime AND b2.status = :confirmed AND h.id <> b2.guest.id)")
                .setParameter("status", BookingStatusEnum.FINISHED)
                .setParameter("endTime", maxEndTime)
                .setParameter("confirmed", BookingStatusEnum.CONFIRMED)
                .executeUpdate();
    }

    public void expireBookingsBefore(LocalDateTime minStartTime) {
        em.createQuery(
                        "UPDATE Booking b SET b.status = :status WHERE b.start < :startTime AND b.status NOT IN :excluded")
                .setParameter("status", BookingStatusEnum.CANCELLED)
                .setParameter("startTime", minStartTime)
                .setParameter("excluded", NON_AUTO_CANCEL_STATES)
                .executeUpdate();
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
        final TypedQuery<Long> countQuery = em.createQuery(hql.toString(), Long.class);
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
