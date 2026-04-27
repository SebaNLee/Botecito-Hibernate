package ar.edu.itba.paw.models;

public class RatingSummary {
    private double averageRating;
    private int totalReviews;

    public RatingSummary() {
        this(0.0, 0);
    }

    public RatingSummary(final double averageRating, final int totalReviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(final double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(final int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public boolean hasReviews() {
        return totalReviews > 0;
    }

    public boolean getHasReviews() {
        return hasReviews();
    }
}
