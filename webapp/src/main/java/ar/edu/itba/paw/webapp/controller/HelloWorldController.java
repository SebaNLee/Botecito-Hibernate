package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ClassUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ClassUserService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloWorldController {
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESERVATION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int TIME_SLOT_STEP_MINUTES = 30;
    private static final int MIN_BOOKING_DURATION_MINUTES = 120;
    private static final int PICKER_MONTHS_AROUND_TODAY = 2;
    private final ItemService itemService;
    private final ClassUserService classUserService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        final ModelAndView mav = new ModelAndView("index");
        addAvailabilityPickerData(
                mav,
                "search",
                buildAvailabilityPickerData(itemService.listAvailabilities(), itemService.listBookings()));
        return mav;
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            @RequestParam(value = "location", required = false) final String requestedLocation,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime,
            @RequestParam(value = "capacity", required = false) final String requestedCapacity,
            @RequestParam(value = "maxWeight", required = false) final String requestedMaxWeight,
            @RequestParam(value = "sort", required = false, defaultValue = "recommended") final String sort) {
        final List<Item> filteredItems = itemService.listItems().stream()
                .filter(item -> matchesRequestedLocation(item, requestedLocation))
                .filter(item -> matchesMarketplaceAvailability(
                        item.getId(), requestedDate, requestedStartTime, requestedEndTime))
                .filter(item -> matchesRequestedCapacity(item, requestedCapacity))
                .filter(item -> matchesRequestedWeight(item, requestedMaxWeight))
                .toList();
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", filteredItems);
        mav.addObject("itemImages", buildItemImagesMap());
        mav.addObject("itemsCount", filteredItems.size());
        mav.addObject("sort", sort);
        addAvailabilityPickerData(
                mav,
                "search",
                buildAvailabilityPickerData(itemService.listAvailabilities(), itemService.listBookings()));
        return mav;
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime) {
        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemService.findItemTypeById(item.get().getTypeId());
        final List<ItemAvailability> itemAvailabilities = itemService.listAvailabilitiesByItemId(itemId);
        final List<ItemBooking> itemBookings = itemService.listBookingsByItemId(itemId);
        final AvailabilityPickerData reservationAvailability =
                buildAvailabilityPickerData(itemAvailabilities, itemBookings);
        final List<String> offeredDates = reservationAvailability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = reservationAvailability.getOfferedTimesByDate();
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject("itemImageUrl", itemService.findImageUrlByItemId(itemId).orElse(""));
        mav.addObject("difficultyLabel", buildDifficultyLabel(item.get().getDifficultyLevel()));
        mav.addObject(
                "ownerInitial",
                owner.map(HelloWorldController::buildOwnerInitial).orElse("I"));
        addAvailabilityPickerData(mav, "reservation", reservationAvailability);
        final String defaultDate = offeredDates.isEmpty() ? "" : offeredDates.getFirst();
        final String reservationDate = resolveSelectedDate(requestedDate, offeredDates, defaultDate);
        final List<String> reservationSlots = offeredTimesByDate.getOrDefault(reservationDate, List.of());
        final String defaultStartTime = reservationSlots.isEmpty() ? "" : reservationSlots.getFirst();
        final String defaultEndTime = reservationSlots.size() > 1 ? reservationSlots.getLast() : defaultStartTime;
        final boolean hasValidRequestedRange = reservationDate.equals(requestedDate)
                && !isBlank(requestedStartTime)
                && !isBlank(requestedEndTime)
                && hasContinuousAvailability(reservationSlots, requestedStartTime, requestedEndTime);
        mav.addObject("reservationDate", reservationDate);
        mav.addObject(
                "reservationStartTime",
                hasValidRequestedRange
                        ? requestedStartTime
                        : resolveSelectedTime(requestedStartTime, reservationSlots, defaultStartTime));
        mav.addObject(
                "reservationEndTime",
                hasValidRequestedRange
                        ? requestedEndTime
                        : resolveSelectedTime(requestedEndTime, reservationSlots, defaultEndTime));
        return mav;
    }

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publish(
            @RequestParam(value = "submitted", required = false, defaultValue = "false") final boolean submitted,
            @ModelAttribute("publishForm") final PublishBoatForm form) {
        final ModelAndView mav = new ModelAndView("publish");
        mav.addObject("submitted", submitted);
        mav.addObject("capacityOptions", buildCapacityOptions());
        mav.addObject("weightOptions", buildWeightOptions());
        return mav;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishSubmit(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("redirect:/publish?submitted=true");
    }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================

    // Note: change class root directory from / to /class/
    // Example: /example from class would be /class/example

    @Autowired
    public HelloWorldController(final ClassUserService classUserService, final ItemService itemService) {
        this.classUserService = classUserService;
        this.itemService = itemService;
    }

    @RequestMapping(value = "/class", method = RequestMethod.GET)
    public ModelAndView helloWorld() {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        mav.addObject("message", "Hello World from Controller");
        return mav;
    }

    @RequestMapping(value = "/class", method = RequestMethod.POST)
    public ModelAndView createClassUser(
            @RequestParam("email") final String email,
            @RequestParam("password") final String password,
            @RequestParam("username") final String username) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final ClassUser classUser = classUserService.createClassUser(email, password, username);
        mav.addObject("message", "Hello World " + classUser.getUsername());
        return mav;
    }

    @RequestMapping(value = "/class/profile/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView helloWorld(@PathVariable("id") final long id) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final Optional<ClassUser> classUser = classUserService.findClassUserById(id);
        mav.addObject("message", "This it the profile for " + classUser.get().getUsername());
        return mav;
    }

    // ====================================
    // TODO reference, demo code from class
    // end
    // ====================================
    private static Map<String, String> buildCapacityOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("2", "2 personas");
        options.put("4", "4 personas");
        options.put("6", "6 personas");
        options.put("8", "8 personas");
        options.put("10", "10 personas");
        options.put("12", "12 personas");
        return options;
    }

    private static Map<String, String> buildWeightOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("400", "400 kg");
        options.put("600", "600 kg");
        options.put("800", "800 kg");
        options.put("1000", "1,000 kg");
        options.put("1200", "1,200 kg");
        return options;
    }

    private static boolean matchesRequestedLocation(final Item item, final String requestedLocation) {
        if (isBlank(requestedLocation)) {
            return true;
        }

        return item.getLocation() != null && item.getLocation().trim().equalsIgnoreCase(requestedLocation.trim());
    }

    private static boolean matchesRequestedCapacity(final Item item, final String requestedCapacity) {
        final Integer parsedCapacity = parseInteger(requestedCapacity);
        if (parsedCapacity == null) {
            return true;
        }

        return item.getCapacityPeople() >= parsedCapacity;
    }

    private static boolean matchesRequestedWeight(final Item item, final String requestedMaxWeight) {
        final Integer parsedWeight = parseInteger(requestedMaxWeight);
        if (parsedWeight == null) {
            return true;
        }

        return item.getMaxWeightKg().compareTo(BigDecimal.valueOf(parsedWeight.longValue())) >= 0;
    }

    private static Integer parseInteger(final String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private Map<Integer, String> buildItemImagesMap() {
        final Map<Integer, String> itemImages = new LinkedHashMap<>();
        for (final Item item : itemService.listItems()) {
            itemImages.put(
                    item.getId(), itemService.findImageUrlByItemId(item.getId()).orElse(""));
        }
        return itemImages;
    }

    private static String buildDifficultyLabel(final Integer difficultyLevel) {
        if (difficultyLevel == null) {
            return "Nivel no definido";
        }
        return "Nivel " + difficultyLevel;
    }

    private boolean matchesMarketplaceAvailability(
            final int itemId,
            final String requestedDate,
            final String requestedStartTime,
            final String requestedEndTime) {
        if (isBlank(requestedDate)) {
            return true;
        }

        final AvailabilityPickerData availabilityData = buildAvailabilityPickerData(
                itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        final List<String> availableTimes =
                availabilityData.getOfferedTimesByDate().get(requestedDate);

        if (availableTimes == null || availableTimes.isEmpty()) {
            return false;
        }

        if (isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return true;
        }

        if (!isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return availableTimes.contains(requestedStartTime);
        }

        if (isBlank(requestedStartTime)) {
            return availableTimes.contains(requestedEndTime);
        }

        return hasContinuousAvailability(availableTimes, requestedStartTime, requestedEndTime);
    }

    private void addAvailabilityPickerData(
            final ModelAndView mav, final String prefix, final AvailabilityPickerData availabilityData) {
        mav.addObject(prefix + "OfferedDatesJson", toJsonArray(availabilityData.getOfferedDates()));
        mav.addObject(prefix + "OccupiedDatesJson", toJsonArray(availabilityData.getOccupiedDates()));
        mav.addObject(prefix + "OfferedTimesJson", toJsonMap(availabilityData.getOfferedTimesByDate()));
        mav.addObject(prefix + "OccupiedTimesJson", toJsonMap(availabilityData.getOccupiedTimesByDate()));
    }

    private static AvailabilityPickerData buildAvailabilityPickerData(
            final List<ItemAvailability> availabilities, final List<ItemBooking> bookings) {
        final Set<Integer> itemIds = new LinkedHashSet<>();
        final Map<Integer, List<ItemAvailability>> availabilitiesByItemId = new LinkedHashMap<>();
        final Map<Integer, List<ItemBooking>> bookingsByItemId = new LinkedHashMap<>();
        final Map<String, TreeSet<String>> scheduledTimesByDate = new TreeMap<>();
        final Map<String, TreeSet<String>> availableTimesByDate = new TreeMap<>();

        for (final ItemAvailability availability : availabilities) {
            itemIds.add(availability.getItemId());
            availabilitiesByItemId
                    .computeIfAbsent(availability.getItemId(), ignored -> new ArrayList<>())
                    .add(availability);
        }

        for (final ItemBooking booking : bookings) {
            itemIds.add(booking.getItemId());
            bookingsByItemId
                    .computeIfAbsent(booking.getItemId(), ignored -> new ArrayList<>())
                    .add(booking);
        }

        for (final Integer itemId : itemIds) {
            final Map<String, TreeSet<String>> itemScheduledTimesByDate =
                    buildScheduledTimesByDate(availabilitiesByItemId.getOrDefault(itemId, List.of()));
            final Map<String, TreeSet<String>> itemBookedTimesByDate =
                    buildBookedTimesByDate(bookingsByItemId.getOrDefault(itemId, List.of()));
            final Map<String, TreeSet<String>> itemAvailableTimesByDate =
                    subtractTimes(itemScheduledTimesByDate, itemBookedTimesByDate);

            mergeTimesByDate(scheduledTimesByDate, itemScheduledTimesByDate);
            mergeTimesByDate(availableTimesByDate, itemAvailableTimesByDate);
        }

        final Map<String, TreeSet<String>> occupiedTimesByDate =
                subtractTimes(scheduledTimesByDate, availableTimesByDate);
        final List<String> offeredDates = new ArrayList<>();
        final List<String> occupiedDates = new ArrayList<>();

        for (final Map.Entry<String, TreeSet<String>> scheduledTimesEntry : scheduledTimesByDate.entrySet()) {
            final String date = scheduledTimesEntry.getKey();
            if (!availableTimesByDate.getOrDefault(date, new TreeSet<>()).isEmpty()) {
                offeredDates.add(date);
            } else if (!scheduledTimesEntry.getValue().isEmpty()) {
                occupiedDates.add(date);
            }
        }

        return new AvailabilityPickerData(
                List.copyOf(offeredDates),
                List.copyOf(occupiedDates),
                toImmutableTimesByDate(availableTimesByDate),
                toImmutableTimesByDate(occupiedTimesByDate));
    }

    private static Map<String, TreeSet<String>> buildScheduledTimesByDate(final List<ItemAvailability> availabilities) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        final LocalDate startDate = availabilityStartDate();
        final LocalDate endDate = pickerEndDate();

        for (final ItemAvailability availability : availabilities) {
            final DayOfWeek weekday = availability.getWeekday();
            final LocalTime startTime = availability.getStartTime();
            final LocalTime endTime = availability.getEndTime();

            for (LocalDate currentDate = startDate;
                    !currentDate.isAfter(endDate);
                    currentDate = currentDate.plusDays(1)) {
                if (currentDate.getDayOfWeek() != weekday) {
                    continue;
                }

                addTimeRange(collectedTimesByDate, currentDate.format(INPUT_DATE_FORMAT), startTime, endTime);
            }
        }

        return collectedTimesByDate;
    }

    private static Map<String, TreeSet<String>> buildBookedTimesByDate(final List<ItemBooking> bookings) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        final LocalDate startDate = availabilityStartDate();
        final LocalDate endDate = pickerEndDate();

        for (final ItemBooking booking : bookings) {
            OffsetDateTime currentTime = booking.getStartTime();
            final OffsetDateTime endTime = booking.getEndTime();

            while (currentTime.isBefore(endTime)) {
                final LocalDate currentDate = currentTime.toLocalDate();
                if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                    collectedTimesByDate
                            .computeIfAbsent(currentDate.format(INPUT_DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(currentTime.format(RESERVATION_TIME_FORMAT));
                }
                currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES);
            }
        }

        return collectedTimesByDate;
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collectedTimesByDate,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        for (LocalTime currentTime = startTime;
                !currentTime.isAfter(endTime);
                currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES)) {
            collectedTimesByDate
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(currentTime.format(RESERVATION_TIME_FORMAT));
        }
    }

    private static void mergeTimesByDate(
            final Map<String, TreeSet<String>> target, final Map<String, TreeSet<String>> source) {
        for (final Map.Entry<String, TreeSet<String>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), ignored -> new TreeSet<>()).addAll(entry.getValue());
        }
    }

    private static Map<String, TreeSet<String>> subtractTimes(
            final Map<String, TreeSet<String>> baseTimesByDate,
            final Map<String, TreeSet<String>> excludedTimesByDate) {
        final Map<String, TreeSet<String>> filteredTimesByDate = new TreeMap<>();

        for (final Map.Entry<String, TreeSet<String>> entry : baseTimesByDate.entrySet()) {
            final TreeSet<String> remainingTimes = new TreeSet<>(entry.getValue());
            remainingTimes.removeAll(excludedTimesByDate.getOrDefault(entry.getKey(), new TreeSet<>()));

            if (!remainingTimes.isEmpty()) {
                filteredTimesByDate.put(entry.getKey(), remainingTimes);
            }
        }

        return filteredTimesByDate;
    }

    private static Map<String, List<String>> toImmutableTimesByDate(final Map<String, TreeSet<String>> timesByDate) {
        final Map<String, List<String>> immutableTimesByDate = new LinkedHashMap<>();
        for (final Map.Entry<String, TreeSet<String>> entry : timesByDate.entrySet()) {
            immutableTimesByDate.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return immutableTimesByDate;
    }

    private static LocalDate availabilityStartDate() {
        return LocalDate.now();
    }

    private static LocalDate pickerEndDate() {
        return LocalDate.now().plusMonths(PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());
    }

    private static String resolveSelectedDate(
            final String requestedDate, final List<String> offeredDates, final String fallbackDate) {
        if (requestedDate != null && offeredDates.contains(requestedDate)) {
            return requestedDate;
        }
        return fallbackDate;
    }

    private static String resolveSelectedTime(
            final String requestedTime, final List<String> offeredTimes, final String fallbackTime) {
        if (requestedTime != null && offeredTimes.contains(requestedTime)) {
            return requestedTime;
        }
        return fallbackTime;
    }

    private static boolean hasContinuousAvailability(
            final List<String> offeredTimes, final String requestedStartTime, final String requestedEndTime) {
        try {
            final Set<String> offeredTimeSet = Set.copyOf(offeredTimes);
            final LocalTime startTime = LocalTime.parse(requestedStartTime);
            final LocalTime endTime = LocalTime.parse(requestedEndTime);

            if (!endTime.isAfter(startTime)) {
                return false;
            }

            if (Duration.between(startTime, endTime).toMinutes() < MIN_BOOKING_DURATION_MINUTES) {
                return false;
            }

            for (LocalTime currentTime = startTime;
                    !currentTime.isAfter(endTime);
                    currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES)) {
                if (!offeredTimeSet.contains(currentTime.format(RESERVATION_TIME_FORMAT))) {
                    return false;
                }
            }

            return true;
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String toJsonArray(final List<String> values) {
        final StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escapeJson(values.get(index))).append('"');
        }
        return json.append(']').toString();
    }

    private static String toJsonMap(final Map<String, List<String>> values) {
        final List<String> entries = new ArrayList<>();
        for (final Map.Entry<String, List<String>> entry : values.entrySet()) {
            entries.add("\"" + escapeJson(entry.getKey()) + "\":" + toJsonArray(entry.getValue()));
        }
        return "{" + String.join(",", entries) + "}";
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String buildOwnerInitial(final User user) {
        if (user.getName() == null || user.getName().isEmpty()) {
            return "I";
        }
        return user.getName().substring(0, 1).toUpperCase();
    }

    private static final class AvailabilityPickerData {
        private final List<String> offeredDates;
        private final List<String> occupiedDates;
        private final Map<String, List<String>> offeredTimesByDate;
        private final Map<String, List<String>> occupiedTimesByDate;

        private AvailabilityPickerData(
                final List<String> offeredDates,
                final List<String> occupiedDates,
                final Map<String, List<String>> offeredTimesByDate,
                final Map<String, List<String>> occupiedTimesByDate) {
            this.offeredDates = offeredDates;
            this.occupiedDates = occupiedDates;
            this.offeredTimesByDate = offeredTimesByDate;
            this.occupiedTimesByDate = occupiedTimesByDate;
        }

        public List<String> getOfferedDates() {
            return offeredDates;
        }

        public List<String> getOccupiedDates() {
            return occupiedDates;
        }

        public Map<String, List<String>> getOfferedTimesByDate() {
            return offeredTimesByDate;
        }

        public Map<String, List<String>> getOccupiedTimesByDate() {
            return occupiedTimesByDate;
        }
    }
}
