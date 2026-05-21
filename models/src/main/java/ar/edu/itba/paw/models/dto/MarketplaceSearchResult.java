package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Item;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class MarketplaceSearchResult {
    private final List<Item> items;
    private final long totalCount;
}
