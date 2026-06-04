package ar.edu.itba.paw.webapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@RequiredArgsConstructor
public final class CookieClearingRememberMeServices implements RememberMeServices, LogoutHandler {

    private final TokenBasedRememberMeServices delegate;
    private final String parameter;

    @Override
    public Authentication autoLogin(final HttpServletRequest request, final HttpServletResponse response) {
        return delegate.autoLogin(request, response);
    }

    @Override
    public void loginFail(final HttpServletRequest request, final HttpServletResponse response) {
        delegate.loginFail(request, response);
    }

    @Override
    public void loginSuccess(
            final HttpServletRequest request, final HttpServletResponse response, final Authentication auth) {
        if (rememberMeRequested(request)) {
            delegate.loginSuccess(request, response, auth);
        } else {
            delegate.logout(request, response, auth);
        }
    }

    @Override
    public void logout(
            final HttpServletRequest request, final HttpServletResponse response, final Authentication auth) {
        delegate.logout(request, response, auth);
    }

    private boolean rememberMeRequested(final HttpServletRequest request) {
        final String value = request.getParameter(parameter);
        return value != null
                && (value.equalsIgnoreCase("true")
                        || value.equalsIgnoreCase("on")
                        || value.equalsIgnoreCase("yes")
                        || value.equals("1"));
    }
}
