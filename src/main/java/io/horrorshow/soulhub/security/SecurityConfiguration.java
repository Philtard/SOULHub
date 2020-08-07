package io.horrorshow.soulhub.security;

import io.horrorshow.soulhub.data.repository.AppRoleRepository;
import io.horrorshow.soulhub.data.repository.AppUserRepository;
import io.horrorshow.soulhub.data.repository.VerificationTokenRepository;
import io.horrorshow.soulhub.service.SOULHubUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String LOGIN_PROCESSING_URL = "/login";
    private static final String LOGIN_FAILURE_URL = "/login?error";
    private static final String LOGIN_URL = "/login";
    private static final String LOGOUT_SUCCESS_URL = "/login";

    private final DataSource dataSource;

    private final AppRoleRepository appRoleRepository;

    private final AppUserRepository appUserRepository;

    private final VerificationTokenRepository verificationTokenRepository;

    private final JavaMailSender mailSender;

    @Autowired
    public SecurityConfiguration(DataSource dataSource,
                                 AppRoleRepository appRoleRepository,
                                 AppUserRepository appUserRepository,
                                 VerificationTokenRepository verificationTokenRepository,
                                 JavaMailSender mailSender) {
        this.dataSource = dataSource;
        this.appRoleRepository = appRoleRepository;
        this.appUserRepository = appUserRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.mailSender = mailSender;
    }

    // TODO extract from UserDetailsService everything none spring security in new UserService
    @Bean
    public UserDetailsService userDetailsService() {
        return new SOULHubUserDetailsService(
                appRoleRepository,
                appUserRepository,
                verificationTokenRepository,
                mailSender);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Require login to access internal pages and configure login form
     *
     * @param http
     *         HttpSecurity
     *
     * @throws Exception
     *         if http security can't be configured
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Not using Spring CSRF here to be able to use plain HTML for the login page
        http.csrf().disable() //

                // Register our CustomRequestCache that saves unauthorized access attempts, so
                // the user is redirected after login.
                .requestCache().requestCache(new CustomRequestCache()) //

                // Restrict access to our application.
                .and().authorizeRequests()

                // Allow all flow internal requests.
                .requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll() //

                // Allow all requests by logged in users.
                .anyRequest().authenticated() //

                // Configure the login page.
                .and().formLogin().loginPage(LOGIN_URL).permitAll() //
                .loginProcessingUrl(LOGIN_PROCESSING_URL) //
                .failureUrl(LOGIN_FAILURE_URL)

                // Configure logout
                .and().logout().logoutSuccessUrl(LOGOUT_SUCCESS_URL)
                .deleteCookies("soulhub-remember-me-cookie")
                .permitAll()
                .and()
                .rememberMe()
                .key("remember-me-key")
                .rememberMeCookieName("soulhub-remember-me-cookie")
                .tokenRepository(persistentTokenRepository())
                .tokenValiditySeconds(24 * 60 * 60)
                .and()
                .exceptionHandling();
    }

    /**
     * Allows access to static resources, bypassing Spring security
     *
     * @param web
     *         WebSecurity
     */
    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(
                // Vaadin Flow static resources
                "/VAADIN/**",
                // the standard favicon URI
                "/favicon.ico",
                // the robots exclusion standard
                "/robots.txt",
                // web application manifest
                "/manifest.webmanifest",
                "/sw.js",
                "/offline-page.html",
                // (development mode) static resources
                "/frontend/**",
                // (development mode) webjars
                "/webjars/**",
                // images
                "/img/**",
                "/icons/**",
                // ace-editor
                "/ace-builds/**",
                // (production mode) static resources
                "/frontend-es5/**", "/frontend-es6/**"
        );
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

}
