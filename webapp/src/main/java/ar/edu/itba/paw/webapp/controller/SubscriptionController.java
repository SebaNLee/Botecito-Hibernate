package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.ProfileListingsViewForm;
import ar.edu.itba.paw.webapp.form.ProfileReviewsViewForm;
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

    @ModelAttribute("profileListingsView")
    public ProfileListingsViewForm defaultProfileListingsView() {
        final ProfileListingsViewForm form = new ProfileListingsViewForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @ModelAttribute("profileReviewsView")
    public ProfileReviewsViewForm defaultProfileReviewsView() {
        final ProfileReviewsViewForm form = new ProfileReviewsViewForm();
        form.setPage(1);
        return form;
    }

    @ModelAttribute("settingsView")
    public SettingsViewForm defaultSettingsView() {
        final SettingsViewForm form = new SettingsViewForm();
        form.setPage(1);
        form.setPageSize(6);
        form.setEdit(false);
        return form;
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/listings/subscribe", method = RequestMethod.POST)
    public ModelAndView subscribeFromProfileListings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileListingsView") final ProfileListingsViewForm profileListingsView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.subscribe(user.getId(), profileUserId);
        return subscriptionPresentation.subscribeFromProfileListingsResult(
                profileListingsView, profileUserId, success, redirectAttributes);
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/listings/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribeFromProfileListings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileListingsView") final ProfileListingsViewForm profileListingsView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), profileUserId);
        return subscriptionPresentation.unsubscribeFromProfileListingsResult(
                profileListingsView, profileUserId, success, redirectAttributes);
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/reviews/subscribe", method = RequestMethod.POST)
    public ModelAndView subscribeFromProfileReviews(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileReviewsView") final ProfileReviewsViewForm profileReviewsView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.subscribe(user.getId(), profileUserId);
        return subscriptionPresentation.subscribeFromProfileReviewsResult(
                profileReviewsView, profileUserId, success, redirectAttributes);
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/reviews/unsubscribe", method = RequestMethod.POST)
    public ModelAndView unsubscribeFromProfileReviews(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int profileUserId,
            @ModelAttribute("profileReviewsView") final ProfileReviewsViewForm profileReviewsView,
            final RedirectAttributes redirectAttributes) {
        final boolean success = subscriptionService.unsubscribe(user.getId(), profileUserId);
        return subscriptionPresentation.unsubscribeFromProfileReviewsResult(
                profileReviewsView, profileUserId, success, redirectAttributes);
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
