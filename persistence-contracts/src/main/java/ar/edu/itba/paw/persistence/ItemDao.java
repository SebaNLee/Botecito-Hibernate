package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<Item> listItems();

    List<Item> listItems(final ItemSearchCriteria criteria, final int limit, final int offset);

    int countItems(final ItemSearchCriteria criteria);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(final int id);

    Optional<Item> findItemByIdForOwner(final int id, final int ownerId);

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

    boolean updatePublicationForOwner(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId);

    boolean hasBlockingBookingsForEdition(int itemId);

    boolean deleteItemById(int itemId);

    boolean deleteItemByIdForOwner(int itemId, int ownerId);

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

    List<ItemBooking> listActiveBookingsByItemId(final int itemId);

    Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken);

    List<ItemBooking> findBookingsByHostDecisionTokens(Collection<String> hostDecisionTokens);

    Optional<ItemBooking> findBookingById(final int bookingId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(final int versionId, final int itemId, final int guestId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(final int versionId, final int itemId, final int ownerId);

    List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId);

    List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId);

    boolean snapshotBookingsForPublicationEdit(final int itemId);

    ItemBooking createBookingRequest(
            final int itemId,
            final int guestId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String requestMessage,
            final String hostDecisionToken);

    boolean resolveBookingByHostDecisionToken(
            final String hostDecisionToken, final BookingState newState, final OffsetDateTime hostDecisionUsedAt);

    int resolveBookingsByHostDecisionTokens(
            Collection<String> hostDecisionTokens, BookingState newState, OffsetDateTime hostDecisionUsedAt);

    List<User> findUsersByIds(Collection<Integer> userIds);

    BookingPaymentProof createPaymentProof(
            final int bookingId,
            final int uploaderId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply);

    Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId);

    Optional<BookingPaymentProof> findPaymentProofById(final int proofId);

    boolean deletePaymentProofByBookingId(final int bookingId);

    boolean markBookingPaymentSubmitted(final int bookingId, final int guestId);

    boolean markBookingPaymentResubmitted(final int bookingId, final int guestId);

    boolean markBookingPaid(final int bookingId, final int ownerId);

    boolean markBookingPaymentRefused(final int bookingId, final int ownerId, final String reason);

    Optional<Review> createReview(
            int bookingId,
            int reviewerUserId,
            int revieweeUserId,
            ReviewTargetType targetType,
            int targetId,
            int rating,
            String comment);

    Optional<Review> findReviewByBookingReviewerAndTargetType(
            int bookingId, int reviewerUserId, ReviewTargetType targetType);

    List<Review> listReviewsByTarget(ReviewTargetType targetType, int targetId);

    List<Review> listLatestReviewsByTarget(ReviewTargetType targetType, int targetId, int limit);

    List<Review> listReviewsByReviewer(int reviewerUserId);

    List<Review> listReviewsByReviewee(int revieweeUserId);

    Optional<Review> findReviewById(int reviewId);

    boolean deleteReview(int reviewId, int reviewerUserId);

    RatingSummary ratingSummaryByTarget(ReviewTargetType targetType, int targetId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    boolean setItemActive(final int itemId, final boolean active);

    boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active);

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

    Integer replacePrimaryImageForOwner(final int itemId, final int ownerId, final byte[] imageData);
}
