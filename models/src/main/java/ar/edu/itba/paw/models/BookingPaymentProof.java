package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingPaymentProof {
    private Integer id;
    private Integer bookingId;
    private Integer uploaderId;
    private String fileName;
    private String contentType;
    private byte[] fileData;
    private OffsetDateTime createdAt;
    private String refusalReason;
    private OffsetDateTime refusedAt;
    private String guestReply;

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }
}
