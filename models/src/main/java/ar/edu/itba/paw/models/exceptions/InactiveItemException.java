package ar.edu.itba.paw.models.exceptions;

public class InactiveItemException extends RuntimeException {

    public InactiveItemException() {
        super("The publication must be active to perform this operation");
    }
}
