package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class V14TelegramDeliveryMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V14__telegram_high_value_alert_channel.sql");

    @Test
    void v14ExtendsCanonicalChannelDeliveryWithDurableRetryAndCrashRecovery() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ALTER TABLE tm_channel_delivery",
                "next_attempt_at TIMESTAMP WITHOUT TIME ZONE",
                "claimed_at TIMESTAMP WITHOUT TIME ZONE",
                "lease_until TIMESTAMP WITHOUT TIME ZONE",
                "claim_token VARCHAR(64)",
                "last_response_code INT",
                "retry_after_seconds INT",
                "recipient_fingerprint VARCHAR(128)",
                "cooldown_key VARCHAR(256)",
                "severity_rank INT NOT NULL DEFAULT 0",
                "'QUEUED', 'SENDING', 'SENT', 'RETRYING', 'FAILED', 'SUPPRESSED', 'NOT_CONFIGURED'",
                "UPDATE tm_channel_delivery",
                "SET status = 'SENT'",
                "WHEN 'SENT' THEN 1",
                "delivered_at DESC NULLS LAST",
                "error_code = 'DUPLICATE_MIGRATED'",
                "claim_token = NULL",
                "lease_until = NULL",
                "DELIVERY_OUTCOME_UNKNOWN",
                "uk_tm_channel_delivery_message_channel_active",
                "error_code IS DISTINCT FROM 'DUPLICATE_MIGRATED'",
                "idx_tm_channel_delivery_due",
                "idx_tm_channel_delivery_cooldown")
                .doesNotContain("DROP TABLE tm_channel_delivery", "DELETE FROM tm_channel_delivery");
    }

    @Test
    void h2SchemaCarriesTheSameQueueFieldsAndOneDeliveryPerMessageChannel() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        assertThat(schema).contains(
                "next_attempt_at TIMESTAMP",
                "claimed_at TIMESTAMP",
                "lease_until TIMESTAMP",
                "claim_token VARCHAR(64)",
                "retry_after_seconds INT",
                "recipient_fingerprint VARCHAR(128)",
                "cooldown_key VARCHAR(256)",
                "severity_rank INT NOT NULL DEFAULT 0",
                "uk_tm_channel_delivery_message_channel UNIQUE (message_id, channel)");
    }

    @Test
    void h2DuplicateFixtureKeepsDeterministicSentAndClearsDuplicateClaims() throws Exception {
        String migration = Files.readString(MIGRATION);
        String duplicateCleanup = migration.substring(
                migration.indexOf("WITH ranked AS"), migration.indexOf("CREATE UNIQUE INDEX"));
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v14-duplicate;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tm_channel_delivery(
                      delivery_id VARCHAR(64) PRIMARY KEY, message_id VARCHAR(64), channel VARCHAR(32),
                      status VARCHAR(32), delivered_at TIMESTAMP, updated_at TIMESTAMP, created_at TIMESTAMP,
                      next_attempt_at TIMESTAMP, claim_token VARCHAR(64), claimed_at TIMESTAMP,
                      lease_until TIMESTAMP, error_code VARCHAR(64), error_message VARCHAR(255)
                    )
                    """);
            statement.execute("""
                    INSERT INTO tm_channel_delivery VALUES
                      ('queued-old','m1','TELEGRAM','QUEUED',NULL,TIMESTAMP '2026-08-16 08:00:00',TIMESTAMP '2026-08-16 08:00:00',CURRENT_TIMESTAMP,'q',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL),
                      ('sent-new','m1','TELEGRAM','SENT',TIMESTAMP '2026-08-16 08:02:00',TIMESTAMP '2026-08-16 08:02:00',TIMESTAMP '2026-08-16 08:01:00',NULL,NULL,NULL,NULL,NULL,NULL),
                      ('sent-old','m2','TELEGRAM','SENT',TIMESTAMP '2026-08-16 08:00:00',TIMESTAMP '2026-08-16 08:00:00',TIMESTAMP '2026-08-16 08:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
                      ('sending-new','m2','TELEGRAM','SENDING',NULL,TIMESTAMP '2026-08-16 08:03:00',TIMESTAMP '2026-08-16 08:03:00',CURRENT_TIMESTAMP,'s',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL),
                      ('sent-a','m3','TELEGRAM','SENT',TIMESTAMP '2026-08-16 08:00:00',TIMESTAMP '2026-08-16 08:00:00',TIMESTAMP '2026-08-16 08:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
                      ('sent-b','m3','TELEGRAM','SENT',TIMESTAMP '2026-08-16 08:05:00',TIMESTAMP '2026-08-16 08:05:00',TIMESTAMP '2026-08-16 08:01:00',NULL,NULL,NULL,NULL,NULL,NULL)
                    """);

            statement.execute(duplicateCleanup);

            try (ResultSet rs = statement.executeQuery("""
                    SELECT message_id, delivery_id, status, next_attempt_at, claim_token, claimed_at, lease_until
                    FROM tm_channel_delivery ORDER BY message_id, delivery_id
                    """)) {
                int sent = 0;
                int migrated = 0;
                while (rs.next()) {
                    if ("SENT".equals(rs.getString("status"))) sent++;
                    if ("SUPPRESSED".equals(rs.getString("status"))) {
                        migrated++;
                        assertThat(rs.getObject("next_attempt_at")).isNull();
                        assertThat(rs.getObject("claim_token")).isNull();
                        assertThat(rs.getObject("claimed_at")).isNull();
                        assertThat(rs.getObject("lease_until")).isNull();
                    }
                }
                assertThat(sent).isEqualTo(3);
                assertThat(migrated).isEqualTo(3);
            }
            assertThat(singleValue(statement, "SELECT delivery_id FROM tm_channel_delivery WHERE message_id='m1' AND status='SENT'"))
                    .isEqualTo("sent-new");
            assertThat(singleValue(statement, "SELECT delivery_id FROM tm_channel_delivery WHERE message_id='m2' AND status='SENT'"))
                    .isEqualTo("sent-old");
            assertThat(singleValue(statement, "SELECT delivery_id FROM tm_channel_delivery WHERE message_id='m3' AND status='SENT'"))
                    .isEqualTo("sent-b");
        }
    }

    private static String singleValue(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }
}
