package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspacePositionCloseEntryRuntimeContractTest {
    @Test
    void closeEntryGateExecutesTheUiReviewAndLifecycleSafetyContract() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));
        String gate = slice(source, "function manualCloseSubmitGate", "function positionCloseActionVisible");
        String visibility = slice(source, "function positionCloseActionVisible", "function prepareManualPositionForm");
        String nodeScript = """
                const assert = require('node:assert/strict');
                %s
                %s
                assert.equal(manualCloseSubmitGate('7101', '7101', true), 'UI_REVIEW_READ_ONLY');
                assert.equal(manualCloseSubmitGate('7102', '7101', false), 'POSITION_ID_MISMATCH');
                assert.equal(manualCloseSubmitGate('7101', '', false), 'MISSING_POSITION_ID');
                assert.equal(manualCloseSubmitGate('7101', '7101', false), 'ALLOW');
                assert.equal(positionCloseActionVisible('OPEN'), true);
                assert.equal(positionCloseActionVisible('open'), true);
                assert.equal(positionCloseActionVisible('PARTIALLY_CLOSED'), true);
                assert.equal(positionCloseActionVisible('CLOSED'), false);
                assert.equal(positionCloseActionVisible(null), false);
                assert.equal(positionCloseActionVisible(undefined), false);
                assert.equal(positionCloseActionVisible(''), false);
                assert.equal(positionCloseActionVisible('UNKNOWN'), false);
                assert.equal(positionCloseActionVisible('UNRECOGNIZED_VALUE'), false);
                const action = { hidden: false };
                syncPositionCloseAction(action, 'OPEN');
                assert.equal(action.hidden, false);
                syncPositionCloseAction(action, 'PARTIALLY_CLOSED');
                assert.equal(action.hidden, false);
                syncPositionCloseAction(action, 'CLOSED');
                assert.equal(action.hidden, true);
                syncPositionCloseAction(action, 'UNKNOWN');
                assert.equal(action.hidden, true);
                syncPositionCloseAction(action, null);
                assert.equal(action.hidden, true);
                console.log('POSITION_CLOSE_ENTRY_RUNTIME=PASS');
                """.formatted(gate, visibility);

        Process process = new ProcessBuilder("node", "-e", nodeScript)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("POSITION_CLOSE_ENTRY_RUNTIME=PASS");
        assertThat(source)
                .contains("manualCloseSubmitGate(event.currentTarget.dataset.positionId, resourceId, uiReviewMode)")
                .contains("if (gate === \"UI_REVIEW_READ_ONLY\") return announce")
                .contains("closeForm.dataset.positionId = String(position.id || resourceId)")
                .contains("closeHeading.textContent = \"记录平仓 · \" + position.assetSymbol")
                .contains("/api/user-positions/\" + encodeURIComponent(resourceId) + \"/manual-close")
                .contains("renderPosition(position, monitor, { showDetailLink: false })")
                .contains("syncPositionCloseAction(closeAction, null)")
                .contains("syncPositionCloseAction(closeAction, position.status)")
                .doesNotContain("String(status || \"\").toUpperCase() !== \"CLOSED\"");
    }

    private static String slice(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        int endIndex = value.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return value.substring(startIndex, endIndex);
    }
}
