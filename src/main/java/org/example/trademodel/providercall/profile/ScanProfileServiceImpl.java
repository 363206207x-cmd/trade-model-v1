package org.example.trademodel.providercall.profile;

import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.UserConfigService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ScanProfileServiceImpl implements ScanProfileService {
    private final UserConfigService userConfigService;
    private final RuleConfigService ruleConfigService;
    private final RuleVersionLogMapper auditMapper;
    private final ProviderCallProperties properties;
    private final ProviderRateBudgetManager budgetManager;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ScanProfileServiceImpl(UserConfigService userConfigService,
                                  RuleConfigService ruleConfigService,
                                  RuleVersionLogMapper auditMapper,
                                  ProviderCallProperties properties,
                                  ProviderRateBudgetManager budgetManager) {
        this(userConfigService, ruleConfigService, auditMapper, properties, budgetManager, Clock.systemUTC());
    }

    public ScanProfileServiceImpl(UserConfigService userConfigService,
                                  RuleConfigService ruleConfigService,
                                  RuleVersionLogMapper auditMapper,
                                  ProviderCallProperties properties,
                                  ProviderRateBudgetManager budgetManager,
                                  Clock clock) {
        this.userConfigService = userConfigService;
        this.ruleConfigService = ruleConfigService;
        this.auditMapper = auditMapper;
        this.properties = properties;
        this.budgetManager = budgetManager;
        this.clock = clock;
    }

    @Override
    public ScanProfileResponse get(String userId) {
        String operator = required(userId, "userId");
        UserConfigDO row = userConfigService.getUserConfig(operator);
        UserScanProfile base = parse(row == null ? null : row.getScanBaseProfile(), properties.getBaseProfile());
        UserScanProfile position = parse(row == null ? null : row.getScanPositionProfile(), UserScanProfile.LOW);
        UserScanProfile pool = parse(row == null ? null : row.getScanPoolProfile(), UserScanProfile.LOW);
        boolean auto = row == null || row.getScanAutoEscalationEnabled() == null
                ? properties.isAutoEscalationEnabled() : row.getScanAutoEscalationEnabled();
        Instant since = row == null || row.getScanUpdatedAt() == null
                ? clock.instant() : row.getScanUpdatedAt().toInstant(ZoneOffset.UTC);
        return response(base, position, pool, auto, since);
    }

    @Override
    public ScanProfileResponse update(String userId, ScanProfileUpdateRequest request) {
        String operator = required(userId, "userId");
        if (request == null) throw new IllegalArgumentException("request is required");
        validateProfile(request.positionMonitorProfile(), "positionMonitorProfile");
        validateProfile(request.poolProfile(), "poolProfile");
        Instant now = clock.instant();
        if (request.manualOverrideUntil() != null
                && (request.manualOverrideUntil().isBefore(now)
                || request.manualOverrideUntil().isAfter(now.plusSeconds(30L * 24 * 60 * 60)))) {
            throw new IllegalArgumentException("manualOverrideUntil must be within the next 30 days");
        }
        UserConfigDO row = userConfigService.getUserConfig(operator);
        if (row == null) {
            row = new UserConfigDO();
            row.setUserId(operator);
        }
        row.setScanBaseProfile(request.baseProfile().name());
        row.setScanPositionProfile(request.positionMonitorProfile().name());
        row.setScanPoolProfile(request.poolProfile().name());
        row.setScanAutoEscalationEnabled(request.autoEscalationEnabled());
        row.setScanManualOverrideUntil(request.manualOverrideUntil() == null ? null
                : LocalDateTime.ofInstant(request.manualOverrideUntil(), ZoneOffset.UTC));
        row.setScanUpdateReason(request.updateReason().trim());
        row.setScanUpdatedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        userConfigService.saveUserConfig(row);
        audit(operator, request, now);
        return response(request.baseProfile(), request.positionMonitorProfile(), request.poolProfile(),
                request.autoEscalationEnabled(), now);
    }

    private ScanProfileResponse response(UserScanProfile base, UserScanProfile position, UserScanProfile pool,
                                         boolean auto, Instant since) {
        RuntimeScanProfile effective = runtime(base);
        return new ScanProfileResponse(base, position, pool, auto, effective,
                base == UserScanProfile.AUTO ? "AUTO_BASE_STANDARD" : "MANUAL_BASE_PROFILE", since, null,
                properties.intervalSeconds(runtime(position), AssetPriority.P0_POSITION, ProviderDatasetType.PRICE),
                properties.intervalSeconds(effective, AssetPriority.P1_CORE, ProviderDatasetType.PRICE),
                properties.intervalSeconds(effective, AssetPriority.P2_CANDIDATE, ProviderDatasetType.PRICE),
                properties.intervalSeconds(runtime(pool), AssetPriority.P3_POOL, ProviderDatasetType.PRICE),
                properties.intervalSeconds(effective, AssetPriority.P1_CORE, ProviderDatasetType.DERIVATIVES),
                budgetManager.state("BINANCE_PUBLIC", ProviderCircuitState.CLOSED));
    }

    private void audit(String operator, ScanProfileUpdateRequest request, Instant now) {
        RuleVersionLogDO row = new RuleVersionLogDO();
        row.setId(UUID.randomUUID().toString());
        row.setRuleVersion(ruleConfigService.resolveActiveRuleVersion());
        row.setChangeCategory("SCAN_PROFILE_CONFIG");
        row.setChangeSummary("user=" + operator + ";baseProfile=" + request.baseProfile()
                + ";positionProfile=" + request.positionMonitorProfile() + ";poolProfile=" + request.poolProfile());
        row.setChangeDetail("autoEscalationEnabled=" + request.autoEscalationEnabled()
                + ";manualOverrideUntil=" + request.manualOverrideUntil()
                + ";updateReason=" + request.updateReason().trim());
        row.setOperator(operator);
        row.setPublishTime(now.toString());
        row.setRollbackFlag("N");
        row.setCreatedBy(operator);
        row.setUpdatedBy(operator);
        row.setIsDeleted(0);
        row.setVersionNo(1);
        auditMapper.insert(row);
    }

    private static void validateProfile(UserScanProfile profile, String field) {
        if (profile == UserScanProfile.AUTO) throw new IllegalArgumentException(field + " cannot be AUTO");
    }

    private static UserScanProfile parse(String raw, UserScanProfile fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return UserScanProfile.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    private static RuntimeScanProfile runtime(UserScanProfile profile) {
        return profile == null || profile == UserScanProfile.AUTO
                ? RuntimeScanProfile.STANDARD : RuntimeScanProfile.valueOf(profile.name());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
