package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import org.springframework.web.servlet.ModelAndView;

public final class AvailabilityJsonHelper {

    private AvailabilityJsonHelper() {}

    public static void addAvailabilityPickerData(
            final ModelAndView mav, final String prefix, final AvailabilityData data) {
        mav.addObject(prefix + "OfferedDatesJson", JsonForHtml.serialize(data.getOfferedDates()));
        mav.addObject(prefix + "OccupiedDatesJson", JsonForHtml.serialize(data.getOccupiedDates()));
        mav.addObject(prefix + "OfferedTimesJson", JsonForHtml.serialize(data.getOfferedTimesByDate()));
        mav.addObject(prefix + "OccupiedTimesJson", JsonForHtml.serialize(data.getOccupiedTimesByDate()));
    }
}
