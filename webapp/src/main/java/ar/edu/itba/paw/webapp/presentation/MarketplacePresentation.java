package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.services.MarketplaceService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final FavouriteService favouriteService;
    private final ToastPresentation toastPresentation;
    private final CoverImageUrlResolver coverImageUrlResolver;

    public ModelAndView marketplace(
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final MarketplaceSearchForm form,
            final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        List<Item> items = List.of();
        long totalCount = 0;

        if (!errors.hasErrors()) {
            final ItemSearchResult result = marketplaceInterface.searchMarketplace(
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
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(items, request));
        addFavouriteModelObjects(mav, viewer, items);
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<Item> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new PageModel<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
    }

    private void addFavouriteModelObjects(
            final ModelAndView mav, final BotecitoUserDetails viewer, final List<Item> items) {
        final Map<Integer, Boolean> favouriteByItemId = new LinkedHashMap<>();
        final Map<Integer, Boolean> canFavouriteByItemId = new LinkedHashMap<>();
        if (viewer == null || items.isEmpty()) {
            mav.addObject("favouriteByItemId", favouriteByItemId);
            mav.addObject("canFavouriteByItemId", canFavouriteByItemId);
            return;
        }
        final Set<Integer> favouriteIds = favouriteService.findFavouriteItemIds(
                viewer.getId(), items.stream().map(Item::getId).toList());
        for (final Item item : items) {
            final boolean canFavourite = item.getHost() == null
                    || item.getHost().getId() == null
                    || item.getHost().getId() != viewer.getId();
            canFavouriteByItemId.put(item.getId(), canFavourite);
            favouriteByItemId.put(item.getId(), favouriteIds.contains(item.getId()));
        }
        mav.addObject("favouriteByItemId", favouriteByItemId);
        mav.addObject("canFavouriteByItemId", canFavouriteByItemId);
    }
}
