package ar.edu.itba.paw.models.nuevo;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class MarketplaceSearchResult {
    private final List<ItemModel> items;
    private final long totalCount;
}
