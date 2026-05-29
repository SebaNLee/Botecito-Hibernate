package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.AdminReportsSearchForm;
import ar.edu.itba.paw.webapp.presentation.AdminReportsPresentation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminReportsController {

    private final AdminReportsPresentation adminReportsPresentation;

    @ModelAttribute("adminReportsSearch")
    public AdminReportsSearchForm defaultAdminReportsSearch() {
        final AdminReportsSearchForm form = new AdminReportsSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/admin/reports", method = RequestMethod.GET)
    public ModelAndView listReports(
            @Valid @ModelAttribute("adminReportsSearch") final AdminReportsSearchForm search,
            final BindingResult errors) {
        return adminReportsPresentation.listReports(search, errors);
    }

    @RequestMapping(value = "/admin/reports/{id:[1-9]\\d*}/dismiss", method = RequestMethod.POST)
    public ModelAndView dismissReport(
            @PathVariable("id") final int reportId,
            @ModelAttribute("adminReportsSearch") final AdminReportsSearchForm search,
            final RedirectAttributes redirectAttributes) {
        return adminReportsPresentation.dismissReport(reportId, search, redirectAttributes);
    }

    @RequestMapping(value = "/admin/reports/{id:[1-9]\\d*}/delete-publication", method = RequestMethod.POST)
    public ModelAndView deletePublicationForReport(
            @PathVariable("id") final int reportId,
            @ModelAttribute("adminReportsSearch") final AdminReportsSearchForm search,
            final RedirectAttributes redirectAttributes) {
        return adminReportsPresentation.deletePublicationForReport(reportId, search, redirectAttributes);
    }
}
