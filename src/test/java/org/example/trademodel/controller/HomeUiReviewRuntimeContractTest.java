package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HomeUiReviewRuntimeContractTest {
    @Test
    void launcherUsesTheActualDashboardWithAnIsolatedProfile() throws Exception {
        String launcher = Files.readString(Path.of("scripts/run-local.sh"));
        String fixture = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/uireview/UiReviewDashboardHomeService.java"));
        String positionSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/uireview/UiReviewPositionMonitoringReadService.java"));

        assertThat(launcher).contains("--ui-review", "SPRING_PROFILES_ACTIVE=\"ui-review\"",
                        "UI_REVIEW_MODE=${UI_REVIEW_MODE}", "HOME_URL=\"${LOCAL_URL}/dashboard\"")
                .doesNotContain("ui-review.html", "home-demo.html", "dashboard-preview.html");
        assertThat(fixture).contains("@Profile(\"ui-review\")", "@Primary",
                        "implements DashboardHomeService", "setAssets(assets)",
                        "positionReadService.homeTopThree", "positionReadService.aggregate")
                .doesNotContain("Mapper", "Repository", "AUTO_ORDER", "AUTO_CLOSE", "AUTO_REVERSE");
        assertThat(positionSource)
                .contains("@Profile(\"ui-review\")", "implements PositionMonitoringReadService",
                        "applyUntrustedMonitorState", "PENDING", "STALE", "INVALID", "SOURCE_UNAVAILABLE",
                        "homeTopThree", "listForUser", "findForUser")
                .doesNotContain("Mapper", "Repository", "manual-close", "save(", "insert(", "update(");
    }

    @Test
    void homeCopyUsesFinalCompactActionsAndContainsNoProhibitedDefaults() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/home.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String visibleSource = html + "\n" + script;

        assertThat(visibleSource).contains("分析", "添加", "已添加", "暂无重点机会", "暂无持仓")
                .doesNotContain(
                        "按需分析", "加入观察资产池", "已在观察资产池",
                        "请先从搜索结果中选择", "请尝试其他名称或交易对",
                        "正在分析…", "正在添加…",
                        "当前没有通过规则校验的 Final Execution Plan",
                        "仅供参考", "不构成投资建议", "请自行判断");
    }

    @Test
    void populatedReviewFixtureIsNotEmbeddedInProductionHtmlOrJavascript() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/home.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(html + script).doesNotContain(
                "ui-review-opportunity", "ui-review-final-btc", "62,800–63,200",
                "SOL 持仓风险显著上升", "美国 CPI 公布");
    }

    @Test
    void opportunityCardRendererExecutesPressedStateContractWithoutVisibleCurrentBadge() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String stateBadge = slice(source, "function stateBadge(asset)", "function opportunityCard(asset, selected)");
        String opportunityCard = slice(source, "function opportunityCard(asset, selected)", "function renderOpportunities(home)");
        String nodeScript = """
                const assert = require('node:assert/strict');
                var contract = { assetStateView: value => ({ label: value, tone: 'neutral' }) };
                var labels = Object.freeze({});
                function has(value) { return value !== null && value !== undefined && value !== ''; }
                function text(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function escapeHtml(value) { return text(value, '').replace(/[&<>'\"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '\"': '&quot;' })[character]); }
                function label(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function number(value) { return String(value); }
                function symbolOf(asset) { return String(asset && asset.symbol || '').toUpperCase(); }
                %s
                %s
                const assets = ['BTCUSDT', 'ETHUSDT', 'SOLUSDT'].map((symbol, index) => ({
                  symbol, name: symbol.slice(0, -4), opportunityState: index === 2 ? 'HIGH_RISK' : 'WAITING_TRIGGER',
                  primaryPlanMode: 'PREPARATION', opportunityScore: 80 - index, confidenceLabel: '80%%',
                  riskLabel: index === 2 ? '高' : '中', primaryTimeframe: '15m', timeframeConflictState: 'ALIGNED',
                  rankingReason: 'fixture', secondaryOpportunityCount: 0, hasFinal: false
                }));
                function render(selected) { return assets.map(asset => opportunityCard(asset, selected)).join(''); }
                const btc = render('BTCUSDT');
                const eth = render('ETHUSDT');
                assert.equal((btc.match(/aria-pressed=\"true\"/g) || []).length, 1);
                assert.equal((btc.match(/aria-pressed=\"false\"/g) || []).length, 2);
                assert.match(btc, /aria-pressed=\"true\" data-symbol=\"BTCUSDT\"/);
                assert.match(eth, /aria-pressed=\"true\" data-symbol=\"ETHUSDT\"/);
                assert.equal((eth.match(/aria-pressed=\"true\"/g) || []).length, 1);
                assert.equal((eth.match(/>当前</g) || []).length, 0);
                assert.match(eth, /HIGH_RISK/);
                console.log('HOME_OPPORTUNITY_PRESSED_STATE=PASS');
                """.formatted(stateBadge, opportunityCard);

        Process process = new ProcessBuilder("node", "-e", nodeScript)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("HOME_OPPORTUNITY_PRESSED_STATE=PASS");
        assertThat(source)
                .contains("event.key === \"Enter\" || event.key === \" \"")
                .contains("contract.replaceUrlParam(\"asset\", selectedSymbol)")
                .contains("audit.textContent = \"审计链尚未形成\"")
                .contains("audit.removeAttribute(\"href\")");
    }

    @Test
    void homeCardRuntimePreservesBackendOrderAndSeparatesObservationSemantics() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String validators = slice(source, "function eligibleOpportunity(asset)", "function selectedFinalAccess(home)");
        String renderers = slice(source, "function stateBadge(asset)", "function trustedMonitor(position)");
        String nodeScript = """
                const assert = require('node:assert/strict');
                var labels = Object.freeze({});
                var selectedSymbol = '';
                var contract = {
                  assetStateView: value => ({ label: String(value || ''), tone: 'neutral' }),
                  replaceUrlParam: () => {}
                };
                function has(value) { return value !== null && value !== undefined && value !== ''; }
                function text(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function escapeHtml(value) { return text(value, '').replace(/[&<>'\"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '\"': '&quot;' })[character]); }
                function label(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function number(value) { return String(value); }
                function time(value) { return has(value) ? String(value) : '—'; }
                function symbolOf(asset) { return String(asset && asset.symbol || '').toUpperCase(); }
                function loadHome() {}
                const nodes = {
                  opportunityGrid: { innerHTML: '', hidden: false, querySelectorAll: () => [] },
                  opportunityEmpty: { hidden: false },
                  opportunityHeading: { textContent: '' }
                };
                var document = { getElementById: id => nodes[id] };
                function setText(id, value) { nodes[id].textContent = value; }
                %s
                %s
                const observation = {
                  assetId: 1, symbol: 'ETHUSDT', name: 'ETH', slotType: 'OBSERVATION',
                  opportunityState: 'NO_QUALIFIED_OPPORTUNITY', dataFreshness: 'FRESH',
                  primaryTimeframe: '5m', latestAnalysisTime: '2026-08-30T10:00:00Z'
                };
                const blockedDecision = {
                  assetId: 2, symbol: 'BTCUSDT', name: 'BTC', slotType: 'DECISION',
                  analysisId: 'analysis-btc', opportunityId: 'opportunity-btc', opportunityScore: 0,
                  opportunityState: 'HIGH_RISK', primaryPlanMode: 'BLOCKED', dataFreshness: 'STALE',
                  confidenceLabel: '80%%', riskLabel: '高', primaryTimeframe: '15m',
                  timeframeConflictState: 'ALIGNED', hasFinal: false
                };
                assert.equal(validObservationCard(observation), true);
                assert.equal(validOpportunityCard(blockedDecision), true);
                assert.equal(validObservationCard({ ...observation, slotType: 'DEFAULT_SLOT' }), false);
                assert.equal(validObservationCard({ ...observation, opportunityScore: 1 }), false);
                assert.equal(validOpportunityCard({ ...blockedDecision, analysisId: null }), false);
                const observationHtml = observationCard(observation, '');
                assert.match(observationHtml, /暂无合格机会/);
                assert.match(observationHtml, /数据状态/);
                assert.doesNotMatch(observationHtml, /机会评分|置信度|风险|最终偏向|计划模式|入场|止损|目标/);
                const decisionHtml = opportunityCard(blockedDecision, '');
                assert.match(decisionHtml, /数据过期/);
                const all = [
                  observation,
                  blockedDecision,
                  { ...observation, assetId: 3, symbol: 'ETHUSDT' },
                  { ...observation, assetId: 4, symbol: 'SOLUSDT' },
                  { ...observation, assetId: 5, symbol: 'ADAUSDT' },
                  { ...observation, assetId: 6, symbol: 'XRPUSDT' },
                  { ...observation, assetId: 7, symbol: 'LINKUSDT' },
                  { ...observation, assetId: 8, symbol: 'AAVEUSDT' },
                  { ...observation, assetId: 9, symbol: 'BNBUSDT', slotType: 'DEFAULT_SLOT' }
                ];
                const count = renderOpportunities({ assets: all, selectedSymbol: 'ETHUSDT' });
                const symbols = [...nodes.opportunityGrid.innerHTML.matchAll(/data-symbol=\"([^\"]+)\"/g)]
                  .map(match => match[1]);
                assert.equal(count, 6);
                assert.deepEqual(symbols, ['ETHUSDT', 'BTCUSDT', 'SOLUSDT', 'ADAUSDT', 'XRPUSDT', 'LINKUSDT']);
                assert.equal(symbols.filter(symbol => symbol === 'ETHUSDT').length, 1);
                assert.equal(nodes.opportunityGrid.innerHTML.includes('DEFAULT_SLOT'), false);
                console.log('HOME_REAL_CARD_RUNTIME=PASS');
                """.formatted(validators, renderers);

        Process process = new ProcessBuilder("node", "-e", nodeScript)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("HOME_REAL_CARD_RUNTIME=PASS");
        String opportunityRender = slice(source, "function renderOpportunities(home)", "function trustedMonitor(position)");
        assertThat(opportunityRender).doesNotContain(".sort(");
    }

    private static String slice(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        int endIndex = value.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return value.substring(startIndex, endIndex);
    }
}
