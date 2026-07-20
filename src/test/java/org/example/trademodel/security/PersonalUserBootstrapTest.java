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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(output).doesNotContain(PASSWORD).contains("status=CREATED", "status=SKIPPED_ALREADY_EXISTS");
    }

    @Test
    void missingConfigurationCreatesNoWeakDefaultUser() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK);

        bootstrap.run(mock(ApplicationArguments.class));

        verify(mapper, never()).insert(any(PersonalUserDO.class));
        verify(mapper, never()).countAll();
    }

    @Test
    void existingUserIsNeverOverwritten() {
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
    void incompleteOrWeakExplicitConfigurationFailsClosed() {
        PersonalUserMapper mapper = mock(PersonalUserMapper.class);
        assertThatThrownBy(() -> new PersonalUserBootstrap(
                true, "operator", "", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK)
                .run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires both");
        assertThatThrownBy(() -> new PersonalUserBootstrap(
                true, "operator", "short", mapper, new BCryptPasswordEncoder(), FIXED_CLOCK)
                .run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minimum length");
    }
}
