package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
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

    private final DetailPresentation detailPresentation;

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView itemDetail(
            @PathVariable("id") final int itemId,
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @RequestParam(value = "reviewPage", defaultValue = "1") final int reviewPage) {
        return detailPresentation.detailPage(itemId, user, request, reviewPage);
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
            return detailPresentation.detailPageWithPreBookingValidationErrors(itemId, user, request, errors, 1);
        }
        return detailPresentation.submitPreBooking(user, itemId, form, request, redirectAttributes);
    }
}
