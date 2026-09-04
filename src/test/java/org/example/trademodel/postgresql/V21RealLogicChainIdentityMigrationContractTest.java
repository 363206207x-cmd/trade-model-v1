package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class V21RealLogicChainIdentityMigrationContractTest {

    @Test
    void migrationAddsDurableTaskMonitorAndDecisionPlanIdentity() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V21__real_logic_chain_identity.sql"));

        assertThat(sql)
                .contains("idempotency_key")
                .contains("result_resource_id")
                .contains("monitor_run_key")
                .contains("decision_id")
                .contains("uk_tm_async_task_idempotency_key")
                .contains("uk_tm_position_monitor_log_run_key")
                .contains("idx_tm_execution_plan_decision_identity");
    }
}
