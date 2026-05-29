package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.webapp.form.AdminReportsSearchForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class AdminReportsPresentation {

    private static final String MESSAGE_PREFIX = "admin.reports";
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportService reportService;
    private final ToastPresentation toastPresentation;

    public ModelAndView listReports(final AdminReportsSearchForm search, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("admin-reports", "adminReportsSearch", search);
        if (errors.hasErrors()) {
            mav.addAllObjects(errors.getModel());
            mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
            mav.addObject("reportPage", new PageModel<Report>(List.of(), 1, 12, 0));
            mav.addObject("reportDatesById", Map.of());
            return mav;
        }

        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final String sortBy =
                search.getSortBy() == null || search.getSortBy().isBlank() ? "newest" : search.getSortBy();
        final PageModel<Report> reportPage = reportService.findReportsForAdmin(page, pageSize, sortBy);
        mav.addObject("reportPage", reportPage);
        mav.addObject("reportDatesById", formatReportDates(reportPage.getContent()));
        return mav;
    }

    public ModelAndView dismissReport(
            final int reportId, final AdminReportsSearchForm search, final RedirectAttributes redirectAttributes) {
        reportService.dismissReport(reportId, 0);
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".dismiss.success");
        return redirectToList(search);
    }

    public ModelAndView deletePublicationForReport(
            final int reportId, final AdminReportsSearchForm search, final RedirectAttributes redirectAttributes) {
        reportService.deletePublicationForReport(reportId, 0);
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".deletePublication.success");
        return redirectToList(search);
    }

    private static ModelAndView redirectToList(final AdminReportsSearchForm search) {
        final StringBuilder target = new StringBuilder("redirect:/admin/reports?");
        appendSearchParams(target, search);
        return new ModelAndView(target.toString());
    }

    static void appendSearchParams(final StringBuilder target, final AdminReportsSearchForm search) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final String sortBy =
                search.getSortBy() == null || search.getSortBy().isBlank() ? "newest" : search.getSortBy();
        target.append("page=").append(page);
        target.append("&pageSize=").append(pageSize);
        target.append("&sortBy=").append(sortBy);
    }

    private static Map<Integer, String> formatReportDates(final List<Report> reports) {
        final Map<Integer, String> dates = new LinkedHashMap<>();
        if (reports == null) {
            return dates;
        }
        for (final Report report : reports) {
            if (report == null || report.getId() == null || report.getCreatedAt() == null) {
                continue;
            }
            dates.put(report.getId(), report.getCreatedAt().format(REPORT_DATE_FORMAT));
        }
        return dates;
    }
}
