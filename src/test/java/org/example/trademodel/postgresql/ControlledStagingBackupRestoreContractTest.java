package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingBackupRestoreContractTest {

    @Test
    void officialScriptsRemainTheOnlyBackupRestorePath() throws Exception {
        String backup = P3hContractTestSupport.read("scripts/prod-backup.sh");
        String restore = P3hContractTestSupport.read("scripts/prod-restore.sh");
        String p3h = P3hContractTestSupport.read(
                "docs/CONTROLLED_STAGING_READONLY_TLS_SECRETSTORE_P3H.md");

        assertThat(backup).contains("pg_dump", "--format=custom", "chmod 600");
        assertThat(restore).contains(
                "pg_restore", "--clean", "--if-exists", "--exit-on-error",
                "I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA");
        assertThat(p3h).contains(
                "scripts/prod-backup.sh", "scripts/prod-restore.sh",
                "trade_model_v1_p3h_recovery", "BACKUP_DURATION_SECONDS",
                "RESTORE_DURATION_SECONDS", "OBSERVED_RPO", "OBSERVED_RTO");
    }

    @Test
    void restoreMayNeverOverwritePrimaryInP3hContract() throws Exception {
        String p3h = P3hContractTestSupport.read(
                "docs/CONTROLLED_STAGING_READONLY_TLS_SECRETSTORE_P3H.md");

        assertThat(p3h).contains("independent recovery database", "never overwrite the staging primary");
    }
}
