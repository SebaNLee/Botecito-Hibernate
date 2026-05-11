package ar.edu.itba.paw.models.nuevo;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreferredLanguageModel {
    ES("es"),
    EN("en");

    private final String persistenceCode;

    public static PreferredLanguageModel fromInput(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return EN;
        }
        return ES;
    }

    public static PreferredLanguageModel fromPersistence(final String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.isBlank()) {
            return ES;
        }
        if ("en".equalsIgnoreCase(preferredLanguage.trim())) {
            return EN;
        }
        return ES;
    }

    public Locale toLocale() {
        return this == EN ? Locale.ENGLISH : Locale.of("es");
    }
}
