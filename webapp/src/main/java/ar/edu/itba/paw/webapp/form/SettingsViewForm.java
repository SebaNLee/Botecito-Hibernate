package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsViewForm {

    @NotNull
    @Min(value = 1, message = "{settings.validation.page.pattern}")
    @Max(value = 100_000, message = "{settings.validation.page.pattern}")
    private Integer page;

    @NotNull(message = "{settings.validation.pageSize.pattern}")
    private Integer pageSize;

    private Boolean edit;

    @AssertTrue(message = "{settings.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        return pageSize != null && (pageSize == 6 || pageSize == 12 || pageSize == 18);
    }
}
