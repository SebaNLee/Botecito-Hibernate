package ar.edu.itba.paw.models.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SearchResult<T> {
    private final List<T> pageElements;
    private final long totalCount;
}
