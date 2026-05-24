package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MyBoatsPresentation {

    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private final ItemService itemService;

    public ModelAndView myBoats(
            final BotecitoUserDetails principal, final HttpServletRequest request, final int page, final int pageSize) {
        final int safePage = Math.max(1, page);
        final int safePageSize = Math.clamp(pageSize, 1, 18);
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final ItemSearchResult result = itemService.listOwnerItems(principal.getId(), safePage, safePageSize);
        final var ownedItems = result.getItems();

        final Map<Integer, Integer> publicationCoverImageIdsByItemId = new LinkedHashMap<>();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();

        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            Integer coverImageId =
                    item.getLatestVersion().getMedia().get(0).getImage().getId();
            if (coverImageId != null) {
                publicationCoverImageIdsByItemId.put(item.getId(), coverImageId);
                imageUrlsByItemId.put(item.getId(), contextPath + IMAGE_PATH_PREFIX + coverImageId);
            } else {
                imageUrlsByItemId.put(item.getId(), contextPath + PLACEHOLDER_IMAGE_PATH);
            }
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("itemPage", new PageModel<>(ownedItems, safePage, safePageSize, (int) result.getTotalCount()));
        return mav;
    }
}
