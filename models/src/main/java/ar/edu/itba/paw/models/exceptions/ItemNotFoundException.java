package ar.edu.itba.paw.models.exceptions;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException() {
        super("Item not found");
    }
}
