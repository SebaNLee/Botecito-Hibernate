package ar.edu.itba.paw.models.nuevo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutcomingSearch {
    private int guestId;
    private BookingSearchModel search;
}
