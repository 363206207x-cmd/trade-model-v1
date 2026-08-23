package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceTaskTerminalSemanticsContractTest {

    @Test
    void onlyQueuedAndRunningTasksAreActiveAndTerminalFailuresAreUserReadable() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));
        String loader = slice(script, "async function loadTasks()", "function updatePoolScanCta");

        assertThat(loader)
                .contains("tasks.map(asyncTaskView)", "asyncTaskView(task)", "taskStateBadge(view)")
                .doesNotContain("task.state === \"RUNNING\" || task.state === \"PARTIAL\"");
        assertThat(script).contains("const asyncTaskView = frontendContract.asyncTaskView");
    }

    private static String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
