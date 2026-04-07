package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<Item> listItems();

    Optional<Item> findItemById(final int id);

    Optional<User> findUserById(final int id);

    Optional<User> findUserByEmail(final String email);

    User createUser(final String givenName, final String lastName, final String email);

    Optional<ItemType> findItemTypeById(final int id);

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

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemId(final int itemId);

    Integer insertImage(final int itemId, final byte[] imageData);
}
