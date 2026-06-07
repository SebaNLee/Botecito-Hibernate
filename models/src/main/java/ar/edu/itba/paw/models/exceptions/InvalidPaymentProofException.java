package ar.edu.itba.paw.models.exceptions;

public class InvalidPaymentProofException extends RuntimeException {

    public InvalidPaymentProofException() {
        super("Payment proof file is missing or invalid");
    }
}
