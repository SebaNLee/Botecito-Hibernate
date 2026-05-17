package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import java.util.List;

public interface MarketplaceDao {
    MarketplaceSearchResult searchMarketplace(MarketplaceQueryModel query);

    List<Availability> getAllAvailabilities();

    List<Booking> getAllBlockingBookings();
}
