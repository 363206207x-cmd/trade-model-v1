package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PersonalUserBootstrap implements ApplicationRunner, HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(PersonalUserBootstrap.class);
    private final boolean authEnabled;
    private final String initialUsername;
    private final String initialPassword;
    private final PersonalUserMapper personalUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AtomicReference<BootstrapReadiness> readiness = new AtomicReference<>();

    public enum BootstrapState {
        USERNAME_MISSING,
        PASSWORD_MISSING,
        PASSWORD_POLICY_REJECTED,
        BOOTSTRAP_READY,
        USER_ALREADY_EXISTS,
        BOOTSTRAP_FAILED
    }

    public record BootstrapReadiness(BootstrapState state, String reasonCode, Instant evaluatedAt) {
        public boolean ready() {
            return state == BootstrapState.BOOTSTRAP_READY || state == BootstrapState.USER_ALREADY_EXISTS;
        }
    }

    @Autowired
    public PersonalUserBootstrap(
            @Value("${trade-model.auth.enabled:true}") boolean authEnabled,
            @Value("${trade-model.auth.initial-username:}") String initialUsername,
            @Value("${trade-model.auth.initial-password:}") String initialPassword,
            PersonalUserMapper personalUserMapper,
            PasswordEncoder passwordEncoder) {
        this(authEnabled, initialUsername, initialPassword, personalUserMapper,
                passwordEncoder, Clock.systemUTC());
    }

    PersonalUserBootstrap(boolean authEnabled,
                          String initialUsername,
                          String initialPassword,
                          PersonalUserMapper personalUserMapper,
                          PasswordEncoder passwordEncoder,
                          Clock clock) {
        this.authEnabled = authEnabled;
        this.initialUsername = initialUsername;
        this.initialPassword = initialPassword;
        this.personalUserMapper = personalUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        readiness.set(new BootstrapReadiness(
                BootstrapState.BOOTSTRAP_FAILED, "BOOTSTRAP_NOT_EVALUATED", this.clock.instant()));
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!authEnabled) {
            transition(BootstrapState.BOOTSTRAP_READY, "AUTH_DISABLED");
            return;
        }
        try {
            if (personalUserMapper.countAll() > 0) {
                transition(BootstrapState.USER_ALREADY_EXISTS, "USER_TABLE_NOT_EMPTY");
                return;
            }
            if (initialUsername == null || initialUsername.isBlank()) {
                transition(BootstrapState.USERNAME_MISSING, "USERNAME_MISSING");
                return;
            }
            if (initialPassword == null || initialPassword.isBlank()) {
                transition(BootstrapState.PASSWORD_MISSING, "PASSWORD_MISSING");
                return;
            }

            String username = PersonalUsernamePolicy.normalize(initialUsername);
            if (!PersonalUsernamePolicy.isValid(username)) {
                transition(BootstrapState.BOOTSTRAP_FAILED, "USERNAME_POLICY_REJECTED");
                return;
            }
            InitialPasswordPolicy.Validation passwordValidation = InitialPasswordPolicy.validate(initialPassword);
            if (!passwordValidation.accepted()) {
                transition(BootstrapState.PASSWORD_POLICY_REJECTED,
                        passwordValidation.reasonCode().name());
                return;
            }
            if (personalUserMapper.findByUsername(username) != null || personalUserMapper.countAll() > 0) {
                transition(BootstrapState.USER_ALREADY_EXISTS, "USER_ALREADY_EXISTS");
                return;
            }

            PersonalUserDO user = new PersonalUserDO();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(initialPassword));
            user.setCreatedAt(UtcLocalTimePolicy.now(clock));
            if (personalUserMapper.insert(user) != 1) {
                transition(BootstrapState.BOOTSTRAP_FAILED, "USER_INSERT_COUNT_INVALID");
                return;
            }
            transition(BootstrapState.BOOTSTRAP_READY, "USER_CREATED");
        } catch (RuntimeException exception) {
            transition(BootstrapState.BOOTSTRAP_FAILED, "BOOTSTRAP_STORE_UNAVAILABLE");
        }
    }

    public BootstrapReadiness readiness() {
        return readiness.get();
    }

    @Override
    public Health health() {
        BootstrapReadiness snapshot = readiness();
        Health.Builder builder = snapshot.ready() ? Health.up() : Health.down();
        return builder.withDetail("state", snapshot.state().name())
                .withDetail("reasonCode", snapshot.reasonCode())
                .withDetail("evaluatedAt", snapshot.evaluatedAt())
                .build();
    }

    private void transition(BootstrapState state, String reasonCode) {
        BootstrapReadiness next = new BootstrapReadiness(state, reasonCode, clock.instant());
        readiness.set(next);
        if (next.ready()) {
            log.info("PERSONAL_USER_BOOTSTRAP state={} reasonCode={}", state, reasonCode);
        } else {
            log.warn("PERSONAL_USER_BOOTSTRAP state={} reasonCode={}", state, reasonCode);
        }
    }
}
