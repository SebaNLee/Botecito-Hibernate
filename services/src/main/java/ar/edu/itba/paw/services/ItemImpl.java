package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.ItemDao;
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
    public ItemSearchResult listOwnerItems(int ownerId, int page, int pageSize) {
        var result = itemDao.listOwnerItems(ownerId, page, pageSize);

        // This loop is safe, result is bounded
        for (Item item : result.getItems()) {
            // Fetch latest version and the cover image
            var media = item.getLatestVersion().getMedia();

            if (media != null && media.size() > 0) {
                @SuppressWarnings("unused")
                var image = media.get(0).getImage();
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Item findItemById(int id) {
        return itemDao.findItemById(id).orElseThrow(ItemNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Image> findImageById(int id) {
        var image = itemDao.findImageById(id);
        image.ifPresent(Image::getData);
        return image;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsItem(Item item, int userId) {
        return item.getHost().getId().equals(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsItem(int itemId, int userId) {
        var item = findItemById(itemId);
        return userOwnsItem(item, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Item requireOwnedItem(int itemId, int userId) {
        var item = findItemById(itemId);
        if (!userOwnsItem(item, userId)) throw new ForbiddenOperationException();
        return item;
    }
}
