package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;

public interface ReportService {

    void createReport(int itemId, int senderId, ReportEnum reason, String description);

    Report findById(int reportId);

    boolean hasReported(int senderId, int itemId);

    PageModel<Report> findReportsForAdmin(int page, int pageSize, String sortBy);

    void dismissReport(int reportId, int adminUserId);

    void deletePublicationForReport(int reportId, int adminUserId);
}
