package ar.edu.itba.paw.services.utils;

import ar.edu.itba.paw.services.MissingUserNamesException;

public final class UserNameRules {

    private UserNameRules() {}

    /**
     * Requires both names to be present with at least one non-whitespace character each. Does not trim or normalize
     * values.
     */
    public static void requireBothLegalNames(final String givenName, final String lastName) {
        if (givenName == null || givenName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new MissingUserNamesException();
        }
    }
}
