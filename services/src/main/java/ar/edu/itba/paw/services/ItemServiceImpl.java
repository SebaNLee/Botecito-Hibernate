package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.DisabledTimeSlot;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.DisabledTimeSlotDao;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ItemServiceImpl implements ItemService {
    private final ItemDao itemDao;
    private final DisabledTimeSlotDao disabledTimeSlotDao;

    @Autowired
    public ItemServiceImpl(final ItemDao itemDao, final DisabledTimeSlotDao disabledTimeSlotDao) {
        this.itemDao = itemDao;
        this.disabledTimeSlotDao = disabledTimeSlotDao;
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
    public boolean setItemActive(final int itemId, final boolean active) {
        return itemDao.setItemActive(itemId, active);
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
    public List<ItemBooking> listBookingsByGuestId(final int guestId) {
        return itemDao.listBookingsByGuestId(guestId);
    }

    @Override
    public List<ItemBooking> listBookingsByOwnerId(final int ownerId) {
        return itemDao.listBookingsByOwnerId(ownerId);
    }

    @Override
    public List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId) {
        return itemDao.listPendingBookingsByOwnerId(ownerId);
    }

    @Override
    public List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId) {
        return itemDao.listPaymentSubmittedBookingsByOwnerId(ownerId);
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
        final Map<Integer, List<DisabledTimeSlot>> disabledSlotsByItemId = new LinkedHashMap<>();
        final List<Item> filteredItems = new ArrayList<>();
        for (final Item item : candidates) {
            final List<DisabledTimeSlot> disabledSlots =
                    disabledSlotsByItemId.computeIfAbsent(item.getId(), id -> disabledTimeSlotDao.listByItem(id));
            if (MarketplaceAvailabilityMatcher.matches(
                    criteria,
                    availabilitiesByItemId.getOrDefault(item.getId(), List.of()),
                    bookingsByItemId.getOrDefault(item.getId(), List.of()),
                    disabledSlots)) {
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
