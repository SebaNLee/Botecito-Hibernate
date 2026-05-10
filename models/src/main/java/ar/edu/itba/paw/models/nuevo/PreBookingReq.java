package ar.edu.itba.paw.models.nuevo;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreBookingReq {
    private int versionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String message;
    private int guestId;
}
