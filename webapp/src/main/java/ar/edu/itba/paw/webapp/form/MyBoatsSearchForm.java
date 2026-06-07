package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyBoatsSearchForm {

    @Size(max = 100, message = "{myBoats.search.validation.searchQuery.size}")
    private String searchQuery;

    @Pattern(regexp = "^$|^(ACTIVE|INACTIVE)$", message = "{myBoats.search.validation.status.pattern}")
    private String status;

    @Pattern(regexp = "^$|^(newest|oldest|nameAsc|nameDesc)$", message = "{myBoats.search.validation.sort.pattern}")
    private String sortBy;

    @Min(value = 1, message = "{myBoats.search.validation.page.pattern}")
    private Integer page;

    private Integer pageSize;

    @AssertTrue(message = "{myBoats.search.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        if (pageSize == null) {
            return true;
        }
        return pageSize == 6 || pageSize == 12 || pageSize == 18;
    }
}
