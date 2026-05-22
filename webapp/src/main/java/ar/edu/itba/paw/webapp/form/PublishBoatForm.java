package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.ImageGalleryUpload;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multi-step publish wizard form. Step groups drive which constraints run per POST;
 * cross-field rules use {@link AssertTrue} like {@link MarketplaceSearchForm}.
 */
@Getter
@Setter
public class PublishBoatForm {

    public static final int MAX_GALLERY_IMAGES = 3;
    private static final int MIN_RANGE_MINUTES = 120;
    private static final int MIN_SEPARATION_MINUTES = 30;

    public interface Step1 {}

    public interface Step2 {}

    public interface Step3 {}

    public interface Step3Edit {}

    @NotBlank(groups = Step1.class, message = "{publish.validation.title.required}")
    @Size(max = 100, groups = Step1.class, message = "{publish.validation.title.max}")
    private String title;

    @Size(max = 1000, groups = Step1.class, message = "{publish.validation.description.max}")
    private String description;

    @NotNull(groups = Step1.class, message = "{publish.validation.location.required}")
    private Integer locationOptionId;

    @NotNull(groups = Step1.class, message = "{publish.validation.capacity.required}")
    @Min(value = 1, groups = Step1.class, message = "{publish.validation.capacity.invalid}")
    @Max(value = 20, groups = Step1.class, message = "{publish.validation.capacity.invalid}")
    private Integer capacity;

    @NotNull(groups = Step1.class, message = "{publish.validation.itemType.required}")
    private Integer itemTypeId;

    @NotNull(groups = Step1.class, message = "{publish.validation.price.required}")
    @Min(value = 1, groups = Step1.class, message = "{publish.validation.price.positive}")
    private Integer pricePerHour;

    @NotNull(groups = Step1.class, message = "{publish.validation.difficulty.required}")
    @Min(value = 1, groups = Step1.class, message = "{publish.validation.difficulty.min}")
    @Max(value = 5, groups = Step1.class, message = "{publish.validation.difficulty.max}")
    private Integer difficulty;

    @NotNull(groups = Step1.class, message = "{publish.validation.weight.required}")
    @Min(value = 1, groups = Step1.class, message = "{publish.validation.weight.min}")
    private Integer weight;

    @ImageGalleryUpload(
            groups = {Step3.class, Step3Edit.class},
            maxFiles = MAX_GALLERY_IMAGES)
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring multipart binding")
    private List<MultipartFile> files = new ArrayList<>();

    private String galleryOrder;

    private List<DayOfWeek> enabledDays = new ArrayList<>();

    private List<AvailabilityRangeBinding> availabilityRanges = new ArrayList<>();

    public void setFiles(final List<MultipartFile> files) {
        this.files = files == null ? new ArrayList<>() : files;
    }

    public void setEnabledDays(final List<DayOfWeek> enabledDays) {
        this.enabledDays = enabledDays == null ? new ArrayList<>() : enabledDays;
    }

    public void setAvailabilityRanges(final List<AvailabilityRangeBinding> availabilityRanges) {
        this.availabilityRanges = availabilityRanges == null ? new ArrayList<>() : availabilityRanges;
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.required}")
    public boolean isAvailabilityPresent() {
        return !completeRanges().isEmpty();
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.format.invalid}")
    public boolean isAvailabilityFormatValid() {
        if (availabilityRanges == null) {
            return true;
        }
        for (final AvailabilityRangeBinding range : availabilityRanges) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.end.invalid}")
    public boolean isAvailabilityEndAfterStart() {
        for (final AvailabilityRangeBinding range : completeRanges()) {
            if (!range.getEndTime().isAfter(range.getStartTime())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.min.duration}")
    public boolean isAvailabilityMinDuration() {
        for (final AvailabilityRangeBinding range : completeRanges()) {
            if (Duration.between(range.getStartTime(), range.getEndTime()).toMinutes() < MIN_RANGE_MINUTES) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.overlap}")
    public boolean isAvailabilityWithoutOverlap() {
        for (final List<AvailabilityRangeBinding> dayRanges : rangesByWeekday().values()) {
            if (!dayRangesAreNonOverlapping(dayRanges)) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(
            groups = {Step2.class, Step3.class, Step3Edit.class},
            message = "{publish.availability.min.separation}")
    public boolean isAvailabilityMinSeparation() {
        for (final List<AvailabilityRangeBinding> dayRanges : rangesByWeekday().values()) {
            if (!dayRangesHaveMinSeparation(dayRanges)) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(groups = Step3Edit.class, message = "{publish.validation.gallery.maxCount}")
    public boolean isEditGalleryWithinLimit() {
        return countGallerySlots() <= MAX_GALLERY_IMAGES;
    }

    private int countGallerySlots() {
        if (galleryOrder == null || galleryOrder.isBlank()) {
            return 0;
        }
        int count = 0;
        for (final String token : galleryOrder.split(",")) {
            if (!token.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<AvailabilityRangeBinding> completeRanges() {
        if (availabilityRanges == null || availabilityRanges.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityRangeBinding> complete = new ArrayList<>();
        for (final AvailabilityRangeBinding range : availabilityRanges) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            if (enabledDays == null || !enabledDays.contains(range.getWeekday())) {
                continue;
            }
            complete.add(range);
        }
        return complete;
    }

    private Map<DayOfWeek, List<AvailabilityRangeBinding>> rangesByWeekday() {
        final Map<DayOfWeek, List<AvailabilityRangeBinding>> grouped = new EnumMap<>(DayOfWeek.class);
        for (final AvailabilityRangeBinding range : completeRanges()) {
            grouped.computeIfAbsent(range.getWeekday(), ignored -> new ArrayList<>())
                    .add(range);
        }
        return grouped;
    }

    private static boolean dayRangesAreNonOverlapping(final List<AvailabilityRangeBinding> ranges) {
        if (ranges.size() < 2) {
            return true;
        }
        final List<AvailabilityRangeBinding> sorted = ranges.stream()
                .sorted(Comparator.comparing(AvailabilityRangeBinding::getStartTime))
                .toList();
        LocalTime previousEnd = null;
        for (final AvailabilityRangeBinding range : sorted) {
            if (previousEnd != null && range.getStartTime().isBefore(previousEnd)) {
                return false;
            }
            previousEnd = range.getEndTime();
        }
        return true;
    }

    private static boolean dayRangesHaveMinSeparation(final List<AvailabilityRangeBinding> ranges) {
        if (ranges.size() < 2) {
            return true;
        }
        final List<AvailabilityRangeBinding> sorted = ranges.stream()
                .sorted(Comparator.comparing(AvailabilityRangeBinding::getStartTime))
                .toList();
        LocalTime previousEnd = null;
        for (final AvailabilityRangeBinding range : sorted) {
            if (previousEnd != null
                    && Duration.between(previousEnd, range.getStartTime()).toMinutes() < MIN_SEPARATION_MINUTES) {
                return false;
            }
            previousEnd = range.getEndTime();
        }
        return true;
    }

    @Getter
    @Setter
    public static class AvailabilityRangeBinding {
        private DayOfWeek weekday;

        @DateTimeFormat(
                iso = ISO.TIME,
                fallbackPatterns = {"H:mm", "HH:mm"})
        private LocalTime startTime;

        @DateTimeFormat(
                iso = ISO.TIME,
                fallbackPatterns = {"H:mm", "HH:mm"})
        private LocalTime endTime;
    }
}
