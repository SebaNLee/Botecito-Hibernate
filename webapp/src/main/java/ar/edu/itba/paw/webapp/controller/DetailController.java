package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.DetailService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
import ar.edu.itba.paw.webapp.presentation.DetailPageFlags;
import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DetailController {

    private final DetailService detailService;
    private final BookingService bookingService;
    private final DetailPageFlagsFactory detailPageFlagsFactory;
    private final DetailPresentation detailPresentation;

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView itemDetail(
            @PathVariable("id") final int itemId,
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @RequestParam(value = "reviewPage", defaultValue = "1") final int reviewPage) {
        final Item item = detailService.getItemDetail(itemId, reviewPage);
        final DetailPageFlags flags = detailPageFlagsFactory.compute(item, user);
        return detailPresentation.detailPage(item, user, flags, request);
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.POST)
    public ModelAndView submitPreBooking(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("preBookingForm") final PreBookingForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            final Item item = detailService.getItemDetail(itemId, 1);
            final DetailPageFlags flags = detailPageFlagsFactory.compute(item, user);
            return detailPresentation.detailPageWithPreBookingValidationErrors(item, user, flags, request, errors);
        }
        bookingService.createBooking(
                itemId, form.getDate(), form.getStartTime(), form.getEndTime(), form.getMessage(), user.getId());
        return detailPresentation.submitPreBookingSuccess(itemId, redirectAttributes);
    }
}
