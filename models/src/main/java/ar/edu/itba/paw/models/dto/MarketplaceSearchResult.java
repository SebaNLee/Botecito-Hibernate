package ar.edu.itba.paw.models.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class MarketplaceSearchResult {
    private final List<MarketplaceCardItem> items;
    private final long totalCount;
}
