package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class PersonalUserBootstrapTest {
    private static final String PASSWORD = "bootstrap-secret-123";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-20T04:05:06Z"), ZoneOffset.UTC);

    @Test
    void explicitConfigurationCreatesOneBcryptUserAndIsIdempotent(CapturedOutput output) {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        PersonalUserDO existing = new PersonalUserDO();
        existing.setUsername("operator");
        when(mapper.findByUsername("operator")).thenReturn(null, existing);
        when(mapper.countAll()).thenReturn(0);
        when(mapper.insert(any(PersonalUserDO.class))).thenReturn(1);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, " Operator ", PASSWORD, mapper, encoder, FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));
        bootstrap.run(mock(ApplicationArguments.class));

        ArgumentCaptor<PersonalUserDO> captor = ArgumentCaptor.forClass(PersonalUserDO.class);
        verify(mapper).insert(captor.capture());
        PersonalUserDO created = captor.getValue();
        assertThat(created.getUsername()).isEqualTo("operator");
        assertThat(created.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(encoder.matches(PASSWORD, created.getPasswordHash())).isTrue();
        assertThat(created.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 20, 4, 5, 6));
        assertThat(output).doesNotContain(PASSWORD)
                .contains("state=BOOTSTRAP_READY", "state=USER_ALREADY_EXISTS");
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
    void existingUserIsStillNotOverwritten() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserDO existing = new PersonalUserDO();
        existing.setUsername("operator");
        existing.setPasswordHash("existing-hash");
        when(mapper.findByUsername("operator")).thenReturn(existing);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "operator", PASSWORD, mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
    }

    @Test
    void incompleteOrWeakExplicitConfigurationFailsReadinessClosed() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserBootstrap missing = new PersonalUserBootstrap(
                true, "operator", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        missing.run(mock(ApplicationArguments.class));
        assertThat(missing.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_MISSING);

        PersonalUserBootstrap weak = new PersonalUserBootstrap(
                true, "operator", "short", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        weak.run(mock(ApplicationArguments.class));
        assertThat(weak.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_POLICY_REJECTED);
        assertThat(weak.readiness().reasonCode()).isEqualTo("PASSWORD_TOO_SHORT");
        assertThat(weak.health().getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void blankBootstrapPasswordDoesNotCreateUser() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);

        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "operator", " ", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);
        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.PASSWORD_MISSING);
    }

    @Test
    void bootstrapRejectsPublicTemplatePassword() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);

        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "operator", "replace-with-long-local-secret",
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
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        assertThat(bootstrap.readiness().state())
                .isEqualTo(PersonalUserBootstrap.BootstrapState.USER_ALREADY_EXISTS);
        assertThat(bootstrap.health().getStatus().getCode()).isEqualTo("UP");
    }
}
