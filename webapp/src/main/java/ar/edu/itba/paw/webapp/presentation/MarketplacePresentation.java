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
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private static final String MESSAGE_PREFIX = "marketplaceSearch";

    private final MarketplaceInterface marketplaceInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView marketplaceGet(final HttpServletRequest request, final MarketplaceSearchForm search) {
        final MarketplaceSearchModel model = toModel(search);
        // Persistence joins MAX(version.id) per item; each ItemModel is the latest
        // version only.
        final MarketplaceSearchResult result = marketplaceInterface.searchMarketplace(model);
        final List<ItemModel> items = result.getItems();
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", search);
        mav.addObject("items", items);
        addListingModelObjects(mav, search, items, result.getTotalCount());
        return mav;
    }

    public ModelAndView marketplaceErrors(
            final HttpServletRequest request, final MarketplaceSearchForm search, final BindingResult errors) {
        // Do not run search: invalid params often leave fields at @ModelAttribute
        // defaults (e.g. page=1),
        // which would still return results while BindingResult reports errors.
        final List<ItemModel> items = List.of();
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("items", items);
        addListingModelObjects(mav, search, items, 0L);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<ItemModel> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new Page<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
        mav.addObject("sort", sortRequestValue(search.getSortBy()));
    }

    private static Double parseminAvgRating(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static String sortRequestValue(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return switch (sortBy) {
            case "price_asc" -> "priceAsc";
            case "price_desc" -> "priceDesc";
            default -> sortBy;
        };
    }

    public MarketplaceSearchModel toModel(final MarketplaceSearchForm form) {
        final MarketplaceSearchModel model = new MarketplaceSearchModel();
        model.setSearchQuery(form.getSearchQuery());
        model.setDate(PresentationUtils.parseDate(form.getDate()));
        model.setStartTime(PresentationUtils.parseTime(form.getStartTime()));
        model.setEndTime(PresentationUtils.parseTime(form.getEndTime()));
        model.setCapacity(form.getCapacity());
        model.setWeight(form.getWeight());
        model.setDifficulty(form.getDifficultyLevel());
        model.setMinAvgRating(parseminAvgRating(form.getMinAvgRating()));
        model.setLocation(form.getLocation());
        model.setItemType(form.getItemType());
        model.setPage(form.getPage());
        model.setPageSize(form.getPageSize());
        model.setSortBy(form.getSortBy());
        return model;
    }
}
