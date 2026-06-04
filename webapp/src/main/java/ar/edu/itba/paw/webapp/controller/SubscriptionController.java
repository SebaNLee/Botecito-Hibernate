package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.SubscriptionPresentation;
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
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPresentation subscriptionPresentation;

    @RequestMapping(value = "/users/{id:[1-9]\\d*}/subscribe", method = RequestMethod.POST)
    public ModelAndView subscribe(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int subscribedToId,
            @RequestParam(value = "return", required = false) final String returnPath,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.subscribe(user.getId(), subscribedToId);
        return subscriptionPresentation.subscribeResult(success, returnPath, redirectAttributes);
    }

    @RequestMapping(value = "/users/{id:[1-9]\\d*}/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribe(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int subscribedToId,
            @RequestParam(value = "return", required = false) final String returnPath,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), subscribedToId);
        return subscriptionPresentation.unsubscribeResult(success, returnPath, redirectAttributes);
    }
}
