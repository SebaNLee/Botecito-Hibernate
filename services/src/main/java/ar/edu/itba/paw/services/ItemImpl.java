package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.persistence.ItemDao;
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
    @Transactional
    public boolean updateMyBoatsItem(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        return itemDao.updateMyBoatsItem(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId);
    }

    @Override
    @Transactional
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        return itemDao.deleteMyBoatsItem(itemId, ownerId);
    }

    @Override
    @Transactional
    public int createPublicationVersion(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        return itemDao.createPublicationVersion(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId);
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

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findImageDataById(final int imageId) {
        return itemDao.findImageDataById(imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listImageIds(final int itemId) {
        return itemDao.listImageIds(itemId);
    }

    @Override
    @Transactional
    public Optional<Integer> uploadGalleryImage(final int itemId, final int ownerId, final byte[] imageData) {
        return itemDao.uploadGalleryImage(itemId, imageData);
    }

    @Override
    @Transactional
    public boolean deleteImageFromGallery(final int imageId) {
        return itemDao.deleteImageFromGallery(imageId);
    }

    @Override
    @Transactional
    public boolean reorderGallery(final int itemId, final int ownerId, final List<Integer> imageIdsInOrder) {
        return itemDao.reorderGallery(itemId, imageIdsInOrder);
    }
}
