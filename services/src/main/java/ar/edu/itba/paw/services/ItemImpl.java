package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
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
    @Transactional(readOnly = true)
    public Optional<Image> findImageWithDataById(final int imageId) {
        return itemDao.findImageWithDataById(imageId);
    }
}
