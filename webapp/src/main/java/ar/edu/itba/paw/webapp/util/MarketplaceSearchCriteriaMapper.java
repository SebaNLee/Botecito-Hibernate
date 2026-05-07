package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSearchSort;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Maps marketplace HTTP search parameters (bound on {@link MarketplaceSearchForm}) into {@link ItemSearchCriteria}
 * for {@link ar.edu.itba.paw.services.ItemService#searchMarketplace}.
 */
public final class MarketplaceSearchCriteriaMapper {

    private MarketplaceSearchCriteriaMapper() {}

    public static ItemSearchCriteria fromMarketplaceForm(final MarketplaceSearchForm form) {
        final String searchQuery = form == null ? null : form.getSearchQuery();
        final String locationOptionId = form == null ? null : form.getLocationOptionId();
        final String date = form == null ? null : form.getDate();
        final String startTime = form == null ? null : form.getStartTime();
        final String endTime = form == null ? null : form.getEndTime();
        final String capacity = form == null ? null : form.getCapacity();
        final String maxWeight = form == null ? null : form.getMaxWeight();
        final String difficulty = form == null ? null : form.getDifficultyLevel();
        final String minRating = form == null ? null : form.getMinRating();
        final String sort = form == null ? null : form.getSort();

        final ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setSearchQuery(searchQuery);
        criteria.setLocationOptionId(parseInt(locationOptionId));
        criteria.setDate(parseLocalDate(date));
        criteria.setStartTime(parseLocalTime(startTime));
        criteria.setEndTime(parseLocalTime(endTime));
        criteria.setCapacity(parseInt(capacity));
        final Integer maxWeightInt = parseInt(maxWeight);
        criteria.setMaxWeightKg(maxWeightInt == null ? null : BigDecimal.valueOf(maxWeightInt.longValue()));
        criteria.setDifficultyLevel(parseRanged(difficulty, 1, 5));
        criteria.setMinAverageRating(parseRanged(minRating, 1, 5));
        criteria.setSort(ItemSearchSort.fromRequestParam(sort));
        return criteria;
    }

    private static Integer parseInt(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseRanged(final String value, final int min, final int max) {
        final Integer parsed = parseInt(value);
        if (parsed == null || parsed < min || parsed > max) {
            return null;
        }
        return parsed;
    }

    private static LocalDate parseLocalDate(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (final DateTimeParseException ex) {
            return null;
        }
    }

    private static LocalTime parseLocalTime(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (final DateTimeParseException ex) {
            return null;
        }
    }
}
