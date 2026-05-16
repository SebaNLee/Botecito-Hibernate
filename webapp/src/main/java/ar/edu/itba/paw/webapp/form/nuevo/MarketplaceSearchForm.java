package ar.edu.itba.paw.webapp.form.nuevo;

import java.time.LocalDate;
import java.time.LocalTime;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Query parameters for GET {@code /marketplace}. Field names and types mirror
 * {@link ar.edu.itba.paw.services.nuevo.MarketplaceInterface}.
 */
@Getter
@Setter
public class MarketplaceSearchForm {

    @Size(max = 100, message = "{marketplace.validation.searchQuery.size}")
    private String searchQuery;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(
            iso = ISO.TIME,
            fallbackPatterns = {"H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss"})
    private LocalTime startTime;

    @DateTimeFormat(
            iso = ISO.TIME,
            fallbackPatterns = {"H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss"})
    private LocalTime endTime;

    @Min(value = 1, message = "{marketplace.validation.number.pattern}")
    @Max(value = 20, message = "{marketplace.validation.number.pattern}")
    private Integer capacity;

    @Min(value = 50, message = "{marketplace.validation.number.pattern}")
    @Max(value = 2000, message = "{marketplace.validation.number.pattern}")
    private Integer weight;

    @Min(value = 1, message = "{marketplace.validation.difficultyOrRating.pattern}")
    @Max(value = 5, message = "{marketplace.validation.difficultyOrRating.pattern}")
    private Integer difficulty;

    private Double minAvgRating;

    @Size(max = 100)
    private String location;

    @Size(max = 100)
    private String itemType;

    @Min(value = 1, message = "{marketplace.validation.page.pattern}")
    private Integer page;

    private Integer pageSize;

    @Pattern(regexp = "^$|^(newest|oldest|priceAsc|priceDesc)$", message = "{marketplace.validation.sort.pattern}")
    private String sortBy;

    @AssertTrue(message = "{marketplace.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        if (pageSize == null) {
            return true;
        }
        return pageSize == 6 || pageSize == 12 || pageSize == 18;
    }

    @AssertTrue(message = "{marketplace.validation.timeOrder}")
    public boolean isSearchTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return startTime.isBefore(endTime);
    }

    @AssertTrue(message = "{marketplace.validation.minAvgRating.pattern}")
    public boolean isMinAvgRatingValid() {
        return minAvgRating == null || (minAvgRating >= 0.5 && minAvgRating <= 5 && minAvgRating % 0.5 == 0);
    }
}
