package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSecretLeakContractTest {

    @TempDir
    Path tempDir;

    @Test
    void leakScanReportsCountWithoutPrintingSecretOrMatchedPath() throws Exception {
        Path secretDir = Files.createDirectory(tempDir.resolve("secrets"));
        String marker = "p3h-sensitive-fixture-value";
        Files.writeString(secretDir.resolve("logical-secret"), marker, StandardCharsets.UTF_8);
        Path logs = Files.createDirectory(tempDir.resolve("logs"));
        Files.writeString(logs.resolve("application.log"), "safe-prefix " + marker,
                StandardCharsets.UTF_8);

        ScriptResult result = run(secretDir, logs);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).isEqualTo("SECRET_LEAK_CANDIDATE_COUNT: 1\n");
        assertThat(result.output()).doesNotContain(marker, logs.toString());
    }

    @Test
    void cleanEvidenceReportsZero() throws Exception {
        Path secretDir = Files.createDirectory(tempDir.resolve("secrets-clean"));
        Files.writeString(secretDir.resolve("logical-secret"), "p3h-sensitive-fixture-value",
                StandardCharsets.UTF_8);
        Path logs = Files.createDirectory(tempDir.resolve("logs-clean"));
        Files.writeString(logs.resolve("application.log"), "status=200 path=/actuator/health",
                StandardCharsets.UTF_8);

        ScriptResult result = run(secretDir, logs);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo("SECRET_LEAK_CANDIDATE_COUNT: 0\n");
    }

    private ScriptResult run(Path secretDir, Path logs) throws Exception {
        Process process = new ProcessBuilder("bash", "scripts/p3h-secret-leak-check.sh",
                secretDir.toString(), logs.toString()).redirectErrorStream(true).start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
