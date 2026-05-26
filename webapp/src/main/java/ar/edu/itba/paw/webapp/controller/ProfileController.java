package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfilePresentation profilePresentation;

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView profile(
            @AuthenticationPrincipal final BotecitoUserDetails viewer,
            @PathVariable("id") final int id,
            @RequestParam(value = "tab", defaultValue = "listings") final String tab,
            @RequestParam(value = "listingsPage", defaultValue = "1") final int listingsPage,
            @RequestParam(value = "listingsPageSize", defaultValue = "6") final int listingsPageSize,
            @RequestParam(value = "reviewsPage", defaultValue = "1") final int reviewsPage,
            @RequestParam(value = "reviewsPageSize", defaultValue = "5") final int reviewsPageSize,
            final HttpServletRequest request) {
        return profilePresentation.profile(
                viewer, id, tab, listingsPage, listingsPageSize, reviewsPage, reviewsPageSize, request);
    }
}
