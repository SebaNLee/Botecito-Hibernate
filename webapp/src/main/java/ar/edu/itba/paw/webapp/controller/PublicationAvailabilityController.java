package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.DisabledTimeSlot;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.DisabledTimeSlotService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.SlotHasActiveBookingsException;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.DisableSlotForm;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.validation.Valid;
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

@Controller
public class PublicationAvailabilityController {

    private static final int PICKER_MONTHS_AROUND_TODAY = 2;
    private static final int SLOT_STEP_MINUTES = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ItemService itemService;
    private final UserService userService;
    private final DisabledTimeSlotService disabledTimeSlotService;

    public PublicationAvailabilityController(
            final ItemService itemService,
            final UserService userService,
            final DisabledTimeSlotService disabledTimeSlotService) {
        this.itemService = itemService;
        this.userService = userService;
        this.disabledTimeSlotService = disabledTimeSlotService;
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @ModelAttribute("disableSlotForm") final DisableSlotForm form) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            return new ModelAndView("redirect:/profile/dashboard?publishAction=forbidden");
        }

        return buildManageAvailabilityView(item, requestedDate);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/disable", method = RequestMethod.POST)
    public ModelAndView disableSlot(
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("disableSlotForm") final DisableSlotForm form,
            final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            return new ModelAndView("redirect:/profile/dashboard?publishAction=forbidden");
        }

        if (errors.hasErrors()) {
            return buildManageAvailabilityView(item, form.getDate());
        }

        final String redirectBase = "redirect:/profile/item/" + itemId + "/availability";
        try {
            final LocalDate date = LocalDate.parse(form.getDate());
            final LocalTime startTime = LocalTime.parse(form.getStartTime());
            final LocalTime endTime = LocalTime.parse(form.getEndTime());
            if (date.isBefore(LocalDate.now())) {
                return new ModelAndView(redirectBase + "?availabilityAction=pastDate");
            }
            disabledTimeSlotService.disable(itemId, date, startTime, endTime);
        } catch (final SlotHasActiveBookingsException exception) {
            return new ModelAndView(redirectBase + "?date=" + form.getDate() + "&availabilityAction=hasBookings");
        } catch (final DateTimeParseException | IllegalArgumentException exception) {
            return new ModelAndView(redirectBase + "?availabilityAction=invalid");
        }

        return new ModelAndView(redirectBase + "?date=" + form.getDate() + "&availabilityAction=disabled");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/enable", method = RequestMethod.POST)
    public ModelAndView enableSlot(
            @PathVariable("id") final int itemId,
            @RequestParam("disabledSlotId") final int disabledSlotId,
            @RequestParam(value = "date", required = false) final String requestedDate) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || !currentUser.getId().equals(item.getOwnerId())) {
            return new ModelAndView("redirect:/profile/dashboard?publishAction=forbidden");
        }

        final boolean removed = disabledTimeSlotService.reEnable(itemId, disabledSlotId);
        final String redirectDate = requestedDate == null || requestedDate.isBlank() ? "" : "&date=" + requestedDate;
        return new ModelAndView("redirect:/profile/item/" + itemId + "/availability?availabilityAction="
                + (removed ? "enabled" : "notFound") + redirectDate);
    }

    private ModelAndView buildManageAvailabilityView(final Item item, final String requestedDate) {
        final List<ItemAvailability> availabilities = itemService.listAvailabilitiesByItemId(item.getId());
        final List<ItemBooking> bookings = itemService.listBookingsByItemId(item.getId());
        final List<DisabledTimeSlot> disabledSlots = disabledTimeSlotService.listByItem(item.getId());

        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate =
                LocalDate.now().plusMonths(PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());

        final Map<String, TreeSet<String>> scheduledTimesByDate =
                buildScheduledTimesByDate(availabilities, startDate, endDate);
        final Map<String, TreeSet<String>> bookedTimesByDate = buildBookedTimesByDate(bookings, startDate, endDate);

        final List<String> offeredDates = new ArrayList<>(scheduledTimesByDate.keySet());
        final String selectedDate = resolveSelectedDate(requestedDate, offeredDates);

        final List<SlotViewModel> slots = selectedDate == null
                ? List.of()
                : buildSlots(
                        scheduledTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        bookedTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        indexDisabledSlotsByStartTime(disabledSlots, selectedDate));

        final List<String> disabledDates = new ArrayList<>();
        for (final DisabledTimeSlot disabled : disabledSlots) {
            final String iso = disabled.getSlotDate().format(DATE_FORMAT);
            if (!disabledDates.contains(iso)) {
                disabledDates.add(iso);
            }
        }

        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", item);
        mav.addObject("offeredDatesJson", toJsonArray(offeredDates));
        mav.addObject("disabledDatesJson", toJsonArray(disabledDates));
        mav.addObject("selectedDate", selectedDate == null ? "" : selectedDate);
        mav.addObject("slots", slots);
        mav.addObject("disabledSlots", disabledSlots);
        return mav;
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

    private static Map<String, TreeSet<String>> buildBookedTimesByDate(
            final List<ItemBooking> bookings, final LocalDate startDate, final LocalDate endDate) {
        final Map<String, TreeSet<String>> collected = new TreeMap<>();
        for (final ItemBooking booking : bookings) {
            if (!isBlockingBooking(booking)) {
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

    private static Map<String, Integer> indexDisabledSlotsByStartTime(
            final List<DisabledTimeSlot> disabledSlots, final String selectedDate) {
        final Map<String, Integer> index = new TreeMap<>();
        final LocalDate parsedSelected;
        try {
            parsedSelected = LocalDate.parse(selectedDate);
        } catch (final DateTimeParseException exception) {
            return index;
        }
        for (final DisabledTimeSlot disabled : disabledSlots) {
            if (!parsedSelected.equals(disabled.getSlotDate())) {
                continue;
            }
            final int startMinute = disabled.getStartTime().toSecondOfDay() / 60;
            final int endMinute = disabled.getEndTime().toSecondOfDay() / 60;
            for (int minute = startMinute; minute < endMinute; minute += SLOT_STEP_MINUTES) {
                index.put(LocalTime.ofSecondOfDay((long) minute * 60).format(TIME_FORMAT), disabled.getId());
            }
        }
        return index;
    }

    private static List<SlotViewModel> buildSlots(
            final TreeSet<String> scheduledTimes,
            final TreeSet<String> bookedTimes,
            final Map<String, Integer> disabledStartToId) {
        final List<SlotViewModel> slots = new ArrayList<>();
        for (final String time : scheduledTimes) {
            final LocalTime startTime = LocalTime.parse(time);
            final String endTime = startTime.plusMinutes(SLOT_STEP_MINUTES).format(TIME_FORMAT);
            final boolean booked = bookedTimes.contains(time);
            final Integer disabledId = disabledStartToId.get(time);
            final String state = disabledId != null ? "DISABLED" : (booked ? "BOOKED" : "AVAILABLE");
            slots.add(new SlotViewModel(time, endTime, state, disabledId));
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
        private final Integer disabledSlotId;

        public SlotViewModel(
                final String startTime, final String endTime, final String state, final Integer disabledSlotId) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.state = state;
            this.disabledSlotId = disabledSlotId;
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

        public Integer getDisabledSlotId() {
            return disabledSlotId;
        }

        public String getModalIdSuffix() {
            return startTime.replace(":", "");
        }
    }
}
