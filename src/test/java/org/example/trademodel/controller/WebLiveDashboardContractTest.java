package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
                "homePollIntervalMs === delay",
                "homeFallbackTimer = window.setInterval",
                "if (!document.hidden && !homeAbortController) lightweightHomeRefresh()",
                "homeStreamConnected ? 60000 : 15000",
                "scheduleHomeFallbackPoll();",
                "visibilitychange",
                "loadHome(selectedSymbol)",
                "stopHomeFallbackPoll()"
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

        assertThat(scheduler).contains("homePollIntervalMs === delay");
        assertThat(selection).contains("loadHome(selectedSymbol)")
                .doesNotContain("setInterval", "scheduleHomeFallbackPoll", "openOrResumeAssetAnalysis", "method: \"POST\"");
    }

    @Test
    void sseUsesOneSixtySecondReconcilerAndOnlyDisconnectUsesFifteenSecondPolling() throws Exception {
        runNode("""
                const assert=require('node:assert/strict'),fs=require('node:fs');
                const source=fs.readFileSync('src/main/resources/static/js/home-runtime.js','utf8');
                const handlers={},timers=new Map();let seq=0,refreshes=0,streams=0;
                let homeFallbackTimer=null,homePollIntervalMs=0,homeStreamConnected=false,homeEventSource=null,
                    homeRuntimeStarted=false,homeAbortController=null,homeLiveState='',currentHome={},selectedSymbol='BTCUSDT';
                const document={hidden:false,addEventListener:(type,fn)=>handlers[type]=fn};
                function EventSource(){streams++;this.close=()=>{};this.addEventListener=()=>{};}
                const window={EventSource,setInterval(fn,delay){timers.set(++seq,{fn,delay});return seq;},
                    clearInterval(id){timers.delete(id);},addEventListener(){}};
                const loadHome=async()=>{refreshes++;},lightweightHomeRefresh=loadHome,
                    renderHeader=()=>{},announce=()=>{},reportHomeRequestFailure=()=>{},applyHomeLiveEvent=()=>{};
                eval(source.slice(source.indexOf('function scheduleHomeFallbackPoll('),source.indexOf('function stableSubmissionId('))
                    + ';startHomeLiveRuntime();startHomeLiveRuntime();');
                assert.equal(streams,1);assert.equal(timers.size,1);assert.equal([...timers.values()][0].delay,15000);
                homeEventSource.onopen();homeEventSource.onopen();
                assert.equal(timers.size,1);assert.equal([...timers.values()][0].delay,60000);
                homeEventSource.onerror();homeEventSource.onerror();
                assert.equal(timers.size,1);assert.equal([...timers.values()][0].delay,15000);
                document.hidden=true;handlers.visibilitychange();assert.equal(timers.size,0);
                document.hidden=false;handlers.visibilitychange();assert.equal(refreshes,1);assert.equal(timers.size,1);
                assert.equal([...timers.values()][0].delay,15000);
                console.log('PASS');
                """);
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
                "const snapshot = await loadAssetPoolProjection()",
                "snapshot.poolMembers",
                "snapshot.assetPool",
                "items.forEach(function (asset)",
                "renderAssetPoolRows(items, projections)",
                "latestPrice",
                "marketBiasLabel",
                "window.TrineDesktopSemantics.confidenceText(live)",
                "window.TrineDesktopSemantics.riskSummary(live || {})",
                "oneHourOpportunityLabel",
                "fourHourTrendLabel",
                "directionCalculatedAt",
                "row.dataset.analysisId",
                "row.dataset.decisionId"
        ).doesNotContain(
                "/api/dashboard/home?limit=6",
                "items.map(loadAssetPoolProjection)",
                "&limit=1",
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

    @Test
    void desktopHoverTimersAndSharedReadOnlyPresentationBehaveAtRuntime() throws Exception {
        runNode("""
                const assert = require('node:assert/strict');
                const fs = require('node:fs');
                const source = fs.readFileSync('src/main/resources/static/js/home-runtime.js', 'utf8');
                const handlers = {}, timers = new Map(); let nextTimer = 0;
                function setTimeout(fn, delay) { timers.set(++nextTimer, {fn, delay}); return nextTimer; }
                function clearTimeout(id) { timers.delete(id); }
                function fire(delay) { [...timers].filter(([id,t]) => t.delay === delay).forEach(([id,t]) => {timers.delete(id); t.fn();}); }
                function node(type, parent) {
                  return {type, parent, dataset:{}, attrs:{}, hidden:false, style:{}, offsetWidth:620, offsetHeight:320,
                    contains(other) { for (let n=other; n; n=n.parent) if(n===this) return true; return false; },
                    closest(selector) { for(let n=this;n;n=n.parent) {
                      if (selector.includes('[data-desktop-hover]') && n.dataset.desktopHover) return n;
                      if (selector.includes('.opportunity-card') && n.type==='card') return n;
                      if (selector.includes('.desktop-hover-drawer') && n.className==='desktop-hover-drawer') return n;
                      if (selector.includes('.service-status-cell') && n.type==='service') return n;
                    } return null; },
                    setAttribute(k,v) {this.attrs[k]=v;}, addEventListener(){},
                    getBoundingClientRect(){return {left:500,top:160,bottom:190};}
                  };
                }
                let drawer;
                const document = {body:{appendChild(n){drawer=n;}}, createElement:()=>node('drawer'),
                  addEventListener(type,fn){(handlers[type] ||= []).push(fn);}};
                const window = {innerWidth:1440,innerHeight:900};
                eval(source.split('/* Desktop Home runtime */')[0]);
                const ui = window.TrineDesktopSemantics;
                assert.equal(ui.beijingTime('2026-09-07T05:03:00Z',true), '13:03');
                assert.equal(ui.beijingTime('2026-09-07T05:03:00',true), '13:03');
                assert.equal(ui.beijingTime('2026-09-07T13:03:00+08:00',true), '13:03');
                process.env.TZ='America/New_York';
                assert.equal(ui.beijingTime('2026-09-07T05:03:00Z',true), '13:03');
                assert.equal(ui.priceText('0.00001230'), '0.0000123');
                assert.equal(ui.priceText('79865.10'), '79,865.1');
                const asset={symbol:'BTCUSDT',riskItems:[{riskType:'LIQUIDITY_RISK',evidenceStatus:'AVAILABLE',currentValue:'72',score:72,severity:'HIGH',primaryEvidence:'<script>',source:'Binance',observedAt:'2026-09-07T05:03:00Z'}]};
                const risk=ui.riskDrawer(asset);
                assert.equal((risk.match(/class="risk-evidence-item"/g)||[]).length,1);
                assert.ok(risk.includes('&lt;script&gt;')); assert.ok(!risk.includes('<script>'));
                assert.ok(risk.includes('证据完整')); assert.ok(risk.includes('72'));
                assert.ok(risk.includes('7 项证据待补齐'));
                assert.ok(!risk.includes('<table>'));
                assert.ok(ui.riskSummary(asset).includes('risk-level-high'));
                assert.ok(!ui.riskSummary({riskItems:[{riskType:'LIQUIDITY_RISK',severity:'HIGH',score:100}]}).includes('risk-level-high'));
                assert.equal(ui.hasConfirmedRisks({riskItems:[]}),false);
                assert.equal(ui.riskDrawer({riskItems:[]}),'');
                assert.ok(ui.riskSummary({riskItems:[]}).includes('风险证据待更新'));
                assert.equal(ui.confidenceText({confidenceLabel:'高',confidenceLevel:'HIGH'}),'—');
                assert.equal(ui.confidenceText({confidenceLabel:'82%'}),'82%');
                assert.equal(ui.confidenceText({confidenceLevel:0}),'0%');
                assert.equal(ui.confidenceText({confidenceLevel:101}),'—');
                const precision={tickSize:'0.005',pricePrecision:3,priceMetadataSource:'BINANCE_SPOT_EXCHANGE_INFO_PRICE_FILTER'};
                assert.equal(ui.planPriceText('100.123456 – 101.7777',precision),'100.123 – 101.778');
                assert.equal(ui.planPriceText('100.123456',{}),'100.123456');
                const providers=['BINANCE_PUBLIC_MARKET_DATA','COINGLASS','OPENAI','GEMINI','XAI','MACRO_NEWS_CONTEXT','TELEGRAM'].map(name=>({name,status:'CONNECTED',connected:true,lastSuccessAt:'2026-09-07T05:03:00Z'}));
                providers[1]={name:'COINGLASS',status:'STALE',connected:true,lastSuccessAt:'2026-09-07T04:03:00Z'};
                const home={diagnostics:{providerReadiness:{providers}}};
                assert.equal(ui.serviceSummary(home),'4/5 · CoinGlass延迟');
                const service=ui.serviceDrawer(home);
                assert.equal((service.match(/<tr>/g)||[]).length,6);
                assert.ok(!/Telegram|MACRO|数据库|调度|心跳/.test(service));
                assert.ok(service.includes('尚未计划'));
                providers[1]={name:'COINGLASS',status:'FAIL_CLOSED',runtimeState:'RUNNING',
                  lastAttemptAt:'2026-09-07T05:04:00Z',lastSuccessAt:'2026-09-07T04:03:00Z',
                  providerDataAt:'2026-09-07T03:03:00Z',nextCheckAt:'2026-09-07T05:05:00Z'};
                assert.equal(ui.serviceSummary(home),'4/5 · CoinGlass检查中');
                const runtimeService=ui.serviceDrawer(home);
                assert.ok(runtimeService.includes('最近尝试')); assert.ok(runtimeService.includes('13:04'));
                assert.ok(runtimeService.includes('11:03')); assert.ok(runtimeService.includes('13:05'));
                assert.ok(runtimeService.includes('risk-level-unknown'));
                providers[1].runtimeState='RATE_LIMITED';
                assert.equal(ui.serviceSummary(home),'4/5 · CoinGlass限流');
                const card=node('card'), riskTrigger=node('trigger',card), inner=node('inner',riskTrigger), inner2=node('inner',riskTrigger);
                riskTrigger.dataset={desktopHover:'risk'};
                const serviceTrigger=node('service'); serviceTrigger.dataset={desktopHover:'service'};
                let opens=0;
                ui.installHoverDrawers(trigger=>{opens++; return trigger.dataset.desktopHover==='risk'?risk:service;});
                const handlerCount = Object.values(handlers).reduce((n,a)=>n+a.length,0);
                ui.installHoverDrawers(()=>{throw Error('duplicate hover controller');});
                assert.equal(Object.values(handlers).reduce((n,a)=>n+a.length,0),handlerCount);
                function emit(type,target,relatedTarget,key) { let stopped=false;
                  for(const fn of handlers[type]||[]) fn({target,relatedTarget,key,stopPropagation(){stopped=true;}});
                  return stopped;
                }
                emit('pointerover',inner); assert.equal(opens,0); assert.equal(timers.size,1);
                emit('pointerout',inner,inner2); emit('pointerover',inner2); assert.equal(timers.size,1);
                fire(250); assert.equal(opens,1); assert.equal(drawer.hidden,false);
                emit('pointerout',riskTrigger,card); fire(300); assert.equal(drawer.hidden,true);
                emit('focusin',riskTrigger); assert.equal(drawer.hidden,false);
                emit('pointerout',riskTrigger,drawer); fire(300); assert.equal(drawer.hidden,false);
                emit('pointerout',drawer,null); assert.equal(drawer.hidden,false); fire(300); assert.equal(drawer.hidden,true);
                emit('focusin',riskTrigger); assert.equal(drawer.hidden,false);
                assert.equal(emit('keydown',riskTrigger,null,'Enter'),true);
                emit('focusin',serviceTrigger); assert.equal(riskTrigger.attrs['aria-expanded'],'false');
                assert.ok(drawer.innerHTML.includes('核心服务')); assert.equal(serviceTrigger.attrs['aria-expanded'],'true');
                emit('keydown',serviceTrigger,null,'Escape'); assert.equal(drawer.hidden,true);
                assert.equal(serviceTrigger.attrs['aria-expanded'],'false');
                assert.equal(emit('click',riskTrigger),true);
                const shared=source.split('/* Desktop Home runtime */')[0];
                assert.ok(!/fetch\\(|analysis-preview|method:.*POST|user-positions/.test(shared));
                console.log('PASS');
                """);
    }

    @Test
    void sixCardsStayStableAndSelectedContextUsesTheSameSnapshotWithoutPosts() throws Exception {
        runNode("""
                const assert=require('node:assert/strict'), fs=require('node:fs');
                const source=fs.readFileSync('src/main/resources/static/js/home-runtime.js','utf8');
                const fn=source.slice(source.indexOf('async function loadHome('),source.indexOf('function liveAsset('));
                let homeRequestSequence=0,homeAbortController=null,homeCardSymbols=[],currentHome={}, stage=0, fail=false, errors=0;
                const initial=['BTCUSDT','ETHUSDT','SOLUSDT','BNBUSDT','ADAUSDT','XRPUSDT'];
                const window={location:{search:''}}, requests=[];
                const symbolOf=a=>a.rawSymbol||a.symbol;
                const validOpportunityCard=()=>true, validObservationCard=()=>true;
                function render(home){currentHome=home;} function clearHomeRequestFailure(){} function announce(){}
                function reportHomeRequestFailure(){errors++;}
                function asset(symbol){return {symbol,analysisId:'run-'+symbol+'-'+stage,decisionId:'decision-'+symbol+'-'+stage,snapshotId:'snapshot-'+stage};}
                async function api(url,options){
                  requests.push({url,options}); if(fail) throw Error('timeout');
                  const q=new URL(url,'https://example.test').searchParams, selected=q.get('selectedSymbol')||initial[0];
                  const members=stage===0?initial:initial.filter(s=>s!==selected).concat('LINKUSDT');
                  return {assets:members.map(asset),assetPool:initial.concat('LINKUSDT').map(asset),snapshotComplete:true,
                    snapshotId:'snapshot-'+stage,projectionVersion:stage+1,selectedAssetContext:asset(selected),selectedSymbol:selected};
                }
                const load=eval(fn+';loadHome');
                (async()=>{
                  await load(initial[0]); assert.deepEqual(homeCardSymbols,initial);
                  for(const symbol of initial){stage++; await load(symbol);
                    assert.deepEqual(currentHome.assets.map(symbolOf),initial);
                    assert.equal(currentHome.assets.find(a=>a.symbol===symbol).decisionId,currentHome.selectedAssetContext.decisionId);
                  }
                  assert.equal(requests.length,7); // one complete GET, not per-missing-card fanout
                  const preserved=currentHome; fail=true; await load(initial[0]);
                  assert.equal(currentHome,preserved); assert.equal(errors,1); fail=false;
                  const queue=[]; api=(url,options)=>new Promise(resolve=>queue.push(resolve));
                  const old=load('BTCUSDT'), newer=load('ETHUSDT');
                  const fresh={assets:initial.map(asset),assetPool:initial.map(asset),snapshotComplete:true,snapshotId:'snapshot-'+stage,
                    projectionVersion:stage+2,selectedAssetContext:asset('ETHUSDT')};
                  queue[1](fresh); await newer;
                  queue[0]({assets:[],selectedAssetContext:asset('BTCUSDT')}); await old;
                  assert.equal(currentHome.selectedAssetContext.symbol,'ETHUSDT');
                  const complete=currentHome;
                  api=async()=>({...fresh,projectionVersion:stage+3,assets:[],assetPool:[]});
                  await load('ETHUSDT'); assert.equal(currentHome,complete);
                  api=async()=>({...fresh,projectionVersion:1});
                  await load('ETHUSDT'); assert.equal(currentHome,complete);
                  assert.ok(requests.every(r=>!r.options.method || r.options.method==='GET'));
                  assert.ok(requests.every(r=>r.url.startsWith('/api/dashboard/home?')));
                  console.log('PASS');
                })().catch(e=>{console.error(e);process.exitCode=1;});
                """);
    }

    @Test
    void conditionalPlanCannotUseAnotherSnapshotOrManufactureMissingPrices() throws Exception {
        runNode("""
                const assert=require('node:assert/strict'),fs=require('node:fs');
                const source=fs.readFileSync('src/main/resources/static/js/home-runtime.js','utf8');
                const window={}; eval(source.split('/* Desktop Home runtime */')[0]);
                const desktop=window.TrineDesktopSemantics;
                const nodes={planContent:{innerHTML:''},planDetailLink:{hidden:false}};
                const document={getElementById:id=>nodes[id]}, setText=()=>{};
                const symbolOf=a=>a.symbol, has=v=>v!==null&&v!==undefined&&v!=='';
                const text=(v,f)=>has(v)?String(v):f, escapeHtml=v=>String(v);
                const selectedFinalAccess=home=>({plan:home.executionSuggestion,visible:false});
                const fn=source.slice(source.indexOf('function planField('),source.indexOf('function collectionLabel('));
                const drawPlan=eval(fn+';renderPlan');
                const asset={symbol:'BTCUSDT',analysisId:'a1',decisionId:'d1',traceId:'t1',marketBias:'BULLISH'};
                const plan={sourceAnalysisId:'a1',sourceDecisionId:'d1',sourceTraceId:'t1',planLifecycleState:'WAITING_TRIGGER',
                  entryZone:'100 – 101',stopLoss:'98',takeProfitRules:'TP1 105；TP2 108',invalidCondition:'价格跌破98'};
                function html(a,p){drawPlan({selectedAssetContext:a,executionSuggestion:p,nextOneHourCloseAt:'2026-09-07T13:00:00Z'});return nodes.planContent.innerHTML;}
                assert.ok(html(asset,{...plan,planLifecycleState:'CURRENT',status:'RULE_CONDITIONAL_PLAN',notTradeInstruction:true}).includes('TP1'));
                assert.ok(html(asset,{...plan,planLifecycleState:'CURRENT',status:'RULE_CONDITIONAL_PLAN',notTradeInstruction:true}).includes('规则参考'));
                assert.ok(!html(asset,{...plan,planLifecycleState:'SUPERSEDED'}).includes('100 – 101'));
                for(const bias of ['STRONG_BULLISH','BULLISH','WEAK_BULLISH','WEAK_BEARISH','BEARISH','STRONG_BEARISH']) {
                  const result=html({...asset,marketBias:bias},plan);
                  assert.ok(result.includes('条件计划 · 等待触发'));
                  for(const value of ['100 – 101','98','105','108','触发后校验：行情新鲜度、方向状态和失效位']) assert.ok(result.includes(value));
                  assert.ok(!result.includes('恢复条件'));
                }
                const blocked=html(asset,{...plan,planLifecycleState:'SUSPENDED',validationStatus:'BLOCKED',pauseReason:'流动性风险72，高于执行上限70'});
                assert.ok(blocked.includes('条件计划 · 当前暂缓执行')); assert.ok(blocked.includes('暂停：流动性风险72，高于执行上限70'));
                assert.ok(!blocked.includes('已阻断'));
                const internal=html(asset,{...plan,validationStatus:'BLOCKED',blockedReason:'ANALYSIS_PREVIEW_NON_FINAL'});
                assert.ok(!internal.includes('ANALYSIS_PREVIEW_NON_FINAL'));
                assert.ok(internal.includes('服务端未提供规则暂停证据'));
                for(const field of ['sourceAnalysisId','sourceDecisionId','sourceTraceId']) {
                  assert.ok(!html(asset,{...plan,[field]:'another-run'}).includes('100 – 101'));
                }
                const conflict=html({...asset,marketBias:'TIMEFRAME_CONFLICT',oneHourOpportunityLabel:'1小时偏空',fourHourTrendLabel:'4小时偏多'},plan);
                assert.ok(conflict.includes('周期冲突：1小时偏空 / 4小时偏多')); assert.ok(!conflict.includes('100 – 101'));
                assert.ok(conflict.includes('暂无条件计划')); assert.ok(conflict.includes('当前动作：等待'));
                assert.ok(conflict.includes('下一根1小时K线闭合后')); assert.ok(conflict.includes('21:00'));
                const missing=html({...asset,marketBias:'INSUFFICIENT_DATA',marketBiasLabel:'数据不足',oneHourOpportunityLabel:'1小时有效',fourHourTrendLabel:'缺少4小时闭合K线'},plan);
                assert.ok(missing.includes('缺少4小时闭合K线')); assert.ok(!missing.includes('100 – 101'));
                assert.ok(html(asset,{...plan,takeProfitRules:null}).includes('可分别展示的 TP1、TP2'));
                assert.ok(!html(asset,{...plan,takeProfitRules:null}).includes('100 – 101'));
                console.log('PASS');
                """);
    }

    @Test
    void completeAssetPoolKeepsAllMembersAndRejectsOlderOrThinResponses() throws Exception {
        runNode("""
                const assert=require('node:assert/strict'),fs=require('node:fs');
                const source=fs.readFileSync('src/main/resources/static/js/workspace.js','utf8');
                const functions=source.slice(source.indexOf('async function loadAssetPoolProjection()'),source.indexOf('function updatePoolScanCta('));
                let assetPoolProjections=[],assetPoolItems=[],assetPoolLoaded=false,poolScanRuntime=null,assetPoolProjectionVersion=0;
                let waiters=[], renders=[], messages=[];
                const api=path=>{assert.equal(path,'/api/dashboard/runtime-snapshot');return new Promise(resolve=>waiters.push(resolve));};
                const document={getElementById:()=>null};
                const renderAssetPoolRows=(items,projections)=>renders.push({items,projections});
                const updatePoolScanCta=()=>{},announce=message=>messages.push(message),empty=()=>{throw Error('unexpected empty');};
                function snapshot(version){const poolMembers=Array.from({length:36},(_,i)=>({symbol:'ASSET'+i}));
                  return {snapshotComplete:true,snapshotId:'snapshot-'+version,projectionVersion:version,poolMembers,
                    assets:poolMembers.slice(version%10,version%10+6),assetPool:poolMembers.map(p=>({...p,rawSymbol:p.symbol,
                      snapshotId:'snapshot-'+version,analysisId:'analysis-'+version+'-'+p.symbol,decisionId:'decision-'+version+'-'+p.symbol}))};}
                eval(functions);
                (async()=>{
                  const older=loadAssetPool(),newer=loadAssetPool();
                  waiters[1](snapshot(2));await newer;waiters[0](snapshot(1));await older;
                  assert.equal(renders.length,1);assert.equal(assetPoolItems.length,36);assert.equal(assetPoolProjectionVersion,2);
                  assert.equal(assetPoolProjections[0].analysisId,'analysis-2-ASSET0');
                  const partial=loadAssetPool(),thin=snapshot(3);thin.assetPool.pop();waiters[2](thin);await partial;
                  assert.equal(renders.length,1);assert.equal(assetPoolItems.length,36);assert.equal(messages.length,1);
                  const refresh=loadAssetPool();waiters[3](snapshot(4));await refresh;
                  assert.deepEqual(assetPoolItems.map(p=>p.symbol),snapshot(1).poolMembers.map(p=>p.symbol));
                  assert.equal(renders.length,2);console.log('PASS');
                })().catch(e=>{console.error(e);process.exitCode=1;});
                """);
    }

    private static void runNode(String script) throws Exception {
        Process process = new ProcessBuilder("node", "-e", script).redirectErrorStream(true).start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) process.destroyForcibly();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("PASS");
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
