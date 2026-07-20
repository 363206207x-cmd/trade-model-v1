package org.example.trademodel.mapper;

import java.time.LocalDateTime;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersonalUserDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PersonalUserMapperIntegrationTest {

    @Autowired
    private PersonalUserMapper personalUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void usernameLookupBcryptStorageUniqueConstraintAndLastLoginUpdateWorkOnH2() {
        String rawPassword = "mapper-secret-123";
        PersonalUserDO user = user("mapper-operator", passwordEncoder.encode(rawPassword));
        assertThat(personalUserMapper.insert(user)).isEqualTo(1);

        PersonalUserDO persisted = personalUserMapper.findByUsername("mapper-operator");
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, persisted.getPasswordHash())).isTrue();
        assertThat(persisted.getLastLoginAt()).isNull();

        LocalDateTime loginAt = LocalDateTime.of(2026, 7, 20, 5, 6, 7);
        assertThat(personalUserMapper.updateLastLoginAt("mapper-operator", loginAt)).isEqualTo(1);
        assertThat(personalUserMapper.findByUsername("mapper-operator").getLastLoginAt()).isEqualTo(loginAt);

        assertThatThrownBy(() -> personalUserMapper.insert(user("mapper-operator", "another-hash")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private static PersonalUserDO user(String username, String passwordHash) {
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(LocalDateTime.of(2026, 7, 20, 0, 0));
        return user;
    }
}
