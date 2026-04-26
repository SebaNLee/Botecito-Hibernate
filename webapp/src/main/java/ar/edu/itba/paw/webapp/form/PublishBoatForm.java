package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.services.utils.TimeRangeList;
import ar.edu.itba.paw.webapp.form.validation.ImageGalleryUpload;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.time.DayOfWeek;
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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

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
    private String marina;

    @NotBlank(groups = Step1.class, message = "{publish.validation.capacity.required}")
    private String capacity;

    @NotBlank(groups = Step1.class, message = "{publish.validation.itemType.required}")
    private String itemTypeId;

    @NotBlank(groups = Step1.class, message = "{publish.validation.price.required}")
    @Pattern(regexp = "^$|\\d+", groups = Step1.class, message = "{publish.validation.price.numeric}")
    private String pricePerHour;

    @Min(value = 1, groups = Step1.class, message = "{publish.validation.difficulty.min}")
    @Max(value = 5, groups = Step1.class, message = "{publish.validation.difficulty.max}")
    private Integer difficultyLevel;

    @Pattern(regexp = "^$|\\d+(\\.\\d{1,2})?", groups = Step1.class, message = "{publish.validation.maxWeight.numeric}")
    private String maxWeight;

    @ImageGalleryUpload(groups = Step1.class)
    private List<MultipartFile> files = new ArrayList<>();

    private final List<UploadedImage> uploadedImages = new ArrayList<>();

    private final Map<DayOfWeek, TimeRangeList> availabilityByWeekday = new EnumMap<>(DayOfWeek.class);

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getMarina() {
        return marina;
    }

    public void setMarina(final String marina) {
        this.marina = marina;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(final String capacity) {
        this.capacity = capacity;
    }

    public String getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(final String itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public String getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(final String pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(final Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(final String maxWeight) {
        this.maxWeight = maxWeight;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring multipart binding")
    public List<MultipartFile> getFiles() {
        return files;
    }

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
            availabilityByWeekday.computeIfAbsent(safeWeekday, ignored -> new TimeRangeList());
            return;
        }
        availabilityByWeekday.remove(safeWeekday);
    }

    public TimeRangeList getAvailabilityFor(final DayOfWeek weekday) {
        return availabilityByWeekday.get(Objects.requireNonNull(weekday));
    }

    public void setAvailabilityFor(final DayOfWeek weekday, final TimeRangeList ranges) {
        final DayOfWeek safeWeekday = Objects.requireNonNull(weekday);
        if (ranges == null) {
            availabilityByWeekday.remove(safeWeekday);
            return;
        }

        final TimeRangeList copy = new TimeRangeList();
        copy.addAll(ranges);
        availabilityByWeekday.put(safeWeekday, copy);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Required for Spring indexed property binding")
    public Map<DayOfWeek, TimeRangeList> getAvailabilityByWeekday() {
        return availabilityByWeekday;
    }

    public void setAvailabilityByWeekday(final Map<DayOfWeek, TimeRangeList> availabilityByWeekday) {
        this.availabilityByWeekday.clear();
        if (availabilityByWeekday == null) {
            return;
        }

        for (final Map.Entry<DayOfWeek, TimeRangeList> dayEntry : availabilityByWeekday.entrySet()) {
            setAvailabilityFor(dayEntry.getKey(), dayEntry.getValue());
        }
    }

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

        public String getContentType() {
            return contentType;
        }
    }
}
