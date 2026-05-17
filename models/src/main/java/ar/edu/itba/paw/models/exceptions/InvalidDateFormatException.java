package ar.edu.itba.paw.models.exceptions;

public class InvalidDateFormatException extends RuntimeException {

    public InvalidDateFormatException(final String raw) {
        super("Invalid date format: " + raw);
    }
}
