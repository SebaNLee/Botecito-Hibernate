package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.PreBookingForm;
import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DetailController {

    private final DetailPresentation detailPresentation;

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public Object itemDetail(
            @PathVariable("id") final int itemId,
            @AuthenticationPrincipal final UserDetails user,
            final HttpServletRequest request) {
        return detailPresentation.detailPage(itemId, request, Optional.empty());
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}/snapshot/{versionId:[1-9]\\d*}", method = RequestMethod.GET)
    public Object itemDetailSnapshot(
            @PathVariable("id") final int itemId,
            @PathVariable("versionId") final int versionId,
            final HttpServletRequest request) {
        return detailPresentation.detailPage(itemId, request, Optional.of((long) versionId));
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.POST)
    public Object submitPreBooking(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("preBookingForm") final PreBookingForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return detailPresentation.detailPageWithPreBookingValidationErrors(itemId, request, errors);
        }
        return detailPresentation.submitPreBooking(request, itemId, form, redirectAttributes);
    }
}
