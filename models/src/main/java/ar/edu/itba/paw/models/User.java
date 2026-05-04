package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"passwordHash", "passwordRecoveryToken"})
public class User {
    private Integer id;

    private OffsetDateTime createdAt;

    private String givenName;
    private String lastName;

    private String email;

    private String phone;
    private String paymentAlias;
    private PreferredLanguage preferredLanguage = PreferredLanguage.ES;
    private String passwordHash;
    private String passwordRecoveryToken;
    private OffsetDateTime passwordRecoveryUsedAt;

    /**
     * Display label joining {@link #givenName} and {@link #lastName} (for mail, UI, etc.). Not a form input: the web
     * layer binds those fields separately. Never {@code null}; treats {@code null} or empty string as absent (no
     * trimming).
     */
    public String getName() {
        final boolean noGiven = givenName == null || givenName.isEmpty();
        final boolean noLast = lastName == null || lastName.isEmpty();
        if (noGiven && noLast) {
            return "";
        }
        if (noGiven) {
            return lastName;
        }
        if (noLast) {
            return givenName;
        }
        return givenName + " " + lastName;
    }
}
