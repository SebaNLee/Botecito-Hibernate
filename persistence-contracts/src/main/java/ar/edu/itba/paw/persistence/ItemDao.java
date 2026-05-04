package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Persistence for listings, publication versions, and item lifecycle (not bookings or media). */
public interface ItemDao {
    List<Item> listItems();

    List<Item> listItems(ItemSearchCriteria criteria, int limit, int offset);

    int countItems(ItemSearchCriteria criteria);

    List<Item> listItemsByOwnerId(int ownerId);

    List<LocationOption> listLocationOptions();

    Optional<Item> findItemById(int id);

    Optional<Item> findItemByIdForOwner(int id, int ownerId);

    Optional<Item> findAnyItemById(int id);

    Optional<ItemType> findItemTypeById(int id);

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

    /** True when non-owner guest bookings would block publication edits (excludes owner personal blocks). */
    boolean hasBlockingBookingsForEdition(int itemId);

    boolean deleteItemById(int itemId);

    boolean deleteItemByIdForOwner(int itemId, int ownerId);

    Item createItem(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            BigDecimal maxWeightKg,
            Integer difficultyLevel,
            int locationOptionId,
            String ownerDeleteToken);

    boolean snapshotBookingsForPublicationEdit(int itemId);

    boolean setItemActive(int itemId, boolean active);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    Integer insertItem(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            BigDecimal maxWeightKg,
            Integer difficultyLevel,
            int locationOptionId);
}
