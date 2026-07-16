package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingP3HPreflightContractTest {

    @TempDir
    Path tempDir;

    @Test
    void missingConfirmationBlocksBeforeNetworkOrSecretAccess() throws Exception {
        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(Map.of());

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
        assertThat(result.output()).doesNotContain("stage.example", "ssh-keyscan");
    }

    @Test
    void missingAttestationBlocksBeforeNetworkOrSecretAccess() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.remove("P3H_SERVER_ATTESTATION_FILE");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED");
        assertThat(result.output()).doesNotContain("ssh-keyscan", "ssh ");
    }

    @Test
    void nonStagingAttestationFailsBeforeNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path attestation = Path.of(inputs.get("P3H_SERVER_ATTESTATION_FILE"));
        String invalid = Files.readString(attestation, StandardCharsets.UTF_8)
                .replace("ENVIRONMENT_CLASS=CONTROLLED_STAGING", "ENVIRONMENT_CLASS=PRODUCTION");
        Files.writeString(attestation, invalid, StandardCharsets.UTF_8);

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION",
                "SERVER_ACCESS: NOT_ATTEMPTED");
    }

    @Test
    void productionIndicatorFailsBeforeAttestationOrNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_STAGING_HOSTNAME", "production.example.invalid");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_PRODUCTION_INDICATOR");
        assertThat(result.output()).doesNotContain(inputs.get("P3H_STAGING_HOSTNAME"));
    }
}
