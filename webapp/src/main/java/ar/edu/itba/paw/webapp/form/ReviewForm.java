package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReviewForm {
    @NotNull(message = "{review.validation.rating.required}")
    @Min(value = 1, message = "{review.validation.rating.range}")
    @Max(value = 5, message = "{review.validation.rating.range}")
    private Integer rating;

    @Size(max = 1000, message = "{review.validation.comment.max}")
    private String comment;

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
}
