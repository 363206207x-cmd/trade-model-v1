package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingLifecycleRound2ContractTest {

    @Test
    void initializeGreenfieldAndSteadyStateStartAreExplicit() throws Exception {
        String start = startScript();

        assertThat(start).contains(
                "P3H_START_MODE",
                "INITIALIZE_GREENFIELD",
                "STEADY_STATE_START",
                "I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION",
                "BLOCKED_GREENFIELD_CONFIRMATION");
        assertThat(start).doesNotContain("if database is empty", "AUTO_DETECT_START_MODE");
    }

    @Test
    void steadyStateValidatesFlywayChecksumsWithoutMigrateRepairBaselineOrClean() throws Exception {
        String start = startScript();
        String flyway = P3hContractTestSupport.read("deploy/p3h/flyway-secret-entrypoint.sh");
        String verify = P3hContractTestSupport.read(
                "deploy/p3h/postgres-steady-state-verify.sh");

        assertThat(start).contains(
                "run --rm --no-deps flyway-validate",
                "run --rm --no-deps steady-state-verify",
                "P3H_READONLY_GRANTS_MODE=STEADY_STATE",
                "FLYWAY_REPEAT: ZERO_MIGRATIONS");
        assertThat(flyway).contains("migrate|validate");
        assertThat(flyway).doesNotContain("repair)", "clean)", "baseline)");
        assertThat(verify).contains(
                "7|7|0|1,2,3,4,5,6,7",
                "P3H_ROLE_AND_GRANT_CONTRACT: PASS");
    }

    @Test
    void activeSecretVersionIsExplicitAndSurvivesSteadyState() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String materializer = P3hContractTestSupport.read(
                "deploy/p3h/p3h-secret-materializer.sh");
        String runner = runner();

        assertThat(compose).contains(
                "P3H_ACTIVE_APP_DATABASE_SECRET_VERSION",
                "P3H_ACTIVE_APP_ADMIN_SECRET_VERSION",
                "app_database_password_v2",
                "app_admin_password_v2");
        assertThat(materializer).contains(
                "active_database_secret", "active_admin_secret", "V1)", "V2)");
        assertThat(runner).contains(
                "ACTIVE_SECRET_VERSION_PRESERVED: PASS",
                "OLD_SECRET_V1_POST_ROTATION: DENIED");
    }

    @Test
    void failureAfterSecretMaterializationCleansSecrets() throws Exception {
        assertThat(startScript()).contains(
                "AFTER_SECRET_MATERIALIZATION",
                "FAILED_START_SECRET_CLEANUP: PASS",
                "remove_materialized_secret_volume");
        assertThat(runner()).contains("exercise_failed_start_cleanup AFTER_SECRET_MATERIALIZATION");
    }

    @Test
    void failureAfterAppStartCleansPartialStack() throws Exception {
        assertThat(startScript()).contains(
                "AFTER_APP_START", "--profile validation down --remove-orphans",
                "FAILED_START_PARTIAL_STACK_CLEANUP: PASS");
        assertThat(runner()).contains("exercise_failed_start_cleanup AFTER_APP_START");
    }

    @Test
    void failureDuringProxyHealthCleansPartialStack() throws Exception {
        assertThat(startScript()).contains("DURING_PROXY_HEALTH");
        assertThat(runner()).contains("exercise_failed_start_cleanup DURING_PROXY_HEALTH");
    }

    @Test
    void failureCleanupNeverDeletesPrimaryVolumeWithoutApproval() throws Exception {
        String start = startScript();

        assertThat(start).contains(
                "FAILED_START_PRIMARY_VOLUME_POLICY: PRESERVED_UNLESS_EXPLICIT_TEARDOWN");
        assertThat(start).doesNotContain("down --volumes", "volume rm p3h_postgresql");
        assertThat(runner()).contains("BLOCKED_FAILED_START_PRIMARY_VOLUME_DELETED");
    }

    @Test
    void dockerRestartCannotBypassSystemdOrchestrator() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String unit = P3hContractTestSupport.read("deploy/p3h/trade-model-p3h.service.template");

        assertThat(compose).doesNotContain("restart: unless-stopped", "restart: always");
        assertThat(compose).contains("restart: \"no\"");
        assertThat(unit).contains(
                "P3H_START_MODE=RENDER_INITIALIZE_GREENFIELD_RECOVER_GREENFIELD_OR_STEADY_STATE_START");
    }

    @Test
    void publicFunctionBlocksGreenfield() throws Exception {
        assertThat(runner()).contains("CREATE FUNCTION public.p3h_probe()");
        assertStrictRoutineInventory();
    }

    @Test
    void nonPublicTableBlocksGreenfield() throws Exception {
        assertThat(runner()).contains("CREATE SCHEMA p3h_probe_schema");
        assertThat(preflight()).contains("SELECT 'schema' AS kind", "c.relkind IN");
    }

    @Test
    void foreignServerBlocksGreenfield() throws Exception {
        assertThat(runner()).contains(
                "CREATE FOREIGN DATA WRAPPER p3h_probe_fdw NO HANDLER",
                "CREATE SERVER p3h_probe_server");
        assertThat(preflight()).contains("pg_foreign_data_wrapper", "pg_foreign_server");
    }

    @Test
    void unapprovedExtensionBlocksGreenfield() throws Exception {
        assertThat(runner()).contains("CREATE EXTENSION hstore");
        assertThat(preflight()).contains("pg_extension WHERE extname <> 'plpgsql'");
    }

    @Test
    void userSequenceBlocksGreenfield() throws Exception {
        assertThat(runner()).contains("CREATE SEQUENCE public.p3h_probe_sequence");
        assertThat(preflight()).contains("'S'");
    }

    @Test
    void cleanDatabasePassesGreenfield() throws Exception {
        assertThat(runner()).contains(
                "BLOCKED_CLEAN_GREENFIELD_PREFLIGHT",
                "BLOCKED_GREENFIELD_CLEAN_RECHECK");
        assertThat(preflight()).contains("GREENFIELD_OBJECT_INVENTORY: PASS_STRICT");
    }

    @Test
    void dirtyWorktreeBlocksEvidenceBuild() throws Exception {
        assertThat(runner()).contains("status --porcelain", "BLOCKED_DIRTY_WORKTREE");
    }

    @Test
    void untrackedInputCannotEnterImage() throws Exception {
        assertThat(runner()).contains(
                "git -C \"${ROOT_DIR}\" archive",
                "check-docker-context-safety.sh",
                "BLOCKED_SOURCE_CHANGED_DURING_ARCHIVE");
        assertThat(runner()).doesNotContain("--tag \"${IMAGE_TAG}\" \"${ROOT_DIR}\"");
    }

    @Test
    void imageBuildUsesExactGitArchive() throws Exception {
        assertThat(runner()).contains(
                "--output=\"${archive_file}\" \"${current_head}\"",
                "--tag \"${IMAGE_TAG}\" \"${ARCHIVE_CONTEXT}\"");
    }

    @Test
    void imageRevisionMatchesExactHead() throws Exception {
        assertThat(runner()).contains(
                "--build-arg \"VCS_REF=${current_head}\"",
                "org.opencontainers.image.revision",
                "BLOCKED_IMAGE_REVISION_MISMATCH");
    }

    private void assertStrictRoutineInventory() throws Exception {
        assertThat(preflight()).contains("FROM pg_proc p", "SELECT 'routine'");
    }

    private String startScript() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/p3h-compose-start.sh");
    }

    private String runner() throws Exception {
        return P3hContractTestSupport.read("scripts/controlled-p3h-compose-offline-smoke.sh");
    }

    private String preflight() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-greenfield-preflight.sh");
    }
}
