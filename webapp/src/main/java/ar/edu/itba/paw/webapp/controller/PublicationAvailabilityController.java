package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.BookingRequestService.BlockSlotOutcome;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerBuilder;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
        final List<ItemAvailability> availabilities = itemService.listAvailabilitiesByItemId(item.getId());
        final List<ItemBooking> bookings = itemService.listBookingsByItemId(item.getId());
        final AvailabilityPickerBuilder.Data availabilityData =
                AvailabilityPickerBuilder.build(availabilities, bookings);
        final List<String> offeredDates = availabilityData.offeredDates();
        final String selectedDate = requestedDate != null && offeredDates.contains(requestedDate)
                ? requestedDate
                : (offeredDates.isEmpty() ? null : offeredDates.get(0));
        final List<ItemBooking> personalBlocks = bookings.stream()
                .filter(b -> b.getGuestId() != null && b.getGuestId() == ownerId)
                .filter(b -> b.getState() == BookingState.BOOKING_CONFIRMED)
                .toList();
        final List<Map<String, Object>> slots = buildSlots(selectedDate, availabilities, bookings, ownerId);
        final List<String> blockedDates = new ArrayList<>();
        for (final ItemBooking block : personalBlocks) {
            if (block.getStartTime() != null) {
                blockedDates.add(block.getStartTime().toLocalDate().toString());
            }
        }

        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", item);
        mav.addObject("offeredDatesJson", toJsonArray(offeredDates));
        mav.addObject("blockedDatesJson", toJsonArray(blockedDates));
        mav.addObject("selectedDate", selectedDate);
        mav.addObject("slots", slots);
        mav.addObject("slotsStateJson", slotsToJson(slots));
        mav.addObject("personalBlocks", personalBlocks);
        mav.addObject("personalBlockRows", List.of());
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

    private static String slotsToJson(final List<Map<String, Object>> slots) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            final Map<String, Object> slot = slots.get(i);
            json.append("{\"start\":\"")
                    .append(slot.get("startTime"))
                    .append("\",\"end\":\"")
                    .append(slot.get("endTime"))
                    .append("\",\"state\":\"")
                    .append(slot.get("state"))
                    .append("\"}");
        }
        return json.append(']').toString();
    }

    private static List<Map<String, Object>> buildSlots(
            final String selectedDate,
            final List<ItemAvailability> availabilities,
            final List<ItemBooking> bookings,
            final int ownerId) {
        if (selectedDate == null || selectedDate.isBlank()) {
            return List.of();
        }
        final LocalDate day;
        try {
            day = LocalDate.parse(selectedDate);
        } catch (final RuntimeException e) {
            return List.of();
        }
        final TreeSet<String> scheduled = new TreeSet<>();
        for (final ItemAvailability availability : availabilities) {
            if (availability.getWeekday() != day.getDayOfWeek()) {
                continue;
            }
            final int startMinute = availability.getStartTime().toSecondOfDay() / 60;
            final int endMinute = availability.getEndTime().toSecondOfDay() / 60;
            for (int minute = startMinute; minute < endMinute; minute += 30) {
                scheduled.add(
                        LocalTime.ofSecondOfDay((long) minute * 60).toString().substring(0, 5));
            }
        }
        final Set<String> guestBooked = new HashSet<>();
        final Map<String, Integer> ownerBlocks = new HashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getStartTime() == null || booking.getEndTime() == null) {
                continue;
            }
            OffsetDateTime cursor = booking.getStartTime();
            while (cursor.isBefore(booking.getEndTime())) {
                if (day.equals(cursor.toLocalDate())) {
                    final String key = cursor.toLocalTime().toString().substring(0, 5);
                    if (booking.getGuestId() != null && booking.getGuestId() == ownerId && booking.getId() != null) {
                        ownerBlocks.put(key, booking.getId());
                    } else {
                        guestBooked.add(key);
                    }
                }
                cursor = cursor.plusMinutes(30);
            }
        }
        final List<Map<String, Object>> slots = new ArrayList<>();
        for (final String time : scheduled) {
            final LocalTime start = LocalTime.parse(time);
            final String end = start.plusMinutes(30).toString().substring(0, 5);
            final Integer blockId = ownerBlocks.get(time);
            final String state = blockId != null ? "BLOCKED" : (guestBooked.contains(time) ? "BOOKED" : "AVAILABLE");
            final Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("startTime", time);
            slot.put("endTime", end);
            slot.put("state", state);
            slot.put("blockBookingId", blockId);
            slot.put("modalIdSuffix", time.replace(":", ""));
            slots.add(slot);
        }
        return slots;
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
