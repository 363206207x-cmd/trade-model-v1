package org.example.trademodel.stress.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealHistoricalFixtureValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void manifestRecordsMissingFixtureWithoutInventingProvenanceOrHash() throws Exception {
        String manifest = Files.readString(Path.of("docs/replay-fixtures/REAL_HISTORICAL_FIXTURE_MANIFEST.yml"));

        assertThat(manifest).contains(
                "source_type: MISSING_REAL_HISTORICAL_FIXTURE",
                "row_count: 0",
                "sha256: null",
                "not_live_provider_call: true",
                "not_profitability_evidence: true");
        assertThat(Files.exists(Path.of("src/test/resources/replay/real"))).isFalse();
        assertThat(Files.exists(Path.of("data/replay/real"))).isFalse();
    }

    @Test
    void loaderValidatesContractComputesHashAndReportsGapWithoutFillingIt() throws Exception {
        Path fixture = tempDir.resolve("CONTRACT_TEST_ONLY.csv");
        Files.writeString(fixture, """
                timestamp_utc,symbol,timeframe,open,high,low,close,volume,event_tag
                2026-01-01T00:00:00Z,TESTUSDT,5m,100,102,99,101,10,ASSERTION_ONLY
                2026-01-01T00:10:00Z,TESTUSDT,5m,101,103,100,102,12,ASSERTION_ONLY
                """);

        RealHistoricalFixtureLoader.LoadedFixture loaded = new RealHistoricalFixtureLoader().load(fixture);

        assertThat(loaded.candles()).hasSize(2);
        assertThat(loaded.validation().rowCount()).isEqualTo(2);
        assertThat(loaded.validation().knownGaps()).hasSize(1);
        assertThat(loaded.sha256()).hasSize(64);
        assertThat(HistoricalCandle.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("eventTag", "expectedLabel", "scenarioLabel");
    }

    @Test
    void validatorRejectsInvalidOhlcAndDuplicateKeys() throws Exception {
        Path invalid = tempDir.resolve("invalid.csv");
        Files.writeString(invalid, """
                timestamp_utc,symbol,timeframe,open,high,low,close,volume
                2026-01-01T00:00:00Z,TESTUSDT,5m,100,99,98,101,10
                """);
        assertThatThrownBy(() -> new RealHistoricalFixtureLoader().load(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_HIGH_BOUNDARY");

        Path duplicate = tempDir.resolve("duplicate.csv");
        Files.writeString(duplicate, """
                timestamp_utc,symbol,timeframe,open,high,low,close,volume
                2026-01-01T00:00:00Z,TESTUSDT,5m,100,102,99,101,10
                2026-01-01T00:00:00Z,TESTUSDT,5m,101,103,100,102,12
                """);
        assertThatThrownBy(() -> new RealHistoricalFixtureLoader().load(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DUPLICATE_SYMBOL_TIMEFRAME_TIMESTAMP");
    }
}
