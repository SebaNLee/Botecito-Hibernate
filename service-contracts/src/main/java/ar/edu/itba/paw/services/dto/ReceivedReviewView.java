package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ReviewTargetType;

public record ReceivedReviewView(
        ReviewTargetType targetType,
        String contextTitle,
        String reviewerName,
        String reviewerEmail,
        int rating,
        String comment,
        String createdAtLabel) {

    public ReviewTargetType getTargetType() {
        return targetType;
    }

    public String getContextTitle() {
        return contextTitle;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
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
