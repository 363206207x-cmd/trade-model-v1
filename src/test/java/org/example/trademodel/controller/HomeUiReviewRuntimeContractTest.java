package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("core-regression")
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
        String semanticClass = slice(source, "function semanticClass(value)", "function toneText(value, raw)");
        String stateBadge = slice(source, "function stateBadge(asset)", "function opportunityCard(asset, selected)");
        String opportunityCard = slice(source, "function opportunityCard(asset, selected)", "function renderOpportunities(home)");
        String nodeScript = """
                const assert = require('node:assert/strict');
                var contract = { assetStateView: value => ({ label: value, tone: 'neutral' }) };
                var labels = Object.freeze({});
                var window = { setTimeout: () => 1 };
                eval(require('node:fs').readFileSync('src/main/resources/static/js/home-runtime.js', 'utf8').split('/* Desktop Home runtime */')[0]);
                var desktop = window.TrineDesktopSemantics;
                var assetCardSnapshots = new Map(), assetCardFieldVersions = new Map(), assetCardPriceTimers = new Map();
                var homeCardSymbols = ['BTCUSDT', 'ETHUSDT', 'SOLUSDT'];
                function has(value) { return value !== null && value !== undefined && value !== ''; }
                function text(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function escapeHtml(value) { return text(value, '').replace(/[&<>'\"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '\"': '&quot;' })[character]); }
                function label(value, fallback) { return has(value) ? String(value) : (fallback || '当前不可查看'); }
                function number(value) { return String(value); }
                function clockTime(value) { return has(value) ? String(value) : '—'; }
                function symbolOf(asset) { return String(asset && asset.symbol || '').toUpperCase(); }
                %s
                %s
                %s
                const assets = ['BTCUSDT', 'ETHUSDT', 'SOLUSDT'].map((symbol, index) => ({
                  symbol, name: ['Bitcoin', 'Ethereum', 'Solana'][index], opportunityState: index === 2 ? 'HIGH_RISK' : 'WAITING_TRIGGER',
                  finalPlanMode: 'PREPARATION', finalMarketBias: 'BULLISH', confidenceLevel: 'HIGH',
                  marketBias: 'BULLISH', marketBiasLabel: '偏多',
                  finalConfidence: 80, confidenceLabel: '80%%', riskLevel: index === 2 ? 'HIGH' : 'MEDIUM',
                  riskLabel: index === 2 ? '高' : '中', oneHourOpportunityLabel: '1小时机会',
                  fourHourTrendLabel: '4小时趋势偏多', hasFinal: true, cardSignalDisplayEnabled: true,
                  cardSignal: { symbol, assetName: ['Bitcoin','Ethereum','Solana'][index], snapshotVersion: 1,
                    featureVersion:'test-only',modelVersion:'test-only',calibrationVersion:'test-only',thresholdVersion:'test-only',
                    spotPrice:100,latestPriceAt:'2026-09-10T00:00:00Z',priceTradeId:1,cardAsOf:'2026-09-10T00:00:00Z',
                    signal:{direction:'WEAK_SHORT',status:'VALID',calibratedConfidence:54,pLong:.18,pShort:.54,
                      oneHourState:'OPPORTUNITY',fourHourTrend:'LONG',signalAsOf:'2026-09-10T00:00:00Z'},
                    risk:{overallLevel:null,items:[],riskVersion:'test-only-risk',riskBasisSide:'SHORT',
                      riskBasisDirection:'WEAK_SHORT',riskBasisSignalAsOf:'2026-09-10T00:00:00Z',
                      riskMarketAsOf:'2026-09-10T00:00:00Z'},health:{status:'HEALTHY'} }
                }));
                function render(selected) { return assets.map(asset => opportunityCard(asset, selected)).join(''); }
                const missingCardSignal = opportunityCard({...assets[0],cardSignal:null},'BTCUSDT');
                assert.equal(missingCardSignal.includes('data-live-field="direction" class="asset-card-unknown">—'), true);
                assert.equal(missingCardSignal.includes('data-live-field="confidence">—'), true);
                assert.match(missingCardSignal, /data-live-field="risk" class="asset-card-risk-unknown"[^>]*>—/);
                assert.equal(missingCardSignal.includes('data-desktop-hover="risk"'), true);
                const missingRiskReason = assetCardRiskDrawer({symbol:'BTCUSDT'});
                assert.ok(missingRiskReason.includes('风险方向、信号时间或版本尚未匹配当前卡片'));
                assert.ok(!missingRiskReason.includes('risk-evidence-item'));
                assert.equal(missingCardSignal.includes('data-live-field="status">数据不足'), true);
                const btc = render('BTCUSDT');
                const eth = render('ETHUSDT');
                assert.equal(eth.includes('data-live-field="direction" class="asset-card-long">偏多'), false);
                assert.equal(eth.includes('data-live-field="confidence">80%%'), false);
                assert.equal(assets[1].finalMarketBias, 'BULLISH');
                assert.equal(assets[1].finalConfidence, 80);
                assert.equal((btc.match(/aria-pressed=\"true\"/g) || []).length, 1);
                assert.equal((btc.match(/aria-pressed=\"false\"/g) || []).length, 2);
                assert.match(btc, /aria-pressed=\"true\" data-symbol=\"BTCUSDT\"/);
                assert.match(eth, /aria-pressed=\"true\" data-symbol=\"ETHUSDT\"/);
                assert.equal((eth.match(/aria-pressed=\"true\"/g) || []).length, 1);
                assert.equal((eth.match(/>当前</g) || []).length, 0);
                assert.doesNotMatch(eth, /HIGH_RISK/);
                assert.equal(eth.includes('data-desktop-hover="risk"'), false);
                assert.equal(eth.includes('尚无独立风险证据'), false);
                assert.equal(eth.includes('data-live-field="risk" class="asset-card-risk-unknown">—'), true);
                assert.equal(eth.includes('data-desktop-hover="risk-status"'), false);
                const pinned = opportunityCard({...assets[1],homePinned:true,planMode:'BLOCKED'},'ETHUSDT');
                assert.ok(pinned.includes('<div class="opportunity-facts"><div class="asset-card-risk-summary"'));
                assert.ok(!pinned.includes('计划阻断'), 'pin/plan state must not supply card signal or risk');
                const evidencedRisk = {riskType:'EVENT_RISK', evidenceStatus:'AVAILABLE', severity:'HIGH',
                  currentValue:'2', source:'fixture-event-source', observedAt:'2026-09-07T05:00:00Z',
                  primaryEvidence:'Test-only independently observed event count', evidenceId:'fixture-evidence-1'};
                const withRisk = opportunityCard({...assets[1],riskItems:[evidencedRisk],cardSignal:{...assets[1].cardSignal,snapshotVersion:2,
                  risk:{...assets[1].cardSignal.risk,overallLevel:'HIGH',items:[{type:'EVENT',assessmentStatus:'ASSESSED',level:'HIGH',evidenceValue:2,
                    source:'fixture-event-source',asOf:'2026-09-07T05:00:00Z',reason:'Test-only independently observed event count'}]}}}, 'ETHUSDT');
                assert.equal(withRisk.includes('data-desktop-hover="risk"'), true);
                assert.equal(withRisk.includes('tabindex="0"'), true);
                assert.equal(withRisk.includes('aria-haspopup="dialog"'), true);
                assert.equal(withRisk.includes('aria-expanded="false"'), true);
                assert.equal(withRisk.includes('aria-label="ETHUSDT 风险详情"'), true);
                assert.equal(desktop.riskDrawer({...assets[1],riskItems:[evidencedRisk]}).includes('fixture-event-source'), true);
                // Complete evidence with no confirmed risk must not become a hover action.
                // An explicit non-risk severity is not inferred from a missing score or a low aggregate score.
                const completeNoRisk = {...assets[1],riskItems:[{...evidencedRisk,severity:'NONE',currentValue:'0'}],
                  cardSignal:{...assets[1].cardSignal,snapshotVersion:3,risk:{...assets[1].cardSignal.risk,overallLevel:'LOW',
                    items:['CHASE','SHOCK','REVERSAL','CROWDING','LIQUIDATION','LIQUIDITY','EVENT','DATA']
                      .map(type=>({type,assessmentStatus:'ASSESSED',level:'NONE'}))}}};
                assert.equal(opportunityCard(completeNoRisk,'ETHUSDT').includes('data-desktop-hover="risk"'), false);
                assert.equal(assetCardRiskDrawer(assetCardSnapshots.get('ETHUSDT')), '');
                assert.equal(desktop.riskDrawer(completeNoRisk), '');
                assert.equal(desktop.riskDrawer({...assets[1],riskItems:[]}), '');
                assert.equal(eth.includes('<b data-live-field="direction" class="asset-card-weak-short">弱偏空</b><span class="metric-separator">·</span><small>置信</small><strong data-live-field="confidence">54%%</strong></div>'), true);
                assert.equal(eth.includes('<strong>ETH</strong><span aria-hidden="true">/</span><small>Ethereum</small>'), true);
                assert.equal(eth.includes('<small>状态</small>'), false);
                assert.equal(eth.includes('<small>数据</small>'), false);
                assert.equal(eth.includes('数据新鲜'), false);
                assert.doesNotMatch(withRisk, /test-only|fixture-event-source|snapshotVersion|modelVersion|calibrationVersion/);
                assert.doesNotMatch(eth, /24h|24小时|undefined%%|NaN%%|更新于/);
                const directions = {STRONG_LONG:'强偏多',LONG:'偏多',WEAK_LONG:'弱偏多',STRONG_SHORT:'强偏空',
                  SHORT:'偏空',WEAK_SHORT:'弱偏空',RANGE:'震荡',WATCH:'观望'};
                let cardVersion = 10;
                for (const [direction, copy] of Object.entries(directions)) {
                  const signal = {...assets[0].cardSignal.signal,direction};
                  const directionHtml = opportunityCard({...assets[0],cardSignal:{...assets[0].cardSignal,
                    snapshotVersion:cardVersion++,signal}},'BTCUSDT');
                  assert.equal(directionHtml.includes('<b data-live-field="direction" class="asset-card-'
                    + direction.toLowerCase().replace(/_/g,'-') + '">' + copy + '</b>'), true);
                  assert.equal(directionHtml.includes('<strong data-live-field="confidence">'
                    + (['RANGE','WATCH'].includes(direction)?'—':'54%%') + '</strong>'), true);
                }
                const freshHtml = opportunityCard({...assets[0],cardSignal:{...assets[0].cardSignal,snapshotVersion:40}},'BTCUSDT');
                const snapshotBeforeOld = JSON.stringify(assetCardSnapshots.get('BTCUSDT'));
                const oldHtml = opportunityCard({...assets[0],cardSignal:{...assets[0].cardSignal,snapshotVersion:39,
                  signal:{...assets[0].cardSignal.signal,direction:'LONG',calibratedConfidence:80}}},'BTCUSDT');
                assert.equal(JSON.stringify(assetCardSnapshots.get('BTCUSDT')), snapshotBeforeOld);
                assert.equal(oldHtml.includes('<b data-live-field="direction" class="asset-card-weak-short">弱偏空</b><span class="metric-separator">·</span><small>置信</small><strong data-live-field="confidence">54%%</strong>'), true);
                const clock = freshHtml.match(/data-live-field="card-time"[^>]*>([^<]*)/)[1];
                assert.match(clock, /^[0-9]{2}:[0-9]{2}:[0-9]{2}$/);
                assert.equal(clock, new Intl.DateTimeFormat('en-GB',{hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'})
                  .format(new Date(assets[0].cardSignal.cardAsOf)));
                for (const field of ['featureVersion','modelVersion','calibrationVersion','thresholdVersion']) {
                  assert.equal(assetCardConfidence({...assets[0].cardSignal,[field]:null}), '—', field);
                  assert.equal(assetCardDirection({...assets[0].cardSignal,[field]:null}), '—', field);
                }
                applyAssetCardEvent({eventType:'ASSET_CARD_PRICE',symbol:'BTCUSDT',snapshotVersion:41,
                  payload:{symbol:'BTCUSDT',snapshotVersion:41,spotPrice:321.5,latestPriceAt:'2026-09-10T00:00:05Z',cardAsOf:'2099-01-01T00:00:00Z'}});
                assert.equal(assetCardSnapshots.get('BTCUSDT').spotPrice, 100, 'missing real trade identity cannot overwrite price');
                applyAssetCardEvent({eventType:'ASSET_CARD_PRICE',symbol:'BTCUSDT',snapshotVersion:0,
                  payload:{symbol:'BTCUSDT',snapshotVersion:0,priceTradeId:2,spotPrice:321.5,latestPriceAt:'2026-09-10T00:00:05Z'}});
                const priceHtml = opportunityCard({...assets[0],cardSignal:null},'BTCUSDT');
                assert.equal(priceHtml.includes('data-live-field="price">$321.5'), true);
                assert.equal(priceHtml.match(/data-live-field="card-time"[^>]*>([^<]*)/)[1], clock);
                console.log('HOME_OPPORTUNITY_PRESSED_STATE=PASS');
                """.formatted(semanticClass, stateBadge, opportunityCard);

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
    void homeRuntimeParsesAnalysisOwnedAssetProvenanceWithoutAddingVisibleCardRows() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String parser = slice(source, "function assetProvenance(asset)", "function opportunityCard(asset, selected)");

        assertThat(parser).contains(
                "analysisId", "analysisVersion", "configurationVersion", "providerMatrixVersion",
                "provider", "sourceId", "priceObservedAt", "oneHourClosedAt", "fourHourClosedAt",
                "freshnessStatus", "dataQualityScore", "directionMaturity", "homeTier",
                "data-analysis-id", "data-analysis-version", "data-direction-maturity", "data-home-tier");
        assertThat(source).doesNotContain(
                "<small>analysisId</small>", "<small>providerMatrixVersion</small>",
                "<small>sourceId</small>", "<small>homeTier</small>");
    }

    @Test
    void homeCardRuntimePreservesBackendOrderAndSeparatesObservationSemantics() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String semanticClass = slice(source, "function semanticClass(value)", "function toneText(value, raw)");
        String validators = slice(source, "function eligibleOpportunity(asset)", "function selectedFinalAccess(home)");
        String renderers = slice(source, "function stateBadge(asset)", "function trustedMonitor(position)");
        String nodeScript = """
                const assert = require('node:assert/strict');
                var labels = Object.freeze({});
                var selectedSymbol = '';
                var homeCardSymbols = [];
                var assetCardSnapshots = new Map(), assetCardFieldVersions = new Map(), assetCardPriceTimers = new Map();
                var window = {};
                eval(require('node:fs').readFileSync('src/main/resources/static/js/home-runtime.js', 'utf8').split('/* Desktop Home runtime */')[0]);
                var desktop = window.TrineDesktopSemantics;
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
                function clockTime(value) { return has(value) ? String(value) : '—'; }
                function symbolOf(asset) { return String(asset && asset.symbol || '').toUpperCase(); }
                function loadHome() {}
                %s
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
                  assetId: 1, symbol: 'ETHUSDT', name: 'Ethereum', slotType: 'OBSERVATION', cardSignalDisplayEnabled: true,
                  opportunityState: 'NO_QUALIFIED_OPPORTUNITY', dataFreshness: 'FRESH',
                  marketBiasLabel: '暂不可判断', confidenceLabel: '—', riskLabel: '暂不可判断',
                  oneHourOpportunityLabel: '1小时分析未完成', fourHourTrendLabel: '4小时分析未完成',
                  latestAnalysisTime: '2026-08-30T10:00:00Z'
                };
                const blockedObservation = {
                  assetId: 2, symbol: 'BTCUSDT', name: 'Bitcoin', slotType: 'DECISION', cardSignalDisplayEnabled: true,
                  analysisId: 'analysis-btc', opportunityId: 'opportunity-btc', opportunityScore: 0,
                  opportunityState: 'HIGH_RISK', finalPlanMode: 'BLOCKED', dataFreshness: 'STALE',
                  marketBiasLabel: '暂不可判断', confidenceLabel: '—', riskLabel: '暂不可判断',
                  oneHourOpportunityLabel: '1小时数据已过期', fourHourTrendLabel: '4小时数据已过期', hasFinal: false
                };
                assert.equal(validObservationCard(observation), true);
                assert.equal(validOpportunityCard(blockedObservation), false);
                assert.equal(validObservationCard(blockedObservation), true);
                assert.equal(validObservationCard({ ...observation, slotType: 'DEFAULT_SLOT' }), false);
                assert.equal(validObservationCard({ ...observation, opportunityScore: 1 }), true);
                assert.equal(validOpportunityCard({ ...blockedObservation, slotType: 'DECISION', analysisId: null }), false);
                const observationHtml = opportunityCard(observation, '');
                assert.equal(observationHtml.includes('<strong>ETH</strong><span aria-hidden="true">/</span><small>Ethereum</small>'), true);
                assert.equal(observationHtml.includes('<small>方向</small><b data-live-field="direction" class="asset-card-unknown">—</b>'), true);
                assert.equal(observationHtml.includes('<small>置信</small><strong data-live-field="confidence">—</strong>'), true);
                assert.equal(observationHtml.includes('data-desktop-hover="risk"'), true);
                const unknownRiskReason = assetCardRiskDrawer({symbol:'ETHUSDT'});
                assert.ok(unknownRiskReason.includes('风险方向、信号时间或版本尚未匹配当前卡片'));
                assert.ok(!unknownRiskReason.includes('risk-evidence-item'));
                assert.equal(observationHtml.includes('尚无独立风险证据'), false);
                assert.match(observationHtml, /data-live-field="risk" class="asset-card-risk-unknown"[^>]*>—/);
                assert.equal(observationHtml.includes('data-desktop-hover="risk-status"'), false);
                assert.equal(desktop.riskDrawer(observation), '');
                assert.match(observationHtml, /1小时数据不足/);
                assert.match(observationHtml, /4小时趋势数据不足/);
                assert.equal(observationHtml.includes('<small>状态</small>'), false);
                assert.equal(observationHtml.includes('<small>数据</small>'), false);
                assert.equal(observationHtml.includes('数据新鲜'), false);
                const blockedHtml = opportunityCard(blockedObservation, '');
                assert.doesNotMatch(blockedHtml, /高风险观察|数据过期/);
                const ruleOnly = {
                  ...observation,
                  symbol: 'BNBUSDT', name: 'BNB', marketBias: 'BEARISH', marketBiasLabel: '偏空',
                  confidenceLabel: '—', confidenceLevel: null, riskLevel: null, riskLabel: '待评估'
                };
                const ruleOnlyHtml = opportunityCard(ruleOnly, '');
                assert.equal(ruleOnlyHtml.includes('data-live-field="direction" class="asset-card-unknown">—'), true);
                assert.equal(ruleOnlyHtml.includes('data-live-field="direction" class="semantic-value semantic-bearish">偏空'), false);
                assert.equal(ruleOnlyHtml.includes('data-live-field="confidence">—'), true);
                assert.equal(ruleOnlyHtml.includes('待重新分析'), false);
                const all = [
                  observation,
                  blockedObservation,
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
                assert.equal(nodes.opportunityHeading.textContent, '重点资产 · 6/6');
                renderOpportunities({ assets: all.slice(0, 2), selectedSymbol: 'ETHUSDT', snapshotComplete:true });
                assert.equal(nodes.opportunityHeading.textContent, '重点资产 · 2/6');
                assert.equal(nodes.opportunityEmpty.hidden, false);
                assert.equal(nodes.opportunityEmpty.textContent, '暂无更多合格资产');
                assert.equal((nodes.opportunityGrid.innerHTML.match(/class="opportunity-card/g)||[]).length,2);
                console.log('HOME_REAL_CARD_RUNTIME=PASS');
                """.formatted(semanticClass, validators, renderers);

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
