package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FavouriteDao {

    boolean create(int userId, int itemId);

    boolean delete(int userId, int itemId);

    boolean exists(int userId, int itemId);

    Set<Integer> findFavouriteItemIds(int userId, Collection<Integer> itemIds);

    List<Item> listFavourites(int userId, int page, int pageSize);

    int countFavourites(int userId);
}
