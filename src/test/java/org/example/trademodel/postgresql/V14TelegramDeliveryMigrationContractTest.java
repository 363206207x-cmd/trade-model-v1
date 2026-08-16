package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
                "error_code = 'DUPLICATE_MIGRATED'",
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
}
