package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.MarketplaceService;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.presentation.MarketplacePresentation;
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

    private final MarketplaceService marketplaceService;
    private final MarketplacePresentation marketplacePresentation;

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
            @Valid @ModelAttribute("marketplaceSearch") final MarketplaceSearchForm search,
            final BindingResult errors) {

        if (errors.hasErrors()) {
            return marketplacePresentation.marketplaceErrors(search, errors);
        }

        final PageModel<Item> itemPage = marketplaceService.searchMarketplace(
                search.getSearchQuery(),
                search.getDate(),
                search.getStartTime(),
                search.getEndTime(),
                search.getCapacity(),
                search.getWeight(),
                search.getDifficulty(),
                search.getMinAvgRating(),
                search.getLocation(),
                search.getItemType(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
        return marketplacePresentation.marketplace(search, itemPage);
    }
}
