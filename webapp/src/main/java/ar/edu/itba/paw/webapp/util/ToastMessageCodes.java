package ar.edu.itba.paw.webapp.util;

import java.util.Set;

/** Whitelist of message codes that may be passed via the {@code toastError} query parameter. */
public final class ToastMessageCodes {

    public static final Set<String> ALLOWED =
            Set.of("publish.validation.images.size", "paymentProof.validation.file.size");

    private ToastMessageCodes() {}

    public static boolean isAllowed(final String code) {
        return code != null && ALLOWED.contains(code);
    }
}
