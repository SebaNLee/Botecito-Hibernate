package ar.edu.itba.paw.persistence.orm.projections;

import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewRowOrm {

    private final Integer id;
    private final Integer bookingId;
    private final Integer senderId;
    private final TargetEnumOrm targetType;
    private final Integer targetId;
    private final BigDecimal rating;
    private final String comment;
    private final LocalDateTime createdAt;
}
