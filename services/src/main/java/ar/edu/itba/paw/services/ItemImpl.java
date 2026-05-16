package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.services.exceptions.VersionNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Transactional
    public boolean deleteMyBoatsItem(final int itemId, final int ownerId) {
        final Optional<Item> item = itemDao.findItemByIdAndOwner(itemId, ownerId);
        if (item.isEmpty()) {
            return false;
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
    public int createPublicationVersion(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final Optional<Version> current = itemDao.findCurrentVersionByItemId(itemId);
        if (current.isEmpty()) {
            throw new VersionNotFoundException(itemId);
        }

        final boolean hasBookings =
                itemDao.hasBookingReferencesByVersionId(current.get().getId());
        if (!hasBookings) {
            current.get().setTitle(title);
            current.get().setDescription(description);
            current.get().setPrice(BigDecimal.valueOf(pricePerHour));
            current.get()
                    .setDifficulty(
                            difficultyLevel != null
                                    ? difficultyLevel
                                    : current.get().getDifficulty());
            current.get().setLocation(itemDao.getLocationReference(locationOptionId));
            current.get().setCreatedAt(LocalDateTime.now());
            return current.get().getId();
        }

        final Version next = new Version();
        next.setItem(current.get().getItem());
        next.setType(current.get().getType());
        next.setTitle(title);
        next.setDescription(description);
        next.setPrice(BigDecimal.valueOf(pricePerHour));
        next.setCapacity(current.get().getCapacity());
        next.setWeight(current.get().getWeight());
        next.setDifficulty(
                difficultyLevel != null ? difficultyLevel : current.get().getDifficulty());
        next.setLocation(itemDao.getLocationReference(locationOptionId));
        next.setTimezone(current.get().getTimezone());
        next.setCreatedAt(LocalDateTime.now());
        itemDao.persistVersion(next);

        itemDao.copyVersionContent(current.get().getId(), next);

        return next.getId();
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
