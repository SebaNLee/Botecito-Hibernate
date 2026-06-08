package ar.edu.itba.paw.webapp.form;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ManageAvailabilityQueryForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}
