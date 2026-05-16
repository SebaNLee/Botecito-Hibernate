package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import java.util.List;

public interface MarketplaceDao {
    MarketplaceSearchResult searchMarketplace(MarketplaceQueryModel query);

    List<AvailabilityOrm> getAllAvailabilities();

    List<BookingOrm> getAllBlockingBookings();
}
