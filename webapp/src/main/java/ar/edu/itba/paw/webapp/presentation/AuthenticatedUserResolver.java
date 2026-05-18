package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.auth.UserAccountDetailsService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserService userService;
    private final UserAccountDetailsService userAccountDetailsService;

    public Optional<BotecitoUserDetails> currentPrincipal() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        final Object principal = authentication.getPrincipal();
        if (principal instanceof BotecitoUserDetails details) {
            return Optional.of(details);
        }
        if (principal instanceof String email) {
            final BotecitoUserDetails reloaded = userAccountDetailsService.loadUserByEmail(email);
            refreshPrincipal(email);
            return Optional.of(reloaded);
        }
        return Optional.empty();
    }

    public Users loadUser(final BotecitoUserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userService.findById(principal.getId()).orElse(null);
    }

    public void refreshPrincipal(final String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final BotecitoUserDetails userDetails = userAccountDetailsService.loadUserByEmail(email.trim());
        final UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        if (authentication != null) {
            refreshed.setDetails(authentication.getDetails());
        }
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }
}
