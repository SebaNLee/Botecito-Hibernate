package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ReportJpaDao implements ReportDao {

    private static final String ADMIN_FETCH_JPQL = "SELECT DISTINCT r FROM Report r "
            + "JOIN FETCH r.sender "
            + "JOIN FETCH r.item i "
            + "LEFT JOIN FETCH i.host "
            + "WHERE r.id = :id";

    private static final String ADMIN_LIST_JPQL = "SELECT DISTINCT r FROM Report r "
            + "JOIN FETCH r.sender "
            + "JOIN FETCH r.item i "
            + "LEFT JOIN FETCH i.host ";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Report> findById(final int id) {
        return entityManager
                .createQuery(ADMIN_FETCH_JPQL, Report.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(this::populateItemTitle);
    }

    @Override
    public Optional<Report> findBySenderAndItem(final int senderId, final int itemId) {
        return entityManager
                .createQuery(
                        "SELECT r FROM Report r WHERE r.sender.id = :senderId AND r.item.id = :itemId", Report.class)
                .setParameter("senderId", senderId)
                .setParameter("itemId", itemId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Report create(final int senderId, final int itemId, final ReportEnum reason, final String description) {
        final Report report = new Report();
        report.setSender(entityManager.getReference(Users.class, senderId));
        report.setItem(entityManager.getReference(Item.class, itemId));
        report.setReason(reason);
        report.setDescription(description);
        report.setCreatedAt(LocalDateTime.now());
        entityManager.persist(report);
        return report;
    }

    @Override
    public void deleteById(final int id) {
        entityManager
                .createQuery("DELETE FROM Report r WHERE r.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void deleteAllByItemId(final int itemId) {
        entityManager
                .createQuery("DELETE FROM Report r WHERE r.item.id = :itemId")
                .setParameter("itemId", itemId)
                .executeUpdate();
    }

    @Override
    public int countAll() {
        return ((Number) entityManager
                        .createQuery("SELECT COUNT(r) FROM Report r")
                        .getSingleResult())
                .intValue();
    }

    @Override
    public List<Report> findAll(final int page, final int pageSize, final boolean newestFirst) {
        final String order = newestFirst ? "DESC" : "ASC";
        final TypedQuery<Report> query = entityManager.createQuery(
                ADMIN_LIST_JPQL + "ORDER BY r.createdAt " + order + ", r.id " + order, Report.class);
        Paging.apply(query, page, pageSize);
        final List<Report> reports = query.getResultList();
        populateItemTitles(reports);
        return reports;
    }

    private Report populateItemTitle(final Report report) {
        if (report == null || report.getItem() == null) {
            return report;
        }
        final Version latest = report.getItem().getLatestVersion();
        if (latest != null) {
            report.setItemTitle(latest.getTitle());
        }
        return report;
    }

    private void populateItemTitles(final List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }
        final Map<Integer, String> titlesByItemId = loadLatestTitlesByItemId(reports);
        for (final Report report : reports) {
            if (report.getItem() == null || report.getItem().getId() == null) {
                continue;
            }
            final String title = titlesByItemId.get(report.getItem().getId());
            if (title != null) {
                report.setItemTitle(title);
            }
        }
    }

    private Map<Integer, String> loadLatestTitlesByItemId(final List<Report> reports) {
        final List<Integer> itemIds = reports.stream()
                .map(Report::getItem)
                .filter(item -> item != null && item.getId() != null)
                .map(Item::getId)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = entityManager
                .createNativeQuery("SELECT v.item_id, v.title FROM version v "
                        + "WHERE v.item_id IN (:itemIds) "
                        + "AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = v.item_id)")
                .setParameter("itemIds", itemIds)
                .getResultList();
        final Map<Integer, String> titles = new HashMap<>();
        for (final Object[] row : rows) {
            titles.put(((Number) row[0]).intValue(), (String) row[1]);
        }
        return titles;
    }
}
