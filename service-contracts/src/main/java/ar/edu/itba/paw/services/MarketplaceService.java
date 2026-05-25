package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import java.time.LocalDate;
import java.time.LocalTime;

public interface MarketplaceService {
    /**
     * Searches marketplace listings. {@code date} may be provided without {@code startTime} and
     * {@code endTime}; in that case results are filtered by weekday only. When both times are
     * present, availability must cover the requested interval on that weekday.
     */
    ItemSearchResult searchMarketplace(
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
}
