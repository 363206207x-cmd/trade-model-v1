package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingRebootContractTest {

    @TempDir
    Path tempDir;

    @Test
    void missingRebootAuthorizationCanNeverBecomePass() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_REBOOT_CONFIRM", "NOT_APPROVED");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_SERVER_REBOOT_EVIDENCE_NOT_RUN",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
        assertThat(result.output()).doesNotContain("SERVER_REBOOT: PASS");
    }

    @Test
    void serviceRestartCannotBeLabeledServerReboot() throws Exception {
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-staging-readonly-deployment-p3h.sh");

        assertThat(runner).contains(
                "I_CONFIRM_CONTROLLED_STAGING_SERVER_REBOOT",
                "BLOCKED_SERVER_REBOOT_EVIDENCE_NOT_RUN");
        assertThat(runner).doesNotContain("SERVER_REBOOT: PASS");
    }
}
