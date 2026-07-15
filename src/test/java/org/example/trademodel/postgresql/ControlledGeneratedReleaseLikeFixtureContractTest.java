package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGeneratedReleaseLikeFixtureContractTest {

    private static final Path GENERATOR = Path.of(
            "scripts/generate-p3-release-like-fixture.sh");
    private static final Path FIXTURE_SQL = Path.of(
            "scripts/p3-generated-fixture-data.sql");
    private static final Path VERIFICATION_SQL = Path.of(
            "scripts/p3-generated-fixture-verification.sql");

    @Test
    void generatorUsesFixedSeedPinnedPostgresqlAndLocalDisposableTarget() throws Exception {
        String script = Files.readString(GENERATOR);

        assertThat(script).contains(
                "SEED=\"${P3_GENERATED_FIXTURE_SEED:-20260715}\"",
                "postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc",
                "HOST=\"127.0.0.1\"",
                "PORT=\"55434\"",
                "DATABASE=\"trade_model_v1_p3_generated_source\"",
                "--pull never",
                "P3_GENERATED_FLYWAY_CONFIRM=\"I_CONFIRM_LOCAL_GENERATED_P3_V6_SCHEMA\"",
                "-Dtest=ControlledGeneratedReleaseLikeFixtureFlywayTest");
        assertThat(script).doesNotContain("curl ", "wget ", "--pull always");
    }

    @Test
    void generatedFixtureCoversRequiredReleaseLikeRelationshipsAndSafetyFlags() throws Exception {
        String fixture = Files.readString(FIXTURE_SQL);
        String verification = Files.readString(VERIFICATION_SQL);
        String generator = Files.readString(GENERATOR);

        assertThat(fixture).contains(
                "20260715",
                "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT",
                "EXECUTION_PLAN:P3P-BTCUSDT-001-A",
                "ANALYSIS:P3A-ETHUSDT-002",
                "EXECUTION_PLAN:P3P-XRPUSDT-001-A",
                "P3P-BTCUSDT-001-B",
                "PLAN_REVALIDATION_REQUIRED",
                "PLAN_BOUNDARY_INCOMPLETE",
                "2026-07-13T00:00:00Z ~ 2026-07-13T01:00:00Z",
                "generate_series(1, 50)",
                "not_trade_instruction", "not_executable", "not_auto_trading",
                "not_order_execution", "not_position_mutation");
        assertThat(verification).contains(
                "ANALYSIS_TOTAL",
                "DECISION_TOTAL",
                "EXECUTION_PLAN_TOTAL",
                "USER_POSITION_TOTAL",
                "OHLCV_TOTAL",
                "EXECUTION_PLAN_UNSAFE",
                "POSITION_MONITOR_REVALIDATION_REASON",
                "POSITION_MONITOR_BOUNDARY_REASON",
                "POSITION_MONITOR_SIBLING_B_REFERENCE",
                "POSITION_MONITOR_UNVERIFIED_SOURCE",
                "AI_REQUIRED_STATE_MARKER_COUNT",
                "AI_CALL_LOG_UNSAFE");
        assertThat(generator).contains(
                "require_metric ANALYSIS_TOTAL 138",
                "require_metric DECISION_TOTAL 120",
                "require_metric EXECUTION_PLAN_TOTAL 121",
                "require_metric ASSET_STATE_TOTAL 9",
                "require_metric ASSET_STATE_DISTINCT_STATUS 8",
                "require_metric USER_POSITION_TOTAL 7",
                "require_metric POSITION_MONITOR_REVALIDATION_REASON 1",
                "require_metric POSITION_MONITOR_BOUNDARY_REASON 1",
                "require_metric POSITION_MONITOR_SIBLING_B_REFERENCE 0",
                "require_metric AI_REQUIRED_STATE_MARKER_COUNT 5",
                "require_metric OHLCV_TOTAL 1200");
    }

    @Test
    void generatorRequiresAggregateSanitizationChecksAndStableFingerprint() throws Exception {
        String script = Files.readString(GENERATOR);

        assertThat(script).contains(
                "current-state-clone-restore-verification.sql",
                "PII_CANDIDATE_TOTAL PRODUCTION_REFERENCE_CANDIDATE_TOTAL",
                "require_metric \"${key}\" 0",
                "current-state-clone-fingerprint.sql",
                "fingerprint-first.txt",
                "fingerprint-second.txt",
                "cmp -s",
                "BLOCKED_NONDETERMINISTIC_GENERATED_FINGERPRINT");
    }

    @Test
    void generatorPassesSqlFilesExplicitlyIntoBoundedBackgroundProcesses() throws Exception {
        String script = Files.readString(GENERATOR);

        assertThat(script).contains(
                "run_bounded_with_input 180 \"${ROOT_DIR}/scripts/p3-generated-fixture-data.sql\"",
                "run_bounded_with_input 180 \"${ROOT_DIR}/scripts/p3-generated-fixture-verification.sql\"",
                "\"$@\" <\"${input_file}\" &",
                "wait_for_bounded_pid \"$!\" \"${timeout_seconds}\"");
        assertThat(script).doesNotContain("local watchdog_pid");
    }

    @Test
    void generatedDumpIsCustomFormatWithoutOwnerAclOrDatabaseCreation() throws Exception {
        String script = Files.readString(GENERATOR);

        assertThat(script).contains(
                "--format=custom",
                "--no-owner", "--no-acl",
                "pg_restore",
                "BLOCKED_GENERATED_DUMP_DATABASE_CREATION",
                "chmod 600 \"${DUMP_FILE}\"");
        assertThat(script).doesNotContain("pg_dumpall", "--create");
    }

    @Test
    void generatedAttestationCannotClaimSanitizedCloneEligibility() throws Exception {
        String script = Files.readString(GENERATOR);

        assertThat(script).contains(
                "DATA_SOURCE_CLASS=GENERATED_RELEASE_LIKE",
                "FIXTURE_SEED=20260715",
                "REAL_USER_DATA_INCLUDED=NO",
                "REAL_ACCOUNT_DATA_INCLUDED=NO",
                "REAL_MARKET_PROVIDER_DATA_INCLUDED=NO",
                "SUITABLE_FOR_FINAL_SANITIZED_CLONE_GATE=NO",
                "FINAL_SANITIZED_CLONE_ELIGIBILITY: NO",
                "PRODUCTION_READINESS: BLOCKED");
        assertThat(script).doesNotContain(
                "DATA_SOURCE_CLASS=SANITIZED_RELEASE_LIKE",
                "P4_ALLOWED: YES",
                "PRODUCTION_READINESS: READY");
    }

    @Test
    void generatedArtifactsStayIgnoredAndContainerCleanupIsMandatory() throws Exception {
        String script = Files.readString(GENERATOR);
        String gitignore = Files.readString(Path.of(".gitignore"));

        assertThat(script).contains(
                ".runtime/p3-input",
                "generated-release-like-v6.dump",
                "generated-release-like.attestation",
                "trap cleanup EXIT",
                "docker rm -f \"${CONTAINER_NAME}\"",
                "BLOCKED_GENERATOR_CONTAINER_CLEANUP");
        assertThat(gitignore).contains("*.dump", ".runtime/");
    }
}
