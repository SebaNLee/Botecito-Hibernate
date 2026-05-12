package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PublishContent {
    private final int typeId;
    private final String title;
    private final String description;
    private final int pricePerHour;
    private final int capacityPeople;
    private final BigDecimal maxWeightKg;
    private final Integer difficultyLevel;
    private final int locationOptionId;
    private final List<AvailabilityWindow> availabilities;
    private final List<ImageUpload> images;
}
