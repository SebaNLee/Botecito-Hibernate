package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.entity.TargetEnum;
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

    @Size(max = 255, message = "{review.validation.comment.max}")
    private String comment;

    private String targetType;

    public String getComment() {
        if (comment == null) {
            return null;
        }
        final String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public TargetEnum getTargetTypeEnum() {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        return TargetEnum.valueOf(targetType.trim());
    }
}
