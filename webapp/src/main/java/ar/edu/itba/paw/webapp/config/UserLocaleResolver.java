package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.AuthenticatedUserResolver;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.LocaleResolver;

@RequiredArgsConstructor
public class UserLocaleResolver implements LocaleResolver {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Override
    public Locale resolveLocale(final HttpServletRequest request) {
        final Optional<BotecitoUserDetails> principal = authenticatedUserResolver.currentPrincipal();
        if (principal.isEmpty()) {
            return request.getLocale();
        }
        final Users user = authenticatedUserResolver.loadUser(principal.get());
        return user != null
                ? PreferredLanguageModel.fromPersistence(user.getLanguage()).toLocale()
                : request.getLocale();
    }

    @Override
    public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {
        // Locale is owned by the user's preference in the database
    }
}
