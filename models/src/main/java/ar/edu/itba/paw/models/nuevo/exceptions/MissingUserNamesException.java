package ar.edu.itba.paw.models.nuevo.exceptions;

public final class MissingUserNamesException extends RuntimeException {

    public MissingUserNamesException() {
        super("Given name and last name are required and must contain non-whitespace characters.");
    }
}
