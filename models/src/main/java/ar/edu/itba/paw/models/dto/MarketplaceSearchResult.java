package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class MarketplaceSearchResult {
    private final List<Version> items;
    private final long totalCount;
}
