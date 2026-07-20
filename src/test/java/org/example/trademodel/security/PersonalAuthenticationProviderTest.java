package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalAuthenticationProviderTest {
    private static final String USERNAME = "operator";
    private static final String PASSWORD = "operator-secret-123";

    @Mock
    private PersonalUserDetailsService userDetailsService;

    @Mock
    private PersonalUserMapper personalUserMapper;

    @Mock
    private LoginAuditLogger loginAuditLogger;

    private LoginAttemptService loginAttemptService;
    private PersonalAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        Clock clock = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);
        loginAttemptService = new LoginAttemptService(
                5, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15), 32, clock);
        provider = new PersonalAuthenticationProvider(
                userDetailsService, personalUserMapper, passwordEncoder,
                loginAttemptService, loginAuditLogger, clock);
    }

    @Test
    void lastLoginPersistenceFailureFailsClosed() {
        stubValidUser();
        when(personalUserMapper.updateLastLoginAt(eq(USERNAME), any())).thenThrow(
                new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> provider.authenticate(authentication(USERNAME, PASSWORD)))
                .isInstanceOf(InternalAuthenticationServiceException.class)
                .hasMessage("Authentication service unavailable");

        verify(loginAuditLogger).failure(USERNAME, "last_login_update_failed");
        verify(loginAuditLogger, never()).success(any());
    }

    @Test
    void authenticationStoreFailureFailsClosed() {
        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(
                new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> provider.authenticate(authentication(USERNAME, PASSWORD)))
                .isInstanceOf(InternalAuthenticationServiceException.class)
                .hasMessage("Authentication service unavailable");

        verify(personalUserMapper, never()).updateLastLoginAt(any(), any());
        verify(loginAuditLogger).failure(USERNAME, "authentication_store_unavailable");
    }

    @Test
    void oversizedUsernameDoesNotReachUserStore() {
        String oversized = "x".repeat(PersonalUsernamePolicy.MAX_LENGTH + 1);

        assertThatThrownBy(() -> provider.authenticate(authentication(oversized, PASSWORD)))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);

        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(loginAuditLogger).failure("<invalid>", "invalid_credentials");
    }

    private static UsernamePasswordAuthenticationToken authentication(String username, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(username, password);
    }

    private void stubValidUser() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(
                User.withUsername(USERNAME)
                        .password(passwordEncoder.encode(PASSWORD))
                        .roles("OPERATOR")
                        .build());
    }
}
