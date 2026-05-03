package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.dto.AvailabilityPickerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;

/**
 * Webapp-layer helper that publishes an {@link AvailabilityPickerData} into the model as
 * JSON-serialized strings consumed by the JSP picker. Pure presentation concern;
 * the slot-conflict computation lives in the services module.
 */
final class AvailabilityPickerSupport {

    private AvailabilityPickerSupport() {}

    static void addAvailabilityPickerData(
            final ModelAndView mav, final String prefix, final AvailabilityPickerData data) {
        mav.addObject(prefix + "OfferedDatesJson", toJsonArray(data.getOfferedDates()));
        mav.addObject(prefix + "OccupiedDatesJson", toJsonArray(data.getOccupiedDates()));
        mav.addObject(prefix + "OfferedTimesJson", toJsonMap(data.getOfferedTimesByDate()));
        mav.addObject(prefix + "OccupiedTimesJson", toJsonMap(data.getOccupiedTimesByDate()));
    }

    private static String toJsonArray(final List<String> values) {
        final StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escapeJson(values.get(index))).append('"');
        }
        return json.append(']').toString();
    }

    private static String toJsonMap(final Map<String, List<String>> values) {
        final List<String> entries = new ArrayList<>();
        for (final Map.Entry<String, List<String>> entry : values.entrySet()) {
            entries.add("\"" + escapeJson(entry.getKey()) + "\":" + toJsonArray(entry.getValue()));
        }
        return "{" + String.join(",", entries) + "}";
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
