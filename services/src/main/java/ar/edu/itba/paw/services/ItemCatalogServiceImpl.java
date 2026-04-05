package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.CatalogUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.persistence.ItemCatalogDao;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ItemCatalogServiceImpl implements ItemCatalogService {
    private final ItemCatalogDao itemCatalogDao;

    @Autowired
    public ItemCatalogServiceImpl(final ItemCatalogDao itemCatalogDao) {
        this.itemCatalogDao = itemCatalogDao;
    }

    @Override
    public List<Item> listItems() {
        return itemCatalogDao.listItems();
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return itemCatalogDao.findItemById(id);
    }

    @Override
    public Optional<CatalogUser> findUserById(final int id) {
        return itemCatalogDao.findUserById(id);
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return itemCatalogDao.findItemTypeById(id);
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return itemCatalogDao.listAvailabilities();
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return itemCatalogDao.listAvailabilitiesByItemId(itemId);
    }

    @Override
    public List<ItemBooking> listBookings() {
        return itemCatalogDao.listBookings();
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return itemCatalogDao.listBookingsByItemId(itemId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return itemCatalogDao.findNextAvailabilityByItemId(itemId);
    }

    @Override
    public Optional<String> findImageUrlByItemId(final int itemId) {
        return itemCatalogDao.findImageUrlByItemId(itemId);
    }
}
