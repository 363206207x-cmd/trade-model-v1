package org.example.trademodel.dashboard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class P3U2IPhoneHomePrototypeContractTest {

    private static final Path PROTOTYPE_DIR =
            Path.of("docs/design/p3-u2-iphone-home-ia-v2");

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
    void aiDecisionModeIsRenderedInTheHeaderWithFailClosedFallback() throws IOException {
        String html = Files.readString(PROTOTYPE_DIR.resolve("index.html"));
        String script = Files.readString(PROTOTYPE_DIR.resolve("interaction.js"));
        String aiHeader = section(html, "<section class=\"ai-section\"", "<div class=\"role-tabs\"");

        assertThat(aiHeader)
                .contains("<span>AI 运行状态</span>")
                .contains("{aiDecision.runStatusLabel}")
                .contains("<span>复核模式</span>")
                .contains("{aiDecision.decisionModeLabel}");
        assertThat(script)
                .contains("status|state|level|label|direction|stance|risk|quality|mode")
                .contains("return \"待同步\";");
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);

        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return source.substring(start, end);
    }
}
