package ar.edu.itba.paw.models.exceptions;

public class SlotOverlapException extends RuntimeException {

    public SlotOverlapException() {
        super("The requested slot overlaps with an existing booking or block");
    }
}
