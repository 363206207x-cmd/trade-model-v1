package org.example.trademodel.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionPlanOffsetTimeContractTest {

    @Test
    void schemaMigrationAndMapperShareOffsetAwarePlanTimeContract() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V7__decision_plan_offset_times.sql"));
        String baseline = Files.readString(Path.of(
                "src/main/resources/db/migration/V1__baseline_schema_tables.sql"));
        String mapper = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/mapper/DecisionResultMapper.java"));

        assertThat(schema).contains(
                "valid_from TIMESTAMP WITH TIME ZONE",
                "expires_at TIMESTAMP WITH TIME ZONE");
        assertThat(migration).contains(
                "ADD COLUMN IF NOT EXISTS valid_from TIMESTAMP WITH TIME ZONE",
                "ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE");
        assertThat(mapper).contains(
                "valid_period, valid_from, expires_at",
                "#{validPeriod}, #{validFrom}, #{expiresAt}",
                "d.valid_from AS validFrom, d.expires_at AS expiresAt");
        assertThat(baseline).doesNotContain(
                "valid_from TIMESTAMP WITH TIME ZONE",
                "expires_at TIMESTAMP WITH TIME ZONE");
    }
}
