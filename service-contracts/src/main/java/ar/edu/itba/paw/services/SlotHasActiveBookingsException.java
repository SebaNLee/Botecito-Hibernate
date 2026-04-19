package ar.edu.itba.paw.services;

public class SlotHasActiveBookingsException extends RuntimeException {

    public SlotHasActiveBookingsException(final String message) {
        super(message);
    }
}
