package ar.edu.itba.paw.webapp.form.nuevo;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketplaceSearchForm {
    @Size(max = 100, message = "{marketplaceSearch.validation.searchQuery.size}")
    private String searchQuery;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "{marketplaceSearch.validation.date.pattern}")
    private String date;

    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}(:\\d{2})?$", message = "{marketplaceSearch.validation.time.pattern}")
    private String startTime;

    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}(:\\d{2})?$", message = "{marketplaceSearch.validation.time.pattern}")
    private String endTime;

    @Min(value = 1, message = "{marketplaceSearch.validation.capacity.min}")
    @Max(value = 20, message = "{marketplaceSearch.validation.capacity.max}")
    private Integer capacity;

    @Min(value = 50, message = "{marketplaceSearch.validation.weight.min}")
    @Max(value = 2000, message = "{marketplaceSearch.validation.weight.max}")
    private Integer weight;

    @Min(value = 1, message = "{marketplaceSearch.validation.difficulty.min}")
    @Max(value = 5, message = "{marketplaceSearch.validation.difficulty.max}")
    private Integer difficultyLevel;

    @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "{marketplaceSearch.validation.locationSlug.pattern}")
    private String locationSlug;

    @Pattern(
            regexp = "^$|^(newest|oldest|price_asc|price_desc)$",
            message = "{marketplaceSearch.validation.sort.pattern}")
    private String sortBy;

    @NotNull
    @Min(value = 1, message = "{marketplaceSearch.validation.page.pattern}")
    private Integer page;

    @NotNull(message = "{marketplaceSearch.validation.pageSize.pattern}")
    private Integer pageSize;

    @AssertTrue(message = "{marketplaceSearch.validation.pageSize.pattern}")
    public boolean isPageSizeValid() {
        return pageSize != null && (pageSize == 6 || pageSize == 12 || pageSize == 18);
    }
}
