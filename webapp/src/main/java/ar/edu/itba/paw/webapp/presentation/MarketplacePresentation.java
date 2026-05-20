package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.MarketplaceService;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private static final String MESSAGE_PREFIX = "marketplace";

    private final MarketplaceService marketplaceInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView marketplace(
            final HttpServletRequest request, final MarketplaceSearchForm form, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        List<Version> items = List.of();
        long totalCount = 0;

        if (!errors.hasErrors()) {
            final MarketplaceSearchResult result = marketplaceInterface.searchMarketplace(
                    form.getSearchQuery(),
                    form.getDate(),
                    form.getStartTime(),
                    form.getEndTime(),
                    form.getCapacity(),
                    form.getWeight(),
                    form.getDifficulty(),
                    form.getMinAvgRating(),
                    form.getLocation(),
                    form.getItemType(),
                    form.getPage(),
                    form.getPageSize(),
                    form.getSortBy());
            items = result.getItems();
            totalCount = result.getTotalCount();
        } else {
            mav.addAllObjects(errors.getModel());
            mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        }
        addListingModelObjects(mav, form, items, totalCount);
        mav.addObject("items", items);
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<Version> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new PageModel<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
    }
}
