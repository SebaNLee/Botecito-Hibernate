package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReportsSearchForm {

    @Pattern(regexp = "^$|^(newest|oldest)$", message = "{admin.reports.validation.sort.pattern}")
    private String sortBy;

    @NotNull
    @Min(value = 1, message = "{admin.reports.validation.page.pattern}")
    private Integer page;

    @NotNull(message = "{admin.reports.validation.pageSize.pattern}")
    private Integer pageSize;

    @AssertTrue(message = "{admin.reports.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        return pageSize != null && (pageSize == 6 || pageSize == 12 || pageSize == 18);
    }
}
