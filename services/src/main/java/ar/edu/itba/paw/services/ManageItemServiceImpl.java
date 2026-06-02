package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.EditDao;
import ar.edu.itba.paw.persistence.ManageItemDao;
import ar.edu.itba.paw.persistence.ReportDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageItemServiceImpl implements ManageItemService {

    private final ManageItemDao manageItemDao;
    private final ItemService itemService;
    private final EditDao editDao;
    private final BookingDao bookingDao;
    private final ReportDao reportDao;

    @Override
    @Transactional
    public void deleteItem(final int itemId, final int ownerId) {
        final Item item = itemService.requireOwnedItem(itemId, ownerId);
        deleteItemInternal(item);
        reportDao.deleteAllByItemId(itemId);
    }

    @Override
    @Transactional
    public void deleteItemAsAdmin(final int itemId) {
        final Item item = itemService.findItemById(itemId);
        deleteItemInternal(item);
    }

    @Override
    @Transactional
    public void setEnabled(final int itemId, final int ownerId, final boolean enabled) {
        final Item item = itemService.requireOwnedItem(itemId, ownerId);
        item.setStatus(enabled ? ItemStatusEnum.ACTIVE : ItemStatusEnum.INACTIVE);
    }

    private void deleteItemInternal(final Item item) {
        bookingDao.deleteAllSelfBlocks(item);

        if (manageItemDao.countVersionsByItemId(item.getId()) > 1) {
            applySoftDelete(item);
            return;
        }

        if (editDao.itemHasBookings(item.getId())) {
            applySoftDelete(item);
        } else {
            manageItemDao.deleteItem(item);
        }
    }

    private void applySoftDelete(final Item item) {
        item.setStatus(ItemStatusEnum.DELETED);
        if (!editDao.itemHasBookings(item.getId())) {
            manageItemDao.findLatestVersionIdByItemId(item.getId()).ifPresent(manageItemDao::deleteVersion);
        }
    }
}
