package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.nuevo.MarketplaceQueryModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import java.util.List;

public interface MarketplaceDao {
    MarketplaceSearchResult searchMarketplace(MarketplaceQueryModel query);

    List<AvailabilityOrm> getAllAvailabilities();

    List<BookingOrm> getAllBlockingBookings();
}
