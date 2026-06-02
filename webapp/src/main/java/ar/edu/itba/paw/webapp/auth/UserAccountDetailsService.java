package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads principals by login email. {@link #loadUserByUsername(String)} is the Spring Security
 * contract; the argument is always the user's email.
 */
@Component
@RequiredArgsConstructor
public class UserAccountDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return loadUserByEmail(username);
    }

    public BotecitoUserDetails loadUserByEmail(final String email) throws UsernameNotFoundException {
        final Users user = userService
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email " + email));
        return fromUser(user);
    }

    public BotecitoUserDetails fromUser(final Users user) throws UsernameNotFoundException {
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("User account is not claimed yet for email " + user.getEmail());
        }
        final boolean isAdmin = Boolean.TRUE.equals(user.getAdmin());
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new BotecitoUserDetails(
                user.getEmail(), user.getPasswordHash(), user.getVerified(), authorities, user.getId(), isAdmin);
    }
}
