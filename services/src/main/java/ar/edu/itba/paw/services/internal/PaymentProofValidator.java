package ar.edu.itba.paw.services.internal;

import java.util.Locale;
import java.util.Set;

public final class PaymentProofValidator {
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5_242_880L;

    private PaymentProofValidator() {}

    public static boolean isValid(final String fileName, final String contentType, final byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_FILE_SIZE) {
            return false;
        }
        if (contentType == null) {
            return false;
        }
        final String lower = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(lower)) {
            return false;
        }
        return matchesMagicBytes(lower, data);
    }

    private static boolean matchesMagicBytes(final String contentType, final byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        switch (contentType) {
            case "image/jpeg":
                return (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
            case "image/png":
                return data.length >= 8
                        && (data[0] & 0xFF) == 0x89
                        && data[1] == 'P'
                        && data[2] == 'N'
                        && data[3] == 'G'
                        && (data[4] & 0xFF) == 0x0D
                        && (data[5] & 0xFF) == 0x0A
                        && (data[6] & 0xFF) == 0x1A
                        && (data[7] & 0xFF) == 0x0A;
            case "image/webp":
                return data.length >= 12
                        && data[0] == 'R'
                        && data[1] == 'I'
                        && data[2] == 'F'
                        && data[3] == 'F'
                        && data[8] == 'W'
                        && data[9] == 'E'
                        && data[10] == 'B'
                        && data[11] == 'P';
            case "application/pdf":
                return data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
            default:
                return false;
        }
    }
}
