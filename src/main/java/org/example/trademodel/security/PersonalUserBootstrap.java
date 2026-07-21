package org.example.trademodel.security;

import java.time.Clock;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PersonalUserBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PersonalUserBootstrap.class);
    private final boolean authEnabled;
    private final String initialUsername;
    private final String initialPassword;
    private final PersonalUserMapper personalUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

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
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!authEnabled) {
            return;
        }
        boolean usernamePresent = initialUsername != null && !initialUsername.isBlank();
        boolean passwordPresent = initialPassword != null && !initialPassword.isBlank();
        if (!usernamePresent && !passwordPresent) {
            log.info("PERSONAL_USER_BOOTSTRAP status=SKIPPED_MISSING_CONFIGURATION");
            return;
        }
        if (!usernamePresent || !passwordPresent) {
            throw new IllegalStateException("Personal user bootstrap requires both username and password");
        }

        String username = PersonalUsernamePolicy.normalize(initialUsername);
        if (!PersonalUsernamePolicy.isValid(username)) {
            throw new IllegalStateException("Personal user bootstrap username is invalid");
        }
        if (InitialPasswordPolicy.isUnsafe(initialPassword)) {
            throw new IllegalStateException("Personal user bootstrap password is unsafe");
        }
        if (personalUserMapper.findByUsername(username) != null) {
            log.info("PERSONAL_USER_BOOTSTRAP username={} status=SKIPPED_ALREADY_EXISTS", username);
            return;
        }
        if (personalUserMapper.countAll() > 0) {
            log.info("PERSONAL_USER_BOOTSTRAP username={} status=SKIPPED_USER_TABLE_NOT_EMPTY", username);
            return;
        }

        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setCreatedAt(UtcLocalTimePolicy.now(clock));
        if (personalUserMapper.insert(user) != 1) {
            throw new IllegalStateException("Personal user bootstrap insert did not create exactly one user");
        }
        log.info("PERSONAL_USER_BOOTSTRAP username={} status=CREATED", username);
    }
}
