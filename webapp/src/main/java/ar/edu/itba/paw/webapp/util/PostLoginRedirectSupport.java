package ar.edu.itba.paw.webapp.util;

/**
 * Validates in-app redirect targets after login and stores the chosen URL in the HTTP session under
 * {@link #SESSION_ATTR}.
 */
public final class PostLoginRedirectSupport {

    public static final String SESSION_ATTR = "POST_LOGIN_REDIRECT";

    private PostLoginRedirectSupport() {}

    public static boolean isSafeInternalRedirect(final String path) {
        if (path == null) {
            return false;
        }
        final String p = path.trim();
        if (p.isEmpty() || !p.startsWith("/")) {
            return false;
        }
        if (p.startsWith("//")) {
            return false;
        }
        if (p.contains("://") || p.contains("\\")) {
            return false;
        }
        if (p.contains("\r") || p.contains("\n")) {
            return false;
        }
        return true;
    }
}
