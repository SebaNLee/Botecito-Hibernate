package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemBooking {
    private Integer id;
    private Integer itemId;
    private Integer guestId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private BookingState state;
    private String requestMessage;
    private String hostDecisionToken;
    private OffsetDateTime hostDecisionUsedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
