package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.BookingRequestService.BlockSlotOutcome;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.dto.OwnerAvailabilityView;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class PublicationAvailabilityController {

    private static final int MAX_RETURN_PATH_LENGTH = 512;
    private static final String DEFAULT_AVAILABILITY_BACK_PATH = "/profile";

    private final ItemService itemService;
    private final UserService userService;
    private final BookingRequestService bookingRequestService;

    @ModelAttribute("blockSlotForm")
    public BlockSlotForm blockSlotForm() {
        return new BlockSlotForm();
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            @ModelAttribute("blockSlotForm") final BlockSlotForm form,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        return buildManageAvailabilityView(item, requestedDate, currentUser.getId(), sanitizeReturnPath(returnParam));
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/disable", method = RequestMethod.POST)
    public ModelAndView blockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnParam,
            @Valid @ModelAttribute("blockSlotForm") final BlockSlotForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        final String safeReturn = sanitizeReturnPath(returnParam);
        if (errors.hasErrors()) {
            return buildManageAvailabilityView(item, form.getDate(), currentUser.getId(), safeReturn);
        }

        final String redirectBase = "redirect:/profile/item/" + itemId + "/availability";
        final BlockSlotOutcome outcome = bookingRequestService.blockSlotForOwner(
                itemId, currentUser.getId(), form.getDate(), form.getStartTime(), form.getEndTime());

        return new ModelAndView(
                appendReturnQuery(buildBlockRedirect(redirectBase, form.getDate(), outcome), safeReturn));
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/enable", method = RequestMethod.POST)
    public ModelAndView unblockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam("blockBookingId") final int blockBookingId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        final String safeReturn = sanitizeReturnPath(returnParam);
        final boolean removed = bookingRequestService.removeOwnerSelfBlock(blockBookingId, currentUser.getId());
        final String redirectDate = requestedDate == null || requestedDate.isBlank() ? "" : "&date=" + requestedDate;
        return new ModelAndView(appendReturnQuery(
                "redirect:/profile/item/" + itemId + "/availability?availabilityAction="
                        + (removed ? "enabled" : "notFound") + redirectDate,
                safeReturn));
    }

    private ModelAndView buildManageAvailabilityView(
            final Item item, final String requestedDate, final int ownerId, final String sanitizedReturnPath) {
        final OwnerAvailabilityView view = itemService.buildOwnerAvailabilityView(item.getId(), requestedDate, ownerId);

        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", item);
        mav.addObject("offeredDatesJson", view.getOfferedDatesJson());
        mav.addObject("blockedDatesJson", view.getBlockedDatesJson());
        mav.addObject("selectedDate", view.getSelectedDate());
        mav.addObject("slots", view.getSlots());
        mav.addObject("slotsStateJson", view.getSlotsStateJson());
        mav.addObject("personalBlocks", view.getPersonalBlocks());
        mav.addObject("personalBlockRows", view.getPersonalBlockRows());
        mav.addObject("manageAvailabilityReturnPath", sanitizedReturnPath);
        mav.addObject(
                "manageAvailabilityBackPath",
                sanitizedReturnPath != null ? sanitizedReturnPath : DEFAULT_AVAILABILITY_BACK_PATH);
        return mav;
    }

    private static String buildBlockRedirect(
            final String redirectBase, final String formDate, final BlockSlotOutcome outcome) {
        return switch (outcome) {
            case BLOCKED -> redirectBase + "?date=" + formDate + "&availabilityAction=blocked";
            case PAST_DATE -> redirectBase + "?availabilityAction=pastDate";
            case OVERLAP -> redirectBase + "?date=" + formDate + "&availabilityAction=hasBookings";
            case INVALID -> redirectBase + "?availabilityAction=invalid";
        };
    }

    /**
     * Accepts in-app paths only (same-origin relative), for a safe "back" target after managing availability.
     */
    static String sanitizeReturnPath(final String raw) {
        if (raw == null) {
            return null;
        }
        String candidate = raw.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        try {
            candidate = URLDecoder.decode(candidate, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
        candidate = candidate.trim();
        if (candidate.length() > MAX_RETURN_PATH_LENGTH) {
            return null;
        }
        if (!candidate.startsWith("/") || candidate.startsWith("//")) {
            return null;
        }
        if (candidate.contains("://") || candidate.contains("\\") || candidate.indexOf('\n') >= 0) {
            return null;
        }
        if (candidate.indexOf('\r') >= 0 || candidate.indexOf('\0') >= 0) {
            return null;
        }
        return candidate;
    }

    private static String appendReturnQuery(final String redirectModelViewUrl, final String sanitizedReturnPath) {
        if (sanitizedReturnPath == null) {
            return redirectModelViewUrl;
        }
        final int q = redirectModelViewUrl.indexOf('?');
        final String sep = q >= 0 ? "&" : "?";
        return redirectModelViewUrl + sep + "return=" + URLEncoder.encode(sanitizedReturnPath, StandardCharsets.UTF_8);
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
