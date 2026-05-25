package ar.edu.itba.paw.webapp.form;

import java.time.LocalDate;
import java.time.LocalTime;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PreBookingForm {

    @NotNull(message = "{prebooking.validation.versionId.required}")
    private Integer versionId;

    @NotNull(message = "{prebooking.validation.date.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull(message = "{prebooking.validation.startTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "{prebooking.validation.endTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @Size(max = 255, message = "{prebooking.validation.message.max}")
    private String message;

    @AssertTrue
    public boolean hasTwoHourGap() {
        return !endTime.isBefore(startTime.plusHours(2));
    }

    private boolean isOnHalfHour(LocalTime time) {
        var minutes = time.getMinute();
        return minutes == 0 || minutes == 30;
    }

    @AssertTrue
    public boolean landsOnHalfHour() {
        return isOnHalfHour(startTime) && isOnHalfHour(endTime);
    }
}
