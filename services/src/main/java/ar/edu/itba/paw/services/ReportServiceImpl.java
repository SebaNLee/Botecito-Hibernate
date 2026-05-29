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
import ar.edu.itba.paw.persistence.ReportDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportDao reportDao;
    private final ItemService itemService;
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

        if (reportDao.findBySenderAndItem(senderId, itemId).isPresent()) {
            throw new ReportAlreadyExistsException();
        }

        reportDao.create(senderId, itemId, reason, normalizeDescription(description));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReported(final int senderId, final int itemId) {
        return reportDao.findBySenderAndItem(senderId, itemId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageModel<Report> findReportsForAdmin(final int page, final int pageSize, final String sortBy) {
        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        final boolean newestFirst = !"oldest".equals(sortBy);
        final int total = reportDao.countAll();
        return new PageModel<>(reportDao.findAll(safePage, safePageSize, newestFirst), safePage, safePageSize, total);
    }

    @Override
    @Transactional
    public void dismissReport(final int reportId, final int adminUserId) {
        final Report report = reportDao.findById(reportId).orElseThrow(ReportNotFoundException::new);
        mailService.sendReportDismissedEmail(
                report.getSender(), report.getItem(), report.getReason(), report.getDescription());
        reportDao.deleteById(reportId);
    }

    @Override
    @Transactional
    public void deletePublicationForReport(final int reportId, final int adminUserId) {
        final Report report = reportDao.findById(reportId).orElseThrow(ReportNotFoundException::new);
        final Item item = report.getItem();
        final Users owner = item.getHost();
        final String itemTitle = report.getItemTitle();

        mailService.sendReportPublicationRemovedEmail(
                report.getSender(), item, report.getReason(), report.getDescription(), itemTitle);
        if (owner != null) {
            mailService.sendPublicationRemovedDueToReportEmail(
                    owner, item, report.getReason(), report.getDescription(), itemTitle);
        }

        reportDao.deleteAllByItemId(item.getId());
        manageItemService.deleteItemAsAdmin(item.getId());
    }

    private static String normalizeDescription(final String description) {
        if (description == null) {
            return null;
        }
        final String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
