package ar.edu.itba.paw.models.exceptions;

public class SelfBlockCollisionException extends RuntimeException {

    public SelfBlockCollisionException() {
        super("The requested self-block overlaps with an existing booking or block");
    }
}
