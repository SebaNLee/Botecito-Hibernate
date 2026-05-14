package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSearchModel {
    private String searchQuery;
    private LocalDate date;
    private BookingStatus status;

    private Integer page;
    private Integer pageSize;
    private String sortBy;
}
