package org.example.trademodel.service.impl;

import org.example.trademodel.dto.PushWatchlistConfigRequest;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.PushWatchlistConfigAuditMapper;
import org.example.trademodel.mapper.RuleConfigMapper;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.example.trademodel.vo.PushWatchlistConfigVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleConfigServiceImplTest {

    @Mock
    private RuleConfigMapper ruleConfigMapper;

    @Mock
    private PushWatchlistConfigAuditMapper pushWatchlistConfigAuditMapper;

    @Test
    void getPushWatchlistConfig_missingConfigFailsClosed() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(null);

        PushWatchlistConfigVO vo = service.getPushWatchlistConfig();

        assertThat(vo.getRuleKey()).isEqualTo(RuleConfigServiceImpl.WATCHLIST_RULE_KEY);
        assertThat(vo.getSymbols()).isEmpty();
        assertThat(vo.getEnabled()).isFalse();
        assertThat(vo.getRuleValue()).isEmpty();
    }

    @Test
    void getPushWatchlistConfig_normalizesEnabledConfig() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(ruleConfig("btcusdt, ETHUSDT , BTCUSDT", true));

        PushWatchlistConfigVO vo = service.getPushWatchlistConfig();

        assertThat(vo.getSymbols()).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(vo.getEnabled()).isTrue();
        assertThat(vo.getRuleValue()).isEqualTo("BTCUSDT,ETHUSDT");
    }

    @Test
    void getPushWatchlistConfig_readsDisabledConfig() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(ruleConfig("btcusdt,ethusdt", false));

        PushWatchlistConfigVO vo = service.getPushWatchlistConfig();

        assertThat(vo.getSymbols()).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(vo.getEnabled()).isFalse();
    }

    @Test
    void updatePushWatchlistConfig_createsNewConfigAndAudit() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(null);
        when(ruleConfigMapper.insertRuleConfig(any(RuleConfigDO.class))).thenReturn(1);
        when(pushWatchlistConfigAuditMapper.insert(any(PushWatchlistConfigAuditVO.class))).thenReturn(1);
        when(ruleConfigMapper.findAllEnabled()).thenReturn(List.of());

        PushWatchlistConfigVO vo = service.updatePushWatchlistConfig(
                request(List.of(" btcusdt ", "ETHUSDT", "btcusdt"), true, "tester", "add watchlist"));

        ArgumentCaptor<RuleConfigDO> configCaptor = ArgumentCaptor.forClass(RuleConfigDO.class);
        verify(ruleConfigMapper).insertRuleConfig(configCaptor.capture());
        RuleConfigDO saved = configCaptor.getValue();
        assertThat(saved.getRuleId()).isNotBlank();
        assertThat(saved.getRuleType()).isEqualTo("push");
        assertThat(saved.getRuleKey()).isEqualTo(RuleConfigServiceImpl.WATCHLIST_RULE_KEY);
        assertThat(saved.getRuleValue()).isEqualTo("BTCUSDT,ETHUSDT");
        assertThat(saved.getDescription()).isEqualTo("Push watchlist symbols");
        assertThat(saved.getVersion()).isEqualTo("p1-watchlist");
        assertThat(saved.getEnabled()).isTrue();

        ArgumentCaptor<PushWatchlistConfigAuditVO> auditCaptor =
                ArgumentCaptor.forClass(PushWatchlistConfigAuditVO.class);
        verify(pushWatchlistConfigAuditMapper).insert(auditCaptor.capture());
        PushWatchlistConfigAuditVO audit = auditCaptor.getValue();
        assertThat(audit.getBeforeSymbols()).isEmpty();
        assertThat(audit.getAfterSymbols()).isEqualTo("BTCUSDT,ETHUSDT");
        assertThat(audit.getBeforeEnabled()).isFalse();
        assertThat(audit.getAfterEnabled()).isTrue();
        assertThat(audit.getChangedBy()).isEqualTo("tester");
        assertThat(audit.getChangeReason()).isEqualTo("add watchlist");
        assertThat(audit.getSource()).isEqualTo("API");
        assertThat(audit.getTraceId()).isNotBlank();
        assertThat(audit.getRuleVersion()).isEqualTo("p1-watchlist");
        assertThat(audit.getCreateTime()).isNotNull();
        verify(ruleConfigMapper).findAllEnabled();

        assertThat(vo.getSymbols()).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(vo.getEnabled()).isTrue();
    }

    @Test
    void updatePushWatchlistConfig_updatesExistingConfigAndAudit() {
        RuleConfigServiceImpl service = service();
        RuleConfigDO oldConfig = ruleConfig("BTCUSDT", false);
        oldConfig.setRuleId("existing-rule");
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(oldConfig);
        when(ruleConfigMapper.updateRuleConfigByKey(any(RuleConfigDO.class))).thenReturn(1);
        when(pushWatchlistConfigAuditMapper.insert(any(PushWatchlistConfigAuditVO.class))).thenReturn(1);
        when(ruleConfigMapper.findAllEnabled()).thenReturn(List.of());

        PushWatchlistConfigVO vo = service.updatePushWatchlistConfig(
                request(List.of("ETHUSDT", " solusdt "), true, "tester", "rotate watchlist"));

        ArgumentCaptor<RuleConfigDO> configCaptor = ArgumentCaptor.forClass(RuleConfigDO.class);
        verify(ruleConfigMapper).updateRuleConfigByKey(configCaptor.capture());
        assertThat(configCaptor.getValue().getRuleId()).isEqualTo("existing-rule");
        assertThat(configCaptor.getValue().getRuleValue()).isEqualTo("ETHUSDT,SOLUSDT");
        assertThat(configCaptor.getValue().getEnabled()).isTrue();

        ArgumentCaptor<PushWatchlistConfigAuditVO> auditCaptor =
                ArgumentCaptor.forClass(PushWatchlistConfigAuditVO.class);
        verify(pushWatchlistConfigAuditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getBeforeSymbols()).isEqualTo("BTCUSDT");
        assertThat(auditCaptor.getValue().getAfterSymbols()).isEqualTo("ETHUSDT,SOLUSDT");
        assertThat(auditCaptor.getValue().getBeforeEnabled()).isFalse();
        assertThat(auditCaptor.getValue().getAfterEnabled()).isTrue();
        verify(ruleConfigMapper).findAllEnabled();

        assertThat(vo.getSymbols()).containsExactly("ETHUSDT", "SOLUSDT");
    }

    @Test
    void updatePushWatchlistConfig_allowsEmptySymbolsAsFailClosedConfig() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(null);
        when(ruleConfigMapper.insertRuleConfig(any(RuleConfigDO.class))).thenReturn(1);
        when(pushWatchlistConfigAuditMapper.insert(any(PushWatchlistConfigAuditVO.class))).thenReturn(1);
        when(ruleConfigMapper.findAllEnabled()).thenReturn(List.of());

        PushWatchlistConfigVO vo = service.updatePushWatchlistConfig(
                request(Arrays.asList(" ", null), true, "tester", "clear watchlist"));

        ArgumentCaptor<RuleConfigDO> configCaptor = ArgumentCaptor.forClass(RuleConfigDO.class);
        verify(ruleConfigMapper).insertRuleConfig(configCaptor.capture());
        assertThat(configCaptor.getValue().getRuleValue()).isEmpty();
        assertThat(vo.getSymbols()).isEmpty();
    }

    @Test
    void updatePushWatchlistConfig_requiresOperator() {
        RuleConfigServiceImpl service = service();

        assertThatThrownBy(() -> service.updatePushWatchlistConfig(
                request(List.of("BTCUSDT"), true, " ", "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operator");
        verifyNoInteractions(ruleConfigMapper, pushWatchlistConfigAuditMapper);
    }

    @Test
    void updatePushWatchlistConfig_requiresReason() {
        RuleConfigServiceImpl service = service();

        assertThatThrownBy(() -> service.updatePushWatchlistConfig(
                request(List.of("BTCUSDT"), true, "tester", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
        verifyNoInteractions(ruleConfigMapper, pushWatchlistConfigAuditMapper);
    }

    @Test
    void updatePushWatchlistConfig_doesNotSwallowAuditFailure() {
        RuleConfigServiceImpl service = service();
        when(ruleConfigMapper.findByRuleKeyIncludingDisabled(RuleConfigServiceImpl.WATCHLIST_RULE_KEY))
                .thenReturn(null);
        when(ruleConfigMapper.insertRuleConfig(any(RuleConfigDO.class))).thenReturn(1);
        when(pushWatchlistConfigAuditMapper.insert(any(PushWatchlistConfigAuditVO.class)))
                .thenThrow(new IllegalStateException("audit unavailable"));

        assertThatThrownBy(() -> service.updatePushWatchlistConfig(
                request(List.of("BTCUSDT"), true, "tester", "reason")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit unavailable");
        verify(ruleConfigMapper, never()).findAllEnabled();
    }

    private RuleConfigServiceImpl service() {
        return new RuleConfigServiceImpl(ruleConfigMapper, pushWatchlistConfigAuditMapper);
    }

    private static PushWatchlistConfigRequest request(List<String> symbols,
                                                      Boolean enabled,
                                                      String operator,
                                                      String reason) {
        PushWatchlistConfigRequest request = new PushWatchlistConfigRequest();
        request.setSymbols(symbols);
        request.setEnabled(enabled);
        request.setOperator(operator);
        request.setReason(reason);
        return request;
    }

    private static RuleConfigDO ruleConfig(String ruleValue, boolean enabled) {
        RuleConfigDO config = new RuleConfigDO();
        config.setRuleId("rule-id");
        config.setRuleType("push");
        config.setRuleKey(RuleConfigServiceImpl.WATCHLIST_RULE_KEY);
        config.setRuleValue(ruleValue);
        config.setDescription("Push watchlist symbols");
        config.setVersion("p1-watchlist");
        config.setEnabled(enabled);
        return config;
    }
}
