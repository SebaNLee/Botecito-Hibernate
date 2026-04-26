package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<Item> listItems();

    List<Item> listItems(final ItemSearchCriteria criteria, final int limit, final int offset);

    int countItems(final ItemSearchCriteria criteria);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(final int id);

    Optional<Item> findAnyItemById(final int id);

    Optional<User> findUserById(final int id);

    Optional<User> findUserByEmail(final String email);

    User createUser(final String givenName, final String lastName, final String email, final String preferredLanguage);

    boolean updateUserProfile(
            final int userId, final String givenName, final String lastName, final String preferredLanguage);

    Optional<ItemType> findItemTypeById(final int id);

    boolean updatePublication(
            int itemId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId);

    boolean hasBlockingBookingsForEdition(int itemId);

    boolean deleteItemById(int itemId);

    Item createItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final String ownerDeleteToken);

    ItemAvailability createItemAvailability(
            final int itemId, final String weekday, final String startTime, final String endTime);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(final int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(final int itemId);

    List<ItemBooking> listBookingsByGuestId(final int guestId);

    List<ItemBooking> listBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId);

    Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken);

    Optional<ItemBooking> findBookingById(final int bookingId);

    ItemBooking createBookingRequest(
            final int itemId,
            final int guestId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String requestMessage,
            final String hostDecisionToken);

    boolean resolveBookingByHostDecisionToken(
            final String hostDecisionToken, final BookingState newState, final OffsetDateTime hostDecisionUsedAt);

    BookingPaymentProof createPaymentProof(
            final int bookingId,
            final int uploaderId,
            final String fileName,
            final String contentType,
            final byte[] fileData);

    Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId);

    Optional<BookingPaymentProof> findPaymentProofById(final int proofId);

    boolean markBookingPaymentSubmitted(final int bookingId, final int guestId);

    boolean markBookingPaid(final int bookingId, final int ownerId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    boolean setItemActive(final int itemId, final boolean active);

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemIdOrdered(final int itemId);

    Optional<Integer> findCoverImageIdByItemId(final int itemId);

    int countImagesByItemId(final int itemId);

    Integer insertItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId);

    Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime);

    Integer insertImage(final int itemId, final byte[] imageData, final int displayOrder);

    boolean deleteImage(final int itemId, final int imageId);

    void reorderImages(final int itemId, final List<Integer> imageIdsInOrder);

    Integer replacePrimaryImage(final int itemId, final byte[] imageData);
}
