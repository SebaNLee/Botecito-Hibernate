package ar.edu.itba.paw.webapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Logs in a user after email verification (no password submitted), mirroring the success sequence of
 * {@link org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter}.
 */
@Component
@RequiredArgsConstructor
public class PostRegistrationAuthenticator {

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy =
            new ChangeSessionIdAuthenticationStrategy();

    private final UserAccountDetailsService userAccountDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void authenticateVerifiedUser(
            final String email, final HttpServletRequest request, final HttpServletResponse response) {
        final BotecitoUserDetails userDetails = userAccountDetailsService.loadUserByEmail(email);
        final UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        final SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authentication, getClass()));
    }
}
