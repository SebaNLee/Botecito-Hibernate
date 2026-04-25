package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

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

    public String getPaymentAlias() {
        return paymentAlias;
    }

    public void setPaymentAlias(final String paymentAlias) {
        this.paymentAlias = paymentAlias;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(final String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordRecoveryToken() {
        return passwordRecoveryToken;
    }

    public void setPasswordRecoveryToken(final String passwordRecoveryToken) {
        this.passwordRecoveryToken = passwordRecoveryToken;
    }

    public OffsetDateTime getPasswordRecoveryUsedAt() {
        return passwordRecoveryUsedAt;
    }

    public void setPasswordRecoveryUsedAt(final OffsetDateTime passwordRecoveryUsedAt) {
        this.passwordRecoveryUsedAt = passwordRecoveryUsedAt;
    }

    public void setPasswordRecoveryUsedAt(final String passwordRecoveryUsedAt) {
        this.passwordRecoveryUsedAt =
                passwordRecoveryUsedAt == null ? null : OffsetDateTime.parse(passwordRecoveryUsedAt);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
