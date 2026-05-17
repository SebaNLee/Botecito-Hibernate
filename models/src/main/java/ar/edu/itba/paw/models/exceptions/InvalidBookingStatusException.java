package ar.edu.itba.paw.models.exceptions;

public class InvalidBookingStatusException extends RuntimeException {

    public InvalidBookingStatusException(final String raw) {
        super("Invalid booking status: " + raw);
    }
}
