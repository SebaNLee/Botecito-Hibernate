package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.MyBoatsSearchForm;
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
public class MyBoatsPresentation {

    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";
    private static final String MESSAGE_PREFIX = "myBoats";

    private final ItemService itemService;
    private final ToastPresentation toastPresentation;

    public ModelAndView myBoatsList(
            final BotecitoUserDetails principal, final HttpServletRequest request, final MyBoatsSearchForm search) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();

        final SearchResult<Item> result = itemService.listOwnerItems(
                principal.getId(),
                search.getSearchQuery(),
                search.getStatus(),
                null,
                page,
                pageSize,
                search.getSortBy());
        final List<Item> ownedItems = result.getPageElements();

        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        addListingModelObjects(mav, search, ownedItems, result.getTotalCount(), request);
        return mav;
    }

    public ModelAndView myBoatsErrors(
            final BotecitoUserDetails principal,
            final HttpServletRequest request,
            final MyBoatsSearchForm search,
            final BindingResult errors) {
        final List<Item> ownedItems = List.of();
        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", new LinkedHashMap<>());
        mav.addObject("imageUrlsByItemId", new LinkedHashMap<>());
        mav.addObject("itemPage", new PageModel<>(ownedItems, 1, 12, 0));
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav,
            final MyBoatsSearchForm search,
            final List<Item> ownedItems,
            final long total,
            final HttpServletRequest request) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final Map<Integer, Integer> publicationCoverImageIdsByItemId = new LinkedHashMap<>();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();

        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }

            var media = item.getLatestVersion().getMedia();
            Integer coverImageId =
                    media != null && media.size() > 0 ? media.get(0).getImage().getId() : null;

            if (coverImageId != null) {
                publicationCoverImageIdsByItemId.put(item.getId(), coverImageId);
                imageUrlsByItemId.put(item.getId(), contextPath + IMAGE_PATH_PREFIX + coverImageId);
            } else {
                imageUrlsByItemId.put(item.getId(), contextPath + PLACEHOLDER_IMAGE_PATH);
            }
        }

        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("itemPage", new PageModel<>(ownedItems, page, pageSize, totalItems));
    }
}
