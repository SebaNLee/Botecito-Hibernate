package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.DetailService;
import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ReportForm;
import ar.edu.itba.paw.webapp.presentation.DetailPageFlags;
import ar.edu.itba.paw.webapp.presentation.DetailPageFlagsFactory;
import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import ar.edu.itba.paw.webapp.presentation.ReportPresentation;
import ar.edu.itba.paw.webapp.presentation.ToastPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private static final String REPORT_MESSAGE_PREFIX = "report";

    private final DetailService detailService;
    private final ReportService reportService;
    private final DetailPageFlagsFactory detailPageFlagsFactory;
    private final MessageSource messageSource;

    @RequestMapping(value = "/item/{id:[1-9]\\d*}/report", method = RequestMethod.POST)
    public ModelAndView submitReport(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @Valid final ReportForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            final Item item = detailService.getItemDetail(itemId, 1);
            final DetailPageFlags flags = detailPageFlagsFactory.compute(item, user);
            return DetailPresentation.detailPageWithReportValidationErrors(
                    item,
                    user,
                    flags,
                    request,
                    form,
                    ToastPresentation.validationToasts(errors, REPORT_MESSAGE_PREFIX, messageSource));
        }
        reportService.createReport(itemId, user.getId(), form.getReason(), form.getDescription());
        return ReportPresentation.submitReportResult(itemId, redirectAttributes);
    }
}
