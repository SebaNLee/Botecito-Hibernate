package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.services.UserService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs out authenticated sessions whose principal id no longer exists in the database.
 * Spring Security stores a snapshot of the user at login time and does not re-validate it on
 * every request.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserExistsFilter extends OncePerRequestFilter {

    private static final String LOGIN_REDIRECT = "/login?sessionExpired=true";
    private static final String REMEMBER_ME_COOKIE = "remember-me";

    private final UserService userService;
    private final CompositeLogoutHandler logoutHandler = createLogoutHandler();

    private static CompositeLogoutHandler createLogoutHandler() {
        final SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.setInvalidateHttpSession(true);
        securityContextLogoutHandler.setClearAuthentication(true);
        return new CompositeLogoutHandler(
                securityContextLogoutHandler, new CookieClearingLogoutHandler(REMEMBER_ME_COOKIE));
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof BotecitoUserDetails details
                && userService.findById(details.getId()).isEmpty()) {
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect(request.getContextPath() + LOGIN_REDIRECT);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
