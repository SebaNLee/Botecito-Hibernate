package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.webapp.form.AdminReportsSearchForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class AdminReportsPresentation {

    private static final String MESSAGE_PREFIX = "admin.reports";

    private AdminReportsPresentation() {}

    public static ModelAndView listReports(final AdminReportsSearchForm search, final PageModel<Report> reportPage) {
        final ModelAndView mav = new ModelAndView("admin-reports", "adminReportsSearch", search);
        mav.addObject("reportPage", reportPage);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public static ModelAndView listReportsErrors(
            final AdminReportsSearchForm search, final BindingResult errors, final MessageSource messageSource) {
        final ModelAndView mav = new ModelAndView("admin-reports", "adminReportsSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", ToastPresentation.validationToasts(errors, MESSAGE_PREFIX, messageSource));
        mav.addObject("reportPage", new PageModel<Report>(List.of(), 1, 12, 0L));
        mav.addObject("hasValidationErrors", true);
        return mav;
    }

    public static ModelAndView dismissReportResult(
            final AdminReportsSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".dismiss.success");
        return redirectToList(search);
    }

    public static ModelAndView deletePublicationForReportResult(
            final AdminReportsSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".deletePublication.success");
        return redirectToList(search);
    }

    private static ModelAndView redirectToList(final AdminReportsSearchForm search) {
        final StringBuilder target = new StringBuilder("redirect:/admin/reports?");
        appendSearchParams(target, search);
        return new ModelAndView(target.toString());
    }

    static void appendSearchParams(final StringBuilder target, final AdminReportsSearchForm search) {
        target.append("page=").append(search.getPage());
        target.append("&pageSize=").append(search.getPageSize());
        target.append("&sortBy=").append(search.getSortBy());
    }
}
