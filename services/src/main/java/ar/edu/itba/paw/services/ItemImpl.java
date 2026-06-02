package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.ItemDao;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemImpl implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemImpl.class);

    private final ItemDao itemDao;

    @Override
    @Transactional(readOnly = true)
    public SearchResult<Item> listOwnerItems(
            int ownerId, String searchQuery, String status, String location, int page, int pageSize, String sortBy) {
        final MyBoatsQueryModel query = MyBoatsQueryModel.builder()
                .ownerId(ownerId)
                .searchQuery(searchQuery)
                .status(status)
                .locationSlug(location)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .build();

        return itemDao.listOwnerItems(query);
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

    @Override
    @Transactional(readOnly = true)
    public Version requireOwnedFullData(int itemId, int userId) {
        var version = requireOwnedItem(itemId, userId).getLatestVersion();
        var avail = version.getAvailabilities();
        var media = version.getMedia();

        // Force hibernate to load the contents of collections
        LOGGER.debug("Force-loading {} availabilities and {} media references.", avail.size(), media.size());

        return version;
    }
}
