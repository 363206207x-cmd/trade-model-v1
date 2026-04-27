package org.example.trademodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.vo.MissedReasonViewVO;

import java.util.Collections;
import java.util.Map;

public final class MissedReasonViewParser {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private MissedReasonViewParser() {
    }

    public static MissedReasonViewVO parse(String reasonJson) {
        MissedReasonViewVO vo = new MissedReasonViewVO();
        vo.setFacts(Collections.emptyMap());
        vo.setRefs(Collections.emptyMap());
        if (reasonJson == null || reasonJson.isBlank()) {
            vo.setParseStatus("EMPTY_REASON_JSON");
            return vo;
        }
        try {
            JsonNode root = JSON.readTree(reasonJson);
            vo.setVersion(nullIfBlank(root.path("version").asText(null)));
            vo.setRule(nullIfBlank(root.path("rule").asText(null)));
            vo.setWhyMissed(nullIfBlank(root.path("whyMissed").asText(null)));
            if (root.path("facts").isObject()) {
                vo.setFacts(JSON.convertValue(root.path("facts"), MAP_TYPE));
            }
            if (root.path("refs").isObject()) {
                vo.setRefs(JSON.convertValue(root.path("refs"), MAP_TYPE));
            }
            vo.setParseStatus("OK");
            return vo;
        } catch (Exception ignored) {
            vo.setParseStatus("PARSE_FAILED");
            return vo;
        }
    }

    private static String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
