package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewModel {
    private int id;
    private int bookingId;
    private int senderId;
    private TargetType targetType;
    private double rating;
    private String comment;
    private LocalDateTime createdAt;
}
