package ar.edu.itba.paw.services;

public class SelfBookingNotAllowedException extends RuntimeException {

    public SelfBookingNotAllowedException() {
        super("Cannot book your own publication");
    }
}
