package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ReviewTargetType;

public record PendingReviewView(
        int bookingId,
        int itemId,
        String itemTitle,
        ReviewTargetType targetType,
        String targetName,
        String targetEmail,
        String dateLabel,
        String timeRangeLabel) {

    public int getBookingId() {
        return bookingId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public ReviewTargetType getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getTimeRangeLabel() {
        return timeRangeLabel;
    }
}
