package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.vo.DecisionBundleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MissedOpportunityServiceImplTest {

    @Mock
    private MissedOpportunityMapper missedOpportunityMapper;

    private MissedOpportunityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MissedOpportunityServiceImpl(missedOpportunityMapper);
    }

    @Test
    void recordFromAuthoritativeAnalysis_isFrozenCompatibilityAndDoesNotInsertMissedOpportunity() {
        DecisionBundleVO decision = baseDecision();

        service.recordFromAuthoritativeAnalysisIfEligible("a-1", " BTCUSDT ", "tr-1", decision, false);

        verify(missedOpportunityMapper, never()).insert(any());
    }

    @Test
    void recordWithHotResetFlag_stillDoesNotInsertInLegacyPath() {
        DecisionBundleVO decision = baseDecision();

        service.recordFromAuthoritativeAnalysisIfEligible("a-2", "ETHUSDT", "tr-2", decision, true);

        verify(missedOpportunityMapper, never()).insert(any());
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
}
