package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.form.validation.ImageGalleryUpload;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multi-step publish wizard form. Step groups drive which constraints run per POST;
 * cross-field rules use {@link AssertTrue} like {@link MarketplaceSearchForm}.
 */
@Getter
@Setter
public class PublishBoatForm {

    public static final int MAX_GALLERY_IMAGES = 3;
    public static final int MAX_PRICE_PER_HOUR = 9_999_999;
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
    @Max(value = MAX_PRICE_PER_HOUR, groups = Step1.class, message = "{publish.validation.price.max}")
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

    public String getTitle() {
        return title == null ? null : title.trim();
    }

    public String getDescription() {
        return description == null ? "" : description.trim();
    }

    public List<AvailabilityWindow> getAvailabilityWindows() {
        final List<AvailabilityWindow> availabilities = new ArrayList<>();
        for (final AvailabilityRangeBinding range : availabilityRanges) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            final AvailabilityWindow window = new AvailabilityWindow();
            window.setWeekday(range.getWeekday());
            window.setStartTime(range.getStartTime());
            window.setEndTime(range.getEndTime());
            availabilities.add(window);
        }
        return availabilities;
    }

    public List<ImageUpload> getPublishImageUploads() {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        final List<ImageUpload> uploads = new ArrayList<>();
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                final byte[] bytes = file.getBytes();
                if (bytes.length > 0) {
                    uploads.add(ImageUpload.ofNew(bytes, file.getContentType()));
                }
            } catch (final IOException ignored) {
                // skip unreadable file
            }
        }
        return uploads;
    }

    public List<ImageUpload> getEditImageUploads() {
        if (galleryOrder == null || galleryOrder.isBlank()) {
            return List.of();
        }
        final List<MultipartFile> uploaded = files == null ? List.of() : files;
        int newFileIndex = 0;
        final List<ImageUpload> images = new ArrayList<>();
        for (final String token : galleryOrder.split(",")) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("e:")) {
                images.add(ImageUpload.ofExisting(Integer.parseInt(trimmed.substring(2))));
                continue;
            }
            if (trimmed.startsWith("n:")) {
                while (newFileIndex < uploaded.size()) {
                    final MultipartFile file = uploaded.get(newFileIndex++);
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    try {
                        final byte[] bytes = file.getBytes();
                        if (bytes.length > 0) {
                            images.add(ImageUpload.ofNew(bytes, file.getContentType()));
                        }
                    } catch (final IOException ignored) {
                        // skip unreadable file
                    }
                    break;
                }
            }
        }
        return images;
    }

    public Map<String, Boolean> getEnabledWeekdaysModel() {
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            model.put(weekday.name(), enabledDays.contains(weekday));
        }
        return model;
    }

    public static boolean hasErrorsOutsideAvailability(final BindingResult errors) {
        return errors.getFieldErrors().stream().anyMatch(error -> !isAvailabilityError(error.getField()));
    }

    public static PublishBoatForm fromVersion(final Version version) {
        final PublishBoatForm form = new PublishBoatForm();
        form.setTitle(version.getTitle());
        form.setDescription(version.getDescription() == null ? "" : version.getDescription());
        form.setItemTypeId(version.getType().getId());
        form.setPricePerHour(version.getPrice().intValue());
        form.setCapacity(version.getCapacity());
        form.setWeight(version.getWeight());
        form.setDifficulty(version.getDifficulty());
        form.setLocationOptionId(version.getLocation().getId());

        final LinkedHashSet<DayOfWeek> enabledDaysSet = new LinkedHashSet<>();
        final List<AvailabilityRangeBinding> ranges = new ArrayList<>();
        if (version.getAvailabilities() != null) {
            for (final Availability availability : version.getAvailabilities()) {
                if (availability.getWeekday() == null
                        || availability.getStartTime() == null
                        || availability.getEndTime() == null) {
                    continue;
                }
                enabledDaysSet.add(DayOfWeek.valueOf(availability.getWeekday().name()));
                final AvailabilityRangeBinding range = new AvailabilityRangeBinding();
                range.setWeekday(DayOfWeek.valueOf(availability.getWeekday().name()));
                range.setStartTime(availability.getStartTime());
                range.setEndTime(availability.getEndTime());
                ranges.add(range);
            }
        }
        form.setEnabledDays(new ArrayList<>(enabledDaysSet));
        form.setAvailabilityRanges(ranges);
        return form;
    }

    private static boolean isAvailabilityError(final String field) {
        if (field == null) {
            return false;
        }
        return field.startsWith("availability") || "availabilityByWeekday".equals(field);
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
