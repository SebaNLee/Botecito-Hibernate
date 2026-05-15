package ar.edu.itba.paw.models.nuevo.exceptions;

public class OutsideAvailabilityException extends RuntimeException {

    public OutsideAvailabilityException() {
        super("The requested interval is not covered by any availability window");
    }
}
