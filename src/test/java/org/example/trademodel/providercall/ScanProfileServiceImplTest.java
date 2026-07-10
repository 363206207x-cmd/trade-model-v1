package org.example.trademodel.providercall;

import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.providercall.profile.ScanProfileResponse;
import org.example.trademodel.providercall.profile.ScanProfileServiceImpl;
import org.example.trademodel.providercall.profile.ScanProfileUpdateRequest;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.UserConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanProfileServiceImplTest {

    @Test
    void manualProfileUpdatePersistsAndWritesUnifiedAudit() {
        Fixture fixture = fixture();
        ScanProfileUpdateRequest request = new ScanProfileUpdateRequest(UserScanProfile.HIGH,
                UserScanProfile.HIGH, UserScanProfile.LOW, true, null, "manual safety increase");

        ScanProfileResponse response = fixture.service.update("operator", request);

        ArgumentCaptor<UserConfigDO> config = ArgumentCaptor.forClass(UserConfigDO.class);
        verify(fixture.userConfigService).saveUserConfig(config.capture());
        assertThat(config.getValue().getScanBaseProfile()).isEqualTo("HIGH");
        assertThat(config.getValue().getScanUpdateReason()).isEqualTo("manual safety increase");
        ArgumentCaptor<RuleVersionLogDO> audit = ArgumentCaptor.forClass(RuleVersionLogDO.class);
        verify(fixture.auditMapper).insert(audit.capture());
        assertThat(audit.getValue().getChangeCategory()).isEqualTo("SCAN_PROFILE_CONFIG");
        assertThat(response.positionPriceIntervalSeconds()).isEqualTo(5);
    }

    @Test
    void positionProfileCannotUseAutoAndBypassExplicitSafetyCadence() {
        Fixture fixture = fixture();
        ScanProfileUpdateRequest request = new ScanProfileUpdateRequest(UserScanProfile.LOW,
                UserScanProfile.AUTO, UserScanProfile.LOW, true, null, "invalid");
        assertThatThrownBy(() -> fixture.service.update("operator", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positionMonitorProfile cannot be AUTO");
    }

    private static Fixture fixture() {
        UserConfigService userConfig = mock(UserConfigService.class);
        when(userConfig.getUserConfig("operator")).thenReturn(null);
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.resolveActiveRuleVersion()).thenReturn("v1");
        RuleVersionLogMapper audit = mock(RuleVersionLogMapper.class);
        when(audit.insert(any())).thenReturn(1);
        ProviderCallProperties properties = new ProviderCallProperties();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        ScanProfileServiceImpl service = new ScanProfileServiceImpl(userConfig, rules, audit, properties, budget,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        return new Fixture(service, userConfig, audit);
    }

    private record Fixture(ScanProfileServiceImpl service, UserConfigService userConfigService,
                           RuleVersionLogMapper auditMapper) {}
}
