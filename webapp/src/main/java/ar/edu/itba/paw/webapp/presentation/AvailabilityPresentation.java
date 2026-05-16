package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.BookingService.BlockSlotOutcome;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class AvailabilityPresentation {

    private static final int MAX_RETURN_PATH_LENGTH = 512;
    private static final String DEFAULT_AVAILABILITY_BACK_PATH = "/profile";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingService bookingInterface;
    private final ItemService itemInterface;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView manageAvailabilityPage(
            final int itemId,
            final String requestedDate,
            final String returnParam,
            final RedirectAttributes redirectAttributes) {
        final Users currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final String safeReturn = sanitizeReturnPath(returnParam);
        return bookingInterface
                .loadOwnerAvailabilityPage(itemId, currentUser.getId(), requestedDate)
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
        final Users currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final String safeReturn = sanitizeReturnPath(returnParam);
        if (itemInterface
                .findMyBoatsItemByIdForOwner(itemId, currentUser.getId())
                .isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }
        if (errors.hasErrors()) {
            return bookingInterface
                    .loadOwnerAvailabilityPage(
                            itemId, currentUser.getId(), form.getDate().toString())
                    .map(model -> buildManageAvailabilityView(model, safeReturn))
                    .orElseGet(() -> {
                        ToastSupport.error(redirectAttributes, "profile.publications.error");
                        return new ModelAndView("redirect:/my-boats");
                    });
        }
        final String dateStr = form.getDate().toString();
        final String startStr = form.getStartTime().format(TIME_FMT);
        final String endStr = form.getEndTime().format(TIME_FMT);
        final String redirectBase = "redirect:/my-boats/" + itemId + "/availability";
        final BlockSlotOutcome outcome =
                bookingInterface.blockSlotForOwner(itemId, currentUser.getId(), dateStr, startStr, endStr);
        return new ModelAndView(appendReturnQuery(buildBlockRedirect(redirectBase, dateStr, outcome), safeReturn));
    }

    public ModelAndView unblockSlot(
            final int itemId,
            final int blockBookingId,
            final String requestedDate,
            final String returnParam,
            final RedirectAttributes redirectAttributes) {
        final Users currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        if (itemInterface
                .findMyBoatsItemByIdForOwner(itemId, currentUser.getId())
                .isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }
        final String safeReturn = sanitizeReturnPath(returnParam);
        final boolean removed = bookingInterface.removeOwnerSelfBlock(blockBookingId, currentUser.getId());
        final String redirectDate = requestedDate == null || requestedDate.isBlank() ? "" : "&date=" + requestedDate;
        return new ModelAndView(appendReturnQuery(
                "redirect:/my-boats/" + itemId + "/availability?availabilityAction="
                        + (removed ? "enabled" : "notFound") + redirectDate,
                safeReturn));
    }

    private ModelAndView buildManageAvailabilityView(
            final OwnerAvailabilityPage model, final String sanitizedReturnPath) {
        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", model.getItem());
        mav.addObject("offeredDatesJson", toJsonArray(model.getOfferedDates()));
        mav.addObject("blockedDatesJson", toJsonArray(model.getBlockedDates()));
        mav.addObject("selectedDate", model.getSelectedDate());
        final Set<Integer> selfBlockIds =
                model.getOwnerSelfBlocks().stream().map(Booking::getId).collect(Collectors.toSet());
        final List<SlotRow> slots = buildSlotGrid(
                model.getSelectedDate(), model.getAvailabilityWindows(), model.getActiveBookings(), selfBlockIds);
        mav.addObject("slots", slots);
        mav.addObject("slotsStateJson", slotsToJson(slots));
        mav.addObject("personalBlockRows", toPersonalBlockRows(model.getOwnerSelfBlocks(), model.getTimezone()));
        mav.addObject("manageAvailabilityReturnPath", sanitizedReturnPath);
        mav.addObject(
                "manageAvailabilityBackPath",
                sanitizedReturnPath != null ? sanitizedReturnPath : DEFAULT_AVAILABILITY_BACK_PATH);
        return mav;
    }

    static List<SlotRow> buildSlotGrid(
            final String selectedDate,
            final List<Availability> availabilities,
            final List<Booking> bookings,
            final Set<Integer> selfBlockBookingIds) {
        if (selectedDate == null || selectedDate.isBlank()) {
            return List.of();
        }
        final LocalDate day = LocalDate.parse(selectedDate);
        final TreeSet<String> scheduled = new TreeSet<>();
        for (final Availability availability : availabilities) {
            if (availability.getWeekday().name().equals(day.getDayOfWeek().name())) {
                final int startMinute = availability.getStartTime().toSecondOfDay() / 60;
                final int endMinute = availability.getEndTime().toSecondOfDay() / 60;
                for (int minute = startMinute; minute < endMinute; minute += 30) {
                    scheduled.add(LocalTime.ofSecondOfDay((long) minute * 60)
                            .toString()
                            .substring(0, 5));
                }
            }
        }
        final Set<String> guestBooked = new HashSet<>();
        final Map<String, Integer> ownerBlocks = new HashMap<>();
        for (final Booking booking : bookings) {
            if (!isBlockingState(booking.getStatus())) {
                continue;
            }
            if (booking.getStart() == null || booking.getEnd() == null) {
                continue;
            }
            OffsetDateTime cursor =
                    booking.getStart().atZone(ZoneOffset.UTC).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
            final OffsetDateTime end =
                    booking.getEnd().atZone(ZoneOffset.UTC).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
            while (cursor.isBefore(end)) {
                if (day.equals(cursor.toLocalDate())) {
                    final String key = cursor.toLocalTime().toString().substring(0, 5);
                    if (booking.getId() != null && selfBlockBookingIds.contains(booking.getId())) {
                        ownerBlocks.put(key, booking.getId());
                    } else {
                        guestBooked.add(key);
                    }
                }
                cursor = cursor.plusMinutes(30);
            }
        }
        final List<SlotRow> slots = new ArrayList<>();
        for (final String time : scheduled) {
            final LocalTime start = LocalTime.parse(time);
            final String end = start.plusMinutes(30).toString().substring(0, 5);
            final Integer blockId = ownerBlocks.get(time);
            final String state = blockId != null ? "BLOCKED" : (guestBooked.contains(time) ? "BOOKED" : "AVAILABLE");
            slots.add(new SlotRow(time, end, state, blockId, time.replace(":", "")));
        }
        return slots;
    }

    private static boolean isBlockingState(final BookingStatusEnum status) {
        return status == BookingStatusEnum.PENDING
                || status == BookingStatusEnum.ACCEPTED
                || status == BookingStatusEnum.PAID
                || status == BookingStatusEnum.CONFIRMED;
    }

    private static List<PersonalBlockRow> toPersonalBlockRows(final List<Booking> selfBlocks, final String timezone) {
        final ZoneId zone = ZoneId.of(timezone);
        return selfBlocks.stream()
                .filter(b -> b.getId() != null && b.getStart() != null && b.getEnd() != null)
                .sorted((a, b) -> a.getStart().compareTo(b.getStart()))
                .map(b -> {
                    final LocalDate date = b.getStart()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalDate();
                    final String startLocal = b.getStart()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .toString()
                            .substring(0, 5);
                    final String endLocal = b.getEnd()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .toString()
                            .substring(0, 5);
                    return new PersonalBlockRow(b.getId(), date.toString(), startLocal, endLocal);
                })
                .toList();
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

    private static String appendReturnQuery(final String redirectUrl, final String sanitizedReturnPath) {
        if (sanitizedReturnPath == null) {
            return redirectUrl;
        }
        final int q = redirectUrl.indexOf('?');
        final String sep = q >= 0 ? "&" : "?";
        return redirectUrl + sep + "return=" + URLEncoder.encode(sanitizedReturnPath, StandardCharsets.UTF_8);
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

    private static String slotsToJson(final List<SlotRow> slots) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            final SlotRow slot = slots.get(i);
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

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class SlotRow {
        private final String startTime;
        private final String endTime;
        private final String state;
        private final Integer blockBookingId;
        private final String modalIdSuffix;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class PersonalBlockRow {
        private final int bookingId;
        private final String dateIso;
        private final String startTime;
        private final String endTime;
    }
}
