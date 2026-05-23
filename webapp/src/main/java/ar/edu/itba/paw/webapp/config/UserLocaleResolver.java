package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

@RequiredArgsConstructor
public class UserLocaleResolver implements LocaleResolver {

    private final UserService userService;

    @Override
    public Locale resolveLocale(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return request.getLocale();
        }
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof BotecitoUserDetails details) || session == null) {
            return request.getLocale();
        }

        Locale locale = (Locale) session.getAttribute("userLocale");
        if (locale == null) {
            final Users user = userService.findById(details.getId()).orElseThrow();
            locale = PreferredLanguageModel.fromPersistence(user.getLanguage()).toLocale();
            session.setAttribute("userLocale", locale);
        }

        return locale;
    }

    @Override
    public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {
        // Locale is owned by the user's preference in the database
    }
}
