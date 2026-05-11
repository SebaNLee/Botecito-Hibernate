package ar.edu.itba.paw.models.nuevo.mail;

import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingMailModel {

    private String token;
    private Integer itemId;
    private String requesterName;
    private String requesterEmail;
    private String description;
    private BookingStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
}
