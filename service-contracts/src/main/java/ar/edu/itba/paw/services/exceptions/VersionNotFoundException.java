package ar.edu.itba.paw.services.exceptions;

public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException(final int itemId) {
        super("No version found for item " + itemId);
    }
}
