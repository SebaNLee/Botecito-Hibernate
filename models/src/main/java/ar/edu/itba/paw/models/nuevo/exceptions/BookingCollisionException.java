package ar.edu.itba.paw.models.nuevo.exceptions;

public class BookingCollisionException extends RuntimeException {

    public BookingCollisionException() {
        super("The requested interval collides with an existing booking");
    }
}
