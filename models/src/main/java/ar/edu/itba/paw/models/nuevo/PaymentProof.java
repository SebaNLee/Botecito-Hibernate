package ar.edu.itba.paw.models.nuevo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentProof {
    private int id;
    private int bookingId;
    private String fileName;
    private String contentType;
    private byte[] fileData;
    private LocalDateTime createdAt;
    private String refuseMsg;
    private LocalDateTime refusedAt;
    private String replyMsg;
    private LocalDateTime repliedAt;
}
