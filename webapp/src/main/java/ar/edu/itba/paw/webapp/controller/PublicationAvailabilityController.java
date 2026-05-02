package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.OverlappingActiveBookingException;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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

    private static final int PICKER_MONTHS_AROUND_TODAY = 2;
    private static final int SLOT_STEP_MINUTES = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
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
        try {
            final LocalDate date = LocalDate.parse(form.getDate());
            final LocalTime startTime = LocalTime.parse(form.getStartTime());
            final LocalTime endTime = LocalTime.parse(form.getEndTime());
            if (date.isBefore(LocalDate.now())) {
                return new ModelAndView(appendReturnQuery(redirectBase + "?availabilityAction=pastDate", safeReturn));
            }
            final ZoneId zone = ZoneId.systemDefault();
            final OffsetDateTime startOdt =
                    LocalDateTime.of(date, startTime).atZone(zone).toOffsetDateTime();
            final OffsetDateTime endOdt =
                    LocalDateTime.of(date, endTime).atZone(zone).toOffsetDateTime();
            bookingRequestService.createOwnerSelfBlock(itemId, currentUser.getId(), startOdt, endOdt);
        } catch (final OverlappingActiveBookingException exception) {
            return new ModelAndView(appendReturnQuery(
                    redirectBase + "?date=" + form.getDate() + "&availabilityAction=hasBookings", safeReturn));
        } catch (final DateTimeParseException | IllegalArgumentException exception) {
            return new ModelAndView(appendReturnQuery(redirectBase + "?availabilityAction=invalid", safeReturn));
        }

        return new ModelAndView(appendReturnQuery(
                redirectBase + "?date=" + form.getDate() + "&availabilityAction=blocked", safeReturn));
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
        final List<ItemBooking> personalBlocks = listPersonalBlocks(bookings, ownerId);
        final List<PersonalBlockListRow> personalBlockRows = toPersonalBlockRows(personalBlocks);

        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate =
                LocalDate.now().plusMonths(PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());

        final Map<String, TreeSet<String>> scheduledTimesByDate =
                buildScheduledTimesByDate(availabilities, startDate, endDate);
        final Map<String, TreeSet<String>> guestBookedTimesByDate =
                buildGuestBookedTimesByDate(bookings, ownerId, startDate, endDate);

        final List<String> offeredDates = new ArrayList<>(scheduledTimesByDate.keySet());
        final String selectedDate = resolveSelectedDate(requestedDate, offeredDates);

        final List<SlotViewModel> slots = selectedDate == null
                ? List.of()
                : buildSlots(
                        scheduledTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        guestBookedTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        indexOwnerPersonalBlockStartsBySlot(personalBlocks, selectedDate));

        final List<String> blockedDates = new ArrayList<>();
        for (final ItemBooking block : personalBlocks) {
            addBookingDatesInRange(block, startDate, endDate, blockedDates);
        }

        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", item);
        mav.addObject("offeredDatesJson", toJsonArray(offeredDates));
        mav.addObject("blockedDatesJson", toJsonArray(blockedDates));
        mav.addObject("selectedDate", selectedDate == null ? "" : selectedDate);
        mav.addObject("slots", slots);
        mav.addObject("slotsStateJson", slotsToJson(slots));
        mav.addObject("personalBlocks", personalBlocks);
        mav.addObject("personalBlockRows", personalBlockRows);
        mav.addObject("manageAvailabilityReturnPath", sanitizedReturnPath);
        mav.addObject(
                "manageAvailabilityBackPath",
                sanitizedReturnPath != null ? sanitizedReturnPath : DEFAULT_AVAILABILITY_BACK_PATH);
        return mav;
    }

    /**
     * Accepts in-app paths only (same-origin relative), for a safe "back" target after managing availability.
     */
    private static String slotsToJson(final List<SlotViewModel> slots) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            final SlotViewModel slot = slots.get(i);
            json.append("{\"start\":\"")
                    .append(jsonEscape(slot.getStartTime()))
                    .append("\",\"end\":\"")
                    .append(jsonEscape(slot.getEndTime()))
                    .append("\",\"state\":\"")
                    .append(jsonEscape(slot.getState()))
                    .append("\"}");
        }
        return json.append(']').toString();
    }

    private static String jsonEscape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
        if (!candidate.startsWith("/")) {
            return null;
        }
        if (candidate.startsWith("//")) {
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

    private static List<PersonalBlockListRow> toPersonalBlockRows(final List<ItemBooking> personalBlocks) {
        final List<ItemBooking> sorted = new ArrayList<>(personalBlocks);
        sorted.sort(Comparator.comparing(ItemBooking::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        final List<PersonalBlockListRow> rows = new ArrayList<>();
        for (final ItemBooking block : sorted) {
            if (block.getId() == null || block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }
            rows.add(new PersonalBlockListRow(
                    block.getId(),
                    block.getStartTime().toLocalDate().format(DATE_FORMAT),
                    block.getStartTime().toLocalTime().format(TIME_FORMAT),
                    block.getEndTime().toLocalTime().format(TIME_FORMAT)));
        }
        return rows;
    }

    private static List<ItemBooking> listPersonalBlocks(final List<ItemBooking> bookings, final int ownerId) {
        final List<ItemBooking> out = new ArrayList<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getGuestId() != null
                    && Objects.equals(booking.getGuestId(), ownerId)
                    && booking.getState() == BookingState.BOOKING_CONFIRMED) {
                out.add(booking);
            }
        }
        return out;
    }

    private static void addBookingDatesInRange(
            final ItemBooking booking,
            final LocalDate rangeStart,
            final LocalDate rangeEnd,
            final List<String> outIsoDates) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return;
        }
        LocalDate d = booking.getStartTime().toLocalDate();
        final LocalDate last = booking.getEndTime().toLocalDate();
        while (!d.isAfter(last)) {
            if (!d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) {
                final String iso = d.format(DATE_FORMAT);
                if (!outIsoDates.contains(iso)) {
                    outIsoDates.add(iso);
                }
            }
            d = d.plusDays(1);
        }
    }

    private static Map<String, TreeSet<String>> buildScheduledTimesByDate(
            final List<ItemAvailability> availabilities, final LocalDate startDate, final LocalDate endDate) {
        final Map<String, TreeSet<String>> collected = new TreeMap<>();
        for (final ItemAvailability availability : availabilities) {
            final DayOfWeek weekday = availability.getWeekday();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (date.getDayOfWeek() != weekday) {
                    continue;
                }
                addTimeRange(
                        collected, date.format(DATE_FORMAT), availability.getStartTime(), availability.getEndTime());
            }
        }
        return collected;
    }

    private static Map<String, TreeSet<String>> buildGuestBookedTimesByDate(
            final List<ItemBooking> bookings, final int ownerId, final LocalDate startDate, final LocalDate endDate) {
        final Map<String, TreeSet<String>> collected = new TreeMap<>();
        for (final ItemBooking booking : bookings) {
            if (!isBlockingBooking(booking)) {
                continue;
            }
            if (booking.getGuestId() != null && Objects.equals(booking.getGuestId(), ownerId)) {
                continue;
            }
            OffsetDateTime cursor = booking.getStartTime();
            final OffsetDateTime end = booking.getEndTime();
            while (cursor.isBefore(end)) {
                final LocalDate date = cursor.toLocalDate();
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    collected
                            .computeIfAbsent(date.format(DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(cursor.toLocalTime().format(TIME_FORMAT));
                }
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
            }
        }
        return collected;
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == null
                || booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED
                || booking.getState() == BookingState.BOOKING_PAYMENT_SUBMITTED
                || booking.getState() == BookingState.BOOKING_PAID;
    }

    private static Map<String, Integer> indexOwnerPersonalBlockStartsBySlot(
            final List<ItemBooking> personalBlocks, final String selectedDateIso) {
        final Map<String, Integer> index = new TreeMap<>();
        final LocalDate parsedSelected;
        try {
            parsedSelected = LocalDate.parse(selectedDateIso);
        } catch (final DateTimeParseException exception) {
            return index;
        }
        for (final ItemBooking block : personalBlocks) {
            if (block.getId() == null || block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }
            OffsetDateTime cursor = block.getStartTime();
            final OffsetDateTime end = block.getEndTime();
            while (cursor.isBefore(end)) {
                if (parsedSelected.equals(cursor.toLocalDate())) {
                    index.put(cursor.toLocalTime().format(TIME_FORMAT), block.getId());
                }
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
            }
        }
        return index;
    }

    private static List<SlotViewModel> buildSlots(
            final TreeSet<String> scheduledTimes,
            final TreeSet<String> guestBookedTimes,
            final Map<String, Integer> ownerBlockStartToBookingId) {
        final List<SlotViewModel> slots = new ArrayList<>();
        for (final String time : scheduledTimes) {
            final LocalTime startTime = LocalTime.parse(time);
            final String endTime = startTime.plusMinutes(SLOT_STEP_MINUTES).format(TIME_FORMAT);
            final boolean guestBooked = guestBookedTimes.contains(time);
            final Integer blockBookingId = ownerBlockStartToBookingId.get(time);
            final String state = blockBookingId != null ? "BLOCKED" : (guestBooked ? "BOOKED" : "AVAILABLE");
            slots.add(new SlotViewModel(time, endTime, state, blockBookingId));
        }
        return slots;
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collected,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute < endMinute; minute += SLOT_STEP_MINUTES) {
            collected
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(LocalTime.ofSecondOfDay((long) minute * 60).format(TIME_FORMAT));
        }
    }

    private static String resolveSelectedDate(final String requestedDate, final List<String> offeredDates) {
        if (requestedDate != null && offeredDates.contains(requestedDate)) {
            return requestedDate;
        }
        return offeredDates.isEmpty() ? null : offeredDates.get(0);
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

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    public static final class SlotViewModel {
        private final String startTime;
        private final String endTime;
        private final String state;
        private final Integer blockBookingId;

        public SlotViewModel(
                final String startTime, final String endTime, final String state, final Integer blockBookingId) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.state = state;
            this.blockBookingId = blockBookingId;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getState() {
            return state;
        }

        public Integer getBlockBookingId() {
            return blockBookingId;
        }

        public String getModalIdSuffix() {
            return startTime.replace(":", "");
        }
    }

    public static final class PersonalBlockListRow {
        private final int bookingId;
        private final String dateIso;
        private final String startTime;
        private final String endTime;

        public PersonalBlockListRow(
                final int bookingId, final String dateIso, final String startTime, final String endTime) {
            this.bookingId = bookingId;
            this.dateIso = dateIso;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public int getBookingId() {
            return bookingId;
        }

        public String getDateIso() {
            return dateIso;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }
    }
}
