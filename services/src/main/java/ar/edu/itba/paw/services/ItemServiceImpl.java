package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
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
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemAvailabilityDao;
import ar.edu.itba.paw.persistence.ItemBookingDao;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.ItemMediaDao;
import ar.edu.itba.paw.persistence.ReviewDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.dto.AuthoredItemReviewSummaryView;
import ar.edu.itba.paw.services.dto.AvailabilityPickerData;
import ar.edu.itba.paw.services.dto.BookingDecisionBatch;
import ar.edu.itba.paw.services.dto.EditConflictView;
import ar.edu.itba.paw.services.dto.GalleryImageUpload;
import ar.edu.itba.paw.services.dto.MarketplaceItemView;
import ar.edu.itba.paw.services.dto.OwnerAvailabilityView;
import ar.edu.itba.paw.services.dto.OwnerDashboardView;
import ar.edu.itba.paw.services.dto.PendingReviewView;
import ar.edu.itba.paw.services.dto.PublicationDraft;
import ar.edu.itba.paw.services.dto.ReceivedBookingView;
import ar.edu.itba.paw.services.internal.AvailabilityPickerBuilder;
import ar.edu.itba.paw.services.internal.BookingDisplayFormatter;
import ar.edu.itba.paw.services.internal.OwnerAvailabilityViewBuilder;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final ItemAvailabilityDao itemAvailabilityDao;
    private final ItemBookingDao itemBookingDao;
    private final ReviewDao reviewDao;
    private final ItemMediaDao itemMediaDao;
    private final UserDao userDao;
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
            if (itemMediaDao.replacePrimaryImage(itemId, primaryImageData) == null) {
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
        ar.edu.itba.paw.services.utils.UserNameRules.requireBothLegalNames(ownerGivenName, ownerLastName);
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

    // ---- View / orchestration extensions ---------------------------------------------------

    @Override
    public OwnerDashboardView buildOwnerDashboard(
            final int ownerId,
            final List<String> statusFilters,
            final String boatNameQuery,
            final int page,
            final int pageSize) {
        final List<Item> ownedItems = itemDao.listItemsByOwnerId(ownerId);
        final Map<Integer, Integer> coverImageIdsByItemId = new LinkedHashMap<>();
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            itemMediaDao
                    .findCoverImageIdByItemId(item.getId())
                    .ifPresent(imageId -> coverImageIdsByItemId.put(item.getId(), imageId));
        }

        final List<ReceivedBookingView> all = buildReceivedBookings(ownerId);
        final List<ReceivedBookingView> filtered = all.stream()
                .filter(b -> BookingDisplayFormatter.matchesAnyStatusFilter(b.getStatusMessageCode(), statusFilters))
                .filter(b -> BookingDisplayFormatter.matchesBoatNameSearch(b.getItemTitle(), boatNameQuery))
                .sorted(Comparator.comparing(
                                ReceivedBookingView::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReceivedBookingView::getId, Comparator.reverseOrder()))
                .toList();
        final Page<ReceivedBookingView> bookingPage = paginate(filtered, page, pageSize);

        final Set<Integer> imageItemIds = new LinkedHashSet<>();
        for (final Item item : ownedItems) {
            if (item != null && item.getId() != null) {
                imageItemIds.add(item.getId());
            }
        }
        for (final ReceivedBookingView booking : bookingPage.getContent()) {
            imageItemIds.add(booking.getItemId());
        }

        final OffsetDateTime now = OffsetDateTime.now();
        final Map<Integer, Boolean> deactivates = new LinkedHashMap<>();
        final Map<Integer, Boolean> disabled = new LinkedHashMap<>();
        for (final Item item : ownedItems) {
            if (item.getId() == null) {
                continue;
            }
            final List<ItemBooking> itemBookings = itemBookingDao.listBookingsByItemId(item.getId());
            deactivates.put(
                    item.getId(),
                    Boolean.TRUE.equals(item.getActive())
                            && itemBookings.stream()
                                    .anyMatch(b -> !Objects.equals(b.getGuestId(), item.getOwnerId())
                                            && BookingDisplayFormatter.shouldRetainBookingForDeletion(b.getState())));
            disabled.put(
                    item.getId(),
                    !Boolean.TRUE.equals(item.getActive())
                            && itemBookings.stream()
                                    .anyMatch(b -> !Objects.equals(b.getGuestId(), item.getOwnerId())
                                            && BookingDisplayFormatter.shouldRetainBookingForDeletion(b.getState())
                                            && b.getEndTime() != null
                                            && b.getEndTime().isAfter(now)));
        }

        final Map<Integer, PendingReviewView> pendingByBookingId = new LinkedHashMap<>();
        for (final PendingReviewView v : buildPendingReviewViews(ownerId)) {
            if (v.getTargetType() == ReviewTargetType.USER) {
                pendingByBookingId.put(v.getBookingId(), v);
            }
        }
        final Map<Integer, AuthoredItemReviewSummaryView> authoredUserByBookingId =
                buildAuthoredReviewsByBookingId(ownerId, ReviewTargetType.USER);

        return new OwnerDashboardView(
                ownedItems,
                coverImageIdsByItemId,
                imageItemIds,
                deactivates,
                disabled,
                bookingPage,
                pendingByBookingId,
                authoredUserByBookingId);
    }

    @Override
    public Optional<MarketplaceItemView> findMarketplaceItemView(
            final int itemId,
            final Integer viewerUserId,
            final Integer requestedSnapshotVersionId,
            final String requestedDate,
            final String requestedStartTime,
            final String requestedEndTime) {
        final Optional<ItemSnapshot> selectedSnapshot = requestedSnapshotVersionId == null || viewerUserId == null
                ? Optional.empty()
                : resolveAuthorizedSnapshotVersion(requestedSnapshotVersionId, itemId, viewerUserId);
        if (requestedSnapshotVersionId != null && selectedSnapshot.isEmpty()) {
            return Optional.empty();
        }

        Optional<Item> item = itemDao.findItemById(itemId);
        if (item.isEmpty() && viewerUserId != null) {
            item = itemDao.findItemByIdForOwner(itemId, viewerUserId);
        }
        if (item.isEmpty() && selectedSnapshot.isPresent()) {
            item = itemDao.findAnyItemById(itemId);
        }
        if (item.isEmpty()) {
            return Optional.empty();
        }

        final boolean isOwner = viewerUserId != null
                && item.get().getOwnerId() != null
                && item.get().getOwnerId().equals(viewerUserId);
        final boolean isActive = Boolean.TRUE.equals(item.get().getActive());
        if (!isActive && !isOwner && selectedSnapshot.isEmpty()) {
            return Optional.empty();
        }

        final boolean hideListingLiveVersionNavigation = selectedSnapshot.isPresent() && !isActive && !isOwner;
        final User owner = item.get().getOwnerId() == null
                ? null
                : userDao.findById(item.get().getOwnerId()).orElse(null);
        final ItemType itemType =
                itemDao.findItemTypeById(item.get().getTypeId()).orElse(null);
        final RatingSummary ratingSummary = reviewService.getItemRatingSummary(itemId);
        final List<Review> reviews = reviewService.listLatestItemReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getReviewerUserId() == null || reviewAuthorNames.containsKey(review.getReviewerUserId())) {
                continue;
            }
            reviewAuthorNames.put(
                    review.getReviewerUserId(),
                    userDao.findById(review.getReviewerUserId())
                            .map(User::getName)
                            .orElse(""));
        }

        final Item displayItem =
                selectedSnapshot.<Item>map(snapshot -> snapshot).orElse(item.get());
        final boolean useSnapshotCover =
                selectedSnapshot.isPresent() && selectedSnapshot.get().getCoverImageData() != null;
        final Integer coverImageId =
                itemMediaDao.findCoverImageIdByItemId(itemId).orElse(null);
        final List<Integer> galleryImageIds =
                useSnapshotCover ? List.of() : itemMediaDao.listImageIdsByItemIdOrdered(itemId);
        final List<ItemSnapshot> guestSnapshots =
                viewerUserId == null ? List.of() : itemBookingDao.listSnapshotsByItemIdForGuest(itemId, viewerUserId);
        final List<ItemSnapshot> hostSnapshots = isOwner && viewerUserId != null
                ? itemBookingDao.listSnapshotsByItemIdForOwner(itemId, viewerUserId)
                : List.of();
        final ReviewService.PendingReviewAction pendingItemReviewAction = viewerUserId == null
                ? null
                : reviewService
                        .findPendingItemReviewAction(viewerUserId, itemId)
                        .orElse(null);

        final AvailabilityPickerData availability = AvailabilityPickerBuilder.build(
                itemAvailabilityDao.listAvailabilitiesByItemId(itemId), itemBookingDao.listBookingsByItemId(itemId));
        final List<String> offeredDates = availability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = availability.getOfferedTimesByDate();
        final String resolvedDate = AvailabilityPickerBuilder.resolveSelectedDate(requestedDate, offeredDates, "");
        final List<String> reservationSlots = offeredTimesByDate.getOrDefault(resolvedDate, List.of());
        final boolean validRequestedRange = resolvedDate.equals(requestedDate)
                && requestedStartTime != null
                && !requestedStartTime.isBlank()
                && requestedEndTime != null
                && !requestedEndTime.isBlank()
                && AvailabilityPickerBuilder.hasContinuousAvailability(
                        reservationSlots, requestedStartTime, requestedEndTime);
        final String resolvedStart = validRequestedRange
                ? requestedStartTime
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedStartTime, reservationSlots, "");
        final String resolvedEnd = validRequestedRange
                ? requestedEndTime
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedEndTime, reservationSlots, "");

        return Optional.of(new MarketplaceItemView(
                item.get(),
                isOwner,
                displayItem,
                selectedSnapshot.orElse(null),
                hideListingLiveVersionNavigation,
                !isActive,
                guestSnapshots,
                hostSnapshots,
                owner,
                itemType,
                ratingSummary,
                reviews,
                reviewAuthorNames,
                coverImageId,
                galleryImageIds,
                useSnapshotCover,
                buildOwnerInitial(owner),
                pendingItemReviewAction,
                availability,
                resolvedDate,
                resolvedStart,
                resolvedEnd));
    }

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

    @Override
    public OwnerAvailabilityView buildOwnerAvailabilityView(
            final int itemId, final String requestedDate, final int ownerId) {
        return OwnerAvailabilityViewBuilder.build(
                itemAvailabilityDao.listAvailabilitiesByItemId(itemId),
                itemBookingDao.listBookingsByItemId(itemId),
                requestedDate,
                ownerId);
    }

    @Override
    public AvailabilityPickerData buildAvailabilityPicker(final int itemId) {
        return AvailabilityPickerBuilder.build(
                itemAvailabilityDao.listAvailabilitiesByItemId(itemId), itemBookingDao.listBookingsByItemId(itemId));
    }

    @Override
    public AvailabilityPickerData buildGlobalAvailabilityPicker() {
        return AvailabilityPickerBuilder.build(itemAvailabilityDao.listAvailabilities(), itemBookingDao.listBookings());
    }

    @Override
    public boolean isRequestedRangeAvailable(
            final int itemId, final String date, final String startTime, final String endTime) {
        if (date == null || date.isBlank()) {
            return true;
        }
        final AvailabilityPickerData data = buildAvailabilityPicker(itemId);
        final List<String> times = data.getOfferedTimesByDate().get(date);
        if (times == null || times.isEmpty()) {
            return false;
        }
        final boolean blankStart = startTime == null || startTime.isBlank();
        final boolean blankEnd = endTime == null || endTime.isBlank();
        if (blankStart && blankEnd) {
            return hasAnyContinuousTwoHourWindow(times);
        }
        if (!blankStart && blankEnd) {
            return hasContinuousTwoHourWindowStartingAt(times, startTime);
        }
        if (blankStart) {
            return hasContinuousTwoHourWindowEndingAt(times, endTime);
        }
        return AvailabilityPickerBuilder.hasContinuousAvailability(times, startTime, endTime);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
    public EditConflictView buildEditConflictView(final int itemId) {
        final List<ItemBooking> bookings = listActiveBookingsByItemId(itemId);
        final Item item = itemDao.findAnyItemById(itemId).orElse(null);
        final Integer pricePerHour = item == null ? null : item.getPricePerHour();
        final Map<Integer, String> guestNames = new LinkedHashMap<>();
        final Map<Integer, String> startLabels = new LinkedHashMap<>();
        final Map<Integer, String> friendlyDates = new LinkedHashMap<>();
        final Map<Integer, String> friendlyTimeRanges = new LinkedHashMap<>();
        final Map<Integer, String> friendlyPrices = new LinkedHashMap<>();
        final Map<Integer, String> statusCodes = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            final int id = booking.getId();
            guestNames.put(
                    id,
                    booking.getGuestId() == null
                            ? ""
                            : userDao.findById(booking.getGuestId())
                                    .map(User::getName)
                                    .orElse(""));
            startLabels.put(id, BookingDisplayFormatter.formatStartLabel(booking.getStartTime()));
            friendlyDates.put(id, BookingDisplayFormatter.formatFriendlyDate(booking.getStartTime()));
            friendlyTimeRanges.put(
                    id, BookingDisplayFormatter.formatFriendlyTimeRange(booking.getStartTime(), booking.getEndTime()));
            friendlyPrices.put(
                    id,
                    BookingDisplayFormatter.formatFriendlyTotalPrice(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            statusCodes.put(
                    id,
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
        }
        return new EditConflictView(
                bookings, guestNames, startLabels, friendlyDates, friendlyTimeRanges, friendlyPrices, statusCodes);
    }

    @Override
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

    @Override
    @Transactional
    public boolean reorderGalleryForOwner(final int itemId, final int ownerId, final List<Integer> imageIdsInOrder) {
        if (itemDao.findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return false;
        }
        reorderImagesForItem(itemId, imageIdsInOrder);
        return true;
    }

    // ---- internal helpers for view assembly --------------------------------------------------

    List<ReceivedBookingView> buildReceivedBookings(final int ownerId) {
        final List<ReceivedBookingView> result = new ArrayList<>();
        final Map<Integer, RatingSummary> requesterRatingByUserId = new LinkedHashMap<>();
        for (final ItemBooking booking : itemBookingDao.listBookingsByOwnerId(ownerId)) {
            if (booking.getItemId() == null || booking.getId() == null || booking.getGuestId() == null) {
                continue;
            }
            if (booking.getGuestId().equals(ownerId)) {
                continue;
            }
            final Item item = itemBookingDao
                    .findSnapshotByBookingIdForOwner(booking.getId(), ownerId)
                    .<Item>map(snapshot -> snapshot)
                    .orElseGet(
                            () -> itemDao.findAnyItemById(booking.getItemId()).orElse(null));
            if (item == null) {
                continue;
            }
            final User requester = userDao.findById(booking.getGuestId()).orElse(null);
            final RatingSummary rating = requesterRatingByUserId.computeIfAbsent(booking.getGuestId(), guestId -> {
                final List<Review> received = reviewDao.listReviewsByReviewee(guestId).stream()
                        .filter(r -> r.getTargetType() == ReviewTargetType.USER)
                        .toList();
                if (received.isEmpty()) {
                    return RatingSummary.empty();
                }
                final double avg = received.stream()
                        .map(Review::getRating)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                return new RatingSummary(avg, received.size());
            });
            final Optional<BookingPaymentProof> proof = itemBookingDao.findPaymentProofByBookingId(booking.getId());
            result.add(new ReceivedBookingView(
                    booking.getId(),
                    booking.getItemId(),
                    itemMediaDao.findCoverImageIdByItemId(booking.getItemId()).orElse(null),
                    item.getTitle(),
                    requester == null ? "" : requester.getName(),
                    requester == null ? "" : requester.getEmail(),
                    rating.getAverageRating(),
                    rating.getTotalReviews(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    BookingDisplayFormatter.formatDateLabel(booking.getStartTime()),
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()),
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), item.getPricePerHour()),
                    "",
                    BookingDisplayFormatter.statusMessageCode(booking.getState()),
                    proof.map(BookingPaymentProof::getFileName).orElse(""),
                    proof.map(BookingPaymentProof::getContentType).orElse(""),
                    proof.map(BookingPaymentProof::getRefusalReason).orElse(""),
                    proof.map(BookingPaymentProof::getGuestReply).orElse(""),
                    booking.getRequestMessage()));
        }
        return result;
    }

    List<PendingReviewView> buildPendingReviewViews(final int userId) {
        final List<PendingReviewView> result = new ArrayList<>();
        for (final ReviewService.PendingReviewAction action : reviewService.listPendingReviewActions(userId)) {
            final Item item = itemDao.findAnyItemById(action.getItemId()).orElse(null);
            final User target = userDao.findById(action.getTargetUserId()).orElse(null);
            if (item == null || target == null) {
                continue;
            }
            result.add(new PendingReviewView(
                    action.getBookingId(),
                    item.getId(),
                    item.getTitle(),
                    action.getTargetType(),
                    target.getName(),
                    target.getEmail(),
                    BookingDisplayFormatter.formatDateLabel(action.getStartTime()),
                    BookingDisplayFormatter.formatTimeRangeLabel(action.getStartTime(), action.getEndTime())));
        }
        return result;
    }

    Map<Integer, AuthoredItemReviewSummaryView> buildAuthoredReviewsByBookingId(
            final int reviewerUserId, final ReviewTargetType targetType) {
        final Map<Integer, AuthoredItemReviewSummaryView> map = new LinkedHashMap<>();
        for (final Review review : reviewService.listAuthoredReviews(reviewerUserId)) {
            if (review.getBookingId() == null || review.getTargetType() != targetType) {
                continue;
            }
            map.put(
                    review.getBookingId(),
                    new AuthoredItemReviewSummaryView(
                            review.getRating() == null ? 0 : review.getRating(),
                            review.getComment() == null ? "" : review.getComment()));
        }
        return map;
    }

    private Optional<ItemSnapshot> resolveAuthorizedSnapshotVersion(
            final int versionId, final int itemId, final int viewerUserId) {
        final Optional<ItemSnapshot> guestSnapshot =
                itemBookingDao.findSnapshotVersionByIdForGuest(versionId, itemId, viewerUserId);
        if (guestSnapshot.isPresent()) {
            return guestSnapshot;
        }
        return itemBookingDao.findSnapshotVersionByIdForOwner(versionId, itemId, viewerUserId);
    }

    private static String buildOwnerInitial(final User user) {
        if (user == null || user.getName() == null || user.getName().isEmpty()) {
            return "I";
        }
        return user.getName().substring(0, 1).toUpperCase();
    }

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

    private static boolean hasAnyContinuousTwoHourWindow(final List<String> times) {
        for (int i = 0; i < times.size(); i++) {
            for (int j = i + 1; j < times.size(); j++) {
                if (AvailabilityPickerBuilder.hasContinuousAvailability(times, times.get(i), times.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowStartingAt(final List<String> times, final String start) {
        if (!times.contains(start)) {
            return false;
        }
        for (final String end : times) {
            if (AvailabilityPickerBuilder.hasContinuousAvailability(times, start, end)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowEndingAt(final List<String> times, final String end) {
        for (final String start : times) {
            if (AvailabilityPickerBuilder.hasContinuousAvailability(times, start, end)) {
                return true;
            }
        }
        return false;
    }
}
