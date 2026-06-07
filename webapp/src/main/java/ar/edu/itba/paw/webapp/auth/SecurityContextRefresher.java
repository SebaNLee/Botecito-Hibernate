package ar.edu.itba.paw.webapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Reloads the authenticated principal from the database (used after the user changes their email,
 * which is the login identifier) and persists the refreshed context through the shared
 * {@link SecurityContextRepository} so it survives the response under the explicit-save model.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRefresher {

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    private final UserAccountDetailsService userAccountDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public void refreshPrincipal(
            final String email, final HttpServletRequest request, final HttpServletResponse response) {
        if (email == null || email.isBlank()) {
            return;
        }
        final Authentication current =
                securityContextHolderStrategy.getContext().getAuthentication();
        final BotecitoUserDetails userDetails = userAccountDetailsService.loadUserByEmail(email.trim());
        final UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        if (current != null) {
            refreshed.setDetails(current.getDetails());
        }
        final SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(refreshed);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
