package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Review {
    private Integer id;
    private Integer bookingId;
    private Integer reviewerUserId;
    private Integer revieweeUserId;
    private ReviewTargetType targetType;
    private Integer targetId;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void setTargetType(final ReviewTargetType targetType) {
        this.targetType = targetType;
    }

    public void setTargetType(final String targetType) {
        this.targetType = targetType == null ? null : ReviewTargetType.valueOf(targetType);
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt == null ? null : OffsetDateTime.parse(updatedAt);
    }
}
