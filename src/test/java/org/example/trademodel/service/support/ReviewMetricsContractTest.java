package org.example.trademodel.service.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("core-regression")
class ReviewMetricsContractTest {

    @Test
    void readyMetricsRequireTheCompleteFrozenMetricSet() {
        assertThatThrownBy(() -> ReviewMetricsContract.normalizeOrThrow("""
                {"schemaVersion":"FUNDAMENTAL_AI_V4_1_REVIEW_METRICS","dataState":"READY",
                 "evidenceTraceabilityRate":1.0}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("structuredOutputCompletenessRate is required");
    }

    @Test
    void insufficientDataPreservesEveryMetricKeyWithoutFakeZeroes() {
        String normalized = ReviewMetricsContract.normalizeOrThrow(insufficientDataJson());

        assertThat(normalized)
                .contains("\"dataState\":\"INSUFFICIENT_DATA\"")
                .contains("\"GPT_FINAL\":null")
                .contains("\"CONFIRMATION\":null")
                .contains("\"MISSED_VALID\":null")
                .doesNotContain("NaN");
    }

    @Test
    void readyMetricsKeepRolePlanModeAndOmissionStatisticsSeparated() {
        String normalized = ReviewMetricsContract.normalizeOrThrow(readyJson());

        assertThat(normalized)
                .contains("\"byRole\"")
                .contains("\"byPlanMode\"")
                .contains("\"failurePathHitRate\":0.5")
                .contains("\"BLOCKED_BY_RISK_VALID\":2");
    }

    @Test
    void invalidRatesAndCountsFailClosed() {
        assertThatThrownBy(() -> ReviewMetricsContract.normalizeOrThrow(
                readyJson().replace("\"fabricatedFillRate\":0.0", "\"fabricatedFillRate\":1.1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fabricatedFillRate");
        assertThatThrownBy(() -> ReviewMetricsContract.normalizeOrThrow(
                readyJson().replace("\"falsePositiveCount\":1", "\"falsePositiveCount\":-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("falsePositiveCount");
    }

    private static String readyJson() {
        return """
                {
                  "schemaVersion":"FUNDAMENTAL_AI_V4_1_REVIEW_METRICS",
                  "dataState":"READY",
                  "evidenceTraceabilityRate":1.0,
                  "structuredOutputCompletenessRate":{
                    "byRole":{"GPT_FINAL":1.0,"GEMINI_REVIEW":0.9,"GROK_CHALLENGE":0.8},
                    "byPlanMode":{"CONFIRMATION":1.0,"REDUCED":0.9,"PREPARATION":0.8,"OBSERVATION":0.7}
                  },
                  "unsupportedConclusionRate":0.0,
                  "fabricatedFillRate":0.0,
                  "confidenceCalibration":0.92,
                  "falsePositiveCount":1,
                  "falseNegativeCount":2,
                  "missedValidOpportunityCount":3,
                  "planModeEffectiveness":{"CONFIRMATION":0.9,"REDUCED":0.8,"PREPARATION":0.7,"OBSERVATION":0.6},
                  "effectiveDowngradeRate":0.75,
                  "failurePathHitRate":0.5,
                  "opportunityOmissionQuality":{"MISSED_VALID":3,"PUSHED_NOT_FILLED_VALID":1,"BLOCKED_BY_RISK_VALID":2}
                }
                """;
    }

    private static String insufficientDataJson() {
        return """
                {
                  "schemaVersion":"FUNDAMENTAL_AI_V4_1_REVIEW_METRICS",
                  "dataState":"INSUFFICIENT_DATA",
                  "evidenceTraceabilityRate":null,
                  "structuredOutputCompletenessRate":{
                    "byRole":{"GPT_FINAL":null,"GEMINI_REVIEW":null,"GROK_CHALLENGE":null},
                    "byPlanMode":{"CONFIRMATION":null,"REDUCED":null,"PREPARATION":null,"OBSERVATION":null}
                  },
                  "unsupportedConclusionRate":null,
                  "fabricatedFillRate":null,
                  "confidenceCalibration":null,
                  "falsePositiveCount":null,
                  "falseNegativeCount":null,
                  "missedValidOpportunityCount":null,
                  "planModeEffectiveness":{"CONFIRMATION":null,"REDUCED":null,"PREPARATION":null,"OBSERVATION":null},
                  "effectiveDowngradeRate":null,
                  "failurePathHitRate":null,
                  "opportunityOmissionQuality":{"MISSED_VALID":null,"PUSHED_NOT_FILLED_VALID":null,"BLOCKED_BY_RISK_VALID":null}
                }
                """;
    }
}
