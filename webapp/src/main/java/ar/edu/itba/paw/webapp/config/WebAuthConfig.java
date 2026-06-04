package ar.edu.itba.paw.webapp.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import ar.edu.itba.paw.webapp.auth.AuthenticatedUserExistsFilter;
import ar.edu.itba.paw.webapp.auth.UserAccountDetailsService;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    private static final String LOGIN_PATH = "/login";
    private static final String REGISTER_PATH = "/register";
    private static final String LOGOUT_PATH = "/logout";
    private static final String ERRORS_PATH = "/errors";
    private static final String REMEMBER_ME_KEY = "botecito-remember-me-secret";
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

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final UserAccountDetailsService userDetailsAccountService,
            final SecurityContextRepository securityContextRepository,
            final AuthenticatedUserExistsFilter authenticatedUserExistsFilter)
            throws Exception {
        http.userDetailsService(userDetailsAccountService)
                .addFilterAfter(authenticatedUserExistsFilter, RememberMeAuthenticationFilter.class)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.invalidSessionUrl(LOGIN_PATH))
                .authorizeHttpRequests(this::configureAuthorization)
                .formLogin(WebAuthConfig::configureFormLogin)
                .rememberMe(remember -> configureRememberMe(remember, userDetailsAccountService))
                .logout(logout -> logout.logoutUrl(LOGOUT_PATH).logoutSuccessUrl(LOGIN_PATH + "?logout=true"))
                .exceptionHandling(ex -> ex.accessDeniedHandler(WebAuthConfig::handleAccessDenied))
                // CSRF stays enabled (Spring default). Plain <form> posts include the token via the
                // csrfInput tag; <form:form> tags inject it through CsrfRequestDataValueProcessor.
                // payment-proof needs SAMEORIGIN for redering PDF in modal iframes
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                antMatcher("/requests/bookings/*/payment-proof"),
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

    private static void configureFormLogin(final FormLoginConfigurer<HttpSecurity> form) {
        form.usernameParameter("j_username")
                .passwordParameter("j_password")
                .loginPage(LOGIN_PATH)
                // Falls back to "/" only when there is no saved request; Spring's RequestCache
                // returns users to the protected page they were bounced from.
                .defaultSuccessUrl("/")
                .failureHandler(WebAuthConfig::handleLoginFailure);
    }

    private static void configureRememberMe(
            final RememberMeConfigurer<HttpSecurity> remember, final UserAccountDetailsService userDetailsService) {
        remember.rememberMeParameter("j_rememberme")
                .userDetailsService(userDetailsService)
                .key(REMEMBER_ME_KEY)
                .tokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
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

    private static void handleAccessDenied(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AccessDeniedException accessDeniedException)
            throws ServletException, IOException {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.getRequestDispatcher(ERRORS_PATH).forward(request, response);
    }
}
