package ar.edu.itba.paw.models.nuevo.exceptions;

public class NoAnticipationException extends RuntimeException {

    public NoAnticipationException() {
        super("Not enough anticipation to perform this action");
    }
}
