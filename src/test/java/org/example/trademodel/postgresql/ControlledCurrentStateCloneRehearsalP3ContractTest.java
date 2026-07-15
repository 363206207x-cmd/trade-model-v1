package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledCurrentStateCloneRehearsalP3ContractTest {

    private static final Path RUNNER = Path.of(
            "scripts/controlled-current-state-clone-rehearsal-p3.sh").toAbsolutePath();
    private static final Set<String> P3_ENVIRONMENT = Set.of(
            "P3_SANITIZED_DUMP_FILE",
            "P3_SANITIZATION_ATTESTATION_FILE",
            "P3_DATASET_ID",
            "P3_DATASET_CLASS",
            "P3_CONFIRM",
            "P3_LOCAL_DB_RECREATE_CONFIRM",
            "P3_TARGET_HOST",
            "P3_TARGET_PORT",
            "P3_SOURCE_DATABASE",
            "P3_REHEARSAL_DATABASE",
            "P3_RECOVERY_DATABASE");

    @TempDir
    Path tempDir;

    @Test
    void missingDumpBlocksBeforeDockerOrDatabaseAccess() throws Exception {
        ScriptResult result = run(Map.of());

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "SOURCE_DATASET_STATUS: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP",
                "P3_RESULT: BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP",
                "DATABASE_ACCESS: NOT_ATTEMPTED",
                "DOCKER_ACTION: NOT_ATTEMPTED",
                "PRODUCTION_READINESS: BLOCKED");
    }

    @Test
    void relativeDumpPathFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.put("P3_SANITIZED_DUMP_FILE", "relative.dump");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3_RESULT: BLOCKED_INVALID_DUMP_PATH");
    }

    @Test
    void missingAttestationFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.remove("P3_SANITIZATION_ATTESTATION_FILE");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3_RESULT: BLOCKED_MISSING_SANITIZATION_ATTESTATION");
    }

    @Test
    void productionStyleDatabaseNameFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.put("P3_SOURCE_DATABASE", "production_primary");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3_RESULT: BLOCKED_UNAPPROVED_DATABASE_NAME");
    }

    @Test
    void nonLocalhostTargetFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.put("P3_TARGET_HOST", "staging-db.internal");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3_RESULT: BLOCKED_NON_LOCALHOST_TARGET");
    }

    @Test
    void nonAllowedDatabaseNameFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.put("P3_RECOVERY_DATABASE", "trade_model_v1_other_recovery");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3_RESULT: BLOCKED_UNAPPROVED_DATABASE_NAME");
    }

    @Test
    void missingLocalRecreateConfirmationFailsClosed() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.remove("P3_LOCAL_DB_RECREATE_CONFIRM");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "P3_RESULT: BLOCKED_LOCAL_RECREATE_CONFIRMATION_REQUIRED");
    }

    @Test
    void productionLikeDatasetIdFailsClosedWithoutEchoingIt() throws Exception {
        Map<String, String> environment = completeInputContract();
        environment.put("P3_DATASET_ID", "production-customer-host");

        ScriptResult result = run(environment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "P3_RESULT: BLOCKED_SENSITIVE_OR_PRODUCTION_LIKE_DATASET_ID");
        assertThat(result.output()).doesNotContain("production-customer-host");
    }

    @Test
    void backupChecksumAndRestoreFingerprintComparisonAreMandatory() throws Exception {
        String script = Files.readString(RUNNER);

        assertThat(script).contains(
                "BACKUP_SHA256=\"$(sha256_file \"${BACKUP_FILE}\")\"",
                "SOURCE_TO_RECOVERY_FINGERPRINT: MATCH",
                "RESTORE_DATA_INTEGRITY_MISMATCH",
                "cmp -s \"${TMP_DIR}/source-verification.txt\"");
    }

    @Test
    void containerCleanupIsRegisteredForSuccessAndFailurePaths() throws Exception {
        String script = Files.readString(RUNNER);

        assertThat(script).contains(
                "trap cleanup EXIT",
                "trap 'exit 130' INT",
                "trap 'exit 143' TERM",
                "docker rm -f \"${CONTAINER_NAME}\"",
                "CONTAINER_CLEANUP=\"PASS\"",
                "BLOCKED_CONTAINER_CLEANUP_FAILED");
    }

    @Test
    void containerQueriesAndAggregateCapturesAreBounded() throws Exception {
        String script = Files.readString(RUNNER);

        assertThat(script).contains(
                "run_bounded 180 docker exec -i",
                "run_bounded 300 docker run",
                "run_bounded 600 run_flyway_action",
                "run_bounded 600 env");
    }

    @Test
    void attestationContentAndPasswordsNeverReachBlockedOutput() throws Exception {
        String secretMarker = "DO_NOT_LEAK_ATTESTATION_VALUE";
        Path attestation = validAttestation(secretMarker);
        Map<String, String> environment = completeInputContract();
        environment.put("P3_SANITIZATION_ATTESTATION_FILE", attestation.toString());
        environment.put("P3_CONFIRM", "wrong-confirmation-" + secretMarker);

        ScriptResult result = run(environment);

        assertThat(result.output()).doesNotContain(secretMarker);
        assertThat(result.output()).doesNotContain("PASSWORD", "JDBC_URL", "response body");
    }

    @Test
    void runnerForbidsUnsafeFlywayCommandsV8AndProductionUrls() throws Exception {
        String script = Files.readString(RUNNER).toLowerCase();

        assertThat(script).doesNotContain(
                "flyway clean",
                "flyway repair",
                "flyway baseline",
                "v8__",
                "jdbc:postgresql://production",
                "jdbc:postgresql://prod");
        assertThat(script).contains(
                "127.0.0.1",
                "trade_model_v1_p3_source",
                "trade_model_v1_p3_rehearsal",
                "trade_model_v1_p3_recovery");
    }

    @Test
    void restoreUsesOwnerAclAndErrorSafetyFlags() throws Exception {
        String restoreScript = Files.readString(Path.of("scripts/prod-restore.sh"));

        assertThat(restoreScript).contains("--no-owner", "--no-acl", "--exit-on-error");
    }

    @Test
    void aggregateSqlNeverReturnsBusinessRowsOrFreeTextValues() throws Exception {
        String fingerprint = Files.readString(Path.of("scripts/current-state-clone-fingerprint.sql"));
        String verification = Files.readString(
                Path.of("scripts/current-state-clone-restore-verification.sql"));

        assertThat(fingerprint).contains("COUNT(*)", "BEGIN TRANSACTION READ ONLY");
        assertThat(verification).contains(
                "SECRET_CANDIDATE_TOTAL",
                "PII_CANDIDATE_TOTAL",
                "PRODUCTION_REFERENCE_CANDIDATE_TOTAL",
                "BEGIN TRANSACTION READ ONLY");
        assertThat(fingerprint).doesNotContain("entry_price", "quantity", "source_ref_id");
        assertThat(verification).doesNotContain("SELECT *");
    }

    @Test
    void historicalInventoryAllowsOnlyTheFixedP3SourceNameAddition() throws Exception {
        String wrapper = Files.readString(Path.of("scripts/historical-time-basis-inventory.sh"));

        assertThat(wrapper).contains("trade_model_v1_p3_source");
        assertThat(wrapper).contains("BLOCKED_PRODUCTION_INDICATOR");
    }

    private Map<String, String> completeInputContract() throws IOException {
        Path dump = tempDir.resolve("sanitized-release-like.dump");
        Files.write(dump, "not-a-real-dump".getBytes(StandardCharsets.UTF_8));
        Path attestation = validAttestation("controlled-process");

        Map<String, String> environment = new HashMap<>();
        environment.put("P3_SANITIZED_DUMP_FILE", dump.toString());
        environment.put("P3_SANITIZATION_ATTESTATION_FILE", attestation.toString());
        environment.put("P3_DATASET_ID", "p3-contract-fixture");
        environment.put("P3_DATASET_CLASS", "SANITIZED_RELEASE_LIKE");
        environment.put("P3_CONFIRM", "I_CONFIRM_SANITIZED_NON_PRODUCTION_RELEASE_LIKE_DATASET");
        environment.put("P3_LOCAL_DB_RECREATE_CONFIRM",
                "I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED");
        return environment;
    }

    private Path validAttestation(String owner) throws IOException {
        Path attestation = tempDir.resolve("attestation-" + Math.abs(owner.hashCode()) + ".txt");
        Files.writeString(attestation, """
                DATA_SOURCE_CLASS=SANITIZED_RELEASE_LIKE
                SANITIZATION_OWNER_OR_PROCESS=%s
                GENERATED_AT_UTC=2026-07-15T00:00:00Z
                SOURCE_POSTGRESQL_VERSION=16
                SOURCE_FLYWAY_VERSION=7
                USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED=YES
                SECRETS_REMOVED=YES
                FREE_TEXT_CLEANED_OR_REPLACED=YES
                LOCAL_CONTROLLED_REHEARSAL_ALLOWED=YES
                NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE=YES
                """.formatted(owner));
        return attestation;
    }

    private ScriptResult run(Map<String, String> additions) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", RUNNER.toString());
        builder.directory(Path.of("").toAbsolutePath().toFile());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        P3_ENVIRONMENT.forEach(environment::remove);
        environment.putAll(additions);

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("P3 contract preflight did not finish within 10 seconds");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ScriptResult(process.exitValue(), output);
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
