package ar.edu.itba.paw.webapp.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import ar.edu.itba.paw.webapp.auth.AuthenticatedUserExistsFilter;
import ar.edu.itba.paw.webapp.auth.CookieClearingRememberMeServices;
import ar.edu.itba.paw.webapp.auth.UserAccountDetailsService;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    private static final String LOGIN_PATH = "/login";
    private static final String REGISTER_PATH = "/register";
    private static final String LOGOUT_PATH = "/logout";
    private static final String ERRORS_PATH = "/errors";
    private static final String REMEMBER_ME_KEY_PROPERTY = "rememberMe.key";
    private static final String REMEMBER_ME_PARAMETER = "j_rememberme";
    private static final int REMEMBER_ME_VALIDITY_SECONDS = (int) TimeUnit.DAYS.toSeconds(30);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Shared between the form-login filter and the programmatic login performed after email
     * verification, so both persist the {@link org.springframework.security.core.context.SecurityContext}
     * the same way under the explicit-save model.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new HttpSessionSecurityContextRepository(), new RequestAttributeSecurityContextRepository());
    }

    /**
     * Single source of truth for saved bounced requests, shared between the {@code ExceptionTranslationFilter}
     * that stores them and the {@link SavedRequestAwareAuthenticationSuccessHandler} that restores them on login.
     */
    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final UserAccountDetailsService userDetailsAccountService,
            final SecurityContextRepository securityContextRepository,
            final AuthenticatedUserExistsFilter authenticatedUserExistsFilter,
            final RequestCache requestCache,
            @Qualifier("credentialsProperties") final Properties credentialsProperties)
            throws Exception {
        final String rememberMeKey = requireRememberMeKey(credentialsProperties);
        http.userDetailsService(userDetailsAccountService)
                .addFilterAfter(authenticatedUserExistsFilter, RememberMeAuthenticationFilter.class)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.invalidSessionUrl(LOGIN_PATH))
                .requestCache(cache -> cache.requestCache(requestCache))
                .authorizeHttpRequests(this::configureAuthorization)
                .formLogin(form -> configureFormLogin(form, savedRequestSuccessHandler(requestCache)))
                .rememberMe(remember -> configureRememberMe(remember, userDetailsAccountService, rememberMeKey))
                .logout(logout -> logout.logoutUrl(LOGOUT_PATH)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl(LOGIN_PATH + "?logout=true"))
                .exceptionHandling(ex -> ex.accessDeniedHandler(WebAuthConfig::handleAccessDenied))
                .csrf(csrf -> csrf.disable())
                // payment-proof needs SAMEORIGIN for rendering PDF in modal iframes
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                antMatcher("/requests/bookings/*/payment-proof"), // payment-proof needs SAMEORIGIN for
                                // redering PDF in modal iframes
                                new XFrameOptionsHeaderWriter(
                                        XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))));
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        antMatcher("/css/**"), antMatcher("/js/**"), antMatcher("/img/**"), antMatcher("/favicon.ico"));
    }

    private void configureAuthorization(
            final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // Use AntPathRequestMatcher so Spring Security does not pick MvcRequestMatcher (Servlet 4).
        auth.requestMatchers(antMatcher(LOGIN_PATH))
                .permitAll()
                .requestMatchers(antMatcher(REGISTER_PATH))
                .anonymous()
                .requestMatchers(publicPageMatchers())
                .permitAll()
                .requestMatchers(antMatcher("/password-recovery/**"))
                .permitAll()
                .requestMatchers(antMatcher("/verify-email/**"))
                .permitAll()
                .requestMatchers(publicResourceMatchers())
                .permitAll()
                .requestMatchers(antMatcher("/admin/**"))
                .hasRole("ADMIN")
                .requestMatchers(antMatcher("/**"))
                .authenticated();
    }

    private static RequestMatcher[] publicPageMatchers() {
        return new RequestMatcher[] {
            antMatcher(HttpMethod.GET, "/"),
            antMatcher(HttpMethod.GET, "/marketplace"),
            antMatcher(HttpMethod.GET, "/location-options"),
            antMatcher(HttpMethod.GET, "/item-type-options"),
            antMatcher(HttpMethod.GET, "/profiles/*"),
            antMatcher(HttpMethod.GET, ERRORS_PATH),
        };
    }

    private static RequestMatcher[] publicResourceMatchers() {
        return new RequestMatcher[] {
            antMatcher(HttpMethod.GET, "/image/*"), antMatcher(HttpMethod.GET, "/item/*"),
        };
    }

    private static void configureFormLogin(
            final FormLoginConfigurer<HttpSecurity> form, final AuthenticationSuccessHandler successHandler) {
        form.usernameParameter("j_username")
                .passwordParameter("j_password")
                .loginPage(LOGIN_PATH)
                .successHandler(successHandler)
                .failureHandler(WebAuthConfig::handleLoginFailure);
    }

    private static AuthenticationSuccessHandler savedRequestSuccessHandler(final RequestCache requestCache) {
        // Returns users to the page they were bounced from; "/" is only the fallback when nothing was saved.
        final SavedRequestAwareAuthenticationSuccessHandler fallback =
                new SavedRequestAwareAuthenticationSuccessHandler();
        fallback.setRequestCache(requestCache);
        fallback.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            final HttpSession session = request.getSession(false);
            if (session != null) {
                final String continueUrl = (String) session.getAttribute("continueUrl");
                if (continueUrl != null) {
                    session.removeAttribute("continueUrl");
                    response.sendRedirect(request.getContextPath() + continueUrl);
                    return;
                }
            }
            fallback.onAuthenticationSuccess(request, response, authentication);
        };
    }

    private static void configureRememberMe(
            final RememberMeConfigurer<HttpSecurity> remember,
            final UserAccountDetailsService userDetailsService,
            final String rememberMeKey) {
        // Spring Security 5.8's remember-me DSL still defaults the signature to MD5 for backward
        // compatibility, so build the services explicitly to sign with SHA-256 (the algorithm name
        // is embedded in the cookie, so old MD5 cookies could still be matched if ever needed).
        final TokenBasedRememberMeServices tokenServices = new TokenBasedRememberMeServices(
                rememberMeKey, userDetailsService, TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);
        tokenServices.setParameter(REMEMBER_ME_PARAMETER);
        tokenServices.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
        final RememberMeServices rememberMeServices =
                new CookieClearingRememberMeServices(tokenServices, REMEMBER_ME_PARAMETER);
        remember.rememberMeServices(rememberMeServices).key(rememberMeKey);
    }

    private static String requireRememberMeKey(final Properties credentialsProperties) {
        final String key = credentialsProperties.getProperty(REMEMBER_ME_KEY_PROPERTY);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing or blank '" + REMEMBER_ME_KEY_PROPERTY + "' in credentials properties");
        }
        return key;
    }

    private static void handleLoginFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception)
            throws IOException {
        response.sendRedirect(request.getContextPath() + loginFailureRedirect(exception));
    }

    private static String loginFailureRedirect(final AuthenticationException exception) {
        return exception instanceof DisabledException ? LOGIN_PATH + "?unverified=true" : LOGIN_PATH + "?error=true";
    }

    /** Filter-chain authorization failures; business-level forbidden operations use {@code ForbiddenOperationException}. */
    private static void handleAccessDenied(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AccessDeniedException accessDeniedException)
            throws ServletException, IOException {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value());
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
        request.getRequestDispatcher(ERRORS_PATH).forward(request, response);
    }
}
