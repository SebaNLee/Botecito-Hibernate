package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.FavouriteDao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavouriteServiceImpl.class);

    private final FavouriteDao favouriteDao;
    private final ItemService itemService;
    private final ReviewService reviewService;

    @Override
    @Transactional
    public boolean addFavourite(final int userId, final int itemId) {
        final Item item = itemService.findItemById(itemId);
        if (!canFavourite(userId, item)) {
            return false;
        }
        favouriteDao.create(userId, itemId);
        LOGGER.info("User {} added favourite item {}", userId, itemId);
        return true;
    }

    @Override
    @Transactional
    public boolean removeFavourite(final int userId, final int itemId) {
        favouriteDao.delete(userId, itemId);
        LOGGER.info("User {} removed favourite item {}", userId, itemId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavourite(final int userId, final int itemId) {
        return favouriteDao.exists(userId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageModel<Item> listFavourites(
            final int userId, final String searchQuery, final int page, final int pageSize, final String sortBy) {
        final FavouritesQueryModel query = FavouritesQueryModel.builder()
                .userId(userId)
                .searchQuery(searchQuery)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .build();
        final PageModel<Item> results = favouriteDao.listFavourites(query);
        reviewService.attachReviewSummaries(results.getContent());
        return results;
    }

    private static boolean canFavourite(final int userId, final Item item) {
        if (item == null || item.getStatus() == ItemStatusEnum.DELETED) {
            return false;
        }
        return item.getHost() == null
                || item.getHost().getId() == null
                || item.getHost().getId() != userId;
    }
}
