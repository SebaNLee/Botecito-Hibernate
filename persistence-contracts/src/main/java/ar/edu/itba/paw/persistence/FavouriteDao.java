package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.ItemSearchResult;
import java.util.Collection;
import java.util.Set;

public interface FavouriteDao {

    boolean create(int userId, int itemId);

    boolean delete(int userId, int itemId);

    boolean exists(int userId, int itemId);

    Set<Integer> findFavouriteItemIds(int userId, Collection<Integer> itemIds);

    ItemSearchResult listFavourites(FavouritesQueryModel query);

    int countFavourites(FavouritesQueryModel query);
}
