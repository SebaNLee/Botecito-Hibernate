package ar.edu.itba.paw.webapp.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import ar.edu.itba.paw.webapp.auth.UserAccountDetailsService;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebAuthConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http, final UserAccountDetailsService userDetailsAccountService) throws Exception {
        http.userDetailsService(userDetailsAccountService)
                .sessionManagement(session -> session.invalidSessionUrl("/login"))
                .authorizeHttpRequests(auth -> auth
                        // Use AntPathRequestMatcher so Spring Security does not pick MvcRequestMatcher (Servlet 4).
                        .requestMatchers(antMatcher("/login"))
                        .permitAll()
                        .requestMatchers(antMatcher("/register"))
                        .anonymous()
                        .requestMatchers(
                                antMatcher(HttpMethod.GET, "/"),
                                antMatcher(HttpMethod.GET, "/marketplace"),
                                antMatcher(HttpMethod.GET, "/location-options"),
                                antMatcher(HttpMethod.GET, "/errors"),
                                antMatcher(HttpMethod.GET, "/403"))
                        .permitAll()
                        .requestMatchers(antMatcher("/password-recovery/**"))
                        .permitAll()
                        .requestMatchers(
                                antMatcher(HttpMethod.GET, "/image/*"),
                                antMatcher(HttpMethod.GET, "/bookings/*/accept"),
                                antMatcher(HttpMethod.GET, "/bookings/*/decline"))
                        .permitAll()
                        .requestMatchers(new RegexRequestMatcher("^/item/[0-9]+$", "GET"))
                        .permitAll()
                        .requestMatchers(antMatcher("/**"))
                        .authenticated())
                .formLogin(form -> form.usernameParameter("j_username")
                        .passwordParameter("j_password")
                        .defaultSuccessUrl("/", false)
                        .loginPage("/login")
                        .failureUrl("/login?error=true"))
                .rememberMe(remember -> remember.rememberMeParameter("j_rememberme")
                        .userDetailsService(userDetailsAccountService)
                        .key("botecito-remember-me-secret")
                        .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30)))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        antMatcher("/css/**"), antMatcher("/js/**"), antMatcher("/img/**"), antMatcher("/favicon.ico"));
    }
}
