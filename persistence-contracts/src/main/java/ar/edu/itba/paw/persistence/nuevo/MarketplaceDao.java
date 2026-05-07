package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.MarketplaceQueryModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;

public interface MarketplaceDao {
    MarketplaceSearchResult searchMarketplace(MarketplaceQueryModel query);
}
