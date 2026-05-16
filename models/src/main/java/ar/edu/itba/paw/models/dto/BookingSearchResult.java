package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.BookingOrm;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BookingSearchResult {
    private final List<BookingOrm> bookings;
    private final long totalCount;
}
