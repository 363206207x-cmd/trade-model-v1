package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.UserScanProfile;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ProviderCallProfilePreferenceServiceImpl implements ProviderCallProfilePreferenceService {
    private final ScanProfileService scanProfileService;
    private final ProviderCallProperties properties;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCallProfilePreferenceServiceImpl(ScanProfileService scanProfileService,
                                                    ProviderCallProperties properties) {
        this(scanProfileService, properties, Clock.systemUTC());
    }

    public ProviderCallProfilePreferenceServiceImpl(ScanProfileService scanProfileService,
                                                    ProviderCallProperties properties,
                                                    Clock clock) {
        this.scanProfileService = scanProfileService;
        this.properties = properties;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public synchronized UserScanProfile getBaseProfile() {
        try {
            return scanProfileService.get(properties.getScanUserId()).configuredProfile();
        } catch (RuntimeException unavailable) {
            return properties.getBaseProfile();
        }
    }

    @Override
    public synchronized ProfilePreferenceChange setBaseProfile(UserScanProfile profile,
                                                               String actor,
                                                               String reason) {
        if (profile == null) throw new IllegalArgumentException("请选择调用基础档位");
        String safeActor = required(actor, "当前管理员身份不可用");
        String safeReason = required(reason, "请填写调整原因");
        UserScanProfile previous = getBaseProfile();
        ScanProfileResponse current = scanProfileService.get(properties.getScanUserId());
        ScanProfileUpdateRequest request = new ScanProfileUpdateRequest(profile,
                current.positionMonitorProfile(), current.poolProfile(), current.autoEscalationEnabled(),
                null, safeReason);
        ScanProfileResponse updated = scanProfileService.update(
                properties.getScanUserId(), safeActor, request);
        return new ProfilePreferenceChange(previous, updated.configuredProfile(),
                label(updated.configuredProfile()), safeActor, safeReason, clock.instant(),
                "EXISTING_USER_CONFIG_OWNER");
    }

    public static String label(UserScanProfile profile) {
        if (profile == null) return "未知档位";
        return switch (profile) {
            case AUTO -> "自动";
            case LOW -> "低频";
            case STANDARD -> "标准";
            case HIGH -> "高频";
        };
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
