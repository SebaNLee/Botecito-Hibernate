package ar.edu.itba.paw.models.dto;

import java.time.LocalTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SelfBlockUpdate {
    private final int bookingId;
    private final LocalTime startTime;
    private final LocalTime endTime;
}
