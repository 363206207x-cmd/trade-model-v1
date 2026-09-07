package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class V22UserPositionMistakeArchiveMigrationContractTest {
    @Test
    void migrationAddsOnlyAuditedOwnerScopedMistakeArchiveState() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V22__user_position_mistake_archive.sql"));

        assertThat(sql)
                .contains("archive_submission_id", "archived_at", "archive_reason")
                .contains("ARCHIVED_MISTAKE")
                .contains("uk_tm_user_position_user_archive_submission")
                .doesNotContain("UPDATE tm_user_position SET status")
                .doesNotContain("close_price =", "closed_at =", "DELETE FROM", "TRUNCATE");
    }
}
