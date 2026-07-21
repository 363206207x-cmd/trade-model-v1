package org.example.trademodel.security;

import java.time.Clock;

import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PersonalAuthenticationProvider implements AuthenticationProvider {
    private static final String GENERIC_FAILURE = "Invalid credentials or login temporarily limited";

    private final PersonalUserDetailsService userDetailsService;
    private final PersonalUserMapper personalUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final LoginAuditLogger loginAuditLogger;
    private final Clock clock;
    private final String dummyPasswordHash;

    @Autowired
    public PersonalAuthenticationProvider(PersonalUserDetailsService userDetailsService,
                                          PersonalUserMapper personalUserMapper,
                                          PasswordEncoder passwordEncoder,
                                          LoginAttemptService loginAttemptService,
                                          LoginAuditLogger loginAuditLogger) {
        this(userDetailsService, personalUserMapper, passwordEncoder, loginAttemptService,
                loginAuditLogger, Clock.systemUTC());
    }

    PersonalAuthenticationProvider(PersonalUserDetailsService userDetailsService,
                                   PersonalUserMapper personalUserMapper,
                                   PasswordEncoder passwordEncoder,
                                   LoginAttemptService loginAttemptService,
                                   LoginAuditLogger loginAuditLogger,
                                   Clock clock) {
        this.userDetailsService = userDetailsService;
        this.personalUserMapper = personalUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.loginAuditLogger = loginAuditLogger;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.dummyPasswordHash = passwordEncoder.encode("non-user timing equalizer");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String normalizedUsername = PersonalUsernamePolicy.normalize(authentication.getName());
        boolean validUsername = PersonalUsernamePolicy.isValid(normalizedUsername);
        String username = validUsername ? normalizedUsername : "<invalid>";
        String rawPassword = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();

        UserDetails userDetails;
        try {
            if (!validUsername) {
                throw new UsernameNotFoundException("Invalid credentials");
            }
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            rejectUnknownUsername(username);
            return null;
        } catch (RuntimeException ex) {
            loginAuditLogger.failure(username, "authentication_store_unavailable");
            throw new InternalAuthenticationServiceException("Authentication service unavailable", ex);
        }

        boolean passwordMatches = passwordEncoder.matches(rawPassword, userDetails.getPassword());
        if (loginAttemptService.isKnownUserBlocked(username)) {
            loginAuditLogger.blocked(username);
            throw new LockedException(GENERIC_FAILURE);
        }
        if (!passwordMatches) {
            rejectKnownUser(username);
        }

        try {
            int updated = personalUserMapper.updateLastLoginAt(
                    userDetails.getUsername(), UtcLocalTimePolicy.now(clock));
            if (updated != 1) {
                throw new IllegalStateException("personal user login timestamp update count was not one");
            }
        } catch (RuntimeException ex) {
            loginAuditLogger.failure(username, "last_login_update_failed");
            throw new InternalAuthenticationServiceException("Authentication service unavailable", ex);
        }

        loginAttemptService.resetKnownUser(username);
        loginAuditLogger.success(username);
        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private void rejectKnownUser(String username) {
        reject(username, loginAttemptService.registerKnownUserFailure(username));
    }

    private void rejectUnknownUsername(String username) {
        reject(username, loginAttemptService.registerUnknownUsernameFailure(username));
    }

    private void reject(String username, LoginAttemptService.FailureResult result) {
        if (result == LoginAttemptService.FailureResult.TEMPORARILY_BLOCKED) {
            loginAuditLogger.blocked(username);
            throw new LockedException(GENERIC_FAILURE);
        }
        loginAuditLogger.failure(username, "invalid_credentials");
        throw new BadCredentialsException(GENERIC_FAILURE);
    }
}
