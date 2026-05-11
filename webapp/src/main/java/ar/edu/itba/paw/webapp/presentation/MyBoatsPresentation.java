package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.models.nuevo.PageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.ItemInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MyBoatsPresentation {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private final UserService userService;
    private final ItemInterface itemInterface;

    public ModelAndView myBoats(final HttpServletRequest request, final int page, final int pageSize) {
        final UserModel currentUser = currentUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        final int totalItems = itemInterface.countMyBoatsItemsByOwnerId(currentUser.getId());
        final List<MyBoatsItem> ownedItems =
                itemInterface.listMyBoatsItemsByOwnerId(currentUser.getId(), safePage, safePageSize);
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final Map<Integer, Integer> publicationCoverImageIdsByItemId = new LinkedHashMap<>();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();
        final Map<Integer, Boolean> publicationDeleteDeactivatesByItemId = new LinkedHashMap<>();
        final Map<Integer, Boolean> publicationDeleteDisabledByItemId = new LinkedHashMap<>();

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
            publicationDeleteDeactivatesByItemId.put(item.getId(), Boolean.TRUE.equals(item.getDeleteDeactivates()));
            publicationDeleteDisabledByItemId.put(item.getId(), Boolean.TRUE.equals(item.getDeleteDisabled()));
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("user", currentUser);
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("publicationDeleteDeactivatesByItemId", publicationDeleteDeactivatesByItemId);
        mav.addObject("publicationDeleteDisabledByItemId", publicationDeleteDisabledByItemId);
        mav.addObject("itemPage", new PageModel<>(ownedItems, safePage, safePageSize, totalItems));
        return mav;
    }

    private UserModel currentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
