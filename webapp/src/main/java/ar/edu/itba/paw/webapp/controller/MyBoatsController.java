package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.SaveSelfBlocksForm;
import ar.edu.itba.paw.webapp.presentation.AvailabilityPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsActionsPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsPresentation;
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
public class MyBoatsController {

    private final MyBoatsPresentation myBoatsPresentation;
    private final MyBoatsActionsPresentation myBoatsActionsPresentation;
    private final AvailabilityPresentation availabilityPresentation;

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "pageSize", defaultValue = "12") final int pageSize) {
        return myBoatsPresentation.myBoats(user, request, page, pageSize);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.disablePublication(user, itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.enablePublication(user, itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.hardDeletePublication(user, itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        return availabilityPresentation.manageAvailabilityPage(
                user, itemId, requestedDate, returnParam, request, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability/save", method = RequestMethod.POST)
    public ModelAndView saveSelfBlocks(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnParam,
            @Valid @ModelAttribute final SaveSelfBlocksForm saveSelfBlocksForm,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return availabilityPresentation.saveSelfBlocks(
                user, itemId, returnParam, saveSelfBlocksForm, errors, redirectAttributes);
    }
}
