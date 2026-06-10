package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.DetailService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import ar.edu.itba.paw.webapp.presentation.ToastPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class DetailController {

    private static final String DETAIL_MESSAGE_PREFIX = "detail";

    private final DetailService detailService;
    private final BookingService bookingService;
    private final MessageSource messageSource;

    @ModelAttribute("itemDetailView")
    public ItemDetailViewForm defaultItemDetailView() {
        final ItemDetailViewForm form = new ItemDetailViewForm();
        form.setPage(1);
        return form;
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView itemDetail(
            @PathVariable("id") final int itemId,
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @Valid @ModelAttribute("itemDetailView") final ItemDetailViewForm itemDetailView,
            final BindingResult errors) {
        itemDetailView.setItemId(itemId);
        final Integer viewerId = user != null ? user.getId() : null;
        if (errors.hasErrors()) {
            final var pageData = detailService.getItemDetailPage(itemId, 1, viewerId);
            return DetailPresentation.detailPageWithViewValidationErrors(
                    pageData,
                    user,
                    request,
                    itemDetailView,
                    errors,
                    ToastPresentation.validationToasts(errors, DETAIL_MESSAGE_PREFIX, messageSource));
        }
        final var pageData = detailService.getItemDetailPage(itemId, itemDetailView.getPage(), viewerId);
        return DetailPresentation.detailPage(pageData, user, request, itemDetailView);
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
            final var pageData = detailService.getItemDetailPage(itemId, 1, user != null ? user.getId() : null);
            return DetailPresentation.detailPageWithPreBookingValidationErrors(
                    pageData,
                    user,
                    request,
                    ToastPresentation.validationToasts(errors, DETAIL_MESSAGE_PREFIX, messageSource));
        }
        bookingService.createBooking(
                itemId, form.getDate(), form.getStartTime(), form.getEndTime(), form.getMessage(), user.getId());
        return DetailPresentation.submitPreBookingSuccess(itemId, redirectAttributes);
    }
}
