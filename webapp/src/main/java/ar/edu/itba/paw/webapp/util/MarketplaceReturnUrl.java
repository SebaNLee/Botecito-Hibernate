package ar.edu.itba.paw.webapp.util;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * Same-origin safe marketplace return URLs for item detail navigation (back link, JS fallbacks).
 */
public final class MarketplaceReturnUrl {

    private static final Pattern ALLOWED_MARKETPLACE_PATH = Pattern.compile("^/marketplace(\\?[^#]*)?$");

    private MarketplaceReturnUrl() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Full href for "back to marketplace" including context path. Only allows {@code /marketplace}
     * with an optional query string (no open redirects).
     */
    public static String marketplaceBackHref(final HttpServletRequest request) {
        return marketplaceBackHref(request, request.getParameter("returnTo"));
    }

    public static String marketplaceBackHref(final HttpServletRequest request, final String returnToParam) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return contextPath + relativeReturnTo(returnToParam);
    }

    /** Sanitized {@code /marketplace} path with optional query (no context path). */
    public static String relativeReturnTo(final String returnToParam) {
        return sanitizeRelativePath(returnToParam);
    }

    private static String sanitizeRelativePath(final String returnToParam) {
        if (returnToParam == null || returnToParam.isBlank()) {
            return "/marketplace";
        }
        final String trimmed = returnToParam.trim();
        if (trimmed.length() > 4096) {
            return "/marketplace";
        }
        if (!ALLOWED_MARKETPLACE_PATH.matcher(trimmed).matches()) {
            return "/marketplace";
        }
        if (containsDisallowedCharacters(trimmed)) {
            return "/marketplace";
        }
        return trimmed;
    }

    private static boolean containsDisallowedCharacters(final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c < 0x20 && c != '\t') {
                return true;
            }
            if (c == '<' || c == '>' || c == '"' || c == '\'' || c == '\\') {
                return true;
            }
        }
        return false;
    }
}
