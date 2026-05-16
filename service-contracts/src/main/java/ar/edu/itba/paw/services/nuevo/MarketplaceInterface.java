package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.AvailabilityData;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import java.time.LocalDate;
import java.time.LocalTime;

public interface MarketplaceInterface {
    MarketplaceSearchResult searchMarketplace(
            String searchQuery, LocalDate date, LocalTime startTime, LocalTime endTime,
            Integer capacity, Integer weight, Integer difficulty, Double minAvgRating,
            String location, String itemType, Integer page, Integer pageSize, String sortBy);

    AvailabilityData buildHomeAvailabilityData();
}
