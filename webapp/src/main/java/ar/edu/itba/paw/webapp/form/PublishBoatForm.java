package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.services.utils.TimeRangeList;
import ar.edu.itba.paw.webapp.form.validation.FileSize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.Email;
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

    @Size(max = 2000, groups = Step1.class, message = "{publish.validation.description.max}")
    private String description;

    @NotBlank(groups = Step1.class, message = "{publish.validation.location.required}")
    @Size(max = 120, groups = Step1.class, message = "{publish.validation.location.max}")
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

    @FileSize(max = 5242880, groups = Step1.class)
    private MultipartFile file;

    private byte[] uploadedImageData = new byte[0];
    private String uploadedImageContentType;

    private final Map<DayOfWeek, TimeRangeList> availabilityByWeekday = new EnumMap<>(DayOfWeek.class);

    @NotBlank(groups = Step3.class, message = "{publish.validation.ownerFirstName.required}")
    @Size(max = 100, groups = Step3.class, message = "{publish.validation.ownerFirstName.max}")
    private String ownerFirstName;

    @NotBlank(groups = Step3.class, message = "{publish.validation.ownerLastName.required}")
    @Size(max = 100, groups = Step3.class, message = "{publish.validation.ownerLastName.max}")
    private String ownerLastName;

    @NotBlank(groups = Step3.class, message = "{publish.validation.ownerEmail.required}")
    @Email(groups = Step3.class, message = "{publish.validation.ownerEmail.invalid}")
    @Size(max = 150, groups = Step3.class, message = "{publish.validation.ownerEmail.max}")
    private String ownerEmail;

    private String ownerPreferredLanguage = "es";

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

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    public byte[] getUploadedImageData() {
        return Arrays.copyOf(uploadedImageData, uploadedImageData.length);
    }

    public void setUploadedImageData(final byte[] uploadedImageData) {
        this.uploadedImageData =
                uploadedImageData == null ? new byte[0] : Arrays.copyOf(uploadedImageData, uploadedImageData.length);
    }

    public String getUploadedImageContentType() {
        return uploadedImageContentType;
    }

    public void setUploadedImageContentType(final String uploadedImageContentType) {
        this.uploadedImageContentType = uploadedImageContentType;
    }

    public boolean hasUploadedImage() {
        return uploadedImageData.length > 0;
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

    // Spring binds map properties through this getter, so it must return the backing map.
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

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public void setOwnerFirstName(final String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public void setOwnerLastName(final String ownerLastName) {
        this.ownerLastName = ownerLastName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(final String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerPreferredLanguage() {
        return ownerPreferredLanguage;
    }

    public void setOwnerPreferredLanguage(final String ownerPreferredLanguage) {
        this.ownerPreferredLanguage = ownerPreferredLanguage;
    }
}
