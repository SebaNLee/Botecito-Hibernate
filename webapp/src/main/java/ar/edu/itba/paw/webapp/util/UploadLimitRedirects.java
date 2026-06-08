package ar.edu.itba.paw.webapp.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

public final class UploadLimitRedirects {

    public static final String TOAST_ERROR_PARAM = "toastError";

    private static final Pattern EDIT_IMAGES_URI = Pattern.compile("/edit/(\\d+)/images");

    private UploadLimitRedirects() {}

    public record Decision(String targetPath, String messageCode) {}

    public static Optional<Decision> earlyRejectDecision(final HttpServletRequest request, final long contentLength) {
        if (contentLength < 0) {
            return Optional.empty();
        }
        final String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return Optional.empty();
        }
        if (requestUri.contains("/payment") && contentLength > UploadLimits.maxPaymentContentLength()) {
            return Optional.of(new Decision(
                    refererPath(request).orElse("/requests/outgoing"), "paymentProof.validation.file.size"));
        }
        if (requestUri.contains("/publish/images") && contentLength > UploadLimits.MAX_REQUEST_BYTES) {
            return Optional.of(new Decision("/publish/images", "publish.validation.images.size"));
        }
        final Matcher editImages = EDIT_IMAGES_URI.matcher(requestUri);
        if (editImages.find() && contentLength > UploadLimits.MAX_REQUEST_BYTES) {
            return Optional.of(
                    new Decision("/edit/" + editImages.group(1) + "/images", "publish.validation.images.size"));
        }
        return Optional.empty();
    }

    public static Optional<Decision> exceededUploadDecision(final HttpServletRequest request) {
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri == null) {
            return Optional.empty();
        }
        if (requestUri.contains("/publish/images")) {
            return Optional.of(new Decision("/publish/images", "publish.validation.images.size"));
        }
        final Matcher editImages = EDIT_IMAGES_URI.matcher(requestUri);
        if (editImages.find()) {
            return Optional.of(
                    new Decision("/edit/" + editImages.group(1) + "/images", "publish.validation.images.size"));
        }
        if (requestUri.contains("/payment")) {
            return Optional.of(new Decision(
                    refererPath(request).orElse("/requests/outgoing"), "paymentProof.validation.file.size"));
        }
        return Optional.empty();
    }

    public static String appendToastError(final String targetPath, final String messageCode) {
        final String separator = targetPath.contains("?") ? "&" : "?";
        return targetPath
                + separator
                + TOAST_ERROR_PARAM
                + "="
                + URLEncoder.encode(messageCode, StandardCharsets.UTF_8);
    }

    public static void sendRedirectWithToast(
            final HttpServletRequest request, final HttpServletResponse response, final Decision decision)
            throws IOException {
        final String location =
                request.getContextPath() + appendToastError(decision.targetPath(), decision.messageCode());
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", location);
    }

    public static ModelAndView redirectWithToast(final Decision decision) {
        return postRedirect(appendToastError(decision.targetPath(), decision.messageCode()));
    }

    public static ModelAndView redirectToReferer(final HttpServletRequest request) {
        return postRedirect(refererPath(request).orElse("/"));
    }

    public static ModelAndView redirectToRefererWithToast(final HttpServletRequest request, final String messageCode) {
        final String targetPath = refererPath(request).orElse("/");
        return postRedirect(appendToastError(targetPath, messageCode));
    }

    public static Optional<String> refererPath(final HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        final String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return Optional.empty();
        }
        try {
            final URI uri = URI.create(referer);
            final String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return Optional.empty();
            }
            final String contextPath = request.getContextPath();
            final String relativePath = contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)
                    ? path.substring(contextPath.length())
                    : path;
            final String query = uri.getRawQuery();
            final String target = query == null || query.isBlank() ? relativePath : relativePath + "?" + query;
            return Optional.of(target);
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static ModelAndView postRedirect(final String target) {
        final RedirectView redirectView = new RedirectView(target);
        redirectView.setContextRelative(true);
        redirectView.setStatusCode(HttpStatus.SEE_OTHER);
        return new ModelAndView(redirectView);
    }
}
