package org.example.trademodel.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    void captureModeGuardsFirstPaintUntilFieldMapIsReady() throws IOException {
        String html = Files.readString(PROTOTYPE_DIR.resolve("index.html"));
        String styles = Files.readString(PROTOTYPE_DIR.resolve("styles.css"));
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));
        int loadingGuard = html.indexOf(
                "document.documentElement.dataset.captureContract = \"loading\";");
        int stylesheet = html.indexOf("<link rel=\"stylesheet\" href=\"styles.css\">");

        assertThat(loadingGuard).isGreaterThanOrEqualTo(0);
        assertThat(stylesheet).isGreaterThan(loadingGuard);
        assertThat(styles)
                .contains("html[data-capture-contract=\"loading\"] body")
                .contains("visibility: hidden;");
        assertThat(script)
                .contains("const setCaptureContract = (status) => {")
                .contains("setCaptureContract(\"loading\");")
                .contains("setCaptureContract(captureUsesSafeEmptyState ? \"ready\" : \"not-requested\");");
    }

    @Test
    void captureModeFailureRemovesTemplateDomAndRendersFailClosedState() throws IOException {
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));
        String readme = Files.readString(PROTOTYPE_DIR.resolve("README.md"));

        assertThat(script)
                .contains("const renderCaptureFailure = () => {")
                .contains("failure.dataset.captureFailure = \"field-map-unavailable\";")
                .contains("message.textContent = \"字段映射加载失败，未展示未初始化内容。\";")
                .contains("document.body.replaceChildren(failure);")
                .contains("setCaptureContract(\"error\");")
                .contains("renderCaptureFailure();");
        assertThat(readme)
                .contains("html[data-capture-contract=\"ready\"], html[data-capture-contract=\"error\"]")
                .contains("getAttribute(\"data-capture-contract\") !== \"ready\"")
                .contains("DOM attribute is the authoritative gate")
                .contains("screenshot generation must stop");
    }

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
        String body = section(html, "<body>", "</body>");
        Map<String, String> emptyStates = fieldEmptyStates();
        Set<String> missingFields = new TreeSet<>();
        Matcher matcher = FIELD_TOKEN.matcher(body);

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

    @Test
    void captureReferenceScreenshotsArePngsAtTheirContractViewports() throws IOException {
        Map<String, List<Integer>> screenshotSizes = Map.of(
                "iphone-17-pro-max-light.png", List.of(440, 956),
                "iphone-17-pro-max-dark.png", List.of(440, 956),
                "iphone-17-pro-max-first-screen.png", List.of(440, 956),
                "iphone-17-pro-max-position-collapsed.png", List.of(440, 956),
                "iphone-17-pro-max-ai-collapsed.png", List.of(440, 956),
                "iphone-17-pro-max-large-text.png", List.of(440, 956),
                "iphone-12-pro-max-light.png", List.of(428, 926),
                "iphone-12-pro-max-dark.png", List.of(428, 926));

        for (Map.Entry<String, List<Integer>> entry : screenshotSizes.entrySet()) {
            Path screenshot = PROTOTYPE_DIR.resolve("screenshots").resolve(entry.getKey());
            byte[] bytes = Files.readAllBytes(screenshot);
            BufferedImage image = ImageIO.read(screenshot.toFile());

            assertThat(bytes[0]).as(entry.getKey()).isEqualTo((byte) 0x89);
            assertThat(bytes[1]).as(entry.getKey()).isEqualTo((byte) 0x50);
            assertThat(bytes[2]).as(entry.getKey()).isEqualTo((byte) 0x4e);
            assertThat(bytes[3]).as(entry.getKey()).isEqualTo((byte) 0x47);
            assertThat(image).as(entry.getKey()).isNotNull();
            assertThat(image.getWidth()).as(entry.getKey()).isEqualTo(entry.getValue().get(0));
            assertThat(image.getHeight()).as(entry.getKey()).isEqualTo(entry.getValue().get(1));
        }
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
