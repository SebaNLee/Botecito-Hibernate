package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileViewForm {

    @Pattern(regexp = "^$|^(listings|reviews)$", message = "{profile.validation.tab.pattern}")
    private String tab;

    @Pattern(regexp = "^$|^(newest|oldest|nameAsc|nameDesc)$", message = "{profile.validation.sortBy.pattern}")
    private String sortBy;

    @NotNull
    @Min(value = 1, message = "{profile.validation.page.pattern}")
    @Max(value = 100_000, message = "{profile.validation.page.pattern}")
    private Integer page;

    @NotNull(message = "{profile.validation.pageSize.pattern}")
    private Integer pageSize;

    @AssertTrue(message = "{profile.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        if ("reviews".equalsIgnoreCase(tab)) {
            return true;
        }
        return pageSize != null && (pageSize == 6 || pageSize == 12 || pageSize == 18);
    }
}
