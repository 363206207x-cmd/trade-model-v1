package org.example.trademodel.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class P3U2IPhoneHomePrototypeContractTest {

    private static final Path PROTOTYPE_DIR =
            Path.of("docs/design/p3-u2-iphone-home-ia-v2");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern FIELD_TOKEN = Pattern.compile("\\{([^{}]+)}");

    @Test
    void captureModeNormalizesAndValidatesPositionBeforeBaseline() throws IOException {
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));

        int declaration = script.indexOf("let initialPositionMarkup = \"\";");
        int normalization = script.indexOf("  applyCaptureSafeEmptyState();");
        int validation = script.indexOf("  validateCaptureSafeEmptyState();");
        int baseline = script.indexOf(
                "  initialPositionMarkup = positionSection ? positionSection.innerHTML : \"\";");

        assertThat(declaration).isGreaterThanOrEqualTo(0);
        assertThat(normalization).isGreaterThan(declaration);
        assertThat(validation).isGreaterThan(normalization);
        assertThat(baseline).isGreaterThan(validation);
        assertThat(script).doesNotContain(
                "const initialPositionMarkup = positionSection ? positionSection.innerHTML : \"\";");
    }

    @Test
    void captureModeProvidesAValidatedFailClosedPositionState() throws IOException {
        String html = Files.readString(PROTOTYPE_DIR.resolve("index.html"));
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));
        String positionSection = section(html, "<section class=\"position-section\"", "</section>");

        assertThat(positionSection)
                .contains("data-position-independent")
                .contains("{positions[0].symbol}")
                .contains("{positions[0].entryPrice}");
        assertThat(script)
                .contains("const validateCaptureSafeEmptyState = () => {")
                .contains("positionSection.textContent || \"\"")
                .contains("Capture-mode position safe-empty normalization failed")
                .contains("textNode.nodeValue = source.replace(tokenPattern,"
                        + " (_token, fieldPath) => fixtureFallback(fieldPath));");
    }

    @Test
    void captureModeUsesFieldMapInsteadOfGenericFallbackAssumptions() throws IOException {
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));

        assertThat(script)
                .contains("const loadCaptureEmptyStates = async () => {")
                .contains("fetch(\"field-map.json\"")
                .contains("typeof field.emptyState !== \"string\"")
                .contains("field.backendField.split(/\\s+\\/\\s+/)")
                .contains("Capture-mode field has no mapped empty state")
                .contains("Capture-mode field has conflicting empty states")
                .doesNotContain("if (fieldPath === \"aiDecision.decisionModeLabel\")")
                .doesNotContain("/blockedReason/i")
                .doesNotContain("/summary|message|conclusion|statusMessage/i")
                .doesNotContain("/status|state|level|label|direction|stance|risk|quality|mode/i")
                .doesNotContain("/blocked|worthOpening|reversed|available/i");
    }

    @Test
    void captureModeMissingAiDecisionUsesFieldSpecificEmptyStates() throws IOException {
        String html = Files.readString(PROTOTYPE_DIR.resolve("index.html"));
        String aiHeader = section(html, "<section class=\"ai-section\"", "<div class=\"role-tabs\"");
        Map<String, String> emptyStates = fieldEmptyStates();

        assertThat(aiHeader)
                .contains("<span>AI 运行状态</span>")
                .contains("{aiDecision.runStatusLabel}")
                .contains("<span>复核模式</span>")
                .contains("{aiDecision.decisionModeLabel}");
        assertThat(emptyStates)
                .containsEntry("executionSuggestion.statusLabel", "当前暂无完整执行计划")
                .containsEntry("aiDecision.runStatusLabel", "未调用")
                .containsEntry("aiDecision.decisionModeLabel", "仅规则判断")
                .containsEntry("aiDecision.consistency.aiApplicable", "不适用");
    }

    @Test
    void everyPrototypeTokenHasAMappedEmptyState() throws IOException {
        String html = Files.readString(PROTOTYPE_DIR.resolve("index.html"));
        Map<String, String> emptyStates = fieldEmptyStates();
        Set<String> missingFields = new TreeSet<>();
        Matcher matcher = FIELD_TOKEN.matcher(html);

        while (matcher.find()) {
            String fieldPath = matcher.group(1);
            boolean mapped = fieldPathCandidates(fieldPath).stream()
                    .anyMatch(emptyStates::containsKey);
            if (!mapped) {
                missingFields.add(fieldPath);
            }
        }

        assertThat(missingFields).isEmpty();
    }

    private static Map<String, String> fieldEmptyStates() throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(
                Files.readString(PROTOTYPE_DIR.resolve("field-map.json")));
        Map<String, String> emptyStates = new LinkedHashMap<>();

        for (JsonNode field : root.path("fields")) {
            JsonNode backendField = field.get("backendField");
            JsonNode emptyState = field.get("emptyState");
            if (backendField == null || !backendField.isTextual()
                    || emptyState == null || !emptyState.isTextual()) {
                continue;
            }
            for (String fieldPath : backendField.asText().split("\\s+/\\s+")) {
                if (!fieldPath.isBlank()) {
                    emptyStates.put(fieldPath.trim(), emptyState.asText());
                }
            }
        }
        return emptyStates;
    }

    private static List<String> fieldPathCandidates(String fieldPath) {
        List<String> candidates = new ArrayList<>();
        candidates.add(fieldPath);
        candidates.add(fieldPath.replaceAll("\\[\\d+]", "[]"));
        candidates.add(fieldPath.replaceAll("\\[[^\\]]+]", "[]"));
        return candidates;
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);

        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return source.substring(start, end);
    }
}
