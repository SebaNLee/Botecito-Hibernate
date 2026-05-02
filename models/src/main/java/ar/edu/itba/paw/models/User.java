package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Integer id;
    private OffsetDateTime createdAt;
    private String givenName;
    private String lastName;
    private String email;
    private String phone;
    private String paymentAlias;
    private String preferredLanguage;
    private String passwordHash;
    private String passwordRecoveryToken;
    private OffsetDateTime passwordRecoveryUsedAt;

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }

    public String getName() {
        if (isBlank(givenName) && isBlank(lastName)) {
            return "";
        }
        if (isBlank(givenName)) {
            return lastName.trim();
        }
        if (isBlank(lastName)) {
            return givenName.trim();
        }
        return givenName.trim() + " " + lastName.trim();
    }

    public void setName(final String name) {
        if (name == null) {
            this.givenName = null;
            this.lastName = null;
            return;
        }

        final String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            this.givenName = "";
            this.lastName = "";
            return;
        }

        final int separatorIndex = trimmedName.indexOf(' ');
        if (separatorIndex < 0) {
            this.givenName = trimmedName;
            this.lastName = "";
            return;
        }

        this.givenName = trimmedName.substring(0, separatorIndex);
        this.lastName = trimmedName.substring(separatorIndex + 1).trim();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
