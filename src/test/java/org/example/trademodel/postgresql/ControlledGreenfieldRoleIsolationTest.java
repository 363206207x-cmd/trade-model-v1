package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGreenfieldRoleIsolationTest {

    @Test
    void migrationBackupRecoveryAndApplicationRolesRemainSeparate() throws Exception {
        String runner = read("scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh");

        assertThat(runner).contains(
                "MIGRATION_ROLE=\"p3g_migrator_",
                "BACKUP_ROLE=\"p3g_backup_",
                "RECOVERY_ROLE=\"p3g_recovery_",
                "PRIMARY_APP_ROLE=\"p3g_app_readonly_",
                "RECOVERY_APP_ROLE=\"p3g_app_recovery_",
                "P3G_CONTROLLED_POSTGRESQL_USERNAME=${MIGRATION_ROLE}",
                "PROD_DATASOURCE_USERNAME=${BACKUP_ROLE}",
                "RESTORE_DATASOURCE_USERNAME=${RECOVERY_ROLE}",
                "BLOCKED_ROLE_DATABASE_SCOPE");
        assertThat(runner).doesNotContain(
                "--env \"P3G_CONTROLLED_POSTGRESQL_USERNAME=${PRIMARY_APP_ROLE}\"",
                "PROD_DATASOURCE_USERNAME=${MIGRATION_ROLE}",
                "RESTORE_DATASOURCE_USERNAME=${MIGRATION_ROLE}");
    }

    @Test
    void backupAndApplicationRolesAreReadOnlyNoInheritAndNarrowlyScoped() throws Exception {
        String backupSql = read("scripts/p3g-backup-reader-role.sql");
        String applicationSql = read("scripts/p3-application-readonly-role.sql");
        String runner = read("scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh");

        assertThat(backupSql).contains(
                "NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT",
                "default_transaction_read_only = on",
                "GRANT CONNECT ON DATABASE",
                "GRANT USAGE ON SCHEMA public",
                "GRANT SELECT ON ALL TABLES",
                "GRANT SELECT ON ALL SEQUENCES");
        assertThat(backupSql).doesNotContain(
                "GRANT INSERT", "GRANT UPDATE", "GRANT DELETE", "GRANT CREATE");
        assertThat(applicationSql).contains(
                "NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT",
                "default_transaction_read_only = on",
                "REVOKE CONNECT, TEMPORARY",
                "GRANT SELECT ON ALL TABLES");
        assertThat(runner).contains(
                "BLOCKED_BACKUP_READER_WRITABLE",
                "BLOCKED_APPLICATION_DATABASE_ROLE_WRITABLE",
                "READ_ONLY_WRITE_PROBE_SQLSTATE: ACCEPTED_25006_OR_42501");
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
