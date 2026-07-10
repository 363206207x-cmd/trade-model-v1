package org.example.trademodel.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OhlcvWriterOwnershipGuardTest {

    @Test
    void noControllerOrDashboardDirectOhlcvWrite() throws Exception {
        String controllers = readJavaTree(Path.of("src/main/java/org/example/trademodel/controller"));
        String dashboard = readJavaTree(Path.of("src/main/java/org/example/trademodel/service/dashboard"));
        String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(controllers).doesNotContain("PersistedOhlcvBarMapper", "tm_persisted_ohlcv_bar");
        assertThat(dashboard).doesNotContain("PersistedOhlcvBarMapper", "tm_persisted_ohlcv_bar");
        assertThat(template).doesNotContain("tm_persisted_ohlcv_bar");
    }

    private static String readJavaTree(Path root) throws Exception {
        StringBuilder source = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(path));
            }
        }
        return source.toString();
    }
}
