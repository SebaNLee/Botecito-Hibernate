package ar.edu.itba.paw.webapp.util;

/**
 * Upload size limits shared across servlet config, filters, validation, and views.
 * {@link #MAX_FILE_BYTES} and {@link #MAX_REQUEST_BYTES} must stay in sync with
 * {@code web.xml} {@code multipart-config}.
 */
public final class UploadLimits {

    public static final long MAX_FILE_BYTES = 5_242_880L;
    public static final long MAX_REQUEST_BYTES = 20_971_520L;
    public static final long MULTIPART_OVERHEAD_BYTES = 4_096L;

    private UploadLimits() {}

    public static long maxPaymentContentLength() {
        return MAX_FILE_BYTES + MULTIPART_OVERHEAD_BYTES;
    }
}
