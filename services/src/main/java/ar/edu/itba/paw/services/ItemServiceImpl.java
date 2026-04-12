package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ItemServiceImpl implements ItemService {
    private static final int TIME_SLOT_STEP_MINUTES = 30;
    private static final int MIN_BOOKING_DURATION_MINUTES = 120;

    private final ItemDao itemDao;

    @Autowired
    public ItemServiceImpl(final ItemDao itemDao) {
        this.itemDao = itemDao;
    }

    @Override
    public List<Item> listItems() {
        return itemDao.listItems();
    }

    @Override
    public Page<Item> searchItems(final ItemSearchCriteria criteria, final int page, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        final int requestedPage = Math.max(1, page);
        if (criteria != null && !isBlank(criteria.getDate())) {
            return searchItemsWithAvailability(criteria, requestedPage, safePageSize);
        }

        final int totalItems = itemDao.countItems(criteria);
        final int resolvedPage = resolvePage(requestedPage, safePageSize, totalItems);
        final List<Item> items = totalItems == 0
                ? List.of()
                : itemDao.listItems(criteria, safePageSize, offsetFor(resolvedPage, safePageSize));
        return new Page<>(items, resolvedPage, safePageSize, totalItems);
    }

    @Override
    public List<LocationOption> listLocationOptions() {
        return itemDao.listLocationOptions();
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return itemDao.findItemById(id);
    }

    @Override
    public Optional<User> findUserById(final int id) {
        return itemDao.findUserById(id);
    }

    @Override
    public Optional<User> findUserByEmail(final String email) {
        return itemDao.findUserByEmail(email);
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return itemDao.findItemTypeById(id);
    }

    @Override
    public Item createPublication(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage,
            final Integer typeId,
            final String title,
            final String description,
            final Integer pricePerHour,
            final Integer capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final Integer locationOptionId,
            final List<ItemAvailability> availabilities) {
        final User ownerUser = resolveOrCreateOwner(ownerGivenName, ownerLastName, ownerEmail, ownerPreferredLanguage);
        final String ownerDeleteToken = UUID.randomUUID().toString();
        final Item item = itemDao.createItem(
                ownerUser.getId(),
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                maxWeightKg,
                difficultyLevel,
                locationOptionId,
                ownerDeleteToken);

        for (final ItemAvailability availability : availabilities) {
            itemDao.createItemAvailability(
                    item.getId(),
                    availability.getWeekday().name(),
                    availability.getStartTime().toString(),
                    availability.getEndTime().toString());
        }
        return item;
    }

    @Override
    public Optional<Item> findItemByOwnerDeleteToken(final String ownerDeleteToken) {
        return itemDao.findItemByOwnerDeleteToken(ownerDeleteToken);
    }

    @Override
    public boolean activateItemByOwnerDeleteToken(final String ownerDeleteToken) {
        return itemDao.activateItemByOwnerDeleteToken(ownerDeleteToken);
    }

    @Override
    public boolean deactivateItemByOwnerDeleteToken(
            final String ownerDeleteToken, final OffsetDateTime ownerDeleteUsedAt) {
        return itemDao.deactivateItemByOwnerDeleteToken(ownerDeleteToken, ownerDeleteUsedAt);
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return itemDao.listAvailabilities();
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return itemDao.listAvailabilitiesByItemId(itemId);
    }

    @Override
    public List<ItemBooking> listBookings() {
        return itemDao.listBookings();
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return itemDao.listBookingsByItemId(itemId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return itemDao.findNextAvailabilityByItemId(itemId);
    }

    @Override
    public Optional<byte[]> findImageById(final int id) {
        return itemDao.findImageById(id);
    }

    @Override
    public List<Integer> listImageIdsByItemId(final int itemId) {
        return itemDao.listImageIdsByItemId(itemId);
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return itemDao.insertAvailability(itemId, weekday, startTime, endTime);
    }

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData) {
        return itemDao.insertImage(itemId, imageData);
    }

    private User resolveOrCreateOwner(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage) {
        final String preferredLanguage = "en".equalsIgnoreCase(ownerPreferredLanguage) ? "en" : "es";
        final Optional<User> existingOwner = itemDao.findUserByEmail(ownerEmail);
        if (existingOwner.isPresent()) {
            final User user = existingOwner.get();
            itemDao.updateUserProfile(user.getId(), ownerGivenName, ownerLastName, preferredLanguage);
            user.setGivenName(ownerGivenName);
            user.setLastName(ownerLastName);
            user.setPreferredLanguage(preferredLanguage);
            return user;
        }
        return itemDao.createUser(ownerGivenName, ownerLastName, ownerEmail, preferredLanguage);
    }

    private Page<Item> searchItemsWithAvailability(
            final ItemSearchCriteria criteria, final int requestedPage, final int pageSize) {
        final int candidateCount = itemDao.countItems(criteria);
        if (candidateCount == 0) {
            return new Page<>(List.of(), 1, pageSize, 0);
        }

        final List<Item> candidates = itemDao.listItems(criteria, candidateCount, 0);
        final Map<Integer, List<ItemAvailability>> availabilitiesByItemId =
                groupAvailabilitiesByItemId(itemDao.listAvailabilities());
        final Map<Integer, List<ItemBooking>> bookingsByItemId = groupBookingsByItemId(itemDao.listBookings());
        final List<Item> filteredItems = new ArrayList<>();
        for (final Item item : candidates) {
            if (matchesMarketplaceAvailability(
                    criteria,
                    availabilitiesByItemId.getOrDefault(item.getId(), List.of()),
                    bookingsByItemId.getOrDefault(item.getId(), List.of()))) {
                filteredItems.add(item);
            }
        }

        final int totalItems = filteredItems.size();
        final int resolvedPage = resolvePage(requestedPage, pageSize, totalItems);
        final int fromIndex = Math.min(offsetFor(resolvedPage, pageSize), totalItems);
        final int toIndex = Math.min(fromIndex + pageSize, totalItems);
        return new Page<>(filteredItems.subList(fromIndex, toIndex), resolvedPage, pageSize, totalItems);
    }

    private static Map<Integer, List<ItemAvailability>> groupAvailabilitiesByItemId(
            final List<ItemAvailability> availabilities) {
        final Map<Integer, List<ItemAvailability>> grouped = new LinkedHashMap<>();
        for (final ItemAvailability availability : availabilities) {
            grouped.computeIfAbsent(availability.getItemId(), ignored -> new ArrayList<>())
                    .add(availability);
        }
        return grouped;
    }

    private static Map<Integer, List<ItemBooking>> groupBookingsByItemId(final List<ItemBooking> bookings) {
        final Map<Integer, List<ItemBooking>> grouped = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            grouped.computeIfAbsent(booking.getItemId(), ignored -> new ArrayList<>())
                    .add(booking);
        }
        return grouped;
    }

    private static boolean matchesMarketplaceAvailability(
            final ItemSearchCriteria criteria,
            final List<ItemAvailability> availabilities,
            final List<ItemBooking> bookings) {
        if (isBlank(criteria.getDate())) {
            return true;
        }

        final TreeSet<String> availableTimes = availableTimesForDate(criteria.getDate(), availabilities, bookings);
        if (availableTimes.isEmpty()) {
            return false;
        }

        if (isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return hasAnyContinuousTwoHourWindow(List.copyOf(availableTimes));
        }

        if (!isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return hasContinuousTwoHourWindowStartingAt(List.copyOf(availableTimes), criteria.getStartTime());
        }

        if (isBlank(criteria.getStartTime())) {
            return hasContinuousTwoHourWindowEndingAt(List.copyOf(availableTimes), criteria.getEndTime());
        }

        return hasContinuousAvailability(List.copyOf(availableTimes), criteria.getStartTime(), criteria.getEndTime());
    }

    private static TreeSet<String> availableTimesForDate(
            final String requestedDate, final List<ItemAvailability> availabilities, final List<ItemBooking> bookings) {
        try {
            final LocalDate date = LocalDate.parse(requestedDate);
            final TreeSet<String> scheduledTimes = new TreeSet<>();
            final TreeSet<String> bookedTimes = new TreeSet<>();
            for (final ItemAvailability availability : availabilities) {
                if (availability.getWeekday() == date.getDayOfWeek()) {
                    addTimeRange(scheduledTimes, availability.getStartTime(), availability.getEndTime());
                }
            }
            for (final ItemBooking booking : bookings) {
                addBookedTimesForDate(bookedTimes, date, booking);
            }
            scheduledTimes.removeAll(bookedTimes);
            return scheduledTimes;
        } catch (final DateTimeParseException exception) {
            return new TreeSet<>();
        }
    }

    private static void addTimeRange(final TreeSet<String> times, final LocalTime startTime, final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute <= endMinute; minute += TIME_SLOT_STEP_MINUTES) {
            times.add(LocalTime.ofSecondOfDay((long) minute * 60).toString());
        }
    }

    private static void addBookedTimesForDate(
            final TreeSet<String> bookedTimes, final LocalDate date, final ItemBooking booking) {
        if (!isBlockingBooking(booking)) {
            return;
        }
        OffsetDateTime currentTime = booking.getStartTime();
        final OffsetDateTime endTime = booking.getEndTime();
        while (currentTime.isBefore(endTime)) {
            if (date.equals(currentTime.toLocalDate())) {
                bookedTimes.add(currentTime.toLocalTime().toString());
            }
            currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES);
        }
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == null
                || booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED;
    }

    private static boolean hasAnyContinuousTwoHourWindow(final List<String> availableTimes) {
        for (int startIndex = 0; startIndex < availableTimes.size(); startIndex++) {
            for (int endIndex = startIndex + 1; endIndex < availableTimes.size(); endIndex++) {
                if (hasContinuousAvailability(
                        availableTimes, availableTimes.get(startIndex), availableTimes.get(endIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowStartingAt(
            final List<String> availableTimes, final String requestedStartTime) {
        if (!availableTimes.contains(requestedStartTime)) {
            return false;
        }
        for (final String possibleEndTime : availableTimes) {
            if (hasContinuousAvailability(availableTimes, requestedStartTime, possibleEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowEndingAt(
            final List<String> availableTimes, final String requestedEndTime) {
        if (!availableTimes.contains(requestedEndTime)) {
            return false;
        }
        for (final String possibleStartTime : availableTimes) {
            if (hasContinuousAvailability(availableTimes, possibleStartTime, requestedEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousAvailability(
            final List<String> availableTimes, final String requestedStartTime, final String requestedEndTime) {
        try {
            final Set<String> availableTimeSet = Set.copyOf(availableTimes);
            final LocalTime startTime = LocalTime.parse(requestedStartTime);
            final LocalTime endTime = LocalTime.parse(requestedEndTime);
            if (!endTime.isAfter(startTime)
                    || Duration.between(startTime, endTime).toMinutes() < MIN_BOOKING_DURATION_MINUTES) {
                return false;
            }
            for (int minute = startTime.toSecondOfDay() / 60;
                    minute <= endTime.toSecondOfDay() / 60;
                    minute += TIME_SLOT_STEP_MINUTES) {
                if (!availableTimeSet.contains(
                        LocalTime.ofSecondOfDay((long) minute * 60).toString())) {
                    return false;
                }
            }
            return true;
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static int resolvePage(final int requestedPage, final int pageSize, final int totalItems) {
        if (totalItems == 0) {
            return 1;
        }
        final int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        return Math.min(requestedPage, totalPages);
    }

    private static int offsetFor(final int page, final int pageSize) {
        return (page - 1) * pageSize;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
