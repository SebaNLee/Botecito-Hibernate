package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockSlotForm {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String date;

    @NotBlank
    @Pattern(regexp = "\\d{2}:\\d{2}")
    private String startTime;

    @NotBlank
    @Pattern(regexp = "\\d{2}:\\d{2}")
    private String endTime;
}
