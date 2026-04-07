package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.util.List;
import java.util.Optional;
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
    public Optional<Item> findItemById(final int id) {
        return itemDao.findItemById(id);
    }

    @Override
    public Optional<User> findUserById(final int id) {
        return itemDao.findUserById(id);
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return itemDao.findItemTypeById(id);
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
    public Integer insertImage(final int itemId, final byte[] imageData) {
        return itemDao.insertImage(itemId, imageData);
    }
}
