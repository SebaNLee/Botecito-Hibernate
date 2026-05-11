package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.BookingSearchForm;
import ar.edu.itba.paw.webapp.presentation.BookingPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Guest booking requests (nuevo stack). Does not use {@link
 * ar.edu.itba.paw.webapp.controller.support.BookingDashboardMvcSupport}.
 */
@Controller
@RequiredArgsConstructor
public class RequestsController {

    private final BookingPresentation bookingPresentation;

    /**
     * Defaults for GET /requests/outgoing before binding; omitted query params keep
     * these values.
     */
    @ModelAttribute("bookingSearch")
    public BookingSearchForm defaultBookingSearch() {
        final BookingSearchForm form = new BookingSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/requests", method = RequestMethod.GET)
    public String requestsRoot() {
        return "redirect:/requests/outgoing";
    }

    @RequestMapping(value = "/requests/outgoing", method = RequestMethod.GET)
    public ModelAndView outgoing(
            final HttpServletRequest request,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookingPresentation.bookingsErrors(request, search, errors);
        }
        return bookingPresentation.bookingsGet(request, search);
    }

    @RequestMapping(value = "/requests/incoming", method = RequestMethod.GET)
    public ModelAndView incoming() {
        return new ModelAndView("nuevo/requests-incoming");
    }
}
