package org.example.trademodel.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void commonEnvelopeUsesFrozenSnakeCaseMetadataAndPreservesEmptyArrays() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(ApiResponse.success(List.of())));

        assertThat(json.path("code").asInt()).isEqualTo(200);
        assertThat(json.path("msg").asText()).isEqualTo("success");
        assertThat(json.path("data").isArray()).isTrue();
        assertThat(json.path("data").isEmpty()).isTrue();
        assertThat(json.path("request_id").asText()).isNotBlank();
        assertThat(json.path("server_time").asText()).endsWith("Z");
        assertThat(json.has("requestId")).isFalse();
        assertThat(json.has("serverTime")).isFalse();
    }
}
