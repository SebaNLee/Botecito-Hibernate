package ar.edu.itba.paw.models.nuevo;

import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public final class PendingReviewActionModel {
    private final int bookingId;
    private final int itemId;
    private final int targetUserId;
    private final ReviewTargetType targetType;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String targetName;
    private final String targetEmail;

    public PendingReviewActionModel(
            final int bookingId,
            final int itemId,
            final int targetUserId,
            final ReviewTargetType targetType,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String targetName,
            final String targetEmail) {
        this.bookingId = bookingId;
        this.itemId = itemId;
        this.targetUserId = targetUserId;
        this.targetType = targetType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.targetName = targetName;
        this.targetEmail = targetEmail;
    }
}
