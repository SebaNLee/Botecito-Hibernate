package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import java.time.LocalDate;
import java.time.LocalTime;

public interface MarketplaceService {
    MarketplaceSearchResult searchMarketplace(
            String searchQuery,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer capacity,
            Integer weight,
            Integer difficulty,
            Double minAvgRating,
            String location,
            String itemType,
            Integer page,
            Integer pageSize,
            String sortBy);

    AvailabilityData buildHomeAvailabilityData();
}
