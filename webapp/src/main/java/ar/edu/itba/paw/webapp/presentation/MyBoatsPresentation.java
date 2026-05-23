package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final ItemService itemInterface;

    public ModelAndView myBoats(
            final BotecitoUserDetails principal, final HttpServletRequest request, final int page, final int pageSize) {
        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        final int totalItems = itemInterface.countMyBoatsItemsByOwnerId(principal.getId());
        final List<MyBoatsItem> ownedItems =
                itemInterface.listMyBoatsItemsByOwnerId(principal.getId(), safePage, safePageSize);
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final Map<Integer, Integer> publicationCoverImageIdsByItemId = new LinkedHashMap<>();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();

        for (final MyBoatsItem item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (item.getCoverImageId() != null) {
                publicationCoverImageIdsByItemId.put(item.getId(), item.getCoverImageId());
                imageUrlsByItemId.put(item.getId(), contextPath + IMAGE_PATH_PREFIX + item.getCoverImageId());
            } else {
                imageUrlsByItemId.put(item.getId(), contextPath + PLACEHOLDER_IMAGE_PATH);
            }
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("itemPage", new PageModel<>(ownedItems, safePage, safePageSize, totalItems));
        return mav;
    }
}
