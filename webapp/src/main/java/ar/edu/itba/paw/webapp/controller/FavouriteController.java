package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.FavouritesSearchForm;
import ar.edu.itba.paw.webapp.presentation.FavouritePresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouritePresentation favouritePresentation;

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
            final HttpServletRequest request,
            @Valid @ModelAttribute("favouritesSearch") final FavouritesSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return favouritePresentation.favouritesErrors(user, request, search, errors);
        }
        return favouritePresentation.favourites(user, request, search);
    }

    @RequestMapping(value = "/items/{id:[1-9]\\d*}/favourite", method = RequestMethod.POST)
    public ModelAndView addFavourite(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnPath,
            final RedirectAttributes redirectAttributes) {
        return favouritePresentation.addFavourite(user, itemId, returnPath, redirectAttributes);
    }

    @RequestMapping(value = "/items/{id:[1-9]\\d*}/unfavourite", method = RequestMethod.POST)
    public ModelAndView removeFavourite(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnPath,
            final RedirectAttributes redirectAttributes) {
        return favouritePresentation.removeFavourite(user, itemId, returnPath, redirectAttributes);
    }
}
