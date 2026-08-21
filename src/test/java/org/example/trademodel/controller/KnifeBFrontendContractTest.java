package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnifeBFrontendContractTest {
    private static final Path WORKSPACE_JS = Path.of("src/main/resources/static/js/workspace.js");
    private static final Path WORKSPACE_HTML = Path.of("src/main/resources/templates/workspace.html");
    private static final Path HOME_JS = Path.of("src/main/resources/static/js/home-runtime.js");

    @Test
    void positionsUseFullOwnerProjectionAndHistoryIsSeparateFromReview() throws Exception {
        String js = Files.readString(WORKSPACE_JS);
        String html = Files.readString(WORKSPACE_HTML);
        assertThat(js).contains("/api/workspace/positions/monitoring")
                .contains("/api/workspace/positions/history?limit=100")
                .contains("/api/workspace/positions/\" + encodeURIComponent(resourceId) + \"/monitoring")
                .doesNotContain("item.analysisId ? \"/reviews/\"");
        assertThat(html).contains("data-position-tab=\"active\"")
                .contains("data-position-tab=\"history\"")
                .doesNotContain("历史 / 复盘");
    }

    @Test
    void analysisUsesFormalModeStructuredRolesAndConflictGate() throws Exception {
        String js = Files.readString(WORKSPACE_JS);
        assertThat(js).contains("analysis.analysisMode")
                .contains("分析模式当前不可查看")
                .contains("analysisAudit.aiRoleResults?.roles?.[role]")
                .contains("analysisDecisionDiff\")?.remove()")
                .contains("has-conflict\", formalConflict")
                .doesNotContain("renderStructured(trace.outputJson)")
                .doesNotContain("analysis.preview === true ? \"ANALYSIS_PREVIEW\" : \"OPPORTUNITY_DECISION\"");
    }

    @Test
    void recheckAndHomeAuditLinksPreserveObjectAndReturnIdentity() throws Exception {
        String workspace = Files.readString(WORKSPACE_JS);
        String home = Files.readString(HOME_JS);
        assertThat(workspace).contains("/api/workspace/rechecks/\" + encodeURIComponent(resourceId) + \"/retry")
                .contains("/reanalyze")
                .contains("messageId: context.messageId")
                .contains("Push Snapshot ID")
                .contains("Recheck ID")
                .contains("safeReturnTo");
        assertThat(home).contains("查看完整审计链")
                .contains("查看分析详情")
                .contains("审计链尚未形成")
                .contains("/audit/\" + encodeURIComponent(trace)");
    }
}
