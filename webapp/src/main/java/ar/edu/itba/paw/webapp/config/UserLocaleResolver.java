package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.services.UserService;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

@RequiredArgsConstructor
public class UserLocaleResolver implements LocaleResolver {

    private final UserService userService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        final UsersOrm user = currentAuthenticatedUser();
        return user != null
                ? PreferredLanguageModel.fromPersistence(user.getLanguage()).toLocale()
                : request.getLocale();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // Locale is owned by the user's preference in the database
    }

    private UsersOrm currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
