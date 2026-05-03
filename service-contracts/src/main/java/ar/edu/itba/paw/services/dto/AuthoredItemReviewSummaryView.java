package ar.edu.itba.paw.services.dto;

public record AuthoredItemReviewSummaryView(int rating, String comment) {

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
