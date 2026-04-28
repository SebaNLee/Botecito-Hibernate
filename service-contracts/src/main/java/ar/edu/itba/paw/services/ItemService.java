package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ItemService {
    List<Item> listItems();

    Page<Item> searchItems(final ItemSearchCriteria criteria, final int page, final int pageSize);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(final int id);

    Optional<Item> findItemByIdForOwner(final int id, final int ownerId);

    Optional<Item> findAnyItemById(final int id);

    Optional<User> findUserById(final int id);

    Optional<User> findUserByEmail(final String email);

    Optional<ItemType> findItemTypeById(final int id);

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

    boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(final int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(final int itemId);

    List<ItemBooking> listBookingsByGuestId(final int guestId);

    List<ItemBooking> listBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listActiveBookingsByItemId(final int itemId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(final int versionId, final int itemId, final int guestId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(final int versionId, final int itemId, final int ownerId);

    List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId);

    List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemIdOrdered(final int itemId);

    Optional<Integer> findCoverImageIdByItemId(final int itemId);

    int countImagesByItemId(final int itemId);

    int maxImagesPerItem();

    Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime);

    Integer appendImage(final int itemId, final byte[] imageData);

    void replaceGallery(final int itemId, final List<byte[]> orderedImages);

    boolean deleteImageFromItem(final int itemId, final int imageId);

    void reorderImagesForItem(final int itemId, final List<Integer> imageIdsInOrder);
}
