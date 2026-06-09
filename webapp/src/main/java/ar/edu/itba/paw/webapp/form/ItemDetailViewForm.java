package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemDetailViewForm {

    @NotNull
    @Min(value = 1, message = "{itemDetail.validation.page.pattern}")
    @Max(value = 100_000, message = "{itemDetail.validation.page.pattern}")
    private Integer page;

    private Integer itemId;
}
