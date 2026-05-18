package ar.edu.itba.paw.models.exceptions;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException() {
        super("The caller is not allowed to perform this operation");
    }
}
