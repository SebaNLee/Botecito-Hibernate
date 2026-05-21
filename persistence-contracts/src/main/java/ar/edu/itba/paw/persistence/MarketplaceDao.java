package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;

public interface MarketplaceDao {
    MarketplaceSearchResult searchMarketplace(MarketplaceQueryModel query);
}
