package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.webapp.util.PostLoginRedirectSupport;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class PostLoginRedirectAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public PostLoginRedirectAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication)
            throws IOException, ServletException {
        final HttpSession session = request.getSession(false);
        String target = null;
        if (session != null) {
            final Object raw = session.getAttribute(PostLoginRedirectSupport.SESSION_ATTR);
            if (raw instanceof String candidate && PostLoginRedirectSupport.isSafeInternalRedirect(candidate)) {
                target = candidate.trim();
                session.removeAttribute(PostLoginRedirectSupport.SESSION_ATTR);
            }
        }
        if (target != null) {
            getRedirectStrategy().sendRedirect(request, response, request.getContextPath() + target);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
