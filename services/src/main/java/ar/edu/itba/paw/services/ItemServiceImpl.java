package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSearchSort;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class ItemServiceImpl implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);
    private static final int MAX_IMAGES_PER_ITEM = 10;
    private static final int TIME_STEP_MINUTES = 30;

    private final ItemDao itemDao;
    private final MailService mailService;
    private final ReviewService reviewService;

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
            LOGGER.warn("Attempt to update non-existent item {} or unauthorized access by owner {}", itemId, ownerId);
            return false;
        }
        if (!itemDao.snapshotBookingsForPublicationEdit(itemId)) {
            LOGGER.error("Could not snapshot bookings for item {}", itemId);
            throw new IllegalStateException("Could not snapshot bookings for item " + itemId);
        }
        if (!itemDao.updatePublicationForOwner(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId)) {
            LOGGER.error("Could not update item {}", itemId);
            throw new IllegalStateException("Could not update item " + itemId);
        }
        if (primaryImageData != null && primaryImageData.length > 0) {
            if (itemDao.replacePrimaryImageForOwner(itemId, ownerId, primaryImageData) == null) {
                LOGGER.error("Could not replace primary image for item {}", itemId);
                throw new IllegalStateException("Could not replace primary image for item " + itemId);
            }
        }
        LOGGER.info("Item {} updated successfully by owner {}", itemId, ownerId);
        return true;
    }

    @Override
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        return itemDao.hasBlockingBookingsForEdition(itemId);
    }

    @Override
    @Transactional
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        boolean deleted = itemDao.deleteItemByIdForOwner(itemId, ownerId);
        if (deleted) {
            LOGGER.info("Item {} deleted by owner {}", itemId, ownerId);
        } else {
            LOGGER.warn("Failed to delete item {} by owner {}", itemId, ownerId);
        }
        return deleted;
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

        ar.edu.itba.paw.services.utils.UserNameRules.requireBothLegalNames(ownerGivenName, ownerLastName);
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
    public RatingSummary getItemRatingSummary(final int itemId) {
        return reviewService.getItemRatingSummary(itemId);
    }

    @Override
    public List<Review> listLatestReviews(final int itemId, final int limit) {
        return reviewService.listLatestItemReviews(itemId, limit);
    }

    @Override
    public Optional<ReviewService.PendingReviewAction> findPendingReviewAction(final int userId, final int itemId) {
        return reviewService.findPendingItemReviewAction(userId, itemId);
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
        ar.edu.itba.paw.services.utils.UserNameRules.requireBothLegalNames(ownerGivenName, ownerLastName);
        final String preferredLanguage = "en".equalsIgnoreCase(ownerPreferredLanguage) ? "en" : "es";
        final Optional<User> existingOwner = itemDao.findUserByEmail(ownerEmail);
        if (existingOwner.isPresent()) {
            final User user = existingOwner.get();
            itemDao.updateUserProfile(user.getId(), ownerGivenName, ownerLastName, preferredLanguage);
            user.setGivenName(ownerGivenName);
            user.setLastName(ownerLastName);
            user.setPreferredLanguage(ar.edu.itba.paw.models.PreferredLanguage.fromPersistence(preferredLanguage));
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

    // ---- web orchestration moved to webapp -------------------------------------------------

    @Override
    public ItemSearchCriteria parseAndValidateSearchCriteria(
            final String searchQuery,
            final String locationOptionId,
            final String date,
            final String startTime,
            final String endTime,
            final String capacity,
            final String maxWeight,
            final String difficulty,
            final String minRating,
            final String sort) {
        final ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setSearchQuery(searchQuery);
        criteria.setLocationOptionId(parseInt(locationOptionId));
        criteria.setDate(parseLocalDate(date));
        criteria.setStartTime(parseLocalTime(startTime));
        criteria.setEndTime(parseLocalTime(endTime));
        criteria.setCapacity(parseInt(capacity));
        final Integer maxWeightInt = parseInt(maxWeight);
        criteria.setMaxWeightKg(maxWeightInt == null ? null : BigDecimal.valueOf(maxWeightInt.longValue()));
        criteria.setDifficultyLevel(parseRanged(difficulty, 1, 5));
        criteria.setMinAverageRating(parseRanged(minRating, 1, 5));
        criteria.setSort(ItemSearchSort.fromRequestParam(sort));
        return criteria;
    }

    @Override
    public Page<Item> searchMarketplace(final ItemSearchCriteria criteria, final int page, final int pageSize) {
        return searchItems(criteria, page, pageSize);
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
    public Item createPublicationFromDraft(final PublicationDraft draft) {
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
        return created;
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
            itemDao.resolveBookingsByHostDecisionTokens(
                    tokensToAccept, BookingState.BOOKING_CONFIRMED, OffsetDateTime.now());
        }
        if (!tokensToDecline.isEmpty()) {
            itemDao.resolveBookingsByHostDecisionTokens(
                    tokensToDecline, BookingState.BOOKING_REJECTED, OffsetDateTime.now());
        }
    }

    @Transactional
    public Integer uploadGalleryImage(final int itemId, final int ownerId, final GalleryImageUpload image) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return null;
        }
        if (image == null || image.getFileData() == null || image.getFileData().length == 0) {
            return null;
        }
        final String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return null;
        }
        return appendImage(itemId, image.getFileData());
    }

    @Transactional
    public boolean reorderGalleryForOwner(final int itemId, final int ownerId, final List<Integer> imageIdsInOrder) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return false;
        }
        reorderImagesForItem(itemId, imageIdsInOrder);
        return true;
    }

    // ---- internal helpers --------------------------------------------------

    private static <T> Page<T> paginate(final List<T> items, final int page, final int pageSize) {
        final int totalItems = items == null ? 0 : items.size();
        final int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        final int resolvedPage = totalPages == 0 ? 1 : Math.min(Math.max(1, page), totalPages);
        final int from = totalItems == 0 ? 0 : Math.min((resolvedPage - 1) * pageSize, totalItems);
        final int to = totalItems == 0 ? 0 : Math.min(from + pageSize, totalItems);
        return new Page<>(items == null ? List.of() : items.subList(from, to), resolvedPage, pageSize, totalItems);
    }

    private static Integer parseInt(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseRanged(final String value, final int min, final int max) {
        final Integer parsed = parseInt(value);
        if (parsed == null || parsed < min || parsed > max) {
            return null;
        }
        return parsed;
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

    private static java.time.LocalTime parseLocalTime(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalTime.parse(value.trim());
        } catch (final java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private static String safeStr(final String value) {
        return value == null ? "" : value.trim();
    }
}
