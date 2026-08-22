package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class HomeTimestampTransportContractTest {
    private static final Instant CLOSED_BAR_AT = Instant.parse("2026-08-20T09:56:00Z");

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void headerUsesInstantAndServicePassesTheSameGlobalTimestampDirectly() throws Exception {
        assertThat(DashboardHomeVO.HeaderVO.class.getDeclaredField("updatedAt").getType())
                .isEqualTo(Instant.class);

        String service = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java"));
        assertThat(service)
                .contains("globalDataUpdateCard(globalDataUpdatedAt)",
                        "home.getHeader().setUpdatedAt(globalDataUpdatedAt)")
                .doesNotContain("LocalDateTime.ofInstant(globalDataUpdatedAt");
    }

    @Test
    void fullHomeJsonUsesTheSameOffsetTimestampForHeaderAndStatus() throws Exception {
        DashboardHomeVO home = homeWithTimestamp(CLOSED_BAR_AT);

        String json = objectMapper.writeValueAsString(home);
        JsonNode root = objectMapper.readTree(json);
        String header = root.path("header").path("updatedAt").asText();
        String status = root.path("systemState").path("dataQuality").path("value").asText();

        assertThat(header).isEqualTo(status);
        assertThat(header).isEqualTo("2026-08-20T09:56:00Z");
        assertThat(header).matches(".*(?:Z|[+-]\\d{2}:\\d{2})$");
        assertThat(json).doesNotContain("\"updatedAt\":\"2026-08-20T09:56:00\"");
    }

    @Test
    void missingTimestampRemainsNullInBothFullHomeJsonFields() throws Exception {
        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(homeWithTimestamp(null)));

        assertThat(root.path("header").path("updatedAt").isNull()
                || root.path("header").path("updatedAt").isMissingNode()).isTrue();
        assertThat(root.path("systemState").path("dataQuality").path("value").isNull()
                || root.path("systemState").path("dataQuality").path("value").isMissingNode()).isTrue();
    }

    @Test
    void productionClockFormatterPassesUtcShanghaiLegacyAndNullMatrix() throws Exception {
        Process process = new ProcessBuilder("node", "scripts/home-timestamp-transport-matrix.mjs")
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains(
                "HOME_TIMESTAMP_TRANSPORT_MATRIX=PASS",
                "UTC_STATUS=09:56 UTC_HEADER=09:56",
                "ASIA_SHANGHAI_STATUS=17:56 ASIA_SHANGHAI_HEADER=17:56",
                "LEGACY_NO_OFFSET_ASIA_SHANGHAI=09:56",
                "NULL_TIMESTAMP=—");
    }

    private DashboardHomeVO homeWithTimestamp(Instant timestamp) {
        DashboardHomeVO home = new DashboardHomeVO();
        home.getHeader().setUpdatedAt(timestamp);
        DashboardHomeVO.StatusCardVO data = new DashboardHomeVO.StatusCardVO();
        data.setValue(timestamp);
        home.getSystemState().setDataQuality(data);
        return home;
    }
}
