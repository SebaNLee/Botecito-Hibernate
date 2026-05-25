package ar.edu.itba.paw.webapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextRefresher {

    private final UserAccountDetailsService userAccountDetailsService;

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
