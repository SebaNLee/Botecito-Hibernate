package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.FavouritesSearchForm;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.presentation.FavouritePresentation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService favouriteService;
    private final MessageSource messageSource;

    @ModelAttribute("favouritesSearch")
    public FavouritesSearchForm defaultFavouritesSearch() {
        final FavouritesSearchForm form = new FavouritesSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/favourites", method = RequestMethod.GET)
    public ModelAndView favourites(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("favouritesSearch") final FavouritesSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return FavouritePresentation.favouritesErrors(search, errors, messageSource);
        }
        final PageModel<Item> itemPage = favouriteService.listFavourites(
                user.getId(), search.getSearchQuery(), search.getPage(), search.getPageSize(), search.getSortBy());
        return FavouritePresentation.favourites(search, itemPage);
    }

    @RequestMapping(value = "/favourites/items/{id:[1-9]\\d*}/favourite", method = RequestMethod.POST)
    public ModelAndView addFavouriteFromFavourites(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @ModelAttribute("favouritesSearch") final FavouritesSearchForm favouritesSearch,
            final RedirectAttributes redirectAttributes) {
        final boolean success = favouriteService.addFavourite(user.getId(), itemId);
        return FavouritePresentation.addFavouriteFromFavouritesResult(favouritesSearch, success, redirectAttributes);
    }

    @RequestMapping(value = "/favourites/items/{id:[1-9]\\d*}/unfavourite", method = RequestMethod.POST)
    public ModelAndView removeFavouriteFromFavourites(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @ModelAttribute("favouritesSearch") final FavouritesSearchForm favouritesSearch,
            final RedirectAttributes redirectAttributes) {
        favouriteService.removeFavourite(user.getId(), itemId);
        return FavouritePresentation.removeFavouriteFromFavouritesResult(favouritesSearch, redirectAttributes);
    }

    @RequestMapping(value = "/items/{id:[1-9]\\d*}/favourite", method = RequestMethod.POST)
    public ModelAndView addFavouriteFromItemDetail(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @ModelAttribute("itemDetailView") final ItemDetailViewForm itemDetailView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = favouriteService.addFavourite(user.getId(), itemId);
        return FavouritePresentation.addFavouriteFromItemDetailResult(
                itemDetailView, itemId, success, redirectAttributes);
    }

    @RequestMapping(value = "/items/{id:[1-9]\\d*}/unfavourite", method = RequestMethod.POST)
    public ModelAndView removeFavouriteFromItemDetail(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @ModelAttribute("itemDetailView") final ItemDetailViewForm itemDetailView,
            final RedirectAttributes redirectAttributes) {
        favouriteService.removeFavourite(user.getId(), itemId);
        return FavouritePresentation.removeFavouriteFromItemDetailResult(itemDetailView, itemId, redirectAttributes);
    }
}
