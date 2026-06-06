package ar.edu.itba.paw.webapp.util;

public final class PostLoginRedirectSupport {

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
