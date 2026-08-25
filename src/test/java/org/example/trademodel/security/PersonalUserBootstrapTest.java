package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.service.MultiUserAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class PersonalUserBootstrapTest {
    private static final String PASSWORD = "Ownr8!Aa";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-20T04:05:06Z"), ZoneOffset.UTC);

    @Test
    void explicitConfigurationCreatesOneBcryptUserAndIsIdempotent(CapturedOutput output) {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        MultiUserAccountService accountService = mock(MultiUserAccountService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(mapper.findByUsername("xuchao")).thenReturn(null);
        when(mapper.countAll()).thenReturn(0, 0, 1);
        when(mapper.countCanonicalOwner()).thenReturn(1);
        when(mapper.insert(any(PersonalUserDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PersonalUserDO.class).setId(1L);
            return 1;
        });
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, " XUCHAO ", PASSWORD, mapper, accountService, encoder, FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));
        bootstrap.run(mock(ApplicationArguments.class));

        ArgumentCaptor<PersonalUserDO> captor = ArgumentCaptor.forClass(PersonalUserDO.class);
        verify(mapper).insert(captor.capture());
        PersonalUserDO created = captor.getValue();
        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getUsername()).isEqualTo("xuchao");
        assertThat(created.getRole()).isEqualTo("OWNER");
        assertThat(created.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(encoder.matches(PASSWORD, created.getPasswordHash())).isTrue();
        assertThat(created.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 20, 4, 5, 6));
        assertThat(output).doesNotContain(PASSWORD)
                .contains("state=BOOTSTRAP_READY", "state=USER_ALREADY_EXISTS");
        verify(accountService, times(2)).provisionAccountDefaults(1L);
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.USER_ALREADY_EXISTS);
    }

    @Test
    void missingConfigurationCreatesNoWeakDefaultUser() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.USERNAME_MISSING);
        assertThat(bootstrap.health().getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void existingCanonicalOwnerIsStillNotOverwritten() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserDO existing = new PersonalUserDO();
        existing.setUsername("xuchao");
        existing.setPasswordHash("existing-hash");
        when(mapper.countAll()).thenReturn(1);
        when(mapper.countCanonicalOwner()).thenReturn(1);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "xuchao", PASSWORD, mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
    }

    @Test
    void incompleteOrWeakExplicitConfigurationFailsReadinessClosed() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserBootstrap missing = new PersonalUserBootstrap(
                true, "xuchao", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        missing.run(mock(ApplicationArguments.class));
        assertThat(missing.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_MISSING);

        PersonalUserBootstrap weak = new PersonalUserBootstrap(
                true, "xuchao", "short", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        weak.run(mock(ApplicationArguments.class));
        assertThat(weak.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_POLICY_REJECTED);
        assertThat(weak.readiness().reasonCode()).isEqualTo("PASSWORD_TOO_SHORT");
        assertThat(weak.health().getStatus().getCode()).isEqualTo("DOWN");

        PersonalUserBootstrap longPassword = new PersonalUserBootstrap(
                true, "xuchao", "123456789", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        longPassword.run(mock(ApplicationArguments.class));
        assertThat(longPassword.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_POLICY_REJECTED);
        assertThat(longPassword.readiness().reasonCode()).isEqualTo("PASSWORD_TOO_LONG");
    }

    @Test
    void blankBootstrapPasswordDoesNotCreateUser() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);

        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "xuchao", " ", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_MISSING);
    }

    @Test
    void bootstrapRejectsPublicTemplatePassword() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);

        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "xuchao", "replace-with-long-local-secret",
                mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_POLICY_REJECTED);
        assertThat(bootstrap.readiness().reasonCode()).isEqualTo("PASSWORD_TEMPLATE_VALUE");
    }

    @Test
    void existingUserNeedsNoInitialCredentialsAndRemainsReady() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        when(mapper.countAll()).thenReturn(1);
        when(mapper.countCanonicalOwner()).thenReturn(1);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.USER_ALREADY_EXISTS);
        assertThat(bootstrap.health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void nonCanonicalBootstrapUsernameFailsClosed() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "operator", PASSWORD, mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.BOOTSTRAP_FAILED);
        assertThat(bootstrap.readiness().reasonCode())
                .isEqualTo("CANONICAL_OWNER_USERNAME_REQUIRED");
    }

    @Test
    void nonCanonicalExistingAccountSetFailsClosed() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        when(mapper.countAll()).thenReturn(1);
        when(mapper.countCanonicalOwner()).thenReturn(0);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.BOOTSTRAP_FAILED);
        assertThat(bootstrap.readiness().reasonCode()).isEqualTo("CANONICAL_OWNER_UNAVAILABLE");
    }
}
