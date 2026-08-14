package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class V13FinalInteractionMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V13__fundamental_ai_v4_1_final_interaction_runtime.sql");
    private static final Path H2_SCHEMA = Path.of("src/main/resources/schema.sql");

    @Test
    void v13AddsOnlyCanonicalRuntimeOwnersAndFailsHistoricalRowsClosed() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ADD COLUMN analysis_mode VARCHAR(32)",
                "'ANALYSIS_PREVIEW'", "'OPPORTUNITY_DECISION'",
                "ADD COLUMN plan_lifecycle_state VARCHAR(32)",
                "'CURRENT', 'NEEDS_REVALIDATION', 'SUPERSEDED'",
                "'TRACKING_STOPPED', 'INVALIDATED', 'EXPIRED'",
                "CREATE TABLE tm_plan_revalidation_record",
                "CREATE TABLE tm_message",
                "CREATE TABLE tm_channel_delivery",
                "CREATE TABLE tm_async_task",
                "CREATE TABLE tm_event_asset_relation",
                "account_risk_coverage_state",
                "missed_reason", "later_outcome",
                "telegram_binding_status", "notification_filters_json", "default_pool_mode")
                .contains(
                        "WHEN final_plan = TRUE THEN 'NEEDS_REVALIDATION'",
                        "NOT NULL DEFAULT 'UNKNOWN'",
                        "NOT NULL DEFAULT 'UNBOUND'")
                .doesNotContain(
                        "DEFAULT 'CURRENT'", "DEFAULT 'SUCCEEDED'", "DEFAULT 'COMPLETE'",
                        "DEFAULT 'BOUND'", "DEFAULT TRUE, -- verified");
    }

    @Test
    void h2SchemaCarriesTheSameOwnersStatesAndSafetyConstraints() throws Exception {
        String sql = Files.readString(H2_SCHEMA);

        assertThat(sql).contains(
                "tm_plan_revalidation_record", "tm_message", "tm_channel_delivery",
                "tm_async_task", "tm_event_asset_relation", "analysis_mode",
                "plan_lifecycle_state", "account_risk_coverage_state",
                "not_trade_instruction", "not_order_execution");
    }
}
