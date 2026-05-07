package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.webapp.controller.support.MarketplaceMvcSupport;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceMvcSupport marketplaceMvcSupport;

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm(final Locale locale) {
        return MarketplaceMvcSupport.newReservationRequestForm(locale);
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            final HttpServletRequest request,
            @Valid @ModelAttribute("marketplaceSearch") final MarketplaceSearchForm search,
            final BindingResult searchErrors) {
        if (searchErrors.hasErrors()) {
            return marketplaceMvcSupport.marketplaceWithSearchBindingErrors(request, search, searchErrors);
        }
        return marketplaceMvcSupport.marketplace(request, search);
    }

    @ResponseBody
    @RequestMapping(
            value = "/location-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LocationOption> locationOptions() {
        return marketplaceMvcSupport.locationOptions();
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime,
            @RequestParam(value = "snapshotVersionId", required = false) final Integer snapshotVersionId,
            @ModelAttribute("reservationRequestForm") final ReservationRequestForm form) {
        return marketplaceMvcSupport.marketplaceItem(
                request, itemId, requestedDate, requestedStartTime, requestedEndTime, snapshotVersionId, form);
    }

    @RequestMapping(value = "/item/{itemId:[0-9]+}/snapshot/{versionId:[0-9]+}/cover", method = RequestMethod.GET)
    public void snapshotCoverImage(
            @PathVariable("itemId") final int itemId,
            @PathVariable("versionId") final int versionId,
            final HttpServletResponse response)
            throws java.io.IOException {
        marketplaceMvcSupport.snapshotCoverImage(response, itemId, versionId);
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView submitMarketplaceItemRequest(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("reservationRequestForm") final ReservationRequestForm form,
            final BindingResult errors) {
        return marketplaceMvcSupport.submitMarketplaceItemRequest(request, itemId, form, errors);
    }
}
