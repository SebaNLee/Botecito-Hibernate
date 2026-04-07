package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
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

    private boolean mondayEnabled;
    private String mondayStartTime = "09:00";
    private String mondayEndTime = "18:00";

    private boolean tuesdayEnabled;
    private String tuesdayStartTime = "09:00";
    private String tuesdayEndTime = "18:00";

    private boolean wednesdayEnabled;
    private String wednesdayStartTime = "09:00";
    private String wednesdayEndTime = "18:00";

    private boolean thursdayEnabled;
    private String thursdayStartTime = "09:00";
    private String thursdayEndTime = "18:00";

    private boolean fridayEnabled;
    private String fridayStartTime = "09:00";
    private String fridayEndTime = "18:00";

    private boolean saturdayEnabled;
    private String saturdayStartTime = "09:00";
    private String saturdayEndTime = "18:00";

    private boolean sundayEnabled;
    private String sundayStartTime = "09:00";
    private String sundayEndTime = "18:00";

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

    public boolean isMondayEnabled() {
        return mondayEnabled;
    }

    public void setMondayEnabled(final boolean mondayEnabled) {
        this.mondayEnabled = mondayEnabled;
    }

    public String getMondayStartTime() {
        return mondayStartTime;
    }

    public void setMondayStartTime(final String mondayStartTime) {
        this.mondayStartTime = mondayStartTime;
    }

    public String getMondayEndTime() {
        return mondayEndTime;
    }

    public void setMondayEndTime(final String mondayEndTime) {
        this.mondayEndTime = mondayEndTime;
    }

    public boolean isTuesdayEnabled() {
        return tuesdayEnabled;
    }

    public void setTuesdayEnabled(final boolean tuesdayEnabled) {
        this.tuesdayEnabled = tuesdayEnabled;
    }

    public String getTuesdayStartTime() {
        return tuesdayStartTime;
    }

    public void setTuesdayStartTime(final String tuesdayStartTime) {
        this.tuesdayStartTime = tuesdayStartTime;
    }

    public String getTuesdayEndTime() {
        return tuesdayEndTime;
    }

    public void setTuesdayEndTime(final String tuesdayEndTime) {
        this.tuesdayEndTime = tuesdayEndTime;
    }

    public boolean isWednesdayEnabled() {
        return wednesdayEnabled;
    }

    public void setWednesdayEnabled(final boolean wednesdayEnabled) {
        this.wednesdayEnabled = wednesdayEnabled;
    }

    public String getWednesdayStartTime() {
        return wednesdayStartTime;
    }

    public void setWednesdayStartTime(final String wednesdayStartTime) {
        this.wednesdayStartTime = wednesdayStartTime;
    }

    public String getWednesdayEndTime() {
        return wednesdayEndTime;
    }

    public void setWednesdayEndTime(final String wednesdayEndTime) {
        this.wednesdayEndTime = wednesdayEndTime;
    }

    public boolean isThursdayEnabled() {
        return thursdayEnabled;
    }

    public void setThursdayEnabled(final boolean thursdayEnabled) {
        this.thursdayEnabled = thursdayEnabled;
    }

    public String getThursdayStartTime() {
        return thursdayStartTime;
    }

    public void setThursdayStartTime(final String thursdayStartTime) {
        this.thursdayStartTime = thursdayStartTime;
    }

    public String getThursdayEndTime() {
        return thursdayEndTime;
    }

    public void setThursdayEndTime(final String thursdayEndTime) {
        this.thursdayEndTime = thursdayEndTime;
    }

    public boolean isFridayEnabled() {
        return fridayEnabled;
    }

    public void setFridayEnabled(final boolean fridayEnabled) {
        this.fridayEnabled = fridayEnabled;
    }

    public String getFridayStartTime() {
        return fridayStartTime;
    }

    public void setFridayStartTime(final String fridayStartTime) {
        this.fridayStartTime = fridayStartTime;
    }

    public String getFridayEndTime() {
        return fridayEndTime;
    }

    public void setFridayEndTime(final String fridayEndTime) {
        this.fridayEndTime = fridayEndTime;
    }

    public boolean isSaturdayEnabled() {
        return saturdayEnabled;
    }

    public void setSaturdayEnabled(final boolean saturdayEnabled) {
        this.saturdayEnabled = saturdayEnabled;
    }

    public String getSaturdayStartTime() {
        return saturdayStartTime;
    }

    public void setSaturdayStartTime(final String saturdayStartTime) {
        this.saturdayStartTime = saturdayStartTime;
    }

    public String getSaturdayEndTime() {
        return saturdayEndTime;
    }

    public void setSaturdayEndTime(final String saturdayEndTime) {
        this.saturdayEndTime = saturdayEndTime;
    }

    public boolean isSundayEnabled() {
        return sundayEnabled;
    }

    public void setSundayEnabled(final boolean sundayEnabled) {
        this.sundayEnabled = sundayEnabled;
    }

    public String getSundayStartTime() {
        return sundayStartTime;
    }

    public void setSundayStartTime(final String sundayStartTime) {
        this.sundayStartTime = sundayStartTime;
    }

    public String getSundayEndTime() {
        return sundayEndTime;
    }

    public void setSundayEndTime(final String sundayEndTime) {
        this.sundayEndTime = sundayEndTime;
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
