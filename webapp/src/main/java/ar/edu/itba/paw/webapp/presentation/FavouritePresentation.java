package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class FavouritePresentation {

    private static final String VIEW_NAME = "favourites";

    private final FavouriteService favouriteService;
    private final CoverImageUrlResolver coverImageUrlResolver;

    public ModelAndView favourites(
            final BotecitoUserDetails principal, final HttpServletRequest request, final int page, final int pageSize) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final PageModel<Item> itemPage = favouriteService.listFavourites(principal.getId(), page, pageSize);
        final ModelAndView mav = new ModelAndView(VIEW_NAME);
        mav.addObject("itemPage", itemPage);
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("pageSize", itemPage.getPageSize());
        mav.addObject("favouritesReturnPath", currentRequestPath(request));
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(itemPage.getContent(), request));
        mav.addObject("favouriteByItemId", booleanMap(itemPage.getContent(), true));
        mav.addObject("canFavouriteByItemId", booleanMap(itemPage.getContent(), true));
        return mav;
    }

    public ModelAndView addFavourite(
            final BotecitoUserDetails principal,
            final int itemId,
            final String returnPath,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        if (!favouriteService.addFavourite(principal.getId(), itemId)) {
            ToastSupport.error(redirectAttributes, "favourite.add.error");
            return redirect(returnPath);
        }
        ToastSupport.success(redirectAttributes, "favourite.added");
        return redirect(returnPath);
    }

    public ModelAndView removeFavourite(
            final BotecitoUserDetails principal,
            final int itemId,
            final String returnPath,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        favouriteService.removeFavourite(principal.getId(), itemId);
        ToastSupport.success(redirectAttributes, "favourite.removed");
        return redirect(returnPath);
    }

    private static Map<Integer, Boolean> booleanMap(final Iterable<Item> items, final boolean value) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        for (final Item item : items) {
            map.put(item.getId(), value);
        }
        return map;
    }

    private static ModelAndView redirect(final String returnPath) {
        return new ModelAndView("redirect:" + safeRelativePath(returnPath));
    }

    private static String currentRequestPath(final HttpServletRequest request) {
        if (request == null) {
            return "/favourites";
        }
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = request.getRequestURI() == null ? "/favourites" : request.getRequestURI();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        final String query = request.getQueryString();
        return query == null || query.isBlank() ? path : path + "?" + query;
    }

    private static String safeRelativePath(final String returnPath) {
        if (returnPath == null || returnPath.isBlank()) {
            return "/favourites";
        }
        final String trimmed = returnPath.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return "/favourites";
        }
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return "/favourites";
        }
        return trimmed;
    }
}
