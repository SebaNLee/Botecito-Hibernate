package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewModel {
    private Integer id;
    private Integer bookingId;
    private Integer senderId;
    private ReviewTargetType targetType;
    private Integer targetId;
    private BigDecimal rating;
    private String comment;
    private OffsetDateTime createdAt;
}
