package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class V15DecisionContractMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V15__v4_1_machine_executable_decision_contract.sql");

    @Test
    void v15AddsMachineExecutableDecisionFieldsWithoutRecreatingOwnedObjects() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ALTER TABLE tm_decision_result",
                "validated_market_bias VARCHAR(32)",
                "direction_data_state VARCHAR(32)",
                "data_quality_score INT",
                "evidence_reliability INT",
                "opportunity_score INT",
                "risk_score INT",
                "final_confidence INT",
                "one_hour_opportunity_quality INT",
                "four_hour_trend_alignment INT",
                "normalization_version VARCHAR(64)",
                "score_version VARCHAR(64)",
                "data_quality_version VARCHAR(64)",
                "provider_matrix_version VARCHAR(64)",
                "idx_tm_decision_home_contract",
                "DROP CONSTRAINT ck_tm_execution_plan_final_boundary",
                "final_plan_mode = 'OBSERVATION'",
                "entry_zone IS NULL",
                "expected_risk_reward IS NULL",
                "entry_logic_status IN ('STILL_VALID', 'WEAKENED', 'INVALIDATED', 'NOT_APPLICABLE')")
                .doesNotContain(
                        "CREATE TABLE tm_decision_result",
                        "CREATE TABLE tm_position_monitor_log",
                        "DROP TABLE");
    }

    @Test
    void h2SchemaCarriesTheSameDecisionAndManualPositionContract() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));

        for (String field : List.of(
                "validated_market_bias", "direction_data_state", "data_quality_score",
                "evidence_reliability", "opportunity_score", "risk_score", "final_confidence",
                "one_hour_opportunity_quality", "four_hour_trend_alignment",
                "normalization_version", "score_version", "data_quality_version",
                "provider_matrix_version")) {
            assertThat(schema).contains(field);
        }
        assertThat(schema).contains(
                "CONSTRAINT ck_tm_decision_validated_bias",
                "CONSTRAINT ck_tm_decision_direction_data_state",
                "CONSTRAINT uk_tm_user_position_id_user UNIQUE (id, user_id)",
                "FOREIGN KEY (user_position_id, user_id) REFERENCES tm_user_position(id, user_id)",
                "CREATE INDEX IF NOT EXISTS idx_tm_decision_home_contract",
                "final_plan_mode = 'OBSERVATION'",
                "leverage_suggestion IS NULL",
                "'NOT_APPLICABLE'")
                .doesNotContain("CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_user_position_id_user");
    }
}
