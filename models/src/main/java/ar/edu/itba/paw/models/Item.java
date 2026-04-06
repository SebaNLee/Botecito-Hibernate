package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Item {
    private Integer id;
    private Integer ownerId;
    private Integer typeId;
    private String title;
    private String description;
    private Integer pricePerHour;
    private Integer capacityPeople;
    private BigDecimal maxWeightKg;
    private Integer difficultyLevel;
    private String location;
    private Boolean active;
    private OffsetDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Integer ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }

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

    public Integer getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(final Integer pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Integer getCapacityPeople() {
        return capacityPeople;
    }

    public void setCapacityPeople(final Integer capacityPeople) {
        this.capacityPeople = capacityPeople;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(final BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(final Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }
}
