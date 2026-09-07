package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebLiveDashboardContractTest {

    @Test
    void desktopUsesAuthenticatedSseWithBoundedFallback() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String controller = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/DashboardHomeController.java"));

        assertThat(controller).contains("/stream").contains("SseEmitter");
        assertThat(desktop).contains("new EventSource(\"/api/dashboard/stream\")")
                .contains("15000").contains("snapshotVersion")
                .contains("visibilitychange")
                .contains("sameLiveDecision")
                .contains("homeRequestSequence")
                .contains("AbortController")
                .contains("analysis-preview?timeframe=1h");
    }

    @Test
    void liveRefreshDoesNotTriggerAiOrMutatePositions() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        assertThat(desktop).contains("function connectHomeStream")
                .contains("function applyHomeLiveEvent")
                .contains("function scheduleHomeFallbackPoll");
        assertThat(desktop.substring(desktop.indexOf("function applyHomeLiveEvent"),
                        desktop.indexOf("function stableSubmissionId")))
                .doesNotContain("analysis-preview", "method: \"POST\"", "/api/user-positions");
    }

    @Test
    void desktopOwnsExactlyOneVisibilityAwareFifteenSecondPoller() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String runtime = desktop.substring(desktop.indexOf("function scheduleHomeFallbackPoll"),
                desktop.indexOf("function stableSubmissionId"));

        assertThat(runtime).contains(
                "if (homeFallbackTimer) return",
                "homeFallbackTimer = window.setInterval",
                "if (!document.hidden) lightweightHomeRefresh()",
                "}, 15000)",
                "scheduleHomeFallbackPoll();",
                "visibilitychange",
                "if (!document.hidden) loadHome(selectedSymbol)"
        ).doesNotContain("homeReconciliationTimer = window.setInterval");
        assertThat(countOccurrences(runtime, "window.setInterval(")).isEqualTo(1);
    }

    @Test
    void repeatedInitializationVisibilityAndAssetSelectionCannotAddPollers() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String scheduler = desktop.substring(desktop.indexOf("function scheduleHomeFallbackPoll"),
                desktop.indexOf("function stopHomeFallbackPoll"));
        String selection = desktop.substring(desktop.indexOf("function renderOpportunities"),
                desktop.indexOf("function trustedMonitor"));

        assertThat(scheduler).contains("if (homeFallbackTimer) return");
        assertThat(selection).contains("openOrResumeAssetAnalysis(asset)")
                .doesNotContain("setInterval", "scheduleHomeFallbackPoll");
    }

    @Test
    void automaticRefreshIsGetOnlyAndOlderResponsesCannotWin() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String loading = desktop.substring(desktop.indexOf("async function loadHome"),
                desktop.indexOf("function stableSubmissionId"));

        assertThat(loading).contains(
                "var sequence = ++homeRequestSequence",
                "homeAbortController.abort()",
                "if (sequence !== homeRequestSequence) return",
                "loadHome(selectedSymbol)"
        ).doesNotContain(
                "method: \"POST\"",
                "/mistake-archive",
                "/manual-close",
                "/analysis-preview",
                "telegram"
        );
    }

    @Test
    void aiResultMustMatchTheSelectedCardAnalysisAndDecisionBeforeItCanRenderOrResume() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String rendering = desktop.substring(desktop.indexOf("function renderAi"),
                desktop.indexOf("function render(home)"));
        String resume = desktop.substring(desktop.indexOf("function openOrResumeAssetAnalysis"),
                desktop.indexOf("function readDraft"));

        assertThat(desktop).contains("function aiMatchesSelectedSnapshot(home, ai)");
        assertThat(rendering).contains(
                "aiMatchesSelectedSnapshot(home, ai)",
                "String(role.traceId) === String(asset.traceId)",
                "当前结果已过期，等待当前批次重新分析",
                "当前同批次审计链尚未形成"
        );
        assertThat(resume).contains(
                "currentAiCompleteForAsset(refreshedAsset)",
                "三 AI 分析已恢复"
        );
    }

    @Test
    void desktopKeepsDirectionalBlockedAndObservationPlansInSixCardGrid() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(desktop).contains(
                "[\"CONFIRMATION\", \"REDUCED\", \"PREPARATION\"].indexOf(finalMode) >= 0",
                "[\"OBSERVATION\", \"BLOCKED\"].indexOf(finalMode) >= 0"
        );
    }

    @Test
    void completedNonFinalPreviewIsNotDescribedAsStillGenerating() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String reasons = desktop.substring(desktop.indexOf("function humanReason"),
                desktop.indexOf("var alertTokenLabels"));

        assertThat(reasons).contains(
                "ANALYSIS_PREVIEW_NON_FINAL",
                "规则参考计划尚未通过 Final 校验，当前不可执行"
        ).doesNotContain("ANALYSIS_PREVIEW_NON_FINAL: \"当前分析仍在生成，完成后自动更新\"");
    }

    @Test
    void desktopPreservesLastGoodSnapshotAndClearsRecoveredRequestError() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(desktop).contains(
                "var homeRequestFailed = false",
                "function reportHomeRequestFailure(error)",
                "function clearHomeRequestFailure()",
                "if (!Array.isArray(currentHome.assets) || !currentHome.assets.length)",
                "clearHomeRequestFailure();"
        );
    }

    @Test
    void restoredPositionDraftCannotEraseIdempotencyOrManualSourceFields() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(desktop).contains(
                "form.elements.submissionId.value = stableSubmissionId(\"position-open\")",
                "form.elements.sourceType.value = \"MANUAL_INDEPENDENT\";\n        if (!has(form.elements.submissionId.value))",
                "form.elements.submissionId.value = stableSubmissionId(\"position-close\")",
                "if (!has(form.elements.closedAt.value)) form.elements.closedAt.value = freshClosedAt"
        );
    }

    @Test
    void asyncPositionSubmitHandlersRetainTheFormAcrossAwaitBoundaries() throws Exception {
        String workspace = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));

        String handlers = workspace.substring(workspace.indexOf("function bindPositionForms()"),
                workspace.indexOf("function preserveDateTimeDialogOnEscape"));
        assertThat(handlers).contains(
                "const form = event.currentTarget",
                "form.dataset.dirty = \"false\"",
                "closeOverlay(form.closest(\"dialog\"))",
                "setPositionSubmitBusy(form, false, \"确认记录\")"
        ).doesNotContain("event.currentTarget.closest(\"dialog\")");
    }

    @Test
    void homeMistakeArchiveRequiresUserConfirmationAndOneGuardedPost() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String template = Files.readString(Path.of("src/main/resources/templates/home.html"));
        String archive = desktop.substring(desktop.indexOf("function openArchiveDialog"),
                desktop.indexOf("function preserveDateTimeDialogOnEscape"));

        assertThat(template).contains(
                "homePositionArchiveDialog",
                "homePositionArchiveForm",
                "归档原因（选填）",
                "确认归档误录"
        );
        assertThat(archive).contains(
                "开仓时间",
                "开仓价",
                "仅移除本系统中的持仓监控记录，不会在交易所平仓或执行交易",
                "if (!manualPositionArchiveVisible(position)) return",
                "if (archiveForm.dataset.submitting === \"true\") return",
                "stableSubmissionId(\"position-mistake-archive\")",
                "/mistake-archive",
                "method: \"POST\"",
                "误录记录已归档"
        );
        assertThat(desktop).contains(
                "function manualPositionArchiveVisible(position)",
                "[\"MANUAL\", \"MANUAL_POSITION\", \"MANUAL_INDEPENDENT\"].indexOf(source) >= 0",
                "&& manualPositionArchiveVisible(position)"
        );
        assertThat(countOccurrences(archive, "/mistake-archive")).isEqualTo(1);
    }

    @Test
    void auditDetailsOpenOnlyFromExplicitUserActionsAndRemainReadOnly() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String audit = desktop.substring(desktop.indexOf("function auditField"),
                desktop.indexOf("function closePositionDialog"));

        assertThat(audit).contains(
                "function openAuditDialog",
                "function openAssetAudit",
                "function openPositionAudit",
                "data-copy-audit-value",
                "Analysis ID",
                "Decision ID",
                "Trace ID"
        ).doesNotContain("method: \"POST\"", "/mistake-archive", "/manual-close");
    }

    @Test
    void assetPoolKeepsFullMembershipAndUsesSameAssetProjectionAsHome() throws Exception {
        String workspace = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));
        String pool = workspace.substring(workspace.indexOf("function renderAssetPoolRows"),
                workspace.indexOf("function updatePoolScanCta"));

        assertThat(pool).contains(
                "const items = await api(\"/api/asset-pool\")",
                "items.map(loadAssetPoolProjection)",
                "items.forEach(function (asset)",
                "selectedSymbol=",
                "home?.selectedAssetContext",
                "renderAssetPoolRows(items, projections)",
                "latestPrice",
                "marketBiasLabel",
                "confidenceLabel",
                "riskItems",
                "oneHourOpportunityLabel",
                "fourHourTrendLabel",
                "directionCalculatedAt",
                "row.dataset.analysisId",
                "row.dataset.decisionId"
        ).doesNotContain(
                "/api/dashboard/home?limit=6",
                "items.slice(",
                "asset.marketType",
                "asset.watchStatus",
                "asset.sourceType"
        );
    }

    @Test
    void workspaceMistakeArchiveIsManualOnlyConfirmedAndSingleSubmitGuarded() throws Exception {
        String workspace = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));
        String template = Files.readString(Path.of("src/main/resources/templates/workspace.html"));
        String archive = workspace.substring(workspace.indexOf("function prepareArchivePositionForm"),
                workspace.indexOf("function preserveDateTimeDialogOnEscape"));

        assertThat(template).contains(
                "archivePositionForm",
                "归档原因（选填）",
                "仅移除本系统中的持仓监控记录，不会在交易所平仓或执行交易"
        );
        assertThat(archive).contains(
                "manualPositionArchiveVisible",
                "开仓时间",
                "开仓价",
                "stableSubmissionId(\"position-mistake-archive\")",
                "if (form.dataset.submitting === \"true\") return",
                "/mistake-archive",
                "method: \"POST\"",
                "误录记录已归档"
        );
        assertThat(countOccurrences(archive, "/mistake-archive")).isEqualTo(1);

        String clickOnly = workspace.substring(workspace.indexOf("const directArchive"),
                workspace.indexOf("document.getElementById(\"actualPositionForm\")?.addEventListener"));
        assertThat(clickOnly).contains("prepareArchivePositionForm(row.position, directArchive)")
                .doesNotContain("method: \"POST\"", "/mistake-archive");
    }

    @Test
    void sevenDirectionTokensRemainSeparateFromRiskTokensAndNativeAuditTitles() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/static/css/semantic-tokens.css"));
        String home = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(tokens).contains(
                "--direction-bullish-strong",
                "--direction-bullish",
                "--direction-bullish-weak",
                "--direction-neutral",
                "--direction-bearish-weak",
                "--direction-bearish",
                "--direction-bearish-strong",
                "--risk-medium",
                "--risk-high"
        );
        assertThat(home).contains("riskSemanticClass")
                .doesNotContain("provenanceSummary) + ' title=\"'");

        String workspace = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));
        String semantic = workspace.substring(workspace.indexOf("function semanticClass"),
                workspace.indexOf("function stateBadge"));
        assertThat(semantic).contains(
                "STRONG_BULLISH", "semantic-strong-bullish",
                "BULLISH", "semantic-bullish",
                "WEAK_BULLISH", "semantic-weak-bullish",
                "NEUTRAL", "semantic-neutral",
                "WEAK_BEARISH", "semantic-weak-bearish",
                "BEARISH", "semantic-bearish",
                "STRONG_BEARISH", "semantic-strong-bearish",
                "RUNNING", "semantic-analyzing",
                "function riskSemanticClass",
                "risk-level-low", "risk-level-medium", "risk-level-high", "risk-level-extreme",
                "risk-level-unknown"
        );
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
