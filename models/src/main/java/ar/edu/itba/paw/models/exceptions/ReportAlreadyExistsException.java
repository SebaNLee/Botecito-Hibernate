package ar.edu.itba.paw.models.exceptions;

public class ReportAlreadyExistsException extends RuntimeException {

    public ReportAlreadyExistsException() {
        super("Report already exists for this item");
    }
}
