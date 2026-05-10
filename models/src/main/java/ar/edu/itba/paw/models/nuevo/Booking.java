package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Booking {
    private int id;
    private int versionId;
    private int guestId;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatus status;
    private String msg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
