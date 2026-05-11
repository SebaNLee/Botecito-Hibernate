package ar.edu.itba.paw.models.nuevo;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BookingSearchResult {
    private final List<Booking> bookings;
    private final long totalCount;
}
