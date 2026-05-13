package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.nuevo.MarketplaceInterface;
import ar.edu.itba.paw.webapp.form.nuevo.MarketplaceSearchForm;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private static final String MESSAGE_PREFIX = "marketplace";

    private final MarketplaceInterface marketplaceInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView marketplace(
            final HttpServletRequest request, final MarketplaceSearchForm form, final BindingResult errors) {
        final MarketplaceSearchModel model = new MarketplaceSearchModel();
        BeanUtils.copyProperties(form, model);

        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        List<ItemModel> items = List.of();
        long totalCount = 0;

        if (!errors.hasErrors()) {
            final MarketplaceSearchResult result = marketplaceInterface.searchMarketplace(model);
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
            final ModelAndView mav, final MarketplaceSearchForm search, final List<ItemModel> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new Page<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
    }
}
