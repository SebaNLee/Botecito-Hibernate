package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Report;
import java.util.Optional;

public interface ReportDao {

    Optional<Report> findById(int id);

    boolean hasReported(int senderId, int itemId);

    void create(Report report);

    void deleteById(int id);

    void deleteAllByItemId(int itemId);

    SearchResult<Report> searchReports(final int page, final int pageSize, final String sortBy);

    SearchResult<Report> searchReports(
            final int page, final int pageSize, final String sortBy, final Item reportedItem);

    long countReports(final Item reportedItem);
}
