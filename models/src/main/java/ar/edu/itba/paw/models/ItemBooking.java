package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

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

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getGuestId() {
        return guestId;
    }

    public void setGuestId(final Integer guestId) {
        this.guestId = guestId;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime == null ? null : OffsetDateTime.parse(startTime);
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime == null ? null : OffsetDateTime.parse(endTime);
    }

    public BookingState getState() {
        return state;
    }

    public void setState(final BookingState state) {
        this.state = state;
    }

    public void setState(final String state) {
        this.state = state == null ? null : BookingState.valueOf(state);
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(final String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public String getHostDecisionToken() {
        return hostDecisionToken;
    }

    public void setHostDecisionToken(final String hostDecisionToken) {
        this.hostDecisionToken = hostDecisionToken;
    }

    public OffsetDateTime getHostDecisionUsedAt() {
        return hostDecisionUsedAt;
    }

    public void setHostDecisionUsedAt(final OffsetDateTime hostDecisionUsedAt) {
        this.hostDecisionUsedAt = hostDecisionUsedAt;
    }

    public void setHostDecisionUsedAt(final String hostDecisionUsedAt) {
        this.hostDecisionUsedAt = hostDecisionUsedAt == null ? null : OffsetDateTime.parse(hostDecisionUsedAt);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt == null ? null : OffsetDateTime.parse(updatedAt);
    }
}
