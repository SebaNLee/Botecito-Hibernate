package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.MarketplaceService;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private static final String MESSAGE_PREFIX = "marketplace";
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private final MarketplaceService marketplaceInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView marketplace(
            final HttpServletRequest request, final MarketplaceSearchForm form, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        List<Item> items = List.of();
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
        mav.addObject("imageUrlsByItemId", coverImageUrlsByItemId(items, request));
        return mav;
    }

    private static Map<Integer, String> coverImageUrlsByItemId(
            final List<Item> items, final HttpServletRequest request) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();
        for (final Item item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            imageUrlsByItemId.put(item.getId(), coverImageUrl(item.getLatestVersion(), contextPath));
        }
        return imageUrlsByItemId;
    }

    private static String coverImageUrl(final Version version, final String contextPath) {
        if (version == null || version.getMedia() == null || version.getMedia().isEmpty()) {
            return contextPath + PLACEHOLDER_IMAGE_PATH;
        }
        return version.getMedia().stream()
                .filter(m -> m.getImage() != null && m.getId() != null)
                .min(Comparator.comparingInt(m -> m.getId().getIndex()))
                .map(Media::getImage)
                .map(image -> contextPath + IMAGE_PATH_PREFIX + image.getId())
                .orElse(contextPath + PLACEHOLDER_IMAGE_PATH);
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<Item> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new PageModel<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
    }
}
