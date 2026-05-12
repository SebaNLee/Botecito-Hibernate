package ar.edu.itba.paw.webapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostRegistrationAuthenticator {

    private static final SecurityContextRepository SECURITY_CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public boolean authenticate(
            final String email,
            final String rawPassword,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final UsernamePasswordAuthenticationToken requestToken =
                new UsernamePasswordAuthenticationToken(email, rawPassword);
        requestToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(requestToken);
        } catch (final AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            final HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }
            return false;
        }

        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        SECURITY_CONTEXT_REPOSITORY.saveContext(context, request, response);
        return true;
    }

    public void authenticateVerifiedUser(
            final String email, final HttpServletRequest request, final HttpServletResponse response) {
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        SECURITY_CONTEXT_REPOSITORY.saveContext(context, request, response);
    }
}
