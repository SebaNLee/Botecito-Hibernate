package ar.edu.itba.paw.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class DisabledTimeSlot {
    private Integer id;
    private Integer itemId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private OffsetDateTime createdAt;

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

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(final LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalTime endTime) {
        this.endTime = endTime;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
