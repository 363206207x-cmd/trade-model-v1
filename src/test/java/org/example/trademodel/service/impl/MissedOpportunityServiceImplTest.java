package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.vo.DecisionBundleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MissedOpportunityServiceImplTest {

    @Mock
    private MissedOpportunityMapper missedOpportunityMapper;
    @Mock
    private RealPositionMapper realPositionMapper;

    private MissedOpportunityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MissedOpportunityServiceImpl(missedOpportunityMapper, realPositionMapper);
    }

    @Test
    void recordEligible_insertsMissedOpportunity() {
        DecisionBundleVO decision = baseDecision();
        when(realPositionMapper.countOpenPositionsBySymbol("BTCUSDT")).thenReturn(0);
        when(missedOpportunityMapper.listByDecisionId("d-1")).thenReturn(Collections.emptyList());

        service.recordFromAuthoritativeAnalysisIfEligible("a-1", " BTCUSDT ", "tr-1", decision, false);

        ArgumentCaptor<MissedOpportunityDO> captor = ArgumentCaptor.forClass(MissedOpportunityDO.class);
        verify(missedOpportunityMapper).insert(captor.capture());
        MissedOpportunityDO row = captor.getValue();
        assertThat(row.getAnalysisId()).isEqualTo("a-1");
        assertThat(row.getDecisionId()).isEqualTo("d-1");
        assertThat(row.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(row.getRuleVersion()).isEqualTo("missed-v1");
        assertThat(row.getReasonJson()).contains("\"category\":\"ordinary_missed\"");
        assertThat(row.getReasonJson()).contains("\"hotResetWouldFire\":false");
    }

    @Test
    void recordWithOpenPosition_skipsInsert() {
        DecisionBundleVO decision = baseDecision();
        when(realPositionMapper.countOpenPositionsBySymbol("ETHUSDT")).thenReturn(1);

        service.recordFromAuthoritativeAnalysisIfEligible("a-2", "ETHUSDT", "tr-2", decision, false);

        verify(missedOpportunityMapper, never()).insert(any());
    }

    @Test
    void recordWhenConfusedScoreAtThreshold_insertsGovernanceMissedAudit() {
        DecisionBundleVO decision = baseDecision();
        decision.setConfusedScore(70);
        when(realPositionMapper.countOpenPositionsBySymbol("BTCUSDT")).thenReturn(0);
        when(missedOpportunityMapper.listByDecisionId("d-1")).thenReturn(Collections.emptyList());

        service.recordFromAuthoritativeAnalysisIfEligible("a-confused", "BTCUSDT", "tr-confused", decision, false);

        ArgumentCaptor<MissedOpportunityDO> captor = ArgumentCaptor.forClass(MissedOpportunityDO.class);
        verify(missedOpportunityMapper).insert(captor.capture());
        MissedOpportunityDO row = captor.getValue();
        assertThat(row.getRuleVersion()).isEqualTo("governance-missed-v1");
        assertThat(row.getReasonJson()).contains("\"category\":\"governance_missed\"");
        assertThat(row.getReasonJson()).contains("\"governanceReasonCode\":\"GOVERNANCE_CONFUSED_SCORE_BLOCK\"");
        assertThat(row.getReasonJson()).contains("\"governanceSourceState\":\"\"");
    }

    @Test
    void recordWhenGovernanceAssetStateBlocks_insertsGovernanceMissedAudit() {
        assertGovernanceBlockedStateRecorded(
                AssetStateEnum.CONFUSED, "GOVERNANCE_ASSET_STATE_CONFUSED");
        assertGovernanceBlockedStateRecorded(
                AssetStateEnum.HIGH_RISK, "GOVERNANCE_ASSET_STATE_HIGH_RISK");
        assertGovernanceBlockedStateRecorded(
                AssetStateEnum.INVALIDATED, "GOVERNANCE_ASSET_STATE_INVALIDATED");
        assertGovernanceBlockedStateRecorded(
                AssetStateEnum.COOLING, "GOVERNANCE_ASSET_STATE_COOLING");
    }

    @Test
    void recordWhenHotResetWouldFire_skipsInsert() {
        DecisionBundleVO decision = baseDecision();

        service.recordFromAuthoritativeAnalysisIfEligible("a-hot", "BTCUSDT", "tr-hot", decision, true);

        verify(missedOpportunityMapper, never()).insert(any());
    }

    @Test
    void recordWhenNotWorthOpening_skipsInsert() {
        DecisionBundleVO decision = baseDecision();
        decision.setIsWorthOpening(false);

        service.recordFromAuthoritativeAnalysisIfEligible("a-noworth", "BTCUSDT", "tr-noworth", decision, false);

        verify(missedOpportunityMapper, never()).insert(any());
    }

    @Test
    void recordWhenDecisionAlreadyExists_skipsDuplicateInsert() {
        DecisionBundleVO decision = baseDecision();
        when(realPositionMapper.countOpenPositionsBySymbol("BTCUSDT")).thenReturn(0);
        when(missedOpportunityMapper.listByDecisionId("d-1")).thenReturn(List.of(new MissedOpportunityDO()));

        service.recordFromAuthoritativeAnalysisIfEligible("a-dupe", "BTCUSDT", "tr-dupe", decision, false);

        verify(missedOpportunityMapper, never()).insert(any());
    }

    @Test
    void governanceMissedShouldNotUseOrdinaryVersion() {
        DecisionBundleVO decision = baseDecision();
        decision.setAssetState(AssetStateEnum.HIGH_RISK);
        when(realPositionMapper.countOpenPositionsBySymbol("BTCUSDT")).thenReturn(0);
        when(missedOpportunityMapper.listByDecisionId("d-1")).thenReturn(Collections.emptyList());

        service.recordFromAuthoritativeAnalysisIfEligible("a-governance", "BTCUSDT", "tr-governance", decision, false);

        ArgumentCaptor<MissedOpportunityDO> captor = ArgumentCaptor.forClass(MissedOpportunityDO.class);
        verify(missedOpportunityMapper).insert(captor.capture());
        MissedOpportunityDO row = captor.getValue();
        assertThat(row.getRuleVersion()).isEqualTo("governance-missed-v1");
        assertThat(row.getRuleVersion()).isNotEqualTo("missed-v1");
        assertThat(row.getReasonJson()).contains("\"category\":\"governance_missed\"");
    }

    @Test
    void recordWhenPositionQueryThrows_treatsAsNoPositionAndInserts() {
        DecisionBundleVO decision = baseDecision();
        when(realPositionMapper.countOpenPositionsBySymbol("SOLUSDT")).thenThrow(new RuntimeException("db error"));
        when(missedOpportunityMapper.listByDecisionId("d-1")).thenReturn(Collections.emptyList());

        service.recordFromAuthoritativeAnalysisIfEligible("a-3", "SOLUSDT", "tr-3", decision, false);

        verify(missedOpportunityMapper).insert(any(MissedOpportunityDO.class));
    }

    @Test
    void query_normalizesInputsAndSanitizesLimit() {
        LocalDate date = LocalDate.now();
        when(missedOpportunityMapper.listByQuery(eq("a-9"), eq("BTCUSDT"), eq(date), eq(200)))
                .thenReturn(List.of(new MissedOpportunityDO()));

        List<MissedOpportunityDO> rows = service.query("  a-9  ", " btcusdt ", date, 999);

        assertThat(rows).hasSize(1);
        verify(missedOpportunityMapper).listByQuery("a-9", "BTCUSDT", date, 200);
    }

    private static DecisionBundleVO baseDecision() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setDecisionId("d-1");
        decision.setIsWorthOpening(true);
        decision.setAssetState(AssetStateEnum.CANDIDATE);
        decision.setConfusedScore(21);
        decision.setMultiTimeframeAligned(true);
        return decision;
    }

    private void assertGovernanceBlockedStateRecorded(AssetStateEnum blocked, String expectedReasonCode) {
        clearInvocations(missedOpportunityMapper, realPositionMapper);
        DecisionBundleVO decision = baseDecision();
        decision.setDecisionId("d-" + blocked.name());
        decision.setAssetState(blocked);
        decision.setConfusedScore(10);
        when(realPositionMapper.countOpenPositionsBySymbol("BTCUSDT")).thenReturn(0);
        when(missedOpportunityMapper.listByDecisionId("d-" + blocked.name())).thenReturn(Collections.emptyList());

        service.recordFromAuthoritativeAnalysisIfEligible(
                "a-" + blocked.name(), "BTCUSDT", "tr-" + blocked.name(), decision, false);

        ArgumentCaptor<MissedOpportunityDO> captor = ArgumentCaptor.forClass(MissedOpportunityDO.class);
        verify(missedOpportunityMapper, times(1)).insert(captor.capture());
        MissedOpportunityDO row = captor.getValue();
        assertThat(row.getRuleVersion()).isEqualTo("governance-missed-v1");
        assertThat(row.getReasonJson()).contains("\"category\":\"governance_missed\"");
        assertThat(row.getReasonJson()).contains("\"governanceReasonCode\":\"" + expectedReasonCode + "\"");
        assertThat(row.getReasonJson()).contains("\"governanceSourceState\":\"" + blocked.name() + "\"");
    }
}
