package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingLifecycleRound3ContractTest {

    @Test
    void migrationStopsAfterV3CanRecoverToV8() throws Exception {
        assertThat(runner()).contains(
                "-e FLYWAY_TARGET=3 migrate",
                "PARTIAL_INITIALIZATION_RECOVERY: PASS",
                "RECOVERED_FLYWAY_VERSION: 8");
        assertThat(start()).contains(
                "RECOVER_GREENFIELD_INITIALIZATION",
                "run --rm --no-deps greenfield-recovery-verify",
                "run --rm --no-deps migrate",
                "FLYWAY_IGNORE_MIGRATION_PATTERNS=*:pending");
        assertThat(runner()).contains(
                "print_sanitized_p3h_status_lines",
                "[A-Z0-9_,.-]+$");
    }

    @Test
    void migrationCompletesButReadonlyGrantFailsCanRecover() throws Exception {
        String script = runner();
        int repairStart = script.indexOf("CURRENT_STAGE=\"post-migration-readonly-grant-recovery\"");
        int repairEnd = script.indexOf("CURRENT_STAGE=\"v2-secret-activation\"");

        assertThat(repairStart).isGreaterThanOrEqualTo(0);
        assertThat(repairEnd).isGreaterThan(repairStart);
        String postBootGrantRepair = script.substring(repairStart, repairEnd);
        assertThat(postBootGrantRepair).contains(
                "post-migration-readonly-grant-recovery",
                "BLOCKED_POST_MIGRATION_GRANT_RECOVERY",
                "P3H_START_MODE=STEADY_STATE_START",
                "STEADY_STATE_RESTART: PASS");
        assertThat(postBootGrantRepair).doesNotContain("P3H_START_MODE=RECOVER_GREENFIELD_INITIALIZATION");
        assertThat(start()).contains(
                "run_core_state_verify",
                "run_full_readonly_state_verify");
    }

    @Test
    void recoveryRejectsNonContiguousFlywayHistory() throws Exception {
        assertThat(runner()).contains(
                "DELETE FROM flyway_schema_history WHERE version='2'",
                "expect_recovery_rejection NONCONTIGUOUS_FLYWAY_HISTORY");
        assertThat(recoveryVerify()).contains(
                "expected_versions=1,2,3,4,5,6,7,8",
                "BLOCKED_FLYWAY_PREFIX");
    }

    @Test
    void recoveryRejectsChecksumMismatch() throws Exception {
        assertThat(runner()).contains(
                "SET checksum=checksum+1 WHERE version='3'",
                "expect_recovery_rejection CHECKSUM_MISMATCH");
        assertThat(start()).contains("run --rm --no-deps flyway-validate");
    }

    @Test
    void recoveryRejectsFailedMigration() throws Exception {
        assertThat(runner()).contains(
                "controlled failed fixture",
                "expect_recovery_rejection FAILED_MIGRATION");
        assertThat(recoveryVerify()).contains("count(*) FILTER (WHERE NOT success)");
    }

    @Test
    void recoveryRejectsUnknownBusinessObject() throws Exception {
        assertThat(runner()).contains(
                "CREATE TABLE public.p3h_unknown_business_object",
                "expect_recovery_rejection UNKNOWN_BUSINESS_OBJECT");
        assertThat(recoveryVerify()).contains(
                "BLOCKED_UNKNOWN_BUSINESS_OBJECT",
                "P3H_RECOVERY_BUSINESS_ROWS: 0");
    }

    @Test
    void recoveryRequiresExplicitConfirmation() throws Exception {
        assertThat(start()).contains(
                "I_CONFIRM_RECOVER_CONTROLLED_GREENFIELD_INITIALIZATION",
                "BLOCKED_GREENFIELD_RECOVERY_CONFIRMATION");
        assertThat(runner()).contains("BLOCKED_RECOVERY_CONFIRMATION_NOT_REQUIRED");
    }

    @Test
    void recoveryNeverUsesBaselineRepairOrClean() throws Exception {
        String recoveryPath = start() + recoveryVerify();
        String flyway = P3hContractTestSupport.read("deploy/p3h/flyway-secret-entrypoint.sh");

        assertThat(recoveryPath).doesNotContain(
                "run --rm --no-deps flyway-repair",
                "run --rm --no-deps flyway-clean",
                "run --rm --no-deps flyway-baseline");
        assertThat(start()).containsOnlyOnce("FLYWAY_IGNORE_MIGRATION_PATTERNS=*:pending");
        assertThat(flyway).contains(
                "migrate|validate", "-baselineOnMigrate=false", "-cleanDisabled=true");
        assertThat(flyway).doesNotContain("repair)", "clean)", "baseline)");
    }

    @Test
    void failedStartStopsPostgresButPreservesVolume() throws Exception {
        assertThat(start()).contains(
                "--profile validation down --remove-orphans",
                "FAILED_START_DATABASE_PROCESS: STOPPED",
                "PRIMARY_DATABASE_VOLUME: PRESENT");
        assertThat(start()).doesNotContain("down --volumes");
    }

    @Test
    void cleanupCommandFailureCannotPrintPass() throws Exception {
        String script = start();
        int measuredCondition = script.indexOf("if [ \"${down_status}\" -eq 0 ]");
        int passOutput = script.indexOf("FAILED_START_PARTIAL_STACK_CLEANUP: PASS");

        assertThat(measuredCondition).isGreaterThanOrEqualTo(0);
        assertThat(passOutput).isGreaterThan(measuredCondition);
        assertThat(script).contains(
                "cleanup_status=98",
                "FAIL_CLEANUP_INCOMPLETE",
                "P3H_COMPOSE_FAILED_STEP: ${P3H_CURRENT_STEP}");
    }

    @Test
    void noProjectContainerRemainsAfterFailure() throws Exception {
        assertThat(start()).contains(
                "docker ps --all --quiet",
                "PROJECT_CONTAINER_COUNT: ${project_container_count}",
                "project_container_count}\" = \"0");
        assertThat(runner()).contains("BLOCKED_FAILED_START_PROJECT_CONTAINER_RETAINED");
    }

    @Test
    void materializedSecretVolumeAbsentAfterFailure() throws Exception {
        assertThat(start()).contains(
                "com.docker.compose.volume=p3h_materialized_secrets",
                "MATERIALIZED_SECRET_VOLUME: ABSENT");
    }

    @Test
    void primaryVolumeSurvivesFailure() throws Exception {
        assertThat(start()).contains(
                "com.docker.compose.volume=p3h_postgresql",
                "primary_database_volume_count}\" = \"1");
        assertThat(runner()).contains("BLOCKED_FAILED_START_PRIMARY_VOLUME_DELETED");
    }

    @Test
    void appRoleMembershipDriftFails() throws Exception {
        String grantsEntrypoint = P3hContractTestSupport.read(
                "deploy/p3h/postgres-readonly-grants-entrypoint.sh");

        assertThat(runner()).contains(
                "GRANT p3h_migration_owner TO p3h_app_readonly",
                "expect_full_readonly_verify_rejection APP_ROLE_MEMBERSHIP");
        assertThat(steadyVerify()).contains("pg_auth_members", "BLOCKED_ROLE_MEMBERSHIP");
        assertThat(grantsEntrypoint).contains("--set=ON_ERROR_STOP=1");
    }

    @Test
    void backupRoleMembershipDriftFails() throws Exception {
        assertThat(runner()).contains(
                "GRANT p3h_recovery_owner TO p3h_backup_reader",
                "expect_full_readonly_verify_rejection BACKUP_ROLE_MEMBERSHIP");
    }

    @Test
    void setRoleToMigrationOwnerIsDenied() throws Exception {
        String probe = P3hContractTestSupport.read("deploy/p3h/p3h-app-readonly-probe.sh");

        assertThat(probe).contains(
                "SET ROLE p3h_migration_owner",
                "BLOCKED_SET_ROLE_ALLOWED",
                "SET_ROLE_TO_MIGRATION_OWNER: DENIED");
    }

    @Test
    void unsafeDefaultInsertPrivilegeFails() throws Exception {
        assertThat(runner()).contains("GRANT INSERT, UPDATE ON TABLES TO p3h_app_readonly");
        assertThat(steadyVerify()).contains("acl.privilege_type <> 'SELECT'");
    }

    @Test
    void unsafeDefaultUpdatePrivilegeFails() throws Exception {
        assertThat(grants()).contains("REVOKE ALL ON TABLES FROM p3h_app_readonly");
        assertThat(runner()).contains("DEFAULT_ACL_SEQUENCE_AND_DATABASE");
    }

    @Test
    void unsafeSequenceUsageFails() throws Exception {
        assertThat(runner()).contains("GRANT USAGE, UPDATE ON ALL SEQUENCES");
        assertThat(steadyVerify()).contains(
                "c.relname <> 'tm_user_id_seq'",
                "has_sequence_privilege('p3h_app_readonly', c.oid, 'USAGE')");
    }

    @Test
    void unsafeSequenceUpdateFails() throws Exception {
        assertThat(steadyVerify()).contains(
                "has_sequence_privilege('p3h_app_readonly', c.oid, 'UPDATE')",
                "has_sequence_privilege('p3h_backup_reader', c.oid, 'UPDATE')");
    }

    @Test
    void backupTempPrivilegeFails() throws Exception {
        assertThat(runner()).contains(
                "GRANT TEMPORARY ON DATABASE trade_model_v1_p3h_primary TO p3h_backup_reader");
        assertThat(steadyVerify()).contains(
                "NOT has_database_privilege('p3h_backup_reader', 'trade_model_v1_p3h_primary', 'TEMP')");
    }

    @Test
    void exactReadonlyDefaultAclPasses() throws Exception {
        assertThat(grants()).contains(
                "REVOKE ALL ON TABLES FROM p3h_app_readonly",
                "REVOKE ALL ON SEQUENCES FROM p3h_backup_reader",
                "GRANT SELECT ON TABLES TO p3h_app_readonly",
                "GRANT SELECT ON SEQUENCES TO p3h_backup_reader");
        assertThat(steadyVerify()).contains(
                "required_default_selects}\" != \"4",
                "unexpected_default_grants}\" != \"0",
                "READONLY_DEFAULT_ACL_CONTRACT: PASS");
    }

    @Test
    void rebootLikeRestartRevalidatesDatabaseRotationAndSessionAuthWithoutFakeUserRotation() throws Exception {
        assertThat(runner()).contains(
                "BLOCKED_REBOOT_V2_DATABASE_SECRET_REJECTED",
                "BLOCKED_REBOOT_REACTIVATED_V1_DATABASE_SECRET",
                "active-admin-v1-after-reboot",
                "inactive-admin-v2-after-reboot",
                "ADMIN_SECRET_ROTATION_STATUS: NOT_RUN_REQUIRES_CONTROLLED_TM_USER_PASSWORD_ROTATION",
                "DATABASE_SECRET_VERSION_AFTER_REBOOT: V2_ACTIVE_V1_DENIED");
    }

    private String start() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/p3h-compose-start.sh");
    }

    private String runner() throws Exception {
        return P3hContractTestSupport.read("scripts/controlled-p3h-compose-offline-smoke.sh");
    }

    private String recoveryVerify() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-greenfield-recovery-verify.sh");
    }

    private String steadyVerify() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-steady-state-verify.sh");
    }

    private String grants() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-readonly-grants.sql");
    }
}
