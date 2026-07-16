package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGreenfieldOfficialBackupRestoreContractTest {

    @Test
    void runnerExecutesOfficialBackupAndRestoreScriptsInPinnedPostgresql16Client() throws Exception {
        String runner = read("scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh");

        assertThat(runner).contains(
                "POSTGRES_IMAGE_CACHE_REFERENCE=\"postgres:16-alpine\"",
                "POSTGRES_EXPECTED_DIGEST=\"${POSTGRES_IMAGE#postgres@}\"",
                "docker image ls --digests --no-trunc",
                "POSTGRES_RUNTIME_IMAGE=\"$(printf '%s\\n' \"${POSTGRES_IMAGE_ROW}\"",
                "\"${POSTGRES_RUNTIME_IMAGE}\" sh -c 'while :; do sleep 3600; done'",
                "--mount \"type=bind,src=${ARCHIVE_CONTEXT},dst=/repo,readonly\"",
                "--mount \"type=bind,src=${OPS_BACKUP_DIR},dst=/evidence\"",
                "\"${OPS_CONTAINER}\" bash /repo/scripts/prod-backup.sh",
                "\"${OPS_CONTAINER}\" bash /repo/scripts/prod-restore.sh",
                "RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA",
                "DIRECT_PG_DUMP_SUBSTITUTION: NO",
                "PROD_BACKUP_SCRIPT: PASS_LOCAL_CONTROLLED",
                "PROD_RESTORE_SCRIPT: PASS_LOCAL_CONTROLLED");
        assertThat(runner).contains(
                "cp \"${OPS_BACKUP_FILE}\" \"${BACKUP_FILE}\"",
                "chmod 600 \"${BACKUP_FILE}\"");
    }

    @Test
    void officialRestoreScriptKeepsFailClosedRestoreFlags() throws Exception {
        String restore = read("scripts/prod-restore.sh");

        assertThat(restore).contains(
                "--clean", "--if-exists", "--no-owner", "--no-acl", "--exit-on-error");
        assertThat(restore).contains(
                "RESTORE_CONFIRM", "I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA");
    }

    @Test
    void primaryRecoveryComparisonUsesStructureContentSchemaAndHistoricalEvidence() throws Exception {
        String runner = read("scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh");

        assertThat(runner).contains(
                "capture_structure_fingerprint \"${RECOVERY_DATABASE}\"",
                "capture_content_fingerprint \"${RECOVERY_DATABASE}\"",
                "capture_flyway_history \"${RECOVERY_DATABASE}\"",
                "capture_schema_types \"${RECOVERY_DATABASE}\"",
                "capture_restore_verification \"${RECOVERY_DATABASE}\"",
                "capture_historical_inventory \"${RECOVERY_DATABASE}\"",
                "RESTORE_STRUCTURE_FINGERPRINT: MATCH",
                "RESTORE_CONTENT_FINGERPRINT: MATCH",
                "FULL_CONTENT_FINGERPRINT_EXECUTED: YES");
    }

    @Test
    void contentFingerprintContractDetectsSameRowCountChanges() throws Exception {
        String fingerprintTest = read(
                "src/test/java/org/example/trademodel/postgresql/ControlledCurrentStateContentFingerprintTest.java");

        assertThat(fingerprintTest).contains(
                "sameRowCountStatusMutationIsDetected",
                "sameRowCountTimeMutationIsDetected",
                "sameRowCountPlanBoundaryMutationIsDetected",
                "assertThat(rowCounts(after)).isEqualTo(rowCounts(before))",
                "assertThat(after).isNotEqualTo(before)");
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
