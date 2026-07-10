package org.example.trademodel.derivatives;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DerivativesRuleConfigSchemaAlignmentTest {
    private static final List<String> REQUIRED_KEYS = List.of(
            "derivatives_evidence_config.oi_change_5m_weak",
            "derivatives_evidence_config.oi_change_5m_strong",
            "derivatives_evidence_config.oi_change_15m_weak",
            "derivatives_evidence_config.oi_change_15m_strong",
            "derivatives_evidence_config.funding_positive_extreme",
            "derivatives_evidence_config.funding_negative_extreme",
            "derivatives_evidence_config.long_short_long_crowding",
            "derivatives_evidence_config.long_short_short_crowding",
            "derivatives_evidence_config.liquidation_spike_5m",
            "derivatives_evidence_config.liquidation_spike_15m",
            "derivatives_evidence_config.liquidation_imbalance_ratio",
            "derivatives_evidence_config.exchange_concentration_high",
            "derivatives_decision_config.derivatives_max_data_age_seconds",
            "derivatives_decision_config.derivatives_required_for_confirm",
            "derivatives_decision_config.derivatives_minimum_dataset_count",
            "derivatives_score_config.derivatives_partial_confidence_penalty",
            "derivatives_score_config.derivatives_stale_confidence_penalty",
            "derivatives_score_config.derivatives_score_cap",
            "derivatives_score_config.derivatives_trend_score_cap",
            "derivatives_decision_config.derivatives_min_data_quality_score",
            "derivatives_decision_config.eight_score_adjustment_cap",
            "derivatives_decision_config.eight_score_adjustment_factor_percent",
            "derivatives_decision_config.derivatives_high_risk_plan_downgrade",
            "derivatives_monitor_config.refresh_seconds");

    @Test
    void h2AndPostgreSqlRuleDefaultsStayAligned() throws Exception {
        String h2 = Files.readString(Path.of("src/main/resources/schema.sql"));
        String postgres = Files.readString(Path.of(
                "src/main/resources/db/migration/V6__derivatives_business_rule_defaults.sql"));
        for (String key : REQUIRED_KEYS) {
            assertThat(h2).contains(key);
            assertThat(postgres).contains(key);
        }
        assertThat(h2).contains("'derivatives_evidence_config.exchange_concentration_high', '0.70'");
        assertThat(postgres).contains("'derivatives_evidence_config.exchange_concentration_high', '0.70'");
        assertThat(postgres).contains("ON CONFLICT (rule_key) DO UPDATE");
        assertThat(REQUIRED_KEYS).hasSize(24);
    }
}
