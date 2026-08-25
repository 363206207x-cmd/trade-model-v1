package org.example.trademodel.service;

import java.time.LocalDateTime;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.UserConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:multi-user-service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=Ownr8!Aa"
})
@Transactional
class MultiUserAccountServiceIntegrationTest {
    private static final String USER_PASSWORD = "User8!Aa";
    private static final String NEXT_PASSWORD = "Next8!Ab";

    @Autowired
    private MultiUserAccountService accountService;

    @Autowired
    private PersonalUserMapper userMapper;

    @Autowired
    private UserConfigMapper userConfigMapper;

    @Autowired
    private AssetPoolItemMapper assetPoolItemMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationCreatesOneUserWithPrivateDefaultsAndNoTelegramBinding() {
        PersonalUserDO registered = accountService.register("Alice_01", USER_PASSWORD);

        assertThat(registered.getUsername()).isEqualTo("alice_01");
        assertThat(registered.getRole()).isEqualTo("USER");
        assertThat(registered.getEnabled()).isTrue();
        assertThat(passwordEncoder.matches(USER_PASSWORD, registered.getPasswordHash())).isTrue();

        UserConfigDO config = userConfigMapper.findByUserId(String.valueOf(registered.getId()));
        assertThat(config).isNotNull();
        assertThat(config.getNotifyChannels()).isEqualTo("IN_APP");
        assertThat(config.getTelegramBindingStatus()).isEqualTo("UNBOUND");
        assertThat(config.getTelegramChatId()).isNull();
        assertThat(assetPoolItemMapper.listUserOverrides(registered.getId()))
                .hasSize(6)
                .allMatch(item -> "USER".equals(item.getOwnerType()))
                .allMatch(item -> registered.getId().equals(item.getOwnerId()));
    }

    @Test
    void bootstrapOwnerHasPrivateDefaultsWithoutOverwritingThemOnReprovision() {
        PersonalUserDO owner = userMapper.findByUsername("xuchao");
        assertThat(owner.getId()).isEqualTo(1L);
        assertThat(userConfigMapper.findByUserId("1")).isNotNull();
        assertThat(assetPoolItemMapper.listUserOverrides(1L)).hasSize(6);

        UserConfigDO config = userConfigMapper.findByUserId("1");
        config.setRiskPreference("CONSERVATIVE");
        userConfigMapper.saveOrUpdate(config);
        accountService.provisionAccountDefaults(1L);

        assertThat(userConfigMapper.findByUserId("1").getRiskPreference())
                .isEqualTo("CONSERVATIVE");
        assertThat(assetPoolItemMapper.listUserOverrides(1L)).hasSize(6);
    }

    @Test
    void registrationFailsClosedWhenCanonicalOwnerIsMissing() {
        assertThat(jdbcTemplate.update("DELETE FROM tm_user WHERE id = 1")).isEqualTo(1);

        assertThat(accountService.registrationAvailability().open()).isFalse();
        assertThatThrownBy(() -> accountService.register("ownerless_user", USER_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canonical owner");
        assertThat(userMapper.findByUsername("ownerless_user")).isNull();
    }

    @Test
    void usernamePasswordAndCapacityContractsFailClosed() {
        assertThatThrownBy(() -> accountService.register("xuchao", USER_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("SYSTEM", USER_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("ab", USER_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("a".repeat(33), USER_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("user@example.com", USER_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("short-password", "1234567"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("long-password", "123456789"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("default-password", "12345678"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("space-password", " User8!A"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.register("samepass", "samepass"))
                .isInstanceOf(IllegalArgumentException.class);

        accountService.register("duplicate", USER_PASSWORD);
        assertThatThrownBy(() -> accountService.register("DUPLICATE", "abcdefgh"))
                .isInstanceOf(IllegalArgumentException.class);

        for (int index = 1; index <= 8; index++) {
            accountService.register("capacity_" + index, USER_PASSWORD);
        }
        assertThat(userMapper.countEnabled()).isEqualTo(10);
        assertThat(accountService.registrationAvailability().open()).isFalse();
        assertThatThrownBy(() -> accountService.register("capacity_blocked", USER_PASSWORD))
                .isInstanceOf(IllegalStateException.class);

        Long disabledId = userMapper.findByUsername("capacity_1").getId();
        accountService.disableUser(disabledId);
        assertThat(accountService.registrationAvailability().open()).isTrue();
        accountService.register("capacity_replacement", USER_PASSWORD);
        assertThatThrownBy(() -> accountService.enableUser(disabledId))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> accountService.disableUser(userMapper.findByUsername("xuchao").getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(userMapper.countAll()).isEqualTo(11);
        assertThat(userMapper.countEnabled()).isEqualTo(10);
    }

    @Test
    void ownerPasswordSetupTokenIsHashedSingleUseAndInvalidatesPriorSessions() {
        PersonalUserDO ownerBefore = userMapper.findByUsername("xuchao");
        String path = accountService.issueOwnerPasswordSetupLink(ownerBefore.getId());
        String token = path.substring(path.indexOf("token=") + 6);

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM tm_owner_password_setup_token WHERE user_id = ?",
                String.class, ownerBefore.getId());
        assertThat(storedHash).hasSize(64).doesNotContain(token);

        accountService.completeOwnerPasswordSetup(token, "Ownr9!Bb", "Ownr9!Bb");
        PersonalUserDO ownerAfter = userMapper.findByUsername("xuchao");
        assertThat(passwordEncoder.matches("Ownr9!Bb", ownerAfter.getPasswordHash())).isTrue();
        assertThat(ownerAfter.getSessionVersion()).isEqualTo(ownerBefore.getSessionVersion() + 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_at FROM tm_owner_password_setup_token WHERE user_id = ?",
                LocalDateTime.class, ownerBefore.getId())).isNotNull();
        assertThatThrownBy(() -> accountService.completeOwnerPasswordSetup(
                token, "Ownr7!Cc", "Ownr7!Cc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ownPasswordChangeRequiresCurrentPasswordAndInvalidatesPriorSessions() {
        PersonalUserDO user = accountService.register("password_user", USER_PASSWORD);
        long versionBefore = userMapper.findById(user.getId()).getSessionVersion();

        assertThatThrownBy(() -> accountService.changeOwnPassword(
                user.getId(), "wrong-password", NEXT_PASSWORD, NEXT_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.changeOwnPassword(
                user.getId(), USER_PASSWORD, "1234567", "1234567"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.changeOwnPassword(
                user.getId(), USER_PASSWORD, NEXT_PASSWORD, "different"))
                .isInstanceOf(IllegalArgumentException.class);

        accountService.changeOwnPassword(user.getId(), USER_PASSWORD, NEXT_PASSWORD, NEXT_PASSWORD);
        PersonalUserDO updated = userMapper.findById(user.getId());
        assertThat(passwordEncoder.matches(NEXT_PASSWORD, updated.getPasswordHash())).isTrue();
        assertThat(updated.getSessionVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void ownerPasswordSetupLinksExpireAndIssuingANewLinkInvalidatesThePriorOne() {
        Long ownerId = userMapper.findByUsername("xuchao").getId();
        String firstPath = accountService.issueOwnerPasswordSetupLink(ownerId);
        String firstToken = firstPath.substring(firstPath.indexOf("token=") + 6);
        String secondPath = accountService.issueOwnerPasswordSetupLink(ownerId);
        String secondToken = secondPath.substring(secondPath.indexOf("token=") + 6);

        assertThatThrownBy(() -> accountService.completeOwnerPasswordSetup(
                firstToken, "Ownr9!Bb", "Ownr9!Bb"))
                .isInstanceOf(IllegalArgumentException.class);

        jdbcTemplate.update("UPDATE tm_owner_password_setup_token SET expires_at = ? WHERE user_id = ? AND used_at IS NULL",
                LocalDateTime.of(2000, 1, 1, 0, 0), ownerId);
        assertThatThrownBy(() -> accountService.completeOwnerPasswordSetup(
                secondToken, "Ownr9!Bb", "Ownr9!Bb"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
