package ar.edu.itba.paw.services;

/** One time row for the owner availability grid (JSP / JSON). */
public final class ManageAvailabilitySlotRow {

    private final String startTime;
    private final String endTime;
    private final String state;
    private final Integer blockBookingId;
    private final String modalIdSuffix;

    public ManageAvailabilitySlotRow(
            final String startTime,
            final String endTime,
            final String state,
            final Integer blockBookingId,
            final String modalIdSuffix) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = state;
        this.blockBookingId = blockBookingId;
        this.modalIdSuffix = modalIdSuffix;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getState() {
        return state;
    }

    public Integer getBlockBookingId() {
        return blockBookingId;
    }

    public String getModalIdSuffix() {
        return modalIdSuffix;
    }
}
