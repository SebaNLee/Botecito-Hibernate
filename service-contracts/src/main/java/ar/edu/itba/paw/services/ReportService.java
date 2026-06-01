package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ReportSearchResult;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;

public interface ReportService {

    void createReport(int itemId, int senderId, ReportEnum reason, String description);

    Report findById(int reportId);

    boolean hasReported(int senderId, int itemId);

    ReportSearchResult searchReports(int page, int pageSize, String sortBy);

    void dismissReport(int reportId);

    void deletePublicationForReport(int reportId);
}
