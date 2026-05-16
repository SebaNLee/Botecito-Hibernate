package ar.edu.itba.paw.webapp.form;

import java.time.LocalDate;
import java.time.LocalTime;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class BlockSlotForm {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.TIME,
            fallbackPatterns = {"H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss"})
    private LocalTime startTime;

    @NotNull
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.TIME,
            fallbackPatterns = {"H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss"})
    private LocalTime endTime;

    @AssertTrue(message = "{blockSlot.validation.timeOrder}")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return startTime.isBefore(endTime);
    }
}
