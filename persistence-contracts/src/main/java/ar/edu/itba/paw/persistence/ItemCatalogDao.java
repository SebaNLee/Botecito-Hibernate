package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.CatalogUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import java.util.List;
import java.util.Optional;

public interface ItemCatalogDao {
    List<Item> listItems();

    Optional<Item> findItemById(final int id);

    Optional<CatalogUser> findUserById(final int id);

    Optional<ItemType> findItemTypeById(final int id);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(final int itemId);

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(final int itemId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId);

    Optional<String> findImageUrlByItemId(final int itemId);
}
