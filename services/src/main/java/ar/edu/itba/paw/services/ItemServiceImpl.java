package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ItemServiceImpl implements ItemService {
    private final ItemDao itemDao;

    @Autowired
    public ItemServiceImpl(final ItemDao itemDao) {
        this.itemDao = itemDao;
    }

    @Override
    public List<Item> listItems() {
        return itemDao.listItems();
    }

    @Override
    public List<LocationOption> listLocationOptions() {
        return itemDao.listLocationOptions();
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return itemDao.findItemById(id);
    }

    @Override
    public Optional<User> findUserById(final int id) {
        return itemDao.findUserById(id);
    }

    @Override
    public Optional<User> findUserByEmail(final String email) {
        return itemDao.findUserByEmail(email);
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return itemDao.findItemTypeById(id);
    }

    @Override
    public Item createPublication(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage,
            final Integer typeId,
            final String title,
            final String description,
            final Integer pricePerHour,
            final Integer capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final Integer locationOptionId,
            final List<ItemAvailability> availabilities) {
        final User ownerUser = resolveOrCreateOwner(ownerGivenName, ownerLastName, ownerEmail, ownerPreferredLanguage);
        final String ownerDeleteToken = UUID.randomUUID().toString();
        final Item item = itemDao.createItem(
                ownerUser.getId(),
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                maxWeightKg,
                difficultyLevel,
                locationOptionId,
                ownerDeleteToken);

        for (final ItemAvailability availability : availabilities) {
            itemDao.createItemAvailability(
                    item.getId(),
                    availability.getWeekday().name(),
                    availability.getStartTime().toString(),
                    availability.getEndTime().toString());
        }
        return item;
    }

    @Override
    public Optional<Item> findItemByOwnerDeleteToken(final String ownerDeleteToken) {
        return itemDao.findItemByOwnerDeleteToken(ownerDeleteToken);
    }

    @Override
    public boolean activateItemByOwnerDeleteToken(final String ownerDeleteToken) {
        return itemDao.activateItemByOwnerDeleteToken(ownerDeleteToken);
    }

    @Override
    public boolean deactivateItemByOwnerDeleteToken(
            final String ownerDeleteToken, final OffsetDateTime ownerDeleteUsedAt) {
        return itemDao.deactivateItemByOwnerDeleteToken(ownerDeleteToken, ownerDeleteUsedAt);
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return itemDao.listAvailabilities();
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return itemDao.listAvailabilitiesByItemId(itemId);
    }

    @Override
    public List<ItemBooking> listBookings() {
        return itemDao.listBookings();
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return itemDao.listBookingsByItemId(itemId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return itemDao.findNextAvailabilityByItemId(itemId);
    }

    @Override
    public Optional<byte[]> findImageById(final int id) {
        return itemDao.findImageById(id);
    }

    @Override
    public List<Integer> listImageIdsByItemId(final int itemId) {
        return itemDao.listImageIdsByItemId(itemId);
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return itemDao.insertAvailability(itemId, weekday, startTime, endTime);
    }

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData) {
        return itemDao.insertImage(itemId, imageData);
    }

    private User resolveOrCreateOwner(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage) {
        final String preferredLanguage = "en".equalsIgnoreCase(ownerPreferredLanguage) ? "en" : "es";
        final Optional<User> existingOwner = itemDao.findUserByEmail(ownerEmail);
        if (existingOwner.isPresent()) {
            final User user = existingOwner.get();
            itemDao.updateUserProfile(user.getId(), ownerGivenName, ownerLastName, preferredLanguage);
            user.setGivenName(ownerGivenName);
            user.setLastName(ownerLastName);
            user.setPreferredLanguage(preferredLanguage);
            return user;
        }
        return itemDao.createUser(ownerGivenName, ownerLastName, ownerEmail, preferredLanguage);
    }
}
