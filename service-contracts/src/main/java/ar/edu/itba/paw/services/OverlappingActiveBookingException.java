package ar.edu.itba.paw.services;

public class OverlappingActiveBookingException extends RuntimeException {

    public OverlappingActiveBookingException() {
        super("Time range overlaps an active booking");
    }
}
