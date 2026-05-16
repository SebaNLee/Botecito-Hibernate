package ar.edu.itba.paw.models.nuevo.exceptions;

public class InvalidDateFormatException extends RuntimeException {

    public InvalidDateFormatException(final String raw) {
        super("Invalid date format: " + raw);
    }
}
