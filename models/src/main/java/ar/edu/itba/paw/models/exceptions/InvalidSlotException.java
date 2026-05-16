package ar.edu.itba.paw.models.exceptions;

public class InvalidSlotException extends RuntimeException {

    public InvalidSlotException() {
        super("The requested slot is invalid");
    }
}
