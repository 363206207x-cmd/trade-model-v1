package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class P3AttestationValidateScriptTest {

    private static final Path VALIDATOR = Path.of(
            "scripts/p3-attestation-validate.sh").toAbsolutePath();

    @TempDir
    Path tempDir;

    private int fileSequence;

    @Test
    void duplicateSecretsRemovedKeyFailsClosed() throws Exception {
        Path attestation = writeGeneratedAttestation("SECRETS_REMOVED=YES\n");

        ScriptResult result = run(attestation, "GENERATED_RELEASE_LIKE", dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_DUPLICATE_KEY");
    }

    @Test
    void conflictingYesNoAttestationFailsClosed() throws Exception {
        Path attestation = writeGeneratedAttestation("SECRETS_REMOVED=NO\n");

        ScriptResult result = run(attestation, "GENERATED_RELEASE_LIKE", dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_DUPLICATE_KEY");
    }

    @Test
    void duplicateDataSourceClassFailsClosed() throws Exception {
        Path attestation = writeGeneratedAttestation(
                "DATA_SOURCE_CLASS=SANITIZED_RELEASE_LIKE\n");

        ScriptResult result = run(attestation, "GENERATED_RELEASE_LIKE", dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_DUPLICATE_KEY");
    }

    @Test
    void malformedGeneratedAtFailsClosed() throws Exception {
        Map<String, String> values = generatedValues();
        values.put("GENERATED_AT_UTC", "2026-07-15 12:00:00");

        ScriptResult result = run(write(values, ""), "GENERATED_RELEASE_LIKE",
                dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_GENERATED_AT_FORMAT");
    }

    @Test
    void futureGeneratedAtFailsClosed() throws Exception {
        Map<String, String> values = generatedValues();
        values.put("GENERATED_AT_UTC", "2999-01-01T00:00:00Z");

        ScriptResult result = run(write(values, ""), "GENERATED_RELEASE_LIKE",
                dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_GENERATED_AT_FUTURE");
    }

    @Test
    void attestedFlywayVersionMismatchFailsClosed() throws Exception {
        ScriptResult result = run(writeGeneratedAttestation(""),
                "GENERATED_RELEASE_LIKE", dumpList("16.14"), "7");

        assertBlocked(result, "BLOCKED_FLYWAY_VERSION_MISMATCH");
    }

    @Test
    void attestedPostgresqlVersionMismatchFailsClosed() throws Exception {
        ScriptResult result = run(writeGeneratedAttestation(""),
                "GENERATED_RELEASE_LIKE", dumpList("15.9"), "6");

        assertBlocked(result, "BLOCKED_POSTGRESQL_VERSION_MISMATCH");
    }

    @Test
    void generatedAttestationCannotPassSanitizedClass() throws Exception {
        ScriptResult result = run(writeGeneratedAttestation(""),
                "SANITIZED_RELEASE_LIKE", dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_DATA_SOURCE_CLASS");
    }

    @Test
    void sanitizedAttestationCannotPassGeneratedClass() throws Exception {
        ScriptResult result = run(write(sanitizedValues(), ""),
                "GENERATED_RELEASE_LIKE", dumpList("16.14"), "7");

        assertBlocked(result, "BLOCKED_DATA_SOURCE_CLASS");
    }

    @Test
    void attestationRawContentNeverReachesEvidence() throws Exception {
        String rawMarker = "DO_NOT_EMIT_RAW_ATTESTATION_VALUE";
        Map<String, String> values = generatedValues();
        values.put("SANITIZATION_OWNER_OR_PROCESS", rawMarker);

        ScriptResult result = run(write(values, ""), "GENERATED_RELEASE_LIKE",
                dumpList("16.14"), "6");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "ATTESTATION_VALIDATION_STATUS: PASS",
                "ATTESTATION_UNIQUENESS_STATUS: PASS",
                "ATTESTATION_RAW_CONTENT: NOT_EMITTED");
        assertThat(result.output()).doesNotContain(rawMarker, "SANITIZATION_OWNER_OR_PROCESS=");
    }

    @Test
    void unknownAttestationKeyFailsClosed() throws Exception {
        ScriptResult result = run(writeGeneratedAttestation("UNREVIEWED_KEY=VALUE\n"),
                "GENERATED_RELEASE_LIKE", dumpList("16.14"), "6");

        assertBlocked(result, "BLOCKED_UNKNOWN_KEY");
    }

    private Path writeGeneratedAttestation(String suffix) throws Exception {
        return write(generatedValues(), suffix);
    }

    private Path write(Map<String, String> values, String suffix) throws Exception {
        Path target = tempDir.resolve("attestation-" + (++fileSequence) + ".txt");
        StringBuilder content = new StringBuilder();
        values.forEach((key, value) -> content.append(key).append('=').append(value).append('\n'));
        content.append(suffix);
        Files.writeString(target, content.toString(), StandardCharsets.UTF_8);
        return target;
    }

    private Path dumpList(String version) throws Exception {
        Path target = tempDir.resolve("dump-list-" + version + ".txt");
        Files.writeString(target, ";     Dumped from database version: " + version + "\n",
                StandardCharsets.UTF_8);
        return target;
    }

    private Map<String, String> generatedValues() {
        Map<String, String> values = baseValues("GENERATED_RELEASE_LIKE", "6");
        values.put("FIXTURE_SEED", "20260715");
        values.put("REAL_USER_DATA_INCLUDED", "NO");
        values.put("REAL_ACCOUNT_DATA_INCLUDED", "NO");
        values.put("REAL_MARKET_PROVIDER_DATA_INCLUDED", "NO");
        values.put("SUITABLE_FOR_FINAL_SANITIZED_CLONE_GATE", "NO");
        return values;
    }

    private Map<String, String> sanitizedValues() {
        return baseValues("SANITIZED_RELEASE_LIKE", "7");
    }

    private Map<String, String> baseValues(String dataSourceClass, String flywayVersion) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("DATA_SOURCE_CLASS", dataSourceClass);
        values.put("SANITIZATION_OWNER_OR_PROCESS", "CONTROLLED_TEST_PROCESS");
        values.put("GENERATED_AT_UTC", Instant.now().minusSeconds(60)
                .truncatedTo(ChronoUnit.SECONDS).toString());
        values.put("SOURCE_POSTGRESQL_VERSION", "16.14");
        values.put("SOURCE_FLYWAY_VERSION", flywayVersion);
        values.put("USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED", "YES");
        values.put("SECRETS_REMOVED", "YES");
        values.put("FREE_TEXT_CLEANED_OR_REPLACED", "YES");
        values.put("LOCAL_CONTROLLED_REHEARSAL_ALLOWED", "YES");
        values.put("NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE", "YES");
        return values;
    }

    private ScriptResult run(Path attestation, String expectedClass, Path dumpList,
                             String flywayVersion) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "bash", VALIDATOR.toString(), attestation.toString(), expectedClass,
                dumpList.toString(), flywayVersion);
        builder.directory(Path.of("").toAbsolutePath().toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("attestation validator did not finish within five seconds");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ScriptResult(process.exitValue(), output);
    }

    private void assertBlocked(ScriptResult result, String reason) {
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "ATTESTATION_VALIDATION_STATUS: " + reason,
                "ATTESTATION_RAW_CONTENT: NOT_EMITTED");
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
