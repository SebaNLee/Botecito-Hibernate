package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.persistence.nuevo.ItemDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemImpl implements ItemInterface {

    private final ItemDao itemDao;

    @Override
    public List<MyBoatsItem> listMyBoatsItemsByOwnerId(final int ownerId) {
        return itemDao.listMyBoatsItemsByOwnerId(ownerId);
    }

    @Override
    public Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(final int itemId, final int ownerId) {
        return itemDao.findMyBoatsItemByIdForOwner(itemId, ownerId);
    }

    @Override
    public Optional<MyBoatsItem> createMyBoatsItem(final ItemCreateModel createModel) {
        if (createModel == null) {
            return Optional.empty();
        }
        if (createModel.getMaxWeightKg() == null) {
            createModel.setMaxWeightKg(BigDecimal.valueOf(2000));
        }
        if (createModel.getDifficultyLevel() == null) {
            createModel.setDifficultyLevel(1);
        }
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
    @Transactional
    public int createPublicationVersion(final int itemId, final int ownerId, final ItemUpdateModel update) {
        return itemDao.createPublicationVersion(itemId, ownerId, update);
    }

    @Override
    @Transactional
    // TODO temp fix for edit item page (should use PublishForm in the futuro)
    public boolean replaceVersionPrimaryImage(final int versionId, final byte[] imageData) {
        return itemDao.replaceVersionPrimaryImage(versionId, imageData);
    }

    @Override
    @Transactional
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return itemDao.setItemActiveForOwner(itemId, ownerId, active);
    }
}
