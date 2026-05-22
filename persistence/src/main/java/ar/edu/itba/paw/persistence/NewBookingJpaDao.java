package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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

    public BookingSearchResult searchBookings(final BookingQueryModel query) {
        // final Map<String, Object> params = new HashMap<>();
        // TODO
        return null;
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

    private static long toLong(final Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
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

    private static String nativeOrderBy(final BookingQueryModel query) {
        return " ORDER BY " + nativeOrderByClause(query) + " ";
    }

    private static String nativeOrderByClause(final BookingQueryModel query) {
        return switch (resolveSortBy(query)) {
            case "oldest" -> " b.created_at ASC, b.id ASC";
            case "start_asc" -> " b.start ASC, b.id ASC";
            case "start_desc" -> " b.start DESC, b.id DESC";
            default -> " b.created_at DESC, b.id DESC";
        };
    }

    private static String jpqlOrderBy(final BookingQueryModel query) {
        return switch (resolveSortBy(query)) {
            case "oldest" -> " b.createdAt ASC, b.id ASC";
            case "start_asc" -> " b.start ASC, b.id ASC";
            case "start_desc" -> " b.start DESC, b.id DESC";
            default -> " b.createdAt DESC, b.id DESC";
        };
    }

    private static String resolveSortBy(final BookingQueryModel query) {
        if (query == null || query.getSortBy() == null) {
            return null;
        }
        return query.getSortBy();
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
