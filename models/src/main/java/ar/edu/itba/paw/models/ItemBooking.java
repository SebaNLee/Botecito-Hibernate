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

    public void setStartTime(final OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime == null ? null : OffsetDateTime.parse(startTime);
    }

    public void setEndTime(final OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime == null ? null : OffsetDateTime.parse(endTime);
    }

    public void setState(final BookingState state) {
        this.state = state;
    }

    public void setState(final String state) {
        this.state = state == null ? null : BookingState.valueOf(state);
    }

    public void setHostDecisionUsedAt(final OffsetDateTime hostDecisionUsedAt) {
        this.hostDecisionUsedAt = hostDecisionUsedAt;
    }

    public void setHostDecisionUsedAt(final String hostDecisionUsedAt) {
        this.hostDecisionUsedAt = hostDecisionUsedAt == null ? null : OffsetDateTime.parse(hostDecisionUsedAt);
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt == null ? null : OffsetDateTime.parse(updatedAt);
    }
}
