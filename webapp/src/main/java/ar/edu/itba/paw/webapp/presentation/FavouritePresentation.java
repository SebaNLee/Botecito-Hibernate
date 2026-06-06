package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.webapp.form.FavouritesSearchForm;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class FavouritePresentation {

    private static final String VIEW_NAME = "favourites";
    private static final String MESSAGE_PREFIX = "favourites";

    private final CoverImageUrlResolver coverImageUrlResolver;
    private final ToastPresentation toastPresentation;

    public ModelAndView favourites(
            final HttpServletRequest request, final FavouritesSearchForm search, final PageModel<Item> itemPage) {
        final ModelAndView mav = new ModelAndView(VIEW_NAME, "favouritesSearch", search);
        addListingModelObjects(mav, itemPage, request);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView favouritesErrors(
            final HttpServletRequest request, final FavouritesSearchForm search, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView(VIEW_NAME, "favouritesSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        addListingModelObjects(mav, new PageModel<>(List.of(), 1, 12, 0), request);
        mav.addObject("hasValidationErrors", true);
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final PageModel<Item> itemPage, final HttpServletRequest request) {
        mav.addObject("itemPage", itemPage);
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("pageSize", itemPage.getPageSize());
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(itemPage.getContent(), request));
        mav.addObject("favouriteByItemId", booleanMap(itemPage.getContent(), true));
        mav.addObject("canFavouriteByItemId", booleanMap(itemPage.getContent(), true));
    }

    public ModelAndView addFavouriteFromFavouritesResult(
            final FavouritesSearchForm search, final boolean success, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "favourite.add.error");
        } else {
            ToastSupport.success(redirectAttributes, "favourite.added");
        }
        addFavouritesSearchParams(redirectAttributes, search);
        return new ModelAndView("redirect:/favourites");
    }

    public ModelAndView removeFavouriteFromFavouritesResult(
            final FavouritesSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.info(redirectAttributes, "favourite.removed");
        addFavouritesSearchParams(redirectAttributes, search);
        return new ModelAndView("redirect:/favourites");
    }

    public ModelAndView addFavouriteFromItemDetailResult(
            final ItemDetailViewForm view,
            final int itemId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "favourite.add.error");
        } else {
            ToastSupport.success(redirectAttributes, "favourite.added");
        }
        return itemDetailRedirect(view, itemId, redirectAttributes);
    }

    public ModelAndView removeFavouriteFromItemDetailResult(
            final ItemDetailViewForm view, final int itemId, final RedirectAttributes redirectAttributes) {
        ToastSupport.info(redirectAttributes, "favourite.removed");
        return itemDetailRedirect(view, itemId, redirectAttributes);
    }

    private static ModelAndView itemDetailRedirect(
            final ItemDetailViewForm view, final int itemId, final RedirectAttributes redirectAttributes) {
        addItemDetailViewParams(redirectAttributes, view);
        final int targetItemId = view != null && view.getItemId() != null ? view.getItemId() : itemId;
        return new ModelAndView("redirect:/item/" + targetItemId);
    }

    private static void addFavouritesSearchParams(
            final RedirectAttributes redirectAttributes, final FavouritesSearchForm search) {
        if (search == null) {
            return;
        }
        redirectAttributes.addAttribute("page", search.getPage());
        redirectAttributes.addAttribute("pageSize", search.getPageSize());
        if (StringUtils.hasText(search.getSortBy()) && !"newest".equals(search.getSortBy())) {
            redirectAttributes.addAttribute("sortBy", search.getSortBy());
        }
        if (StringUtils.hasText(search.getSearchQuery())) {
            redirectAttributes.addAttribute("searchQuery", search.getSearchQuery());
        }
    }

    private static void addItemDetailViewParams(
            final RedirectAttributes redirectAttributes, final ItemDetailViewForm view) {
        if (view == null || view.getItemId() == null) {
            return;
        }
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
        }
    }

    private static Map<Integer, Boolean> booleanMap(final Iterable<Item> items, final boolean value) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        for (final Item item : items) {
            map.put(item.getId(), value);
        }
        return map;
    }
}
