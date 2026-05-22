package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.persistence.ItemDao;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemImpl implements ItemService {

    private final ItemDao itemDao;

    @Override
    @Transactional(readOnly = true)
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId, final int page, final int pageSize) {
        return itemDao.listMyBoatsItemsByOwnerId(ownerId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        return itemDao.countMyBoatsItemsByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        return itemDao.findMyBoatsItemByIdForOwner(itemId, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public MyBoatsItem requireOwnedItem(final int itemId, final int callerId) {
        return findMyBoatsItemByIdForOwner(itemId, callerId).orElseThrow(ForbiddenOperationException::new);
    }

    @Override
    @Transactional
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        final Optional<Item> item = itemDao.findItemByIdAndOwner(itemId, ownerId);
        if (item.isEmpty()) {
            throw new ForbiddenOperationException();
        }

        if (itemDao.hasActiveOrFutureBookings(itemId)) {
            if (item.get().getStatus() == ItemStatusEnum.ACTIVE) {
                item.get().setStatus(ItemStatusEnum.INACTIVE);
                return true;
            }
            return false;
        }

        itemDao.deleteItem(item.get());
        return true;
    }

    @Override
    @Transactional
    public void setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        if (!itemDao.setItemActiveForOwner(itemId, ownerId, active)) {
            throw new ForbiddenOperationException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Image> findImageWithDataById(final int imageId) {
        return itemDao.findImageWithDataById(imageId);
    }
}
