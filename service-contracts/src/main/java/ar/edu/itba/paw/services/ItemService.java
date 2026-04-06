package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import java.util.List;
import java.util.Optional;

public interface ItemService {
    List<Item> listItems();

    Optional<Item> findItemById(final int id);

    Optional<User> findUserById(final int id);

    Optional<ItemType> findItemTypeById(final int id);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(final int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(final int itemId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    Optional<byte[]> findImageById(final int id);

    List<Integer> listImageIdsByItemId(final int itemId);

    Integer insertImage(final int itemId, final byte[] imageData);
}
