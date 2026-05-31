package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.FavouritePresentation;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
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

    @RequestMapping(value = "/favourites", method = RequestMethod.GET)
    public ModelAndView favourites(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "pageSize", defaultValue = "12") final int pageSize) {
        return favouritePresentation.favourites(user, request, page, pageSize);
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
