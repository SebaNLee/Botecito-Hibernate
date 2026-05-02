package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class ItemServiceImpl implements ItemService {

    private static final int MAX_IMAGES_PER_ITEM = 10;
    private static final int TIME_STEP_MINUTES = 30;

    private final ItemDao itemDao;

    @Override
    public List<Item> listItems() {
        return itemDao.listItems();
    }

    @Override
    public Page<Item> searchItems(final ItemSearchCriteria criteria, final int page, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        final int requestedPage = Math.max(1, page);
        if (criteria != null && needsAvailabilityPostFilter(criteria)) {
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
    public Optional<Item> findItemByIdForOwner(final int id, final int ownerId) {
        return itemDao.findItemByIdForOwner(id, ownerId);
    }

    @Override
    public Optional<Item> findAnyItemById(final int id) {
        return itemDao.findAnyItemById(id);
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
    @Transactional
    public boolean updatePublicationForOwner(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId,
            final byte[] primaryImageData) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return false;
        }
        if (!itemDao.snapshotBookingsForPublicationEdit(itemId)) {
            throw new IllegalStateException("Could not snapshot bookings for item " + itemId);
        }
        if (!itemDao.updatePublicationForOwner(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId)) {
            throw new IllegalStateException("Could not update item " + itemId);
        }
        if (primaryImageData != null && primaryImageData.length > 0) {
            if (itemDao.replacePrimaryImageForOwner(itemId, ownerId, primaryImageData) == null) {
                throw new IllegalStateException("Could not replace primary image for item " + itemId);
            }
        }
        return true;
    }

    @Override
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        return itemDao.hasBlockingBookingsForEdition(itemId);
    }

    @Override
    @Transactional
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        return itemDao.deleteItemByIdForOwner(itemId, ownerId);
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
        final int validatedTypeId = validateItemTypeId(typeId);
        final int validatedLocationOptionId = validateLocationOptionId(locationOptionId);
        final int validatedCapacityPeople = requirePositive(capacityPeople, "capacity people");
        final int validatedPricePerHour = requirePositive(pricePerHour, "price per hour");
        validateAvailabilities(availabilities);

        final User ownerUser = resolveOrCreateOwner(ownerGivenName, ownerLastName, ownerEmail, ownerPreferredLanguage);
        final String ownerDeleteToken = UUID.randomUUID().toString();
        final Item item = itemDao.createItem(
                ownerUser.getId(),
                validatedTypeId,
                title,
                description,
                validatedPricePerHour,
                validatedCapacityPeople,
                maxWeightKg,
                difficultyLevel,
                validatedLocationOptionId,
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
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return itemDao.setItemActiveForOwner(itemId, ownerId, active);
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
    public List<ItemBooking> listActiveBookingsByItemId(final int itemId) {
        return itemDao.listActiveBookingsByItemId(itemId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId) {
        return itemDao.findSnapshotByBookingIdForGuest(bookingId, guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId) {
        return itemDao.findSnapshotByBookingIdForOwner(bookingId, ownerId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(
            final int versionId, final int itemId, final int guestId) {
        return itemDao.findSnapshotVersionByIdForGuest(versionId, itemId, guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(
            final int versionId, final int itemId, final int ownerId) {
        return itemDao.findSnapshotVersionByIdForOwner(versionId, itemId, ownerId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId) {
        return itemDao.listSnapshotsByItemIdForGuest(itemId, guestId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId) {
        return itemDao.listSnapshotsByItemIdForOwner(itemId, ownerId);
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
    public List<Integer> listImageIdsByItemIdOrdered(final int itemId) {
        return itemDao.listImageIdsByItemIdOrdered(itemId);
    }

    @Override
    public Optional<Integer> findCoverImageIdByItemId(final int itemId) {
        return itemDao.findCoverImageIdByItemId(itemId);
    }

    @Override
    public int countImagesByItemId(final int itemId) {
        return itemDao.countImagesByItemId(itemId);
    }

    @Override
    public int maxImagesPerItem() {
        return MAX_IMAGES_PER_ITEM;
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return itemDao.insertAvailability(itemId, weekday, startTime, endTime);
    }

    @Override
    @Transactional
    public Integer appendImage(final int itemId, final byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }
        final int existing = itemDao.countImagesByItemId(itemId);
        if (existing >= MAX_IMAGES_PER_ITEM) {
            throw new IllegalArgumentException("Image gallery is full (max " + MAX_IMAGES_PER_ITEM + ")");
        }
        return itemDao.insertImage(itemId, imageData, existing);
    }

    @Override
    @Transactional
    public void replaceGallery(final int itemId, final List<byte[]> orderedImages) {
        if (orderedImages == null) {
            return;
        }
        if (orderedImages.size() > MAX_IMAGES_PER_ITEM) {
            throw new IllegalArgumentException("Gallery exceeds max " + MAX_IMAGES_PER_ITEM);
        }
        for (final Integer existingId : itemDao.listImageIdsByItemIdOrdered(itemId)) {
            itemDao.deleteImage(itemId, existingId);
        }
        int position = 0;
        for (final byte[] data : orderedImages) {
            if (data == null || data.length == 0) {
                continue;
            }
            itemDao.insertImage(itemId, data, position);
            position++;
        }
    }

    @Override
    @Transactional
    public boolean deleteImageFromItem(final int itemId, final int imageId) {
        return itemDao.deleteImage(itemId, imageId);
    }

    @Override
    @Transactional
    public void reorderImagesForItem(final int itemId, final List<Integer> imageIdsInOrder) {
        if (imageIdsInOrder == null || imageIdsInOrder.isEmpty()) {
            return;
        }
        final List<Integer> currentIds = itemDao.listImageIdsByItemIdOrdered(itemId);
        if (currentIds.size() != imageIdsInOrder.size()
                || !new java.util.HashSet<>(currentIds).equals(new java.util.HashSet<>(imageIdsInOrder))) {
            throw new IllegalArgumentException("Reorder list does not match current gallery contents");
        }
        itemDao.reorderImages(itemId, imageIdsInOrder);
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
        final int validatedTypeId = requirePositive(typeId, "item type id");
        if (itemDao.findItemTypeById(validatedTypeId).isEmpty()) {
            throw new IllegalArgumentException("item type does not exist");
        }
        return validatedTypeId;
    }

    private int validateLocationOptionId(final Integer locationOptionId) {
        final int validatedLocationOptionId = requirePositive(locationOptionId, "location option id");
        final boolean exists = itemDao.listLocationOptions().stream()
                .anyMatch(location -> location.getId() != null && location.getId() == validatedLocationOptionId);
        if (!exists) {
            throw new IllegalArgumentException("location option does not exist");
        }
        return validatedLocationOptionId;
    }

    private static void validateAvailabilities(final List<ItemAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            throw new IllegalArgumentException("at least one availability is required");
        }
        for (final ItemAvailability availability : availabilities) {
            if (availability == null) {
                throw new IllegalArgumentException("availability is required");
            }
            validateAvailabilitySlot(availability.getWeekday(), availability.getStartTime(), availability.getEndTime());
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
    }

    private static boolean isThirtyMinuteStep(final LocalTime time) {
        return time.getMinute() % TIME_STEP_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private static int requirePositive(final Integer value, final String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
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

    private static boolean needsAvailabilityPostFilter(final ItemSearchCriteria criteria) {
        if (!isBlank(criteria.getDate())) {
            return true;
        }
        return !isBlank(criteria.getStartTime()) || !isBlank(criteria.getEndTime());
    }
}
