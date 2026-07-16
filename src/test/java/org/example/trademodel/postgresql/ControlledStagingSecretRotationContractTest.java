package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSecretRotationContractTest {

    @TempDir
    Path tempDir;

    @Test
    void completeRedactedRotationEvidencePassesWithoutSecretValues() throws Exception {
        Path evidence = tempDir.resolve("rotation.txt");
        Files.writeString(evidence, """
                OLD_VERSION_PRECHECK=PASS
                NEW_VERSION_PRE_ACTIVATION=DENIED
                NEW_VERSION_POST_ACTIVATION=PASS
                OLD_VERSION_POST_REVOCATION=DENIED
                SECRET_VALUE_EXPOSED=NO
                """, StandardCharsets.UTF_8);

        ScriptResult result = run("ADMIN", evidence);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "ADMIN_SECRET_ROTATION: PASS", "OLD_ADMIN_SECRET: REVOKED");
        assertThat(result.output()).doesNotContain("password", "fixture-secret");
    }

    @Test
    void oldSecretStillValidFailsClosed() throws Exception {
        Path evidence = tempDir.resolve("rotation-fail.txt");
        Files.writeString(evidence, """
                OLD_VERSION_PRECHECK=PASS
                NEW_VERSION_PRE_ACTIVATION=DENIED
                NEW_VERSION_POST_ACTIVATION=PASS
                OLD_VERSION_POST_REVOCATION=PASS
                SECRET_VALUE_EXPOSED=NO
                """, StandardCharsets.UTF_8);

        ScriptResult result = run("DATABASE", evidence);

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).doesNotContain("APP_DATABASE_SECRET_ROTATION: PASS");
    }

    @Test
    void extraEvidenceFieldIsRejectedWithoutPrintingItsValue() throws Exception {
        String forbiddenValue = "fixture-value-that-must-not-be-printed";
        Path evidence = tempDir.resolve("rotation-extra-field.txt");
        Files.writeString(evidence, """
                OLD_VERSION_PRECHECK=PASS
                NEW_VERSION_PRE_ACTIVATION=DENIED
                NEW_VERSION_POST_ACTIVATION=PASS
                OLD_VERSION_POST_REVOCATION=DENIED
                SECRET_VALUE_EXPOSED=NO
                UNAPPROVED_FIELD=%s
                """.formatted(forbiddenValue), StandardCharsets.UTF_8);

        ScriptResult result = run("ADMIN", evidence);

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).isEqualTo("P3H_SECRET_ROTATION: BLOCKED_EVIDENCE\n");
        assertThat(result.output()).doesNotContain(forbiddenValue);
    }

    private ScriptResult run(String mode, Path evidence) throws Exception {
        Process process = new ProcessBuilder("bash", "scripts/p3h-secret-rotation-drill.sh",
                mode, evidence.toString()).redirectErrorStream(true).start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
