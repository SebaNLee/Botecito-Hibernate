package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.MarketplaceSearchModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;

public interface MarketplaceInterface {
    MarketplaceSearchResult searchMarketplace(MarketplaceSearchModel search);
}
