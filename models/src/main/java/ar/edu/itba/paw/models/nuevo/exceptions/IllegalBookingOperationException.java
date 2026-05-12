package ar.edu.itba.paw.models.nuevo.exceptions;

public class IllegalBookingOperationException extends RuntimeException {

    public IllegalBookingOperationException() {
        super("This user cannot perform this operation on this booking");
    }
}
