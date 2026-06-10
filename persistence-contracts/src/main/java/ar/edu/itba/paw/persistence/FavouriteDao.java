package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;

public interface FavouriteDao {

    boolean create(int userId, int itemId);

    boolean delete(int userId, int itemId);

    boolean exists(int userId, int itemId);

    PageModel<Item> listFavourites(FavouritesQueryModel query);

    long countFavourites(FavouritesQueryModel query);
}
