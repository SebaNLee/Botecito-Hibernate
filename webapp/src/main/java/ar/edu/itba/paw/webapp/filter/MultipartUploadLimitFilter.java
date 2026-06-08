package ar.edu.itba.paw.webapp.filter;

import ar.edu.itba.paw.webapp.util.UploadLimitRedirects;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects oversized multipart uploads from {@code Content-Length} before the servlet parses the
 * body, then redirects back to the originating page with a {@code toastError} query parameter.
 */
public class MultipartUploadLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        if (!isMultipartPost(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final var decision = UploadLimitRedirects.earlyRejectDecision(request, request.getContentLengthLong());
        if (decision.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        UploadLimitRedirects.sendRedirectWithToast(request, response, decision.get());
    }

    private static boolean isMultipartPost(final HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        final String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }
}
