package ar.edu.itba.paw.webapp.form;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for GET {@code /marketplace}. Field names match request parameter names for
 * Spring MVC binding.
 */
@Getter
@Setter
public class MarketplaceSearchForm {

    private static final java.util.regex.Pattern TIME_TOKEN =
            java.util.regex.Pattern.compile("^\\d{1,2}:\\d{2}(:\\d{2})?$");

    @Size(max = 120, message = "{marketplaceSearch.validation.searchQuery.size}")
    private String searchQuery;

    @Pattern(regexp = "^$|^[0-9]{1,12}$", message = "{marketplaceSearch.validation.locationOptionId.pattern}")
    private String locationOptionId;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "{marketplaceSearch.validation.date.pattern}")
    private String date;

    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}(:\\d{2})?$", message = "{marketplaceSearch.validation.time.pattern}")
    private String startTime;

    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}(:\\d{2})?$", message = "{marketplaceSearch.validation.time.pattern}")
    private String endTime;

    @Pattern(regexp = "^$|^[0-9]{1,9}$", message = "{marketplaceSearch.validation.number.pattern}")
    private String capacity;

    @Pattern(regexp = "^$|^[0-9]{1,9}$", message = "{marketplaceSearch.validation.number.pattern}")
    private String maxWeight;

    @Pattern(regexp = "^$|^[1-5]$", message = "{marketplaceSearch.validation.difficultyOrRating.pattern}")
    private String difficultyLevel;

    @Pattern(regexp = "^$|^[1-5]$", message = "{marketplaceSearch.validation.difficultyOrRating.pattern}")
    private String minRating;

    @Pattern(
            regexp = "^$|^(newest|oldest|priceAsc|priceDesc)$",
            message = "{marketplaceSearch.validation.sort.pattern}")
    private String sort;

    @Pattern(regexp = "^$|^[1-9][0-9]*$", message = "{marketplaceSearch.validation.page.pattern}")
    private String page;

    @Pattern(regexp = "^$|^(6|12|18)$", message = "{marketplaceSearch.validation.pageSize.pattern}")
    private String pageSize;

    @AssertTrue(message = "{marketplaceSearch.validation.timeOrder}")
    public boolean isSearchTimeRangeValid() {
        if (isBlank(startTime) || isBlank(endTime)) {
            return true;
        }
        if (!TIME_TOKEN.matcher(startTime.trim()).matches()
                || !TIME_TOKEN.matcher(endTime.trim()).matches()) {
            return true;
        }
        try {
            final LocalTime start = LocalTime.parse(startTime.trim());
            final LocalTime end = LocalTime.parse(endTime.trim());
            return start.isBefore(end);
        } catch (final DateTimeParseException ignored) {
            return true;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
