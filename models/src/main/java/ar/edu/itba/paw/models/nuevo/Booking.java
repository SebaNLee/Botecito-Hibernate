package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Booking {
    private int id;
    private int versionId;
    private String versionTitle;
    private int guestId;
    private String hostName;
    private String alias;
    private String timezone;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatus status;
    private String msg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** From {@code payment_proof} when present (host refusal). */
    private String proofRefuseMsg;

    private LocalDateTime proofRefusedAt;
    /** From {@code payment_proof} when present (guest clarification on resubmit). */
    private String proofReplyMsg;

    private LocalDateTime proofRepliedAt;
}
