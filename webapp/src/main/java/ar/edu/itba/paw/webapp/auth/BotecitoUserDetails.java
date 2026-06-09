package ar.edu.itba.paw.webapp.auth;

import java.util.Collection;
import java.util.Objects;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Authenticated principal. Spring Security stores the login email as
 * {@link #getUsername()};
 * there is no separate username. {@link #isEnabled()} reflects email
 * verification ({@code Users.verified}).
 */
@Getter
public class BotecitoUserDetails extends User {

    private final int id;
    private final boolean admin;

    public BotecitoUserDetails(
            final String email,
            final String password,
            final boolean verified,
            final Collection<? extends GrantedAuthority> authorities,
            final int id,
            final boolean admin) {
        super(email, password, verified, true, true, true, authorities);
        this.id = id;
        this.admin = admin;
    }

    /** Login identifier (same value Spring exposes as {@link #getUsername()}). */
    public String getEmail() {
        return getUsername();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BotecitoUserDetails that)) {
            return false;
        }
        return id == that.id && super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
