package ar.edu.itba.paw.models.exceptions;

public class PastSlotException extends RuntimeException {

    public PastSlotException() {
        super("The requested slot is in the past");
    }
}
