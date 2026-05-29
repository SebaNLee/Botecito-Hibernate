package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;
import java.util.List;
import java.util.Optional;

public interface ReportDao {

    Optional<Report> findById(int id);

    Optional<Report> findBySenderAndItem(int senderId, int itemId);

    Report create(int senderId, int itemId, ReportEnum reason, String description);

    void deleteById(int id);

    void deleteAllByItemId(int itemId);

    int countAll();

    List<Report> findAll(int page, int pageSize, boolean newestFirst);
}
