package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExternalContextControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void macroImportQueryAndDashboardStatusExposeReviewOnlySafetyWithoutForbiddenActions() throws Exception {
        String id = "controller-macro-" + System.nanoTime();
        String body = macroJson(id);

        mockMvc.perform(post("/api/external-context/macro-events/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(false))
                .andExpect(jsonPath("$.event.eventId").value(id))
                .andExpect(jsonPath("$.event.reviewOnly").value(true))
                .andExpect(jsonPath("$.event.notExecutable").value(true))
                .andExpect(jsonPath("$.event.notExternalFetch").value(true));

        mockMvc.perform(get("/api/external-context/events/MACRO/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(id))
                .andExpect(jsonPath("$.sourceTraceId").value("trace-" + id));

        String dashboard = mockMvc.perform(get("/api/external-context/dashboard-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(dashboard).doesNotContain("tradeAllowed", "orderAllowed", "executionAuthorized",
                "providerPayload", "executablePayload", "buy", "sell", "open", "close");
    }

    @Test
    void newsImportEndpointRejectsMissingSourcePublishedAt() throws Exception {
        String id = "controller-news-missing-" + System.nanoTime();
        String body = newsJson(id).replaceFirst("\\,\\\"sourcePublishedAt\\\":\\\"[^\\\"]+\\\"", "");

        mockMvc.perform(post("/api/external-context/news-events/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("invalid request"));
    }

    private String macroJson(String id) {
        LocalDateTime now = LocalDateTime.now();
        return "{"
                + "\"eventId\":\"" + id + "\","
                + "\"eventType\":\"RATE_DECISION\","
                + "\"title\":\"Macro event\","
                + "\"affectedSymbols\":\"BTCUSDT\","
                + "\"marketScope\":\"CRYPTO\","
                + "\"eventTime\":\"" + now.minusMinutes(1) + "\","
                + "\"windowStart\":\"" + now.minusMinutes(5) + "\","
                + "\"windowEnd\":\"" + now.plusMinutes(30) + "\","
                + "\"impactScore\":80,\"severity\":\"HIGH\",\"direction\":\"NEUTRAL\","
                + "\"provider\":\"UNIT_CONTROLLER\",\"sourceType\":\"CALENDAR\","
                + "\"sourceReference\":\"unit://" + id + "\",\"sourceTraceId\":\"trace-" + id + "\","
                + "\"sourceEventId\":\"source-" + id + "\",\"status\":\"ACTIVE\",\"dedupeKey\":\"dedupe-" + id + "\""
                + "}";
    }

    private String newsJson(String id) {
        LocalDateTime now = LocalDateTime.now();
        return "{"
                + "\"eventId\":\"" + id + "\",\"headline\":\"Major news\","
                + "\"affectedSymbols\":\"BTCUSDT\",\"marketScope\":\"CRYPTO\","
                + "\"eventTime\":\"" + now.minusMinutes(1) + "\","
                + "\"windowStart\":\"" + now.minusMinutes(5) + "\","
                + "\"windowEnd\":\"" + now.plusMinutes(30) + "\","
                + "\"impactScore\":90,\"severity\":\"CRITICAL\",\"direction\":\"BEARISH\","
                + "\"provider\":\"UNIT_CONTROLLER\",\"sourceType\":\"WIRE\","
                + "\"sourceReference\":\"unit://" + id + "\",\"sourceTraceId\":\"trace-" + id + "\","
                + "\"sourceEventId\":\"source-" + id + "\",\"sourcePublishedAt\":\"" + now.minusMinutes(2) + "\","
                + "\"status\":\"ACTIVE\",\"dedupeKey\":\"dedupe-" + id + "\""
                + "}";
    }
}
