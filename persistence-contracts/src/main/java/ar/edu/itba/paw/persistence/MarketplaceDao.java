package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;

public interface MarketplaceDao {
    PageModel<Item> searchMarketplace(MarketplaceQueryModel query);
}
