package ar.edu.itba.paw.persistence;

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

    Optional<User> findUserById(final int id);

    Optional<User> findUserByEmail(final String email);

    User createUser(final String givenName, final String lastName, final String email, final String preferredLanguage);

    boolean updateUserProfile(
            final int userId, final String givenName, final String lastName, final String preferredLanguage);

    Optional<ItemType> findItemTypeById(final int id);

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

    Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken);

    ItemBooking createBookingRequest(
            final int itemId,
            final int guestId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String requestMessage,
            final String hostDecisionToken);

    boolean resolveBookingByHostDecisionToken(
            final String hostDecisionToken, final BookingState newState, final OffsetDateTime hostDecisionUsedAt);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    Optional<Item> findItemByOwnerDeleteToken(final String ownerDeleteToken);

    boolean activateItemByOwnerDeleteToken(final String ownerDeleteToken);

    boolean deactivateItemByOwnerDeleteToken(final String ownerDeleteToken, final OffsetDateTime ownerDeleteUsedAt);

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemId(final int itemId);

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

    Integer insertImage(final int itemId, final byte[] imageData);
}
