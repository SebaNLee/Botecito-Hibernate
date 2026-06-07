package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileReviewsViewForm {

    @NotNull
    @Min(value = 1, message = "{profile.validation.page.pattern}")
    @Max(value = 100_000, message = "{profile.validation.page.pattern}")
    private Integer page;
}
