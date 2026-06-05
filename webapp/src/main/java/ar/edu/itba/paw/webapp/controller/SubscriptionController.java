package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.ProfileViewForm;
import ar.edu.itba.paw.webapp.form.SettingsViewForm;
import ar.edu.itba.paw.webapp.presentation.SubscriptionPresentation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPresentation subscriptionPresentation;

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/subscribe", method = RequestMethod.POST)
    public ModelAndView subscribeFromProfile(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileView") final ProfileViewForm profileView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.subscribe(user.getId(), profileUserId);
        return subscriptionPresentation.subscribeFromProfileResult(
                profileView, profileUserId, success, redirectAttributes);
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribeFromProfile(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileView") final ProfileViewForm profileView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), profileUserId);
        return subscriptionPresentation.unsubscribeFromProfileResult(
                profileView, profileUserId, success, redirectAttributes);
    }

    @RequestMapping(value = "/users/{id:[1-9]\\d*}/subscribe", method = RequestMethod.POST)
    public ModelAndView subscribeFromItemDetail(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int subscribedToId,
            @ModelAttribute("itemDetailView") final ItemDetailViewForm itemDetailView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.subscribe(user.getId(), subscribedToId);
        return subscriptionPresentation.subscribeFromItemDetailResult(itemDetailView, success, redirectAttributes);
    }

    @RequestMapping(value = "/users/{id:[1-9]\\d*}/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribeFromItemDetail(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int subscribedToId,
            @ModelAttribute("itemDetailView") final ItemDetailViewForm itemDetailView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), subscribedToId);
        return subscriptionPresentation.unsubscribeFromItemDetailResult(itemDetailView, success, redirectAttributes);
    }

    @RequestMapping(value = "/settings/subscriptions/{id:[1-9]\\d*}/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribeFromSettings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int subscribedToId,
            @ModelAttribute("settingsView") final SettingsViewForm settingsView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), subscribedToId);
        return subscriptionPresentation.unsubscribeFromSettingsResult(settingsView, success, redirectAttributes);
    }
}
