package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class NewBookingJpaDao {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private static final String NATIVE_JOIN =
            "booking b JOIN version v ON b.version_id = v.id JOIN item i ON v.item_id = i.id ";

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

    private boolean wouldCollide(final Booking booking) {
        var query = em.createQuery(
                "SELECT COUNT(b) > 0 FROM Booking b WHERE b.start <= :requestedEnd AND :requestedStart <= b.end",
                Boolean.class);
        query.setParameter(":requestedStart", booking.getStart()).setParameter(":requestedEnd", booking.getEnd());
        return query.getSingleResult();
    }

    public boolean canInsertBooking(final Booking booking) {
        return !wouldCollide(booking);
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
        long totalCount = countBookings(query);

        // Get IDs of the bookings that will be returned in THIS page

        final Map<String, Object> params = new HashMap<>();

        StringBuilder sql = new StringBuilder("SELECT DISTINCT b.id FROM ");
        sql.append(NATIVE_JOIN);
        sql.append(getFilter(query, params)).append(nativeOrderBy(query));

        var nativeQuery = em.createNativeQuery(sql.toString());
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        final int pageSize = resolvePageSize(query);
        final int page = resolvePage(query);
        Paging.apply(nativeQuery, page, pageSize);
        final List<Integer> ids = Paging.toIntegerIds(nativeQuery.getResultList());

        if (ids.isEmpty()) {
            return new BookingSearchResult(List.of(), totalCount);
        }

        // Fetch the rest of the data

        String jpql =
                "SELECT DISTINCT b FROM Booking b JOIN FETCH b.guest JOIN FETCH b.version v JOIN FETCH v.item i JOIN FETCH i.host WHERE b.id IN :ids"
                        + jpqlOrderBy(query);
        var dataQuery = em.createQuery(jpql, Booking.class).setParameter("ids", ids);

        List<Booking> results = dataQuery.getResultList();

        return new BookingSearchResult(results, totalCount);
    }

    // Using b = booking, v = b.version, i = b.version.item
    private static String getFilter(BookingQueryModel query, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();

        if (query.isAsHost()) {
            // Host query, host must be the caller and not be the guest
            clauses.add("i.host_id = :callerId AND (b.guest_id IS NULL OR b.guest_id <> :callerId)");
        } else {
            // Guest query, guest must be the caller and not the host
            clauses.add("b.guest_id = :callerId AND (i.host_id IS NULL OR i.host_id <> :callerId)");
        }
        params.put("callerId", query.getCallerId());

        if (hasText(query.getSearchQuery())) {
            clauses.add("v.title ILIKE :searchQuery ESCAPE '!'");
            params.put("searchQuery", setupSearchQuery(query.getSearchQuery()));
        }

        if (query.getDate() != null) {
            clauses.add("b.end > :dayStart AND b.start < :dayEnd");

            var dayStart = query.getDate().atStartOfDay(ZoneOffset.UTC).toLocalDateTime();
            params.put("dayStart", dayStart);
            params.put("dayEnd", dayStart.plusDays(1));
        }

        if (query.getStatus() != null) {
            clauses.add("b.status = CAST(:status AS booking_status_enum)");
            params.put("status", query.getStatus());
        }

        if (!clauses.isEmpty()) {
            return "WHERE " + String.join(" AND ", clauses);
        }
        return "";
    }

    private long countBookings(final BookingQueryModel query) {
        final Map<String, Object> params = new HashMap<>();

        StringBuilder sql = new StringBuilder("SELECT DISTINCT b.id FROM ");
        sql.append(NATIVE_JOIN);
        sql.append(getFilter(query, params)).append(nativeOrderBy(query));

        var countQuery = em.createNativeQuery(sql.toString());
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
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

    private static int resolvePage(final BookingQueryModel query) {
        if (query == null) {
            return Paging.DEFAULT_PAGE;
        }
        return Paging.resolvePage(query.getPage());
    }

    private static int resolvePageSize(final BookingQueryModel query) {
        if (query == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Paging.resolvePageSize(query.getPageSize(), DEFAULT_PAGE_SIZE, 6, 12, 18);
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
