package ar.edu.itba.paw.services;

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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ItemServiceImpl implements ItemService {
    private static final long MIN_AVAILABILITY_MINUTES = 120;
    private static final int TIME_STEP_MINUTES = 30;
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

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

    public List<Item> listItemsByOwnerId(final int ownerId) {
        return itemDao.listItemsByOwnerId(ownerId);
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
        final String normalizedOwnerGivenName = ServiceInputValidator.requireText(
                ownerGivenName, "owner given name", ServiceInputValidator.NAME_MAX_LENGTH);
        final String normalizedOwnerLastName = ServiceInputValidator.requireText(
                ownerLastName, "owner last name", ServiceInputValidator.NAME_MAX_LENGTH);
        final String normalizedOwnerEmail = ServiceInputValidator.requireEmail(ownerEmail);
        final int validatedTypeId = validateItemTypeId(typeId);
        final String normalizedTitle =
                ServiceInputValidator.requireText(title, "title", ServiceInputValidator.TITLE_MAX_LENGTH);
        final String normalizedDescription = ServiceInputValidator.optionalText(
                description, "description", ServiceInputValidator.DESCRIPTION_MAX_LENGTH);
        final int validatedPricePerHour = ServiceInputValidator.requireNonNegative(pricePerHour, "price per hour");
        final int validatedCapacityPeople = ServiceInputValidator.requirePositive(capacityPeople, "capacity people");
        final BigDecimal validatedMaxWeightKg =
                ServiceInputValidator.requirePositiveIfPresent(maxWeightKg, "max weight");
        validateDifficultyLevel(difficultyLevel);
        final int validatedLocationOptionId = validateLocationOptionId(locationOptionId);
        validateAvailabilities(availabilities);

        final User ownerUser = resolveOrCreateOwner(
                normalizedOwnerGivenName, normalizedOwnerLastName, normalizedOwnerEmail, ownerPreferredLanguage);
        final String ownerDeleteToken = UUID.randomUUID().toString();
        final Item item = itemDao.createItem(
                ownerUser.getId(),
                validatedTypeId,
                normalizedTitle,
                normalizedDescription,
                validatedPricePerHour,
                validatedCapacityPeople,
                validatedMaxWeightKg,
                difficultyLevel,
                validatedLocationOptionId,
                ownerDeleteToken);
        if (!itemDao.activateItemByOwnerDeleteToken(ownerDeleteToken)) {
            throw new IllegalStateException("Could not activate created item " + item.getId());
        }
        item.setActive(Boolean.TRUE);

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
        validateExistingItemId(itemId);
        validateAvailabilitySlot(weekday, startTime, endTime);
        return itemDao.insertAvailability(itemId, weekday, startTime, endTime);
    }

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData) {
        validateExistingItemId(itemId);
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("image data is required");
        }
        if (imageData.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("image data must be at most 5 MB");
        }
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

    private int validateItemTypeId(final Integer typeId) {
        final int validatedTypeId = ServiceInputValidator.requirePositive(typeId, "item type id");
        if (itemDao.findItemTypeById(validatedTypeId).isEmpty()) {
            throw new IllegalArgumentException("item type does not exist");
        }
        return validatedTypeId;
    }

    private int validateLocationOptionId(final Integer locationOptionId) {
        final int validatedLocationOptionId =
                ServiceInputValidator.requirePositive(locationOptionId, "location option id");
        final boolean exists = itemDao.listLocationOptions().stream()
                .anyMatch(location -> location.getId() != null && location.getId() == validatedLocationOptionId);
        if (!exists) {
            throw new IllegalArgumentException("location option does not exist");
        }
        return validatedLocationOptionId;
    }

    private void validateExistingItemId(final int itemId) {
        if (itemId <= 0 || itemDao.findItemById(itemId).isEmpty()) {
            throw new IllegalArgumentException("item does not exist");
        }
    }

    private static void validateDifficultyLevel(final Integer difficultyLevel) {
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 5)) {
            throw new IllegalArgumentException("difficulty level must be between 1 and 5");
        }
    }

    private static void validateAvailabilities(final List<ItemAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            throw new IllegalArgumentException("at least one availability is required");
        }

        final Map<DayOfWeek, List<ItemAvailability>> byWeekday = new EnumMap<>(DayOfWeek.class);
        for (final ItemAvailability availability : availabilities) {
            if (availability == null) {
                throw new IllegalArgumentException("availability is required");
            }
            validateAvailabilitySlot(availability.getWeekday(), availability.getStartTime(), availability.getEndTime());
            byWeekday
                    .computeIfAbsent(availability.getWeekday(), ignored -> new ArrayList<>())
                    .add(availability);
        }

        for (final List<ItemAvailability> dayAvailabilities : byWeekday.values()) {
            dayAvailabilities.sort(Comparator.comparing(ItemAvailability::getStartTime));
            for (int i = 1; i < dayAvailabilities.size(); i++) {
                if (dayAvailabilities
                        .get(i)
                        .getStartTime()
                        .isBefore(dayAvailabilities.get(i - 1).getEndTime())) {
                    throw new IllegalArgumentException("availability ranges cannot overlap");
                }
            }
        }
    }

    private static void validateAvailabilitySlot(
            final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        if (weekday == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("availability weekday, start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("availability end time must be after start time");
        }
        if (!isThirtyMinuteStep(startTime) || !isThirtyMinuteStep(endTime)) {
            throw new IllegalArgumentException("availability times must use 30 minute steps");
        }
        if (Duration.between(startTime, endTime).toMinutes() < MIN_AVAILABILITY_MINUTES) {
            throw new IllegalArgumentException("availability must be at least two hours long");
        }
    }

    private static boolean isThirtyMinuteStep(final LocalTime time) {
        return time.getMinute() % TIME_STEP_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
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
            if (MarketplaceAvailabilityMatcher.matches(
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
