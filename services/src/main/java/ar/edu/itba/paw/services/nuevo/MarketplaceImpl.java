package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.MarketplaceQueryModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import ar.edu.itba.paw.persistence.nuevo.MarketplaceDao;
import java.time.DayOfWeek;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class MarketplaceImpl implements MarketplaceInterface {

    private final MarketplaceDao marketplaceDao;

    @Override
    public MarketplaceSearchResult searchMarketplace(final MarketplaceSearchModel search) {
        return marketplaceDao.searchMarketplace(toQuery(search));
    }

    private static MarketplaceQueryModel toQuery(final MarketplaceSearchModel search) {
        final MarketplaceQueryModel query = new MarketplaceQueryModel();
        final DayOfWeek weekday = ServiceUtils.dateToDayOfWeek(search.getDate());
        query.setSearchQuery(search.getSearchQuery());
        query.setWeekday(weekday);
        query.setStartTime(search.getStartTime());
        query.setEndTime(search.getEndTime());
        query.setCapacity(search.getCapacity());
        query.setWeight(search.getWeight());
        query.setDifficulty(search.getDifficulty());
        query.setMinAvgRating(search.getMinAvgRating());
        query.setLocationSlug(search.getLocation());
        query.setItemTypeSlug(search.getItemType());
        query.setPage(search.getPage());
        query.setPageSize(search.getPageSize());
        query.setSortBy(search.getSortBy());
        return query;
    }
}
