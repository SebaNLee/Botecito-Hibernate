package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public final class PendingReviewActionModel {
    private final int bookingId;
    private final int itemId;
    private final int targetUserId;
    private final TargetType targetType;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String targetName;
    private final String targetEmail;

    public PendingReviewActionModel(
            final int bookingId,
            final int itemId,
            final int targetUserId,
            final TargetType targetType,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
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
