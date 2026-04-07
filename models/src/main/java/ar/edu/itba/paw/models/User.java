package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public class User {
    private Integer id;
    private OffsetDateTime createdAt;
    private String givenName;
    private String lastName;
    private String email;
    private String phone;
    private String preferredLanguage;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(final String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
