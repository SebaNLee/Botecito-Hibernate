package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSearchForm {

    @Size(max = 100, message = "{bookingSearch.validation.searchQuery.size}")
    private String searchQuery;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "{bookingSearch.validation.date.pattern}")
    private String date;

    @Pattern(
            regexp = "^$|^(PENDING|ACCEPTED|REJECTED|PAID|CONFIRMED|REFUSED|CANCELLED|FINISHED)$",
            message = "{bookingSearch.validation.status.pattern}")
    private String status;

    @NotBlank(message = "{bookingSearch.validation.sort.pattern}")
    @Pattern(regexp = "^(newest|oldest|start_asc|start_desc)$", message = "{bookingSearch.validation.sort.pattern}")
    private String sortBy;

    @NotNull
    @Min(value = 1, message = "{bookingSearch.validation.page.pattern}")
    private Integer page;

    @NotNull(message = "{bookingSearch.validation.pageSize.pattern}")
    private Integer pageSize;

    @AssertTrue(message = "{bookingSearch.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        return pageSize != null && (pageSize == 6 || pageSize == 12 || pageSize == 18);
    }
}
