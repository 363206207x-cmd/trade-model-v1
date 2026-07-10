package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlOhlcvIngestionSqlCompatibilityTest {

    @Test
    void ohlcvMigrationAndMapperUsePostgreSqlCompatibleContract() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V4__ohlcv_ingestion_provenance.sql"));
        String baseline = Files.readString(Path.of(
                "src/main/resources/db/migration/V1__baseline_schema_tables.sql"));
        String indexes = Files.readString(Path.of(
                "src/main/resources/db/migration/V2__baseline_schema_indexes.sql"));
        String mapper = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/mapper/PersistedOhlcvBarMapper.java"));

        assertThat(migration).contains(
                "TIMESTAMP WITHOUT TIME ZONE",
                "source_status VARCHAR(32)",
                "freshness_status VARCHAR(32)",
                "ingestion_run_id VARCHAR(64)",
                "CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_ingestion_run");
        assertThat(baseline).contains("open_price DECIMAL(20, 8)", "volume DECIMAL(28, 8)");
        assertThat(indexes).contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_persisted_ohlcv_bar_source");
        assertThat(mapper).contains("INSERT INTO tm_persisted_ohlcv_bar", "#{openTimeMs}", "#{fetchTime}");
        assertThat(migration + mapper).doesNotContain("AUTO_INCREMENT", "MERGE INTO", "ON DUPLICATE KEY");
    }
}
