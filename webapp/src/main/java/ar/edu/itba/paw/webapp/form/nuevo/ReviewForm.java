package ar.edu.itba.paw.webapp.form.nuevo;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {
    @NotNull(message = "{review.validation.rating.required}")
    @Min(value = 1, message = "{review.validation.rating.range}")
    @Max(value = 5, message = "{review.validation.rating.range}")
    private Integer rating;

    @Size(max = 1000, message = "{review.validation.comment.max}")
    private String comment;
}
