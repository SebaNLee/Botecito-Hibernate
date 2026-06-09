package ar.edu.itba.paw.webapp.util;

/**
 * Upload size limits shared across servlet config, validation, and views.
 * {@link #MAX_FILE_BYTES} and {@link #MAX_REQUEST_BYTES} must stay in sync with
 * {@code web.xml} {@code multipart-config}. The container enforces these while parsing
 * multipart bodies and stops reading once a part or request exceeds the cap.
 */
public final class UploadLimits {

    public static final long MAX_FILE_BYTES = 5_242_880L;
    public static final long MAX_REQUEST_BYTES = 20_971_520L;

    private UploadLimits() {}
}
