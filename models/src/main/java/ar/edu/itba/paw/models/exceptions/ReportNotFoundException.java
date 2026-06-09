package ar.edu.itba.paw.models.exceptions;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException() {
        super("Report not found");
    }
}
