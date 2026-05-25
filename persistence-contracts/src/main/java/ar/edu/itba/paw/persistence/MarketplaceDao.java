package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;

public interface MarketplaceDao {
    ItemSearchResult searchMarketplace(MarketplaceQueryModel query);
}
