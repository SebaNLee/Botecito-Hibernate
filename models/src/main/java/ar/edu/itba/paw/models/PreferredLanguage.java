package ar.edu.itba.paw.models;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Values stored in {@code users.preferred_language} ({@code es} or {@code en}).
 */
@Getter
@RequiredArgsConstructor
public enum PreferredLanguage {
    ES("es"),
    EN("en");

    private final String persistenceCode;

    /**
     * Normalizes request or form input; unknown values default to Spanish (same as legacy services).
     */
    public static PreferredLanguage fromInput(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return EN;
        }
        return ES;
    }

    /**
     * Reads {@code users.preferred_language}. Defensive default for null, blank, or unexpected values.
     */
    public static PreferredLanguage fromPersistence(final String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.isBlank()) {
            return ES;
        }
        final String trimmed = preferredLanguage.trim();
        if ("en".equalsIgnoreCase(trimmed)) {
            return EN;
        }
        return ES;
    }

    public Locale toLocale() {
        return this == EN ? Locale.ENGLISH : Locale.of("es");
    }
}
