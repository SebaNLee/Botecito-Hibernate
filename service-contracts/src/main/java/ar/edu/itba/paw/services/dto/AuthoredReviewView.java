package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ReviewTargetType;

public record AuthoredReviewView(
        int reviewId,
        int bookingId,
        ReviewTargetType targetType,
        String contextTitle,
        String revieweeName,
        String revieweeEmail,
        int rating,
        String comment,
        String createdAtLabel) {

    public int getReviewId() {
        return reviewId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public ReviewTargetType getTargetType() {
        return targetType;
    }

    public String getContextTitle() {
        return contextTitle;
    }

    public String getRevieweeName() {
        return revieweeName;
    }

    public String getRevieweeEmail() {
        return revieweeEmail;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedAtLabel() {
        return createdAtLabel;
    }
}
