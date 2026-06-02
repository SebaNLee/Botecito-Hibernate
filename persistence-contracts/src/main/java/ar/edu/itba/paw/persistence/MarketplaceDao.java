package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;

public interface MarketplaceDao {
    SearchResult<Item> searchMarketplace(MarketplaceQueryModel query);
}
