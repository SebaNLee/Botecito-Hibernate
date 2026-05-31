package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.FavouriteDao;
import ar.edu.itba.paw.persistence.ItemDao;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private final FavouriteDao favouriteDao;
    private final ItemDao itemDao;

    @Override
    @Transactional
    public boolean addFavourite(final int userId, final int itemId) {
        final Item item = itemDao.findItemById(itemId).orElse(null);
        if (!canFavourite(userId, item)) {
            return false;
        }
        favouriteDao.create(userId, itemId);
        return true;
    }

    @Override
    @Transactional
    public boolean removeFavourite(final int userId, final int itemId) {
        favouriteDao.delete(userId, itemId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavourite(final int userId, final int itemId) {
        return favouriteDao.exists(userId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> findFavouriteItemIds(final int userId, final Collection<Integer> itemIds) {
        return favouriteDao.findFavouriteItemIds(userId, itemIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageModel<Item> listFavourites(final int userId, final int page, final int pageSize) {
        final int safePage = Math.max(1, page);
        final int safePageSize = pageSize > 0 ? Math.min(pageSize, 18) : DEFAULT_PAGE_SIZE;
        return new PageModel<>(
                favouriteDao.listFavourites(userId, safePage, safePageSize),
                safePage,
                safePageSize,
                favouriteDao.countFavourites(userId));
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
