package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.BookingRequestService.BlockSlotOutcome;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ManageAvailabilityPageModel;
import ar.edu.itba.paw.services.ManageAvailabilitySlotRow;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public final class PublicationAvailabilityMvcSupport {

    private static final int MAX_RETURN_PATH_LENGTH = 512;
    private static final String DEFAULT_AVAILABILITY_BACK_PATH = "/profile";

    private final UserService userService;
    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;

    public ModelAndView manageAvailabilityPage(
            final int itemId,
            final String requestedDate,
            final String returnParam,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final String safeReturn = sanitizeReturnPath(returnParam);
        return itemService
                .loadManageAvailabilityPageModel(itemId, currentUser.getId(), requestedDate)
                .map(model -> buildManageAvailabilityView(model, safeReturn))
                .orElseGet(() -> {
                    ToastSupport.error(redirectAttributes, "profile.publications.error");
                    return new ModelAndView("redirect:/my-boats");
                });
    }

    public ModelAndView blockSlot(
            final int itemId,
            final String returnParam,
            final BlockSlotForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final String safeReturn = sanitizeReturnPath(returnParam);
        if (itemService.findItemByIdForOwner(itemId, currentUser.getId()).isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }
        if (errors.hasErrors()) {
            return itemService
                    .loadManageAvailabilityPageModel(itemId, currentUser.getId(), form.getDate())
                    .map(model -> buildManageAvailabilityView(model, safeReturn))
                    .orElseGet(() -> {
                        ToastSupport.error(redirectAttributes, "profile.publications.error");
                        return new ModelAndView("redirect:/my-boats");
                    });
        }
        final String redirectBase = "redirect:/profile/item/" + itemId + "/availability";
        final BlockSlotOutcome outcome = bookingRequestService.blockSlotForOwner(
                itemId, currentUser.getId(), form.getDate(), form.getStartTime(), form.getEndTime());
        return new ModelAndView(
                appendReturnQuery(buildBlockRedirect(redirectBase, form.getDate(), outcome), safeReturn));
    }

    public ModelAndView unblockSlot(
            final int itemId,
            final int blockBookingId,
            final String requestedDate,
            final String returnParam,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        if (itemService.findItemByIdForOwner(itemId, currentUser.getId()).isEmpty()) {
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
            final ManageAvailabilityPageModel model, final String sanitizedReturnPath) {
        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", model.getItem());
        mav.addObject("offeredDatesJson", toJsonArray(model.getOfferedDates()));
        mav.addObject("blockedDatesJson", toJsonArray(model.getBlockedDates()));
        mav.addObject("selectedDate", model.getSelectedDate());
        mav.addObject("slots", model.getSlots());
        mav.addObject("slotsStateJson", slotsToJson(model.getSlots()));
        mav.addObject("personalBlockRows", model.getPersonalBlockRows());
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

    private static String toJsonArray(final List<String> values) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(values.get(i).replace("\"", "\\\"")).append('"');
        }
        return json.append(']').toString();
    }

    private static String slotsToJson(final List<ManageAvailabilitySlotRow> slots) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            final ManageAvailabilitySlotRow slot = slots.get(i);
            json.append("{\"start\":\"")
                    .append(slot.getStartTime())
                    .append("\",\"end\":\"")
                    .append(slot.getEndTime())
                    .append("\",\"state\":\"")
                    .append(slot.getState())
                    .append("\"}");
        }
        return json.append(']').toString();
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
