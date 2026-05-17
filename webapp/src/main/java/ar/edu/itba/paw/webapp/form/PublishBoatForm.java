package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.ImageGalleryUpload;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PublishBoatForm {

    public interface Step1 {}

    public interface Step3 {}

    @NotBlank(groups = Step1.class, message = "{publish.validation.title.required}")
    @Size(max = 100, groups = Step1.class, message = "{publish.validation.title.max}")
    private String title;

    @Size(max = 1000, groups = Step1.class, message = "{publish.validation.description.max}")
    private String description;

    @NotBlank(groups = Step1.class, message = "{publish.validation.location.required}")
    @Pattern(regexp = "\\d+", groups = Step1.class, message = "{publish.validation.location.invalid}")
    private String locationOptionId;

    @NotBlank(groups = Step1.class, message = "{publish.validation.capacity.required}")
    @Pattern(regexp = "^([1-9]|1[0-9]|20)$", groups = Step1.class, message = "{publish.validation.capacity.invalid}")
    private String capacity;

    @NotBlank(groups = Step1.class, message = "{publish.validation.itemType.required}")
    private String itemTypeId;

    @NotBlank(groups = Step1.class, message = "{publish.validation.price.required}")
    @Pattern(regexp = "^[1-9]\\d*$", groups = Step1.class, message = "{publish.validation.price.positive}")
    private String pricePerHour;

    @NotNull(groups = Step1.class, message = "{publish.validation.difficulty.required}")
    @Min(value = 1, groups = Step1.class, message = "{publish.validation.difficulty.min}")
    @Max(value = 5, groups = Step1.class, message = "{publish.validation.difficulty.max}")
    private Integer difficultyLevel;

    @Pattern(regexp = "^$|\\d+(\\.\\d{1,2})?", groups = Step1.class, message = "{publish.validation.maxWeight.numeric}")
    private String maxWeight;

    @ImageGalleryUpload(groups = Step1.class)
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring multipart binding")
    private List<MultipartFile> files = new ArrayList<>();

    private final List<UploadedImage> uploadedImages = new ArrayList<>();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Required for Spring indexed property binding")
    private final Map<DayOfWeek, List<TimeSlot>> availabilityByWeekday = new EnumMap<>(DayOfWeek.class);

    public void setFiles(final List<MultipartFile> files) {
        this.files = files == null ? new ArrayList<>() : files;
    }

    public List<UploadedImage> getUploadedImages() {
        return Collections.unmodifiableList(uploadedImages);
    }

    public int getUploadedImageCount() {
        return uploadedImages.size();
    }

    public boolean hasUploadedImages() {
        return !uploadedImages.isEmpty();
    }

    public void appendUploadedImage(final byte[] data, final String contentType) {
        if (data == null || data.length == 0) {
            return;
        }
        uploadedImages.add(new UploadedImage(data, contentType));
    }

    public void removeUploadedImageAt(final int index) {
        if (index < 0 || index >= uploadedImages.size()) {
            return;
        }
        uploadedImages.remove(index);
    }

    public void reorderUploadedImages(final List<Integer> newOrder) {
        if (newOrder == null || newOrder.size() != uploadedImages.size()) {
            return;
        }
        final List<UploadedImage> reordered = new ArrayList<>(uploadedImages.size());
        for (final Integer originalIndex : newOrder) {
            if (originalIndex == null || originalIndex < 0 || originalIndex >= uploadedImages.size()) {
                return;
            }
            reordered.add(uploadedImages.get(originalIndex));
        }
        uploadedImages.clear();
        uploadedImages.addAll(reordered);
    }

    public UploadedImage getUploadedImageAt(final int index) {
        if (index < 0 || index >= uploadedImages.size()) {
            return null;
        }
        return uploadedImages.get(index);
    }

    public List<byte[]> orderedImageBytes() {
        final List<byte[]> result = new ArrayList<>(uploadedImages.size());
        for (final UploadedImage image : uploadedImages) {
            result.add(image.getData());
        }
        return result;
    }

    public boolean isDayEnabled(final DayOfWeek weekday) {
        return availabilityByWeekday.containsKey(Objects.requireNonNull(weekday));
    }

    public void setDayEnabled(final DayOfWeek weekday, final boolean enabled) {
        final DayOfWeek safeWeekday = Objects.requireNonNull(weekday);
        if (enabled) {
            availabilityByWeekday.computeIfAbsent(safeWeekday, ignored -> new ArrayList<>());
            return;
        }
        availabilityByWeekday.remove(safeWeekday);
    }

    public List<TimeSlot> getAvailabilityFor(final DayOfWeek weekday) {
        return availabilityByWeekday.get(Objects.requireNonNull(weekday));
    }

    public void setAvailabilityFor(final DayOfWeek weekday, final List<TimeSlot> ranges) {
        final DayOfWeek safeWeekday = Objects.requireNonNull(weekday);
        if (ranges == null) {
            availabilityByWeekday.remove(safeWeekday);
            return;
        }

        availabilityByWeekday.put(safeWeekday, new ArrayList<>(ranges));
    }

    public void setAvailabilityByWeekday(final Map<DayOfWeek, List<TimeSlot>> availabilityByWeekday) {
        this.availabilityByWeekday.clear();
        if (availabilityByWeekday == null) {
            return;
        }

        for (final Map.Entry<DayOfWeek, List<TimeSlot>> dayEntry : availabilityByWeekday.entrySet()) {
            setAvailabilityFor(dayEntry.getKey(), dayEntry.getValue());
        }
    }

    public static final class TimeSlot {
        private final LocalTime start;
        private final LocalTime end;

        private TimeSlot(final LocalTime start, final LocalTime end) {
            if (start == null || end == null) {
                throw new IllegalArgumentException("start and end times must not be null");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("end time must be after start time");
            }
            this.start = start;
            this.end = end;
        }

        public static TimeSlot of(final LocalTime start, final LocalTime end) {
            return new TimeSlot(start, end);
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }
    }

    @Getter
    public static final class UploadedImage implements Serializable {
        private static final long serialVersionUID = 1L;

        private final byte[] data;
        private final String contentType;

        public UploadedImage(final byte[] data, final String contentType) {
            this.data = data == null ? new byte[0] : Arrays.copyOf(data, data.length);
            this.contentType = contentType;
        }

        public byte[] getData() {
            return Arrays.copyOf(data, data.length);
        }

        public int getSize() {
            return data.length;
        }
    }
}
