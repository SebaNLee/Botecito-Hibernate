package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ItemService {
    List<Item> listItems();

    Page<Item> searchItems(ItemSearchCriteria criteria, int page, int pageSize);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(int id);

    Optional<Item> findItemByIdForOwner(int id, int ownerId);

    Optional<Item> findAnyItemById(int id);

    Optional<User> findUserById(int id);

    Optional<User> findUserByEmail(String email);

    Optional<ItemType> findItemTypeById(int id);

    boolean updatePublicationForOwner(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId,
            byte[] primaryImageData);

    boolean hasBlockingBookingsForEdition(int itemId);

    Map<Integer, Boolean> publicationDeleteDeactivatesByItemId(List<Item> ownedItems);

    Map<Integer, Boolean> publicationDeleteDisabledByItemId(List<Item> ownedItems);

    boolean deleteItemByIdForOwner(int itemId, int ownerId);

    Item createPublication(
            String ownerGivenName,
            String ownerLastName,
            String ownerEmail,
            String ownerPreferredLanguage,
            Integer typeId,
            String title,
            String description,
            Integer pricePerHour,
            Integer capacityPeople,
            BigDecimal maxWeightKg,
            Integer difficultyLevel,
            Integer locationOptionId,
            List<ItemAvailability> availabilities);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(int itemId);

    List<ItemBooking> listBookingsByGuestId(int guestId);

    List<ItemBooking> listBookingsByOwnerId(int ownerId);

    List<ItemBooking> listPendingBookingsByOwnerId(int ownerId);

    List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(int ownerId);

    List<ItemBooking> listActiveBookingsByItemId(int itemId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(int bookingId, int guestId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(int bookingId, int ownerId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(int versionId, int itemId, int guestId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(int versionId, int itemId, int ownerId);

    List<ItemSnapshot> listSnapshotsByItemIdForGuest(int itemId, int guestId);

    List<ItemSnapshot> listSnapshotsByItemIdForOwner(int itemId, int ownerId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(int itemId);

    Optional<byte[]> findImageById(int id);

    List<Integer> listImageIdsByItemIdOrdered(int itemId);

    Optional<Integer> findCoverImageIdByItemId(int itemId);

    int countImagesByItemId(int itemId);

    int maxImagesPerItem();

    Integer insertAvailability(int itemId, DayOfWeek weekday, LocalTime startTime, LocalTime endTime);

    Integer appendImage(int itemId, byte[] imageData);

    void replaceGallery(int itemId, List<byte[]> orderedImages);

    boolean deleteImageFromItem(int itemId, int imageId);

    void reorderImagesForItem(int itemId, List<Integer> imageIdsInOrder);

    ItemSearchCriteria parseAndValidateSearchCriteria(
            String searchQuery,
            String locationOptionId,
            String date,
            String startTime,
            String endTime,
            String capacity,
            String maxWeight,
            String difficulty,
            String minRating,
            String sort);

    Page<Item> searchMarketplace(ItemSearchCriteria criteria, int page, int pageSize);

    java.util.Map<String, String> validatePublicationDraft(PublicationDraft draft);

    Item createPublicationFromDraft(PublicationDraft draft);

    boolean hasPublicationChanges(
            int itemId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId,
            byte[] primaryImageData);

    void resolveEditConflict(int itemId, BookingDecisionBatch decisions);

    GalleryOwnerUploadResult uploadGalleryImage(int itemId, int ownerId, GalleryImageUpload image);

    boolean reorderGalleryForOwner(int itemId, int ownerId, List<Integer> imageIdsInOrder);

    /** Comma-separated image ids; returns an empty list if the input is blank or any token is not a valid id. */
    List<Integer> parseGalleryImageOrderCsv(String csv);

    RatingSummary getItemRatingSummary(int itemId);

    List<Review> listLatestReviews(int itemId, int limit);

    Optional<ReviewService.PendingReviewAction> findPendingReviewAction(int userId, int itemId);
}
