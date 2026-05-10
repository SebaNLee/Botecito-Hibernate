package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.MarketplaceMvcSupport;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.presentation.MarketplacePresentation;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplacePresentation marketplacePresentation;
    private final MarketplaceMvcSupport marketplaceMvcSupport;

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm(final Locale locale) {
        return MarketplaceMvcSupport.newReservationRequestForm(locale);
    }

    /**
     * Defaults for GET /marketplace before request binding; omitted query params
     * stay at these values.
     */
    @ModelAttribute("marketplaceSearch")
    public MarketplaceSearchForm defaultMarketplaceSearch() {
        final MarketplaceSearchForm form = new MarketplaceSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            final HttpServletRequest request,
            @Valid @ModelAttribute("marketplaceSearch") final MarketplaceSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return marketplacePresentation.marketplaceErrors(request, search, errors);
        }
        return marketplacePresentation.marketplaceGet(request, search);
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.POST)
    public ModelAndView submitMarketplaceItemRequest(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("reservationRequestForm") final ReservationRequestForm form,
            final BindingResult errors) {
        return marketplaceMvcSupport.submitMarketplaceItemRequest(request, itemId, form, errors);
    }
}
