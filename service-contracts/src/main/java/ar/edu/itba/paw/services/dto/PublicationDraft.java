package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ItemAvailability;
import java.math.BigDecimal;
import java.util.List;

public final class PublicationDraft {
    private final String ownerGivenName;
    private final String ownerLastName;
    private final String ownerEmail;
    private final String ownerPreferredLanguage;
    private final Integer typeId;
    private final String title;
    private final String description;
    private final Integer pricePerHour;
    private final Integer capacityPeople;
    private final BigDecimal maxWeightKg;
    private final Integer difficultyLevel;
    private final Integer locationOptionId;
    private final List<ItemAvailability> availabilities;
    private final List<GalleryImageUpload> images;

    public PublicationDraft(
            final String ownerGivenName,
            final String ownerLastName,
            final String ownerEmail,
            final String ownerPreferredLanguage,
            final Integer typeId,
            final String title,
            final String description,
            final Integer pricePerHour,
            final Integer capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final Integer locationOptionId,
            final List<ItemAvailability> availabilities,
            final List<GalleryImageUpload> images) {
        this.ownerGivenName = ownerGivenName;
        this.ownerLastName = ownerLastName;
        this.ownerEmail = ownerEmail;
        this.ownerPreferredLanguage = ownerPreferredLanguage;
        this.typeId = typeId;
        this.title = title;
        this.description = description;
        this.pricePerHour = pricePerHour;
        this.capacityPeople = capacityPeople;
        this.maxWeightKg = maxWeightKg;
        this.difficultyLevel = difficultyLevel;
        this.locationOptionId = locationOptionId;
        this.availabilities = availabilities == null ? List.of() : List.copyOf(availabilities);
        this.images = images == null ? List.of() : List.copyOf(images);
    }

    public String getOwnerGivenName() {
        return ownerGivenName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerPreferredLanguage() {
        return ownerPreferredLanguage;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPricePerHour() {
        return pricePerHour;
    }

    public Integer getCapacityPeople() {
        return capacityPeople;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public Integer getLocationOptionId() {
        return locationOptionId;
    }

    public List<ItemAvailability> getAvailabilities() {
        return availabilities;
    }

    public List<GalleryImageUpload> getImages() {
        return images;
    }
}
