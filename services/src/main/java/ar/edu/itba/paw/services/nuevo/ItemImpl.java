package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.nuevo.ItemDao;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemImpl implements ItemInterface {

    private final ItemDao itemDao;

    @Override
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId, final int page, final int pageSize) {
        return itemDao.listMyBoatsItemsByOwnerId(ownerId, page, pageSize);
    }

    @Override
    public int countMyBoatsItemsByOwnerId(final int ownerId) {
        return itemDao.countMyBoatsItemsByOwnerId(ownerId);
    }

    @Override
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        return itemDao.findMyBoatsItemByIdForOwner(itemId, ownerId);
    }

    @Override
    public Optional<MyBoatsItem> createMyBoatsItem(final ItemCreateModel createModel) {
        return itemDao.createMyBoatsItem(createModel);
    }

    @Override
    public boolean updateMyBoatsItem(final int itemId, final int ownerId, final ItemUpdateModel updateModel) {
        return itemDao.updateMyBoatsItem(itemId, ownerId, updateModel);
    }

    @Override
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        return itemDao.deleteMyBoatsItem(itemId, ownerId);
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return itemDao.setItemActiveForOwner(itemId, ownerId, active);
    }
}
