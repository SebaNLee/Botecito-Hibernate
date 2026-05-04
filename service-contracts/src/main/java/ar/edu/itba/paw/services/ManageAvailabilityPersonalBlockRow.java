package ar.edu.itba.paw.services;

/** Summary row for the owner's self-block list on the manage availability page. */
public final class ManageAvailabilityPersonalBlockRow {

    private final int bookingId;
    private final String dateIso;
    private final String startTime;
    private final String endTime;

    public ManageAvailabilityPersonalBlockRow(
            final int bookingId, final String dateIso, final String startTime, final String endTime) {
        this.bookingId = bookingId;
        this.dateIso = dateIso;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getBookingId() {
        return bookingId;
    }

    public String getDateIso() {
        return dateIso;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}
