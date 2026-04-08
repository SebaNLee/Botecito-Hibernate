package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private boolean mondayEnabled;
    private boolean tuesdayEnabled;
    private boolean wednesdayEnabled;
    private boolean thursdayEnabled;
    private boolean fridayEnabled;
    private boolean saturdayEnabled;
    private boolean sundayEnabled;

    private List<AvailabilitySlotForm> availabilitySlots = new ArrayList<>();

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

    public boolean isMondayEnabled() {
        return mondayEnabled;
    }

    public void setMondayEnabled(final boolean mondayEnabled) {
        this.mondayEnabled = mondayEnabled;
    }

    public boolean isTuesdayEnabled() {
        return tuesdayEnabled;
    }

    public void setTuesdayEnabled(final boolean tuesdayEnabled) {
        this.tuesdayEnabled = tuesdayEnabled;
    }

    public boolean isWednesdayEnabled() {
        return wednesdayEnabled;
    }

    public void setWednesdayEnabled(final boolean wednesdayEnabled) {
        this.wednesdayEnabled = wednesdayEnabled;
    }

    public boolean isThursdayEnabled() {
        return thursdayEnabled;
    }

    public void setThursdayEnabled(final boolean thursdayEnabled) {
        this.thursdayEnabled = thursdayEnabled;
    }

    public boolean isFridayEnabled() {
        return fridayEnabled;
    }

    public void setFridayEnabled(final boolean fridayEnabled) {
        this.fridayEnabled = fridayEnabled;
    }

    public boolean isSaturdayEnabled() {
        return saturdayEnabled;
    }

    public void setSaturdayEnabled(final boolean saturdayEnabled) {
        this.saturdayEnabled = saturdayEnabled;
    }

    public boolean isSundayEnabled() {
        return sundayEnabled;
    }

    public void setSundayEnabled(final boolean sundayEnabled) {
        this.sundayEnabled = sundayEnabled;
    }

    // Spring binds indexed properties through this getter, so it must return the backing list.
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Required for Spring indexed property binding")
    public List<AvailabilitySlotForm> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(final List<AvailabilitySlotForm> availabilitySlots) {
        this.availabilitySlots = availabilitySlots == null ? new ArrayList<>() : new ArrayList<>(availabilitySlots);
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
