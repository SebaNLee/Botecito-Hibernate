package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ReportSearchResult;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class ReportJpaDao implements ReportDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Report> findById(final int id) {
        return Optional.ofNullable(em.find(Report.class, id));
    }

    @Override
    public boolean hasReported(final int senderId, final int itemId) {
        return em.createQuery(
                        "SELECT COUNT(r) > 0 FROM Report r WHERE r.sender.id = :senderId AND r.item.id = :itemId",
                        Boolean.class)
                .setParameter("senderId", senderId)
                .setParameter("itemId", itemId)
                .getSingleResult();
    }

    @Override
    public void create(Report report) {
        em.persist(report);
    }

    @Override
    public void deleteById(final int id) {
        em.createQuery("DELETE FROM Report r WHERE r.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void deleteAllByItemId(final int itemId) {
        em.createQuery("DELETE FROM Report r WHERE r.item.id = :itemId")
                .setParameter("itemId", itemId)
                .executeUpdate();
    }

    @Override
    public ReportSearchResult searchReports(final int page, final int pageSize, final String sortBy) {
        final long totalCount = countAll();

        var nativeQuery = em.createNativeQuery("SELECT r.id FROM reports r " + nativeOrderBy(sortBy));
        Paging.apply(nativeQuery, page, pageSize);
        final List<Integer> ids = Paging.toIntegerIds(nativeQuery.getResultList());

        if (ids.isEmpty()) return new ReportSearchResult(List.of(), totalCount);

        var query = em.createQuery(
                "SELECT DISTINCT r FROM Report r JOIN FETCH r.sender JOIN FETCH r.item WHERE r.id IN :ids "
                        + jpqlOrderBy(sortBy),
                Report.class);
        query.setParameter("ids", ids);

        return new ReportSearchResult(query.getResultList(), totalCount);
    }

    private long countAll() {
        return ((Number) em.createNativeQuery("SELECT COUNT(r) FROM reports r").getSingleResult()).longValue();
    }

    private String nativeOrderBy(final String sortBy) {
        return "ORDER BY "
                + switch (sortBy) {
                    case "oldest" -> "r.created_at ASC, r.id ASC";
                    case "newest" -> "r.created_at DESC, r.id DESC";
                    default -> "r.created_at DESC, r.id DESC";
                };
    }

    private String jpqlOrderBy(final String sortBy) {
        return "ORDER BY "
                + switch (sortBy) {
                    case "oldest" -> "r.createdAt ASC, r.id ASC";
                    case "newest" -> "r.createdAt DESC, r.id DESC";
                    default -> "r.createdAt DESC, r.id DESC";
                };
    }
}
