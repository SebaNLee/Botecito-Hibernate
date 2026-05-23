package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.persistence.EditDao;
import ar.edu.itba.paw.persistence.ManageItemDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageItemServiceImpl implements ManageItemService {

    private final ManageItemDao manageItemDao;
    private final EditDao editDao;

    @Override
    @Transactional
    public void deleteItem(final int itemId, final int ownerId) {
        final Item item = requireOwnedItem(itemId, ownerId);

        if (manageItemDao.countVersionsByItemId(itemId) > 1) {
            applySoftDelete(item);
            return;
        }

        if (editDao.itemHasBookings(itemId)) {
            applySoftDelete(item);
        } else {
            manageItemDao.deleteItem(item);
        }
    }

    @Override
    @Transactional
    public void setEnabled(final int itemId, final int ownerId, final boolean enabled) {
        final Item item = requireOwnedItem(itemId, ownerId);
        item.setStatus(enabled ? ItemStatusEnum.ACTIVE : ItemStatusEnum.INACTIVE);
    }

    private Item requireOwnedItem(final int itemId, final int ownerId) {
        final Item item = manageItemDao.findItemById(itemId).orElseThrow(ForbiddenOperationException::new);
        if (item.getHost() == null || item.getHost().getId() != ownerId) {
            throw new ForbiddenOperationException();
        }
        return item;
    }

    private void applySoftDelete(final Item item) {
        item.setStatus(ItemStatusEnum.DELETED);
        if (!editDao.itemHasBookings(item.getId())) {
            manageItemDao.findLatestVersionIdByItemId(item.getId()).ifPresent(manageItemDao::deleteVersion);
        }
    }
}
