package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Item;
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
public class BookingJpaDao implements BookingDao {

    private static final String NATIVE_JOIN =
            "booking b JOIN version v ON b.version_id = v.id JOIN item i ON v.item_id = i.id ";

    private static final String HQL_BOOKING_MAIL_FETCH =
            "SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof "
                    + "INNER JOIN FETCH b.version v INNER JOIN FETCH v.item i LEFT JOIN FETCH i.host ";

    private static final EnumSet<BookingStatusEnum> NON_AUTO_CANCEL_STATES = EnumSet.of(
            BookingStatusEnum.CONFIRMED,
            BookingStatusEnum.CANCELLED,
            BookingStatusEnum.FINISHED,
            BookingStatusEnum.REJECTED);

    @PersistenceContext
    private EntityManager em;

    @Override
    public void insertBooking(final Booking booking) {
        em.persist(booking);
    }

    @Override
    public boolean itemHasBookings(final Item item) {
        final Integer versionId = item.getLatestVersion().getId();
        if (versionId == null) {
            return false;
        }
        final Long count = em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.version.id = :versionId", Long.class)
                .setParameter("versionId", versionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    private boolean wouldCollide(final Booking booking, final EnumSet<BookingStatusEnum> blockingStates) {
        String jpql =
                "SELECT COUNT(b) > 0 FROM Booking b WHERE b.version.item = :item AND b.start <= :requestedEnd AND :requestedStart <= b.end AND b.status IN :blockingStates";

        var query = em.createQuery(jpql, Boolean.class);
        query.setParameter("item", booking.getVersion().getItem())
                .setParameter("requestedStart", booking.getStart())
                .setParameter("requestedEnd", booking.getEnd())
                .setParameter("blockingStates", blockingStates);
        return query.getSingleResult();
    }

    @Override
    public boolean canInsertBooking(final Booking booking, EnumSet<BookingStatusEnum> blockingStates) {
        return !wouldCollide(booking, blockingStates);
    }

    @Override
    public boolean canUpdate(
            final Booking booking, final int excludeBookingId, final EnumSet<BookingStatusEnum> blockingStates) {
        String jpql = "SELECT COUNT(b) > 0 FROM Booking b WHERE b.version.item = :item AND b.id <> :excludeId"
                + " AND b.start <= :requestedEnd AND :requestedStart <= b.end"
                + " AND b.status IN :blockingStates";

        var query = em.createQuery(jpql, Boolean.class);
        query.setParameter("item", booking.getVersion().getItem())
                .setParameter("excludeId", excludeBookingId)
                .setParameter("requestedStart", booking.getStart())
                .setParameter("requestedEnd", booking.getEnd())
                .setParameter("blockingStates", blockingStates);
        return !query.getSingleResult();
    }

    @Override
    public boolean deleteBooking(final int id) {
        final Booking booking = em.find(Booking.class, id);
        if (booking == null) return false;
        em.remove(booking);
        return true;
    }

    @Override
    public Optional<Booking> findById(final int bookingId) {
        final List<Booking> rows = em.createQuery(HQL_BOOKING_MAIL_FETCH + "WHERE b.id = :bookingId", Booking.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Booking> getUpcomingBookings(Item item, LocalDateTime minDate, LocalDateTime maxDate) {
        var query = em.createQuery(
                        "SELECT b FROM Booking b JOIN FETCH b.version v JOIN FETCH v.item i WHERE i = :item AND b.start >= :minDate AND b.end <= :maxDate",
                        Booking.class)
                .setParameter("item", item)
                .setParameter("minDate", minDate)
                .setParameter("maxDate", maxDate);
        return query.getResultList();
    }

    @Override
    public void uploadPayment(final PaymentProof proof) {
        em.persist(proof);
    }

    @Override
    public SearchResult<Booking> searchBookings(final BookingQueryModel query) {
        long totalCount = countBookings(query);

        final Map<String, Object> params = new HashMap<>();

        StringBuilder sql = new StringBuilder("SELECT b.id FROM ");
        sql.append(NATIVE_JOIN);
        sql.append(getFilter(query, params)).append(nativeOrderBy(query));

        var nativeQuery = em.createNativeQuery(sql.toString());
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        Paging.apply(nativeQuery, query.getPage(), query.getPageSize());
        final List<Integer> ids = Paging.toIntegerIds(nativeQuery.getResultList());

        if (ids.isEmpty()) {
            return new SearchResult<>(List.of(), totalCount);
        }

        String jpql =
                "SELECT DISTINCT b FROM Booking b JOIN FETCH b.guest LEFT JOIN FETCH b.paymentProof JOIN FETCH b.version v JOIN FETCH v.item i JOIN FETCH i.host LEFT JOIN FETCH v.media WHERE b.id IN :ids"
                        + jpqlOrderBy(query);
        var dataQuery = em.createQuery(jpql, Booking.class).setParameter("ids", ids);

        List<Booking> results = dataQuery.getResultList();

        return new SearchResult<>(results, totalCount);
    }

    private static String getFilter(BookingQueryModel query, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();

        if (query.isAsHost()) {
            clauses.add("i.host_id = :callerId AND (b.guest_id IS NULL OR b.guest_id <> :callerId)");
        } else {
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
            params.put("status", query.getStatus().name());
        }

        if (!clauses.isEmpty()) {
            return "WHERE " + String.join(" AND ", clauses);
        }
        return "";
    }

    private long countBookings(final BookingQueryModel query) {
        final Map<String, Object> params = new HashMap<>();

        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT b.id) FROM ");
        sql.append(NATIVE_JOIN);
        sql.append(getFilter(query, params));

        var countQuery = em.createNativeQuery(sql.toString());
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    @Override
    public void finalizeBookingsBefore(LocalDateTime minEndTime) {
        em.createQuery("UPDATE Booking b SET b.status = :status WHERE b.id IN ("
                        + "SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i INNER JOIN i.host h "
                        + "WHERE b2.end < :endTime AND b2.status = :confirmed AND h.id <> b2.guest.id)")
                .setParameter("status", BookingStatusEnum.FINISHED)
                .setParameter("endTime", minEndTime)
                .setParameter("confirmed", BookingStatusEnum.CONFIRMED)
                .executeUpdate();
    }

    @Override
    public void expireBookingsBefore(LocalDateTime minStartTime, EnumSet<BookingStatusEnum> cancellableStates) {
        em.createQuery(
                        "UPDATE Booking b SET b.status = :status WHERE b.start < :startTime AND b.status IN :cancellable")
                .setParameter("status", BookingStatusEnum.CANCELLED)
                .setParameter("startTime", minStartTime)
                .setParameter("cancellable", cancellableStates)
                .executeUpdate();
    }

    @Override
    public void deleteSelfBlocksBefore(LocalDateTime minEndTime) {
        em.createQuery(
                        "DELETE FROM Booking b WHERE b.end < :endTime AND b.id IN (SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i WHERE b2.guest = i.host)")
                .setParameter("endTime", minEndTime)
                .executeUpdate();
    }

    @Override
    public List<Booking> findBookingsToFinalizeBefore(final LocalDateTime maxEndTime) {
        return em.createQuery(
                        HQL_BOOKING_MAIL_FETCH
                                + "WHERE b.end < :endTime AND b.status = :confirmed AND i.host.id <> b.guest.id",
                        Booking.class)
                .setParameter("endTime", maxEndTime)
                .setParameter("confirmed", BookingStatusEnum.CONFIRMED)
                .getResultList();
    }

    @Override
    public List<Booking> findBookingsToExpireBefore(final LocalDateTime minStartTime) {
        return em.createQuery(
                        HQL_BOOKING_MAIL_FETCH + "WHERE b.start < :startTime AND b.status NOT IN :excluded",
                        Booking.class)
                .setParameter("startTime", minStartTime)
                .setParameter("excluded", NON_AUTO_CANCEL_STATES)
                .getResultList();
    }

    @Override
    public void deleteAllSelfBlocks(Item item) {
        em.createQuery(
                        "DELETE FROM Booking b WHERE b.id IN (SELECT b2.id FROM Booking b2 INNER JOIN b2.version v INNER JOIN v.item i WHERE b2.guest = i.host AND i = :item)")
                .setParameter("item", item)
                .executeUpdate();
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
        return " ORDER BY " + jpqlOrderByClause(query) + " ";
    }

    private static String jpqlOrderByClause(final BookingQueryModel query) {
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
