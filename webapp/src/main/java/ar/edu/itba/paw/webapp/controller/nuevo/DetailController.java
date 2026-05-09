package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.controller.support.MarketplaceMvcSupport;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class DetailController {

    private final DetailPresentation detailPresentation;

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm(final Locale locale) {
        return MarketplaceMvcSupport.newReservationRequestForm(locale);
    }

    /**
     * Positive integer {@code id} in the path only (pattern {@code [1-9][0-9]*}, so no {@code 0}). Whether that id
     * exists or is visible is decided only in the service/DAO layer; the presentation shows a toast when the backend
     * returns empty.
     */
    @RequestMapping(value = "/nuevo/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView itemDetail(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "returnTo", required = false) final String returnTo,
            final HttpServletRequest request,
            @ModelAttribute("reservationRequestForm") final ReservationRequestForm reservationRequestForm) {
        return detailPresentation.detailGet(itemId, request, reservationRequestForm, returnTo);
    }
}
