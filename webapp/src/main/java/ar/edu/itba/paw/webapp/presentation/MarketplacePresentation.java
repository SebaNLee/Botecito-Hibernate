package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
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

    private final ToastPresentation toastPresentation;
    private final CoverImageUrlResolver coverImageUrlResolver;

    public ModelAndView marketplace(
            final HttpServletRequest request, final MarketplaceSearchForm form, final SearchResult<Item> result) {
        final List<Item> items = result.getPageElements();
        final long totalCount = result.getTotalCount();
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        addListingModelObjects(mav, form, items, totalCount);
        mav.addObject("items", items);
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(items, request));
        return mav;
    }

    public ModelAndView marketplaceErrors(
            final HttpServletRequest request, final MarketplaceSearchForm form, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        addListingModelObjects(mav, form, List.of(), 0);
        mav.addObject("items", List.of());
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(List.of(), request));
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<Item> items, final long total) {
        final int page = search.getPage();
        final int pageSize = search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new PageModel<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
    }
}
