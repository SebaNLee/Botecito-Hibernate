package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

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

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(final Integer bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getReviewerUserId() {
        return reviewerUserId;
    }

    public void setReviewerUserId(final Integer reviewerUserId) {
        this.reviewerUserId = reviewerUserId;
    }

    public Integer getRevieweeUserId() {
        return revieweeUserId;
    }

    public void setRevieweeUserId(final Integer revieweeUserId) {
        this.revieweeUserId = revieweeUserId;
    }

    public ReviewTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(final ReviewTargetType targetType) {
        this.targetType = targetType;
    }

    public void setTargetType(final String targetType) {
        this.targetType = targetType == null ? null : ReviewTargetType.valueOf(targetType);
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(final Integer targetId) {
        this.targetId = targetId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(final Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt == null ? null : OffsetDateTime.parse(updatedAt);
    }
}
