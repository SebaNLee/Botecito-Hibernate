package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;

public interface MarketplaceDao {
    SearchResult<ar.edu.itba.paw.models.entity.Item> searchMarketplace(MarketplaceQueryModel query);
}
