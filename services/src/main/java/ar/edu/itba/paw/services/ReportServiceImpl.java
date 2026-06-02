package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.models.exceptions.ReportAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ReportNotFoundException;
import ar.edu.itba.paw.models.exceptions.UserNotFoundException;
import ar.edu.itba.paw.persistence.ReportDao;
import ar.edu.itba.paw.services.util.DateTimeUtils;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportDao reportDao;
    private final ItemService itemService;
    private final UserService userService;
    private final ManageItemService manageItemService;
    private final MailService mailService;

    @Override
    @Transactional
    public void createReport(final int itemId, final int senderId, final ReportEnum reason, final String description) {

        final Item item = itemService.findItemById(itemId);
        if (item.getStatus() != ItemStatusEnum.ACTIVE) {
            throw new ItemNotFoundException();
        }

        final Users host = item.getHost();
        if (host != null && host.getId() != null && host.getId() == senderId) {
            throw new ForbiddenOperationException();
        }

        final Users sender = userService.findById(senderId).orElseThrow(UserNotFoundException::new);

        if (hasReported(senderId, itemId)) {
            throw new ReportAlreadyExistsException();
        }

        Report report = Report.builder()
                .sender(sender)
                .item(item)
                .reason(reason)
                .description(normalizeDescription(description))
                .createdAt(DateTimeUtils.getCurrent())
                .build();

        reportDao.create(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Report findById(int reportId) {
        var report = reportDao.findById(reportId).orElseThrow(ReportNotFoundException::new);
        report.setItemTitle(report.getItem().getLatestVersion().getTitle());
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReported(final int senderId, final int itemId) {
        return reportDao.hasReported(senderId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<Report> searchReports(final int page, final int pageSize, final String sortBy) {
        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        var result = reportDao.searchReports(safePage, safePageSize, sortBy);

        // Set transients
        for (Report report : result.getPageElements()) {
            report.setItemTitle(report.getItem().getLatestVersion().getTitle());
        }
        return result;
    }

    @Override
    @Transactional
    public void dismissReport(final int reportId) {
        final Report report = findById(reportId);
        notifyDismissal(report);
        reportDao.deleteById(reportId);
    }

    @Override
    @Transactional
    public void deletePublicationForReport(final int reportId) {
        final Report report = findById(reportId);
        final Item item = report.getItem();
        final Users owner = item.getHost();

        notifyReporters(report, this::notifySuccess);
        if (owner != null) {
            mailService.sendPublicationRemovedEmail(MailServiceImpl.getItemRemovedEmail(report));
        }

        reportDao.deleteAllByItemId(item.getId());
        manageItemService.deleteItemAsAdmin(item.getId());
    }

    // TODO: como referencia a futuro, para listas sin cota, hay que usar una version generalizada de esto
    private void notifyReporters(final Report originalReport, final Consumer<Report> notifyAction) {
        int page = 1;
        final int pageSize = 18;
        Item item = originalReport.getItem();
        final int totalPages = (int) Math.ceil((double) reportDao.countReports(item) / pageSize);
        List<Report> batch = null;

        while (page <= totalPages) {
            var search = reportDao.searchReports(page, pageSize, "newest", item);
            batch = search.getPageElements();
            for (Report report : batch) {
                notifyAction.accept(report);
            }
            page++;
        }
    }

    private void notifySuccess(final Report report) {
        mailService.sendReportSuccessEmail(MailServiceImpl.getReporterEmail(report));
    }

    private void notifyDismissal(final Report report) {
        mailService.sendReportDismissedEmail(MailServiceImpl.getReporterEmail(report));
    }

    private static String normalizeDescription(final String description) {
        if (description == null) {
            return null;
        }
        final String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
