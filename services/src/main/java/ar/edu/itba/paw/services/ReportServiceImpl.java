package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
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
import ar.edu.itba.paw.services.util.PagedProcessing;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportDao reportDao;
    private final ItemService itemService;
    private final UserService userService;
    private final MailService mailService;
    private final BookingService bookingService;

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
        LOGGER.info("User {} reported item {} for {}", senderId, itemId, reason);
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
    public PageModel<Report> searchReports(final int page, final int pageSize, final String sortBy) {
        final PageModel<Report> reportPage = reportDao.searchReports(page, pageSize, sortBy);

        for (final Report report : reportPage.getContent()) {
            report.setItemTitle(report.getItem().getLatestVersion().getTitle());
        }
        return reportPage;
    }

    @Override
    @Transactional
    public void dismissReport(final int reportId) {
        final Report report = findById(reportId);
        notifyDismissal(report);
        reportDao.deleteById(reportId);
        LOGGER.info("Report {} dismissed", reportId);
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

        LOGGER.info("Item {} deleted for report {}", item.getId(), reportId);

        boolean isSoft = itemService.getVersionCount(item.getId()) > 1 || bookingService.itemHasBookings(item);
        itemService.deleteItem(item, isSoft);
    }

    @Override
    @Transactional
    public void deleteAllByItemId(int itemId) {
        LOGGER.info("All reports deleted for item {}", itemId);
        reportDao.deleteAllByItemId(itemId);
    }

    private void notifyReporters(final Report originalReport, final Consumer<Report> notifyAction) {
        Item item = originalReport.getItem();

        PagedProcessing.batchAction(
                20,
                reportDao.countReports(item),
                (page, pageSize) -> reportDao.searchReports(page, pageSize, "newest", item),
                notifyAction);
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
