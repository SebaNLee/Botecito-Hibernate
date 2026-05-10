package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemAvailabilityDao;
import ar.edu.itba.paw.persistence.ItemBookingDao;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.ItemMediaDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.services.utils.UserNameRules;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public final class ItemServiceImpl implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);
    private static final int MAX_IMAGES_PER_ITEM = 10;
    private static final long MAX_GALLERY_UPLOAD_BYTES_PER_FILE = 5_242_880L;
    private static final List<String> ALLOWED_GALLERY_UPLOAD_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final int TIME_STEP_MINUTES = 30;

    private final ItemDao itemDao;
    private final ItemAvailabilityDao itemAvailabilityDao;
    private final ItemBookingDao itemBookingDao;
    private final ItemMediaDao itemMediaDao;
    private final UserDao userDao;
    private final MailService mailService;

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
        return userDao.findById(id);
    }

    @Override
    public Optional<User> findUserByEmail(final String email) {
        return userDao.findByEmail(email);
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
            LOGGER.warn("Attempt to update non-existent item {} or unauthorized access by owner {}", itemId, ownerId);
            return false;
        }
        try {
            if (!itemDao.snapshotBookingsForPublicationEdit(itemId)) {
                LOGGER.error("Could not snapshot bookings for item {}", itemId);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
            if (!itemDao.updatePublicationForOwner(
                    itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId)) {
                LOGGER.error("Could not update item {}", itemId);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
            if (primaryImageData != null && primaryImageData.length > 0) {
                if (itemMediaDao.replacePrimaryImage(itemId, primaryImageData) == null) {
                    LOGGER.error("Could not replace primary image for item {}", itemId);
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return false;
                }
            }
            LOGGER.info("Item {} updated successfully by owner {}", itemId, ownerId);
            return true;
        } catch (final DataAccessException e) {
            LOGGER.error("Database error while updating publication for item {}", itemId, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Override
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        return itemDao.hasBlockingBookingsForEdition(itemId);
    }

    @Override
    @Transactional
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        try {
            final boolean deleted = itemDao.deleteItemByIdForOwner(itemId, ownerId);
            if (deleted) {
                LOGGER.info("Item {} deleted by owner {}", itemId, ownerId);
            } else {
                LOGGER.warn("Failed to delete item {} by owner {}", itemId, ownerId);
            }
            return deleted;
        } catch (final DataAccessException e) {
            LOGGER.error("Database error while deleting item {} for owner {}", itemId, ownerId, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
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

        LOGGER.info("Creating new publication for owner with email {}", ownerEmail);

        UserNameRules.requireBothLegalNames(ownerGivenName, ownerLastName);
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
            itemAvailabilityDao.createItemAvailability(
                    item.getId(),
                    availability.getWeekday().name(),
                    availability.getStartTime().toString(),
                    availability.getEndTime().toString());
        }

        sendPublishConfirmationEmail(ownerUser, item);
        LOGGER.info("Item {} created successfully for owner {}", item.getId(), ownerUser.getId());
        return item;
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return itemDao.setItemActiveForOwner(itemId, ownerId, active);
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return itemAvailabilityDao.listAvailabilities();
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return itemAvailabilityDao.listAvailabilitiesByItemId(itemId);
    }

    @Override
    public List<ItemBooking> listBookings() {
        return itemBookingDao.listBookings();
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return itemBookingDao.listBookingsByItemId(itemId);
    }

    @Override
    public List<ItemBooking> listBookingsByGuestId(final int guestId) {
        return itemBookingDao.listBookingsByGuestId(guestId);
    }

    @Override
    public List<ItemBooking> listBookingsByOwnerId(final int ownerId) {
        return itemBookingDao.listBookingsByOwnerId(ownerId);
    }

    @Override
    public List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId) {
        return itemBookingDao.listPendingBookingsByOwnerId(ownerId);
    }

    @Override
    public List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId) {
        return itemBookingDao.listPaymentSubmittedBookingsByOwnerId(ownerId);
    }

    @Override
    public List<ItemBooking> listActiveBookingsByItemId(final int itemId) {
        return itemBookingDao.listActiveBookingsByItemId(itemId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId) {
        return itemBookingDao.findSnapshotByBookingIdForGuest(bookingId, guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId) {
        return itemBookingDao.findSnapshotByBookingIdForOwner(bookingId, ownerId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(
            final int versionId, final int itemId, final int guestId) {
        return itemBookingDao.findSnapshotVersionByIdForGuest(versionId, itemId, guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(
            final int versionId, final int itemId, final int ownerId) {
        return itemBookingDao.findSnapshotVersionByIdForOwner(versionId, itemId, ownerId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId) {
        return itemBookingDao.listSnapshotsByItemIdForGuest(itemId, guestId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId) {
        return itemBookingDao.listSnapshotsByItemIdForOwner(itemId, ownerId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return itemAvailabilityDao.findNextAvailabilityByItemId(itemId);
    }

    @Override
    public Optional<byte[]> findImageById(final int id) {
        return itemMediaDao.findImageById(id);
    }

    @Override
    public List<Integer> listImageIdsByItemIdOrdered(final int itemId) {
        return itemMediaDao.listImageIdsByItemIdOrdered(itemId);
    }

    @Override
    public Optional<Integer> findCoverImageIdByItemId(final int itemId) {
        return itemMediaDao.findCoverImageIdByItemId(itemId);
    }

    @Override
    public int countImagesByItemId(final int itemId) {
        return itemMediaDao.countImagesByItemId(itemId);
    }

    @Override
    public int maxImagesPerItem() {
        return MAX_IMAGES_PER_ITEM;
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return itemAvailabilityDao.insertAvailability(itemId, weekday, startTime, endTime);
    }

    @Override
    @Transactional
    public Integer appendImage(final int itemId, final byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }
        final int existing = itemMediaDao.countImagesByItemId(itemId);
        if (existing >= MAX_IMAGES_PER_ITEM) {
            throw new IllegalArgumentException("Image gallery is full (max " + MAX_IMAGES_PER_ITEM + ")");
        }
        return itemMediaDao.insertImage(itemId, imageData, existing);
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
        for (final Integer existingId : itemMediaDao.listImageIdsByItemIdOrdered(itemId)) {
            itemMediaDao.deleteImage(itemId, existingId);
        }
        int position = 0;
        for (final byte[] data : orderedImages) {
            if (data == null || data.length == 0) {
                continue;
            }
            itemMediaDao.insertImage(itemId, data, position);
            position++;
        }
    }

    @Override
    @Transactional
    public boolean deleteImageFromItem(final int itemId, final int imageId) {
        return itemMediaDao.deleteImage(itemId, imageId);
    }

    @Override
    @Transactional
    public void reorderImagesForItem(final int itemId, final List<Integer> imageIdsInOrder) {
        if (imageIdsInOrder == null || imageIdsInOrder.isEmpty()) {
            return;
        }
        final List<Integer> currentIds = itemMediaDao.listImageIdsByItemIdOrdered(itemId);
        if (currentIds.size() != imageIdsInOrder.size()
                || !new java.util.HashSet<>(currentIds).equals(new java.util.HashSet<>(imageIdsInOrder))) {
            throw new IllegalArgumentException("Reorder list does not match current gallery contents");
        }
        itemMediaDao.reorderImages(itemId, imageIdsInOrder);
    }

    private User resolveOrCreateOwner(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage) {
        UserNameRules.requireBothLegalNames(ownerGivenName, ownerLastName);
        final String preferredLanguage = "en".equalsIgnoreCase(ownerPreferredLanguage) ? "en" : "es";
        final Optional<User> existingOwner = userDao.findByEmail(ownerEmail);
        if (existingOwner.isPresent()) {
            final User user = existingOwner.get();
            userDao.updateBasicProfileNamesAndLanguage(user.getId(), ownerGivenName, ownerLastName, preferredLanguage);
            user.setGivenName(ownerGivenName);
            user.setLastName(ownerLastName);
            user.setPreferredLanguage(ar.edu.itba.paw.models.PreferredLanguage.fromPersistence(preferredLanguage));
            return user;
        }
        return userDao.createUserWithoutCredentials(ownerGivenName, ownerLastName, ownerEmail, preferredLanguage);
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
                groupAvailabilitiesByItemId(itemAvailabilityDao.listAvailabilities());
        final Map<Integer, List<ItemBooking>> bookingsByItemId = groupBookingsByItemId(itemBookingDao.listBookings());
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

    private static boolean needsAvailabilityPostFilter(final ItemSearchCriteria criteria) {
        if (criteria.getDate() != null) {
            return true;
        }
        return criteria.getStartTime() != null || criteria.getEndTime() != null;
    }

    private void sendPublishConfirmationEmail(final User ownerUser, final Item item) {
        try {
            mailService.sendPublishConfirmationEmail(ownerUser.getEmail(), ownerUser.getName(), item.getTitle());
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger publish confirmation email for item {}.", item.getId(), e);
        }
    }

    @Override
    public Page<Item> searchMarketplace(final ItemSearchCriteria criteria, final int page, final int pageSize) {
        return searchItems(criteria, page, pageSize);
    }

    @Override
    public Map<Integer, Boolean> publicationDeleteDeactivatesByItemId(final List<Item> ownedItems) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasBlocking =
                    listBookingsByItemId(item.getId()).stream().anyMatch(b -> isExternalBlockingBooking(item, b));
            map.put(item.getId(), Boolean.TRUE.equals(item.getActive()) && hasBlocking);
        }
        return map;
    }

    @Override
    public Map<Integer, Boolean> publicationDeleteDisabledByItemId(final List<Item> ownedItems) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        final OffsetDateTime now = OffsetDateTime.now();
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasFutureBlocking = listBookingsByItemId(item.getId()).stream()
                    .anyMatch(b -> isExternalBlockingBooking(item, b)
                            && b.getEndTime() != null
                            && b.getEndTime().isAfter(now));
            map.put(item.getId(), !Boolean.TRUE.equals(item.getActive()) && hasFutureBlocking);
        }
        return map;
    }

    private static boolean isExternalBlockingBooking(final Item item, final ItemBooking booking) {
        if (booking == null || booking.getState() == null) {
            return false;
        }
        if (booking.getState() == BookingState.BOOKING_REJECTED
                || booking.getState() == BookingState.BOOKING_CANCELLED) {
            return false;
        }
        if (item == null || item.getOwnerId() == null) {
            return booking.getGuestId() != null;
        }
        return booking.getGuestId() != null && !booking.getGuestId().equals(item.getOwnerId());
    }

    public Map<String, String> validatePublicationDraft(final PublicationDraft draft) {
        final Map<String, String> errors = new LinkedHashMap<>();
        if (draft == null) {
            errors.put("draft", "publish.validation.required");
            return errors;
        }
        final List<ItemAvailability> availabilities = draft.getAvailabilities();
        if (availabilities.isEmpty()) {
            errors.put("availabilityByWeekday", "publish.availability.required");
            return errors;
        }
        final Map<DayOfWeek, List<ItemAvailability>> grouped = new LinkedHashMap<>();
        for (final ItemAvailability availability : availabilities) {
            if (availability == null
                    || availability.getWeekday() == null
                    || availability.getStartTime() == null
                    || availability.getEndTime() == null) {
                errors.put("availabilityByWeekday", "publish.availability.format.invalid");
                return errors;
            }
            grouped.computeIfAbsent(availability.getWeekday(), ignored -> new ArrayList<>())
                    .add(availability);
        }
        for (final Map.Entry<DayOfWeek, List<ItemAvailability>> entry : grouped.entrySet()) {
            final List<ItemAvailability> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparing(ItemAvailability::getStartTime));
            LocalTime previousEnd = null;
            for (final ItemAvailability slot : sorted) {
                if (!slot.getEndTime().isAfter(slot.getStartTime())) {
                    errors.put("availabilityByWeekday", "publish.availability.end.invalid");
                    return errors;
                }
                if (Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes() < 120) {
                    errors.put("availabilityByWeekday", "publish.availability.min.duration");
                    return errors;
                }
                if (previousEnd != null) {
                    if (slot.getStartTime().isBefore(previousEnd)) {
                        errors.put("availabilityByWeekday", "publish.availability.overlap");
                        return errors;
                    }
                    if (Duration.between(previousEnd, slot.getStartTime()).toMinutes() < 30) {
                        errors.put("availabilityByWeekday", "publish.availability.min.separation");
                        return errors;
                    }
                }
                previousEnd = slot.getEndTime();
            }
        }
        return errors;
    }

    @Transactional
    public Optional<Item> createPublicationFromDraft(final PublicationDraft draft) {
        try {
            final Item created = createPublication(
                    draft.getOwnerGivenName(),
                    draft.getOwnerLastName(),
                    draft.getOwnerEmail(),
                    draft.getOwnerPreferredLanguage(),
                    draft.getTypeId(),
                    draft.getTitle(),
                    draft.getDescription(),
                    draft.getPricePerHour(),
                    draft.getCapacityPeople(),
                    draft.getMaxWeightKg(),
                    draft.getDifficultyLevel(),
                    draft.getLocationOptionId(),
                    draft.getAvailabilities());
            if (!draft.getImages().isEmpty()) {
                final List<byte[]> bytes = new ArrayList<>();
                for (final GalleryImageUpload image : draft.getImages()) {
                    bytes.add(image.getFileData());
                }
                replaceGallery(created.getId(), bytes);
            }
            return Optional.of(created);
        } catch (final RuntimeException e) {
            LOGGER.error("Failed to create publication from draft", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }
    }

    public boolean hasPublicationChanges(
            final int itemId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId,
            final byte[] primaryImageData) {
        final Optional<Item> current = itemDao.findAnyItemById(itemId);
        if (current.isEmpty()) {
            return primaryImageData != null && primaryImageData.length > 0;
        }
        final Item item = current.get();
        if (!Objects.equals(safeStr(item.getTitle()), safeStr(title))) {
            return true;
        }
        if (!Objects.equals(safeStr(item.getDescription()), safeStr(description))) {
            return true;
        }
        if (item.getPricePerHour() == null || item.getPricePerHour() != pricePerHour) {
            return true;
        }
        if (!Objects.equals(item.getDifficultyLevel(), difficultyLevel)) {
            return true;
        }
        if (item.getLocationOptionId() == null || item.getLocationOptionId() != locationOptionId) {
            return true;
        }
        return primaryImageData != null && primaryImageData.length > 0;
    }

    @Transactional
    public void resolveEditConflict(final int itemId, final BookingDecisionBatch decisions) {
        if (decisions == null || decisions.getDecisions().isEmpty()) {
            return;
        }
        final List<String> tokensToAccept = new ArrayList<>();
        final List<String> tokensToDecline = new ArrayList<>();
        for (final BookingDecisionBatch.Decision decision : decisions.getDecisions()) {
            if (decision == null || decision.getToken() == null) {
                continue;
            }
            if (decision.getNewState() == BookingState.BOOKING_CONFIRMED) {
                tokensToAccept.add(decision.getToken());
            } else if (decision.getNewState() == BookingState.BOOKING_REJECTED) {
                tokensToDecline.add(decision.getToken());
            }
        }
        if (!tokensToAccept.isEmpty()) {
            itemBookingDao.resolveBookingsByHostDecisionTokens(
                    tokensToAccept, BookingState.BOOKING_CONFIRMED, OffsetDateTime.now());
        }
        if (!tokensToDecline.isEmpty()) {
            itemBookingDao.resolveBookingsByHostDecisionTokens(
                    tokensToDecline, BookingState.BOOKING_REJECTED, OffsetDateTime.now());
        }
    }

    @Override
    @Transactional
    public GalleryOwnerUploadResult uploadGalleryImage(
            final int itemId, final int ownerId, final GalleryImageUpload image) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.NOT_OWNER);
        }
        if (image == null || image.getFileData() == null || image.getFileData().length == 0) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.EMPTY_FILE);
        }
        if (image.getFileData().length > MAX_GALLERY_UPLOAD_BYTES_PER_FILE) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.FILE_TOO_LARGE);
        }
        final String contentType = image.getContentType();
        if (contentType == null
                || !ALLOWED_GALLERY_UPLOAD_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.INVALID_CONTENT_TYPE);
        }
        final int existing = itemMediaDao.countImagesByItemId(itemId);
        if (existing >= MAX_IMAGES_PER_ITEM) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.GALLERY_FULL);
        }
        final Integer newId = itemMediaDao.insertImage(itemId, image.getFileData(), existing);
        if (newId == null) {
            return GalleryOwnerUploadResult.failure(GalleryOwnerUploadStatus.EMPTY_FILE);
        }
        return GalleryOwnerUploadResult.success(newId);
    }

    @Override
    public List<Integer> parseGalleryImageOrderCsv(final String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        final List<Integer> result = new ArrayList<>();
        for (final String token : csv.split(",")) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (final NumberFormatException ex) {
                return List.of();
            }
        }
        return result;
    }

    @Override
    @Transactional
    public boolean reorderGalleryForOwner(final int itemId, final int ownerId, final List<Integer> imageIdsInOrder) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return false;
        }
        try {
            reorderImagesForItem(itemId, imageIdsInOrder);
            return true;
        } catch (final IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public boolean isGuestRequestedBookingRangeAvailable(
            final int itemId, final String date, final String startTime, final String endTime) {
        if (date == null || date.isBlank()) {
            return true;
        }
        final AvailabilityPickerBuilder.Data data = AvailabilityPickerBuilder.build(
                itemAvailabilityDao.listAvailabilitiesByItemId(itemId), itemBookingDao.listBookingsByItemId(itemId));
        final List<String> times = data.offeredTimesByDate().get(date);
        if (times == null || times.isEmpty()) {
            return false;
        }
        final boolean blankStart = startTime == null || startTime.isBlank();
        final boolean blankEnd = endTime == null || endTime.isBlank();
        if (blankStart || blankEnd) {
            return false;
        }
        return AvailabilityPickerBuilder.hasContinuousAvailability(times, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManageAvailabilityPageModel> loadManageAvailabilityPageModel(
            final int itemId, final int ownerId, final String requestedDate) {
        final Optional<Item> itemOpt = findItemById(itemId);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }
        final Item item = itemOpt.get();
        if (item.getOwnerId() == null || !item.getOwnerId().equals(ownerId)) {
            return Optional.empty();
        }
        final List<ItemAvailability> availabilities = listAvailabilitiesByItemId(item.getId());
        final List<ItemBooking> bookings = listBookingsByItemId(item.getId());
        final AvailabilityPickerBuilder.Data availabilityData =
                AvailabilityPickerBuilder.build(availabilities, bookings);
        final List<String> offeredDates = availabilityData.offeredDates();
        final String selectedDate =
                requestedDate != null && !requestedDate.isBlank() && offeredDates.contains(requestedDate)
                        ? requestedDate
                        : (offeredDates.isEmpty() ? null : offeredDates.get(0));

        final List<ItemBooking> personalBlocks = bookings.stream()
                .filter(b -> b.getGuestId() != null && b.getGuestId().equals(ownerId))
                .filter(b -> b.getState() == BookingState.BOOKING_CONFIRMED)
                .toList();

        final List<ManageAvailabilityPersonalBlockRow> personalBlockRows = personalBlocks.stream()
                .filter(block -> block.getId() != null && block.getStartTime() != null && block.getEndTime() != null)
                .sorted(Comparator.comparing(ItemBooking::getStartTime))
                .map(block -> new ManageAvailabilityPersonalBlockRow(
                        block.getId(),
                        block.getStartTime().toLocalDate().toString(),
                        block.getStartTime().toLocalTime().toString().substring(0, 5),
                        block.getEndTime().toLocalTime().toString().substring(0, 5)))
                .toList();

        final List<String> blockedDates = new ArrayList<>();
        for (final ItemBooking block : personalBlocks) {
            if (block.getStartTime() != null) {
                blockedDates.add(block.getStartTime().toLocalDate().toString());
            }
        }

        final List<ManageAvailabilitySlotRow> slots =
                buildOwnerAvailabilitySlots(selectedDate, availabilities, bookings, ownerId);

        return Optional.of(new ManageAvailabilityPageModel(
                item, offeredDates, blockedDates, selectedDate, slots, personalBlockRows));
    }

    private static List<ManageAvailabilitySlotRow> buildOwnerAvailabilitySlots(
            final String selectedDate,
            final List<ItemAvailability> availabilities,
            final List<ItemBooking> bookings,
            final int ownerId) {
        if (selectedDate == null || selectedDate.isBlank()) {
            return List.of();
        }
        final LocalDate day = parseLocalDate(selectedDate);
        if (day == null) {
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
            if (booking.getState() == null || !isBlockingStateForOwnerSlotGrid(booking.getState())) {
                continue;
            }
            if (booking.getStartTime() == null || booking.getEndTime() == null) {
                continue;
            }
            OffsetDateTime cursor = booking.getStartTime();
            while (cursor.isBefore(booking.getEndTime())) {
                if (day.equals(cursor.toLocalDate())) {
                    final String key = cursor.toLocalTime().toString().substring(0, 5);
                    if (booking.getGuestId() != null
                            && booking.getGuestId().equals(ownerId)
                            && booking.getId() != null) {
                        ownerBlocks.put(key, booking.getId());
                    } else {
                        guestBooked.add(key);
                    }
                }
                cursor = cursor.plusMinutes(30);
            }
        }
        final List<ManageAvailabilitySlotRow> slots = new ArrayList<>();
        for (final String time : scheduled) {
            final LocalTime start = LocalTime.parse(time);
            final String end = start.plusMinutes(30).toString().substring(0, 5);
            final Integer blockId = ownerBlocks.get(time);
            final String state = blockId != null ? "BLOCKED" : (guestBooked.contains(time) ? "BOOKED" : "AVAILABLE");
            slots.add(new ManageAvailabilitySlotRow(time, end, state, blockId, time.replace(":", "")));
        }
        return slots;
    }

    private static boolean isBlockingStateForOwnerSlotGrid(final BookingState state) {
        return state == BookingState.BOOKING_PENDING
                || state == BookingState.BOOKING_CONFIRMED
                || state == BookingState.BOOKING_PAYMENT_SUBMITTED
                || state == BookingState.BOOKING_PAID;
    }

    private static java.time.LocalDate parseLocalDate(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (final java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private static String safeStr(final String value) {
        return value == null ? "" : value.trim();
    }
}
