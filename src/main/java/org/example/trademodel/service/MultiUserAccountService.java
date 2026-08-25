package org.example.trademodel.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.entity.OwnerPasswordSetupTokenDO;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.mapper.OwnerPasswordSetupTokenMapper;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.UserConfigMapper;
import org.example.trademodel.security.PersonalUsernamePolicy;
import org.example.trademodel.security.RegistrationPasswordPolicy;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MultiUserAccountService {
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_MINUTES = 15;

    private final PersonalUserMapper userMapper;
    private final UserConfigMapper userConfigMapper;
    private final AssetPoolItemMapper assetPoolItemMapper;
    private final OwnerPasswordSetupTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final boolean registrationEnabled;
    private final int configuredMaximumAccounts;
    private final SecureRandom secureRandom = new SecureRandom();

    public MultiUserAccountService(PersonalUserMapper userMapper,
                                   UserConfigMapper userConfigMapper,
                                   AssetPoolItemMapper assetPoolItemMapper,
                                   OwnerPasswordSetupTokenMapper tokenMapper,
                                   PasswordEncoder passwordEncoder,
                                   Clock clock,
                                   @Value("${registration.enabled:true}") boolean registrationEnabled,
                                   @Value("${registration.max-active-users:10}") int configuredMaximumAccounts) {
        this.userMapper = userMapper;
        this.userConfigMapper = userConfigMapper;
        this.assetPoolItemMapper = assetPoolItemMapper;
        this.tokenMapper = tokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.registrationEnabled = registrationEnabled;
        this.configuredMaximumAccounts = Math.max(0, configuredMaximumAccounts);
    }

    public RegistrationAvailability registrationAvailability() {
        int limit = effectiveLimit(userMapper.selectRegistrationLimit());
        int enabled = userMapper.countEnabled();
        boolean canonicalOwnerAvailable = userMapper.countCanonicalOwner() == 1;
        return new RegistrationAvailability(
                registrationEnabled && canonicalOwnerAvailable && enabled < limit,
                enabled,
                limit);
    }

    @Transactional
    public PersonalUserDO register(String rawUsername, String password) {
        String username = PersonalUsernamePolicy.normalize(rawUsername);
        if (!registrationEnabled) {
            throw new IllegalStateException("registration is disabled");
        }
        if (!PersonalUsernamePolicy.isRegistrationValid(username)) {
            throw new IllegalArgumentException("username format is invalid");
        }
        if (PersonalUsernamePolicy.isReservedRegistrationUsername(username)) {
            throw new IllegalArgumentException("username is already registered or reserved");
        }
        RegistrationPasswordPolicy.requireValid(password, username);
        int limit = effectiveLimit(userMapper.lockRegistrationGuard());
        requireCanonicalOwner();
        if (userMapper.countEnabled() >= limit) {
            throw new IllegalStateException("registration capacity reached");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("username is already registered");
        }
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setEnabled(true);
        user.setSessionVersion(0L);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            if (userMapper.insert(user) != 1 || user.getId() == null) {
                throw new IllegalStateException("account registration did not persist exactly once");
            }
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalArgumentException("username is already registered", duplicate);
        }
        provisionAccountDefaults(user.getId(), now);
        return user;
    }

    public List<AccountView> listAccounts() {
        return userMapper.listAll().stream().map(AccountView::from).toList();
    }

    @Transactional
    public void disableUser(Long userId) {
        requireManagedUser(userId);
        if (userMapper.disableUser(userId, UtcLocalTimePolicy.now(clock)) != 1) {
            throw new IllegalStateException("user is already disabled");
        }
    }

    @Transactional
    public void enableUser(Long userId) {
        requireManagedUser(userId);
        int limit = effectiveLimit(userMapper.lockRegistrationGuard());
        if (userMapper.countEnabled() >= limit) {
            throw new IllegalStateException("active account limit reached");
        }
        if (userMapper.enableUser(userId, UtcLocalTimePolicy.now(clock)) != 1) {
            throw new IllegalStateException("user is already enabled");
        }
    }

    @Transactional
    public void forceLogout(Long userId) {
        requireManagedUser(userId);
        if (userMapper.forceLogout(userId, UtcLocalTimePolicy.now(clock)) != 1) {
            throw new IllegalStateException("user session generation was not updated");
        }
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword,
                                  String newPassword, String confirmation) {
        PersonalUserDO user = userId == null ? null : userMapper.findById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("current password is invalid");
        }
        if (!passwordEquals(newPassword, confirmation)) {
            throw new IllegalArgumentException("password confirmation does not match");
        }
        RegistrationPasswordPolicy.requireValid(newPassword, user.getUsername());
        if (userMapper.updatePassword(userId, passwordEncoder.encode(newPassword),
                UtcLocalTimePolicy.now(clock)) != 1) {
            throw new IllegalStateException("password was not updated exactly once");
        }
    }

    @Transactional
    public String issueOwnerPasswordSetupLink(Long ownerUserId) {
        PersonalUserDO owner = userMapper.findById(ownerUserId);
        if (owner == null || !"OWNER".equals(owner.getRole()) || !"xuchao".equals(owner.getUsername())) {
            throw new IllegalArgumentException("owner account required");
        }
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        tokenMapper.invalidateUnused(ownerUserId, now);
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OwnerPasswordSetupTokenDO row = new OwnerPasswordSetupTokenDO();
        row.setUserId(ownerUserId);
        row.setTokenHash(hashToken(token));
        row.setCreatedAt(now);
        row.setExpiresAt(now.plusMinutes(TOKEN_MINUTES));
        if (tokenMapper.insert(row) != 1) {
            throw new IllegalStateException("password setup token was not persisted");
        }
        return "/owner/password-setup?token=" + token;
    }

    @Transactional
    public void completeOwnerPasswordSetup(String token, String password, String confirmation) {
        if (token == null || token.isBlank() || !passwordEquals(password, confirmation)) {
            throw new IllegalArgumentException("password setup request is invalid");
        }
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        OwnerPasswordSetupTokenDO row = tokenMapper.lockUsable(hashToken(token), now);
        if (row == null) {
            throw new IllegalArgumentException("password setup link is invalid or expired");
        }
        PersonalUserDO owner = userMapper.findById(row.getUserId());
        if (owner == null || !"OWNER".equals(owner.getRole())) {
            throw new IllegalStateException("password setup owner is unavailable");
        }
        RegistrationPasswordPolicy.requireValid(password, owner.getUsername());
        if (userMapper.updatePassword(owner.getId(), passwordEncoder.encode(password), now) != 1
                || tokenMapper.markUsed(row.getId(), now) != 1) {
            throw new IllegalStateException("password setup was not completed exactly once");
        }
    }

    private PersonalUserDO requireManagedUser(Long userId) {
        PersonalUserDO user = userId == null ? null : userMapper.findById(userId);
        if (user == null || !"USER".equals(user.getRole())) {
            throw new IllegalArgumentException("managed USER account not found");
        }
        return user;
    }

    @Transactional
    public void provisionAccountDefaults(Long userId) {
        provisionAccountDefaults(userId, UtcLocalTimePolicy.now(clock));
    }

    private void provisionAccountDefaults(Long userId, LocalDateTime now) {
        if (userId == null || userId <= 0 || userMapper.findById(userId) == null) {
            throw new IllegalArgumentException("account owner is unavailable");
        }
        if (userConfigMapper.findByUserId(String.valueOf(userId)) == null) {
            createDefaultConfig(userId);
        }
        materializeMissingDefaultPool(userId, now);
    }

    private void requireCanonicalOwner() {
        if (userMapper.countCanonicalOwner() != 1) {
            throw new IllegalStateException("canonical owner is unavailable");
        }
    }

    private void createDefaultConfig(Long userId) {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(String.valueOf(userId));
        config.setRiskPreference("BALANCED");
        config.setAiModelPreference("DEFAULT");
        config.setNotifyChannels("IN_APP");
        config.setCooldownMinutes(15);
        config.setScanBaseProfile("AUTO");
        config.setScanPositionProfile("AUTO");
        config.setScanPoolProfile("AUTO");
        config.setScanAutoEscalationEnabled(true);
        config.setTelegramChatId(null);
        config.setTelegramBindingStatus("UNBOUND");
        config.setDefaultPoolMode("SYSTEM_DEFAULT");
        if (userConfigMapper.saveOrUpdate(config) != 1) {
            throw new IllegalStateException("default user configuration was not persisted");
        }
    }

    private void materializeMissingDefaultPool(Long userId, LocalDateTime now) {
        for (AssetPoolItemDO source : assetPoolItemMapper.listSystemDefaults()) {
            if (assetPoolItemMapper.selectByOwnerAndSymbol("USER", userId, source.getSymbol()) != null) {
                continue;
            }
            AssetPoolItemDO row = new AssetPoolItemDO();
            row.setOwnerType("USER");
            row.setOwnerId(userId);
            row.setAssetId(source.getAssetId());
            row.setSymbol(source.getSymbol());
            row.setDisplayName(source.getDisplayName());
            row.setMarketType(source.getMarketType());
            row.setQuoteAsset(source.getQuoteAsset());
            row.setActive(source.getActive());
            row.setFocusEnabled(source.getFocusEnabled());
            row.setSortOrder(source.getSortOrder());
            row.setSourceType("USER_OVERRIDE");
            row.setWatchStatus("OBSERVING");
            row.setVersion(1);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            assetPoolItemMapper.upsert(row);
        }
    }

    private static boolean passwordEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int effectiveLimit(int databaseLimit) {
        return Math.min(databaseLimit, configuredMaximumAccounts);
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public record RegistrationAvailability(boolean open, int enabledAccounts, int maximumAccounts) {
    }

    public record AccountView(Long id, String username, String role, boolean enabled,
                              long sessionVersion, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        static AccountView from(PersonalUserDO user) {
            return new AccountView(user.getId(), user.getUsername(), user.getRole(),
                    Boolean.TRUE.equals(user.getEnabled()),
                    user.getSessionVersion() == null ? 0L : user.getSessionVersion(),
                    user.getCreatedAt(), user.getLastLoginAt());
        }
    }
}
