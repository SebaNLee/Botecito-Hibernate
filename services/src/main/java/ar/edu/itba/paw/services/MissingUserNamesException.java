package ar.edu.itba.paw.services;

/**
 * Raised when a user cannot be created or updated because given name or last name is null, empty, or whitespace-only.
 * Enforced in the service layer so it cannot be bypassed by omitting or blanking form fields (for example via {@code
 * curl}).
 */
public final class MissingUserNamesException extends RuntimeException {

    public MissingUserNamesException() {
        super("Given name and last name are required and must contain non-whitespace characters.");
    }
}
