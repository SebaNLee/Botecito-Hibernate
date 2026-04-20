package ar.edu.itba.paw.webapp.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// TODO I kinda read the deprecated warnings (2022), should be fine. I think it is bc of this old af stack we are using
@Configuration
@EnableWebSecurity
public class WebAuthConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ar.edu.itba.paw.webapp.auth.UserAccountDetailsService userDetailsAccountService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsAccountService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsAccountService)
                .sessionManagement()
                .invalidSessionUrl("/login")
                .and()
                .authorizeRequests()
                .antMatchers("/login")
                .anonymous()
                .antMatchers("/register")
                .anonymous()
                .antMatchers(HttpMethod.POST, "/item/*")
                .authenticated()
                .antMatchers(HttpMethod.POST, "/bookings/**")
                .authenticated()
                .antMatchers("/profile/**")
                .authenticated()
                .antMatchers("/publish/**")
                .authenticated()
                .antMatchers("/**")
                .permitAll()
                .and()
                .formLogin()
                .usernameParameter("j_username")
                .passwordParameter("j_password")
                .defaultSuccessUrl("/", false)
                .loginPage("/login")
                .failureUrl("/login?error=true")
                .and()
                .rememberMe()
                .rememberMeParameter("j_rememberme")
                .userDetailsService(userDetailsAccountService)
                .key("botecito-remember-me-secret")
                .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30))
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .and()
                .exceptionHandling()
                .accessDeniedPage("/403")
                .and()
                .csrf()
                .disable();
    }

    @Override
    public void configure(final WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico");
    }
}
