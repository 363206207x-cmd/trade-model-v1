package org.example.trademodel.analysisrun;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisPersistenceIdsTest {
    private static final List<String> SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

    @Test
    void evidenceIdsAreAnalysisScopedAcrossSixAssetsAtTheSameTimestamp() {
        Set<String> ids = new HashSet<>();

        for (String symbol : SYMBOLS) {
            String id = AnalysisPersistenceIds.derivativesEvidenceId(
                    "ana-same-time-" + symbol, "DERIVATIVES_DATA_UNAVAILABLE", 0);
            assertThat(id).hasSizeLessThanOrEqualTo(64);
            ids.add(id);
        }

        assertThat(ids).hasSize(6);
    }

    @Test
    void sameAnalysisAndSequenceRemainDeterministicForIdempotentRecovery() {
        String first = AnalysisPersistenceIds.derivativesEvidenceId(
                "ana-recovery", "DERIVATIVES_DATA_UNAVAILABLE", 0);
        String second = AnalysisPersistenceIds.derivativesEvidenceId(
                "ana-recovery", "DERIVATIVES_DATA_UNAVAILABLE", 0);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void missingAnalysisIdentityFailsClosed() {
        assertThatThrownBy(() -> AnalysisPersistenceIds.derivativesEvidenceId(
                " ", "DERIVATIVES_DATA_UNAVAILABLE", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DERIVATIVES_ANALYSIS_ID_REQUIRED");
    }

    @Test
    void randomPersistenceIdsDoNotCollideAcrossSixAssets() {
        Set<String> evidenceIds = new HashSet<>();
        Set<String> scoreIds = new HashSet<>();
        Set<String> decisionIds = new HashSet<>();

        for (int i = 0; i < SYMBOLS.size(); i++) {
            evidenceIds.add(AnalysisPersistenceIds.evidenceId());
            scoreIds.add(AnalysisPersistenceIds.scoreId());
            decisionIds.add(AnalysisPersistenceIds.decisionId());
        }

        assertThat(evidenceIds).hasSize(6).allMatch(id -> id.length() <= 64);
        assertThat(scoreIds).hasSize(6).allMatch(id -> id.length() <= 64);
        assertThat(decisionIds).hasSize(6).allMatch(id -> id.length() <= 64);
    }
}
