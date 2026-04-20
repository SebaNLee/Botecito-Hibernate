package ar.edu.itba.paw.services;

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
import java.util.List;
import java.util.Optional;

public interface ItemService {
    List<Item> listItems();

    Page<Item> searchItems(final ItemSearchCriteria criteria, final int page, final int pageSize);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(final int id);

    Optional<User> findUserById(final int id);

    Optional<User> findUserByEmail(final String email);

    Optional<ItemType> findItemTypeById(final int id);

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

    boolean setItemActive(final int itemId, final boolean active);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(final int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(final int itemId);

    List<ItemBooking> listBookingsByGuestId(final int guestId);

    List<ItemBooking> listBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId);

    List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemId(final int itemId);

    Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime);

    Integer insertImage(final int itemId, final byte[] imageData);
}
