package ar.edu.itba.paw.webapp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;

/**
 * Webapp-layer helper that publishes availability payload into the model as
 * JSON-serialized strings consumed by the JSP picker.
 */
public final class AvailabilityPickerSupport {

    private AvailabilityPickerSupport() {}

    public static void addAvailabilityPickerData(
            final ModelAndView mav, final String prefix, final AvailabilityPickerBuilder.Data data) {
        mav.addObject(prefix + "OfferedDatesJson", toJsonArray(data.offeredDates()));
        mav.addObject(prefix + "OccupiedDatesJson", toJsonArray(data.occupiedDates()));
        mav.addObject(prefix + "OfferedTimesJson", toJsonMap(data.offeredTimesByDate()));
        mav.addObject(prefix + "OccupiedTimesJson", toJsonMap(data.occupiedTimesByDate()));
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
