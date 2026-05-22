package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class BookingQueryModel {
    private int callerId;
    private boolean asHost;
    private String searchQuery;
    private LocalDate date;
    private BookingStatusEnum status;
    private Integer page;
    private Integer pageSize;
    private String sortBy;
}
