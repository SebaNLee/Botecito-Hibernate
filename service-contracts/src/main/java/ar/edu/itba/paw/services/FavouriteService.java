package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;

public interface FavouriteService {

    boolean addFavourite(int userId, int itemId);

    boolean removeFavourite(int userId, int itemId);

    boolean isFavourite(int userId, int itemId);

    PageModel<Item> listFavourites(int userId, String searchQuery, int page, int pageSize, String sortBy);
}
