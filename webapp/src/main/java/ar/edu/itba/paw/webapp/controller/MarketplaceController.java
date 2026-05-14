package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.nuevo.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.presentation.MarketplacePresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplacePresentation marketplacePresentation;

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

        return marketplacePresentation.marketplace(request, search, errors);
    }
}
