package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
}
