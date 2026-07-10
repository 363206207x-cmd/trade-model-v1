package org.example.trademodel.stress.replay;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;

final class RealHistoricalFixtureLoader {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp_utc", "symbol", "timeframe", "open", "high", "low", "close", "volume");

    LoadedFixture load(Path fixture) throws IOException {
        if (fixture == null || !Files.isRegularFile(fixture)) {
            throw new IllegalArgumentException("REAL_HISTORICAL_FIXTURE_FILE_MISSING");
        }
        List<String> lines = Files.readAllLines(fixture, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("REAL_HISTORICAL_FIXTURE_EMPTY");
        }
        Map<String, Integer> columns = columns(lines.get(0));
        if (!columns.keySet().containsAll(REQUIRED_COLUMNS)) {
            throw new IllegalArgumentException("FIXTURE_REQUIRED_COLUMNS_MISSING");
        }

        List<HistoricalCandle> candles = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            String[] values = lines.get(i).split(",", -1);
            candles.add(new HistoricalCandle(
                    Instant.parse(value(values, columns, "timestamp_utc")),
                    value(values, columns, "symbol"),
                    value(values, columns, "timeframe"),
                    decimal(values, columns, "open"),
                    decimal(values, columns, "high"),
                    decimal(values, columns, "low"),
                    decimal(values, columns, "close"),
                    decimal(values, columns, "volume")));
        }
        HistoricalFixtureValidation validation = HistoricalCandleValidator.validate(candles);
        return new LoadedFixture(List.copyOf(candles), validation, sha256(fixture));
    }

    private static Map<String, Integer> columns(String header) {
        String[] names = header.split(",", -1);
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            String normalized = names[i].trim().toLowerCase(Locale.ROOT);
            if (columns.put(normalized, i) != null) {
                throw new IllegalArgumentException("DUPLICATE_FIXTURE_COLUMN:" + normalized);
            }
        }
        return columns;
    }

    private static String value(String[] values, Map<String, Integer> columns, String column) {
        int index = columns.get(column);
        if (index >= values.length || values[index].isBlank()) {
            throw new IllegalArgumentException("FIXTURE_VALUE_MISSING:" + column);
        }
        return values[index].trim();
    }

    private static BigDecimal decimal(String[] values, Map<String, Integer> columns, String column) {
        return new BigDecimal(value(values, columns, column));
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA_256_UNAVAILABLE", e);
        }
    }

    record LoadedFixture(List<HistoricalCandle> candles,
                         HistoricalFixtureValidation validation,
                         String sha256) {
    }
}
