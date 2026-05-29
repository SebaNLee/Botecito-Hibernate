package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ReportForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class ReportPresentation {

    private static final String MESSAGE_PREFIX = "report";

    private final DetailPresentation detailPresentation;
    private final ReportService reportService;
    private final ToastPresentation toastPresentation;

    public ModelAndView detailWithReportValidationErrors(
            final int itemId,
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final ReportForm form,
            final BindingResult errors) {
        final ModelAndView mav = detailPresentation.detailPage(itemId, viewer, request, 1);
        mav.addObject("reportForm", form);
        mav.addObject("openReportModal", true);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView submitReport(
            final int itemId,
            final BotecitoUserDetails viewer,
            final ReportForm form,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        reportService.createReport(itemId, viewer.getId(), form.getReason(), form.getDescription());
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".success");
        return detailRedirect(itemId, request);
    }

    private static ModelAndView detailRedirect(final int itemId, final HttpServletRequest request) {
        final String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return new ModelAndView("redirect:/item/" + itemId);
        }
        return new ModelAndView("redirect:/item/" + itemId + "?" + query);
    }
}
