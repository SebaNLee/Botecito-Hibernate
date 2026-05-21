package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.MarketplaceSearchResult;
import ar.edu.itba.paw.persistence.MarketplaceDao;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class MarketplaceImpl implements MarketplaceService {

    private final MarketplaceDao marketplaceDao;

    @Override
    @Transactional(readOnly = true)
    public MarketplaceSearchResult searchMarketplace(
            final String searchQuery,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime,
            final Integer capacity,
            final Integer weight,
            final Integer difficulty,
            final Double minAvgRating,
            final String location,
            final String itemType,
            final Integer page,
            final Integer pageSize,
            final String sortBy) {
        final MarketplaceQueryModel query = new MarketplaceQueryModel();
        final DayOfWeek weekday = ServiceUtils.dateToDayOfWeek(date);
        query.setSearchQuery(searchQuery);
        query.setWeekday(weekday);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setCapacity(capacity);
        query.setWeight(weight);
        query.setDifficulty(difficulty);
        query.setMinAvgRating(minAvgRating);
        query.setLocationSlug(location);
        query.setItemTypeSlug(itemType);
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setSortBy(sortBy);
        return marketplaceDao.searchMarketplace(query);
    }
}
