package ar.edu.itba.paw.models.nuevo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncomingSearch {
    private int hostId;
    private BookingSearchModel search;
}
