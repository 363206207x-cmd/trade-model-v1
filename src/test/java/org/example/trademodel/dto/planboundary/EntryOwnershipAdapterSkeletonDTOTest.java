package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntryOwnershipAdapterSkeletonDTOTest {

    @Test
    void ruleOwnedEntryCandidateCarriesRequiredFieldsWithoutDerivingValues() {
        RuleOwnedEntryCandidateDTO candidate = new RuleOwnedEntryCandidateDTO();
        candidate.setSymbol("BTCUSDT");
        candidate.setDecisionTimeframe("15m");
        candidate.setCandidateEntryBoundary(new BigDecimal("1.23"));
        candidate.setEntrySourceType("rule-owned-boundary");
        candidate.setEntrySourceTimeframe("15m");
        candidate.setEntrySourceReason("fixture-only");
        candidate.setEntrySourceRef("source-ref");
        candidate.setRuleId("entry-rule");
        candidate.setRuleVersion("v1");
        candidate.setSourceWindow("2026-05-18T00:00:00Z/2026-05-18T00:15:00Z");

        assertThat(candidate.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(candidate.getDecisionTimeframe()).isEqualTo("15m");
        assertThat(candidate.getCandidateEntryBoundary()).isEqualByComparingTo("1.23");
        assertThat(candidate.getEntrySourceType()).isEqualTo("rule-owned-boundary");
        assertThat(candidate.getEntrySourceTimeframe()).isEqualTo("15m");
        assertThat(candidate.getEntrySourceReason()).isEqualTo("fixture-only");
        assertThat(candidate.getEntrySourceRef()).isEqualTo("source-ref");
        assertThat(candidate.getRuleId()).isEqualTo("entry-rule");
        assertThat(candidate.getRuleVersion()).isEqualTo("v1");
        assertThat(candidate.getSourceWindow()).isEqualTo("2026-05-18T00:00:00Z/2026-05-18T00:15:00Z");
    }

    @Test
    void freshnessMetadataCarriesRequiredFieldsAndCopiesMissingFields() {
        EntrySourceFreshnessDTO freshness = new EntrySourceFreshnessDTO();
        List<String> missingFields = List.of("observedAtMs");
        freshness.setFreshnessStatus("MISSING");
        freshness.setStaleReasonCode("ENTRY_SOURCE_MISSING_OBSERVED_AT");
        freshness.setStaleReasonText("observedAtMs must be present before ownership resolution");
        freshness.setObservedAtMs(100L);
        freshness.setDecisionCreateTimeMs(200L);
        freshness.setMissingFields(missingFields);

        assertThat(freshness.getFreshnessStatus()).isEqualTo("MISSING");
        assertThat(freshness.getStaleReasonCode()).isEqualTo("ENTRY_SOURCE_MISSING_OBSERVED_AT");
        assertThat(freshness.getStaleReasonText())
                .isEqualTo("observedAtMs must be present before ownership resolution");
        assertThat(freshness.getObservedAtMs()).isEqualTo(100L);
        assertThat(freshness.getDecisionCreateTimeMs()).isEqualTo(200L);
        assertThat(freshness.getMissingFields()).containsExactly("observedAtMs");
        assertThat(freshness.getMissingFields()).isNotSameAs(missingFields);
    }

    @Test
    void conflictFlagsDefaultToNullInsteadOfFalse() {
        EntrySourceConflictDTO conflict = new EntrySourceConflictDTO();

        assertThat(conflict.getConflictsWithStop()).isNull();
        assertThat(conflict.getConflictsWithTakeProfit()).isNull();
        assertThat(conflict.getConflictsWithRiskReward()).isNull();
        assertThat(conflict.getConflictsWithLiquidity()).isNull();
        assertThat(conflict.getConflictsWithMultiTimeframe()).isNull();
        assertThat(conflict.getConflictsWithEvent()).isNull();
        assertThat(conflict.getConflictsWithWick()).isNull();
    }

    @Test
    void nullableConflictFlagsDistinguishMissingEvaluationFromExplicitNonConflict() {
        EntrySourceConflictDTO conflict = new EntrySourceConflictDTO();
        conflict.setConflictsWithStop(Boolean.FALSE);
        conflict.setConflictsWithTakeProfit(Boolean.TRUE);
        conflict.setConflictReasons(List.of("fixture-conflict"));
        conflict.setMissingFields(List.of("conflictsWithRiskReward"));

        assertThat(conflict.getConflictsWithStop()).isFalse();
        assertThat(conflict.getConflictsWithTakeProfit()).isTrue();
        assertThat(conflict.getConflictsWithRiskReward()).isNull();
        assertThat(conflict.getConflictReasons()).containsExactly("fixture-conflict");
        assertThat(conflict.getMissingFields()).containsExactly("conflictsWithRiskReward");
    }

    @Test
    void requestKeepsRuntimeKlineContextSeparateFromRuleOwnedCandidate() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("15m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("100.00"));

        RuleOwnedEntryCandidateDTO candidate = new RuleOwnedEntryCandidateDTO();
        candidate.setSymbol("BTCUSDT");
        candidate.setDecisionTimeframe("15m");
        candidate.setCandidateEntryBoundary(new BigDecimal("1.23"));

        EntrySourceFreshnessDTO freshness = new EntrySourceFreshnessDTO();
        EntrySourceConflictDTO conflict = new EntrySourceConflictDTO();
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        request.setRuntimeKlineContext(runtimeKlineContext);
        request.setRuleOwnedEntryCandidate(candidate);
        request.setFreshness(freshness);
        request.setConflict(conflict);

        assertThat(request.getRuntimeKlineContext()).isSameAs(runtimeKlineContext);
        assertThat(request.getRuleOwnedEntryCandidate()).isSameAs(candidate);
        assertThat(request.getFreshness()).isSameAs(freshness);
        assertThat(request.getConflict()).isSameAs(conflict);
        assertThat(request.getRuntimeKlineContext().getLatestPrice())
                .isNotEqualByComparingTo(request.getRuleOwnedEntryCandidate().getCandidateEntryBoundary());
        assertThat(request.isManualReviewRequired()).isTrue();
        assertThat(request.isNotTradeInstruction()).isTrue();
    }
}
