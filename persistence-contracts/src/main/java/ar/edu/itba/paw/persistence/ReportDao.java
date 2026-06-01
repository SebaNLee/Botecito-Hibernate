package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Report;
import java.util.List;
import java.util.Optional;

public interface ReportDao {

    Optional<Report> findById(int id);

    boolean hasReported(int senderId, int itemId);

    void create(Report report);

    void deleteById(int id);

    void deleteAllByItemId(int itemId);

    int countAll();

    List<Report> findAll(int page, int pageSize, boolean newestFirst);
}
