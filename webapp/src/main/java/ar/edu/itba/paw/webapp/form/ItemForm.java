package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.*;

public class ItemForm {
    @NotNull
    @Min(1)
    private Integer typeId;

    @NotNull
    @Size(min = 1, max = 100)
    private String title;

    private String description;

    @NotNull
    @Min(0)
    private Integer pricePerHour;

    @NotNull
    @Min(1)
    private Integer capacityPeople;

    @Min(0)
    private Double maxWeightKg;

    @Min(1)
    @Max(5)
    private Integer difficultyLevel;

    @NotNull
    @Size(min = 1, max = 120)
    private String location;

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(Integer pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Integer getCapacityPeople() {
        return capacityPeople;
    }

    public void setCapacityPeople(Integer capacityPeople) {
        this.capacityPeople = capacityPeople;
    }

    public Double getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(Double maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
