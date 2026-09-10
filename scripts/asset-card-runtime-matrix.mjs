import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";
import { spawnSync } from "node:child_process";

const source = fs.readFileSync("src/main/resources/static/js/home-runtime.js", "utf8");
assert.ok(source.includes("function applyAssetCardEvent("), "production Home must own a field-only asset-card event handler");
const bootstrap = "    desktop.installHoverDrawers(function (trigger) {";
assert.equal(source.split(bootstrap).length, 2);
const hoverBody = source.slice(source.indexOf(bootstrap) + bootstrap.length, source.indexOf("    bindSearch();", source.indexOf(bootstrap)))
  .trim().replace(/\}\);$/, "");
const exported = `globalThis.cardTest = {
  opportunityCard, applyAssetCardEvent, mergeAssetCardSnapshot, lightweightHomeRefresh, assetCardRiskDrawer,
  scheduleHomeFallbackPoll, stopHomeFallbackPoll, connectHomeStream, assetCardClock,
  setHome(value) { currentHome = value; homeCardSymbols = value.assets.map(symbolOf);
    value.assets.forEach(asset => opportunityCard(asset, value.selectedSymbol)); },
  setConnected(value) { homeStreamConnected = value; },
  stream() { return homeEventSource; },
  snapshot(symbol) { return assetCardSnapshots.get(symbol); },
  riskDrawerForTrigger(trigger) { ${hoverBody} },
  setApi(value) { api = value; },
  setHomeLoader(value) { loadHome = value; },
  applyHomeLiveEvent
}; return;
`;

function fixture() {
  let nextTimer = 1;
  const timeouts = new Map(), intervals = new Map(), requests = [], nodes = new Map();
  function element() {
    return { textContent: "", innerHTML: "", className: "", hidden: false, attributes: {},
      setAttribute(key, value) { this.attributes[key] = value; },
      removeAttribute(key) { delete this.attributes[key]; } };
  }
  for (const symbol of ["BTCUSDT", "ETHUSDT"]) {
    const fields = new Map(["price", "direction", "confidence", "status", "risk", "risk-items", "one-hour", "four-hour", "card-time"]
      .map(name => [name, element()]));
    nodes.set(symbol, { ...element(), fields,
      querySelector(selector) { return fields.get(selector.match(/data-live-field="([^"]+)"/)?.[1]) || null; } });
  }
  const document = { body: { dataset: { pageKey: "home" } }, hidden: false,
    querySelector(selector) { return selector.startsWith("meta") ? null : nodes.get(selector.match(/data-symbol="([^"]+)"/)?.[1]) || null; },
    getElementById() { return null; }, addEventListener() {} };
  const window = { CSS: { escape: value => value }, TradeModelFrontendContract: {},
    setTimeout(callback, delay) { const id = nextTimer++; timeouts.set(id, { callback, delay }); return id; },
    clearTimeout(id) { timeouts.delete(id); },
    setInterval(callback, delay) { const id = nextTimer++; intervals.set(id, { callback, delay }); return id; },
    clearInterval(id) { intervals.delete(id); }, addEventListener() {}, location: { search: "" } };
  class EventSource {
    constructor(url) { this.url = url; this.listeners = {}; }
    addEventListener(type, callback) { this.listeners[type] = callback; }
    close() { this.closed = true; }
  }
  window.EventSource = EventSource;
  const context = vm.createContext({ window, document, console, URLSearchParams, AbortController,
    EventSource, Intl, Date, setTimeout: window.setTimeout, clearTimeout: window.clearTimeout });
  vm.runInContext(source.replace(bootstrap, exported + bootstrap), context);
  const home = { assets: ["BTCUSDT", "ETHUSDT"].map((symbol, index) => ({ assetId: index + 1, rawSymbol: symbol,
    name: symbol, slot: index + 1, homePinned: true, finalConfidence: 99, latestPrice: 99999,
    cardSignalDisplayEnabled: true, hasFinal: true, marketBiasLabel: "偏空",
    finalMarketBias: "STRONG_BEARISH", riskLevel: "LOW" })), selectedSymbol: "BTCUSDT",
    positions: [{ positionId: "123", markPrice: 101 }], executionSuggestion: { status: "BLOCKED" },
    aiDecision: { tabs: [{ role: "GPT_FINAL" }] } };
  context.cardTest.setHome(home);
  context.cardTest.setApi(async (url, options) => { requests.push({ url, options }); return []; });
  return { ...context.cardTest, context, window, document, home, nodes, intervals, requests, timeouts,
    flushPrices() { for (const [id, timer] of [...timeouts]) { timeouts.delete(id); timer.callback(); } } };
}
function snapshot(symbol = "BTCUSDT", version = 1) {
  return { symbol, assetName: "Bitcoin", spotPrice: 100, latestPriceAt: "2026-09-10T00:00:00Z",
    signal: { direction: "LONG", status: "VALID", calibratedConfidence: 72, pLong: .72, pShort: .2,
      oneHourState: "OPPORTUNITY", fourHourTrend: "LONG", signalAsOf: "2026-09-10T00:00:00Z" },
    risk: { overallLevel: "HIGH", items: [{ type: "CHASE", assessmentStatus: "ASSESSED", level: "HIGH",
      evidenceValue: 2, source: "SPOT", asOf: "2026-09-10T00:00:00Z", reason: "结构延伸" }], riskAsOf: "2026-09-10T00:00:00Z" },
    health: { status: "HEALTHY" }, cardAsOf: "2026-09-10T00:00:00Z", snapshotVersion: version,
    featureVersion: "test-only-features", modelVersion: "test-only-model", calibrationVersion: "test-only-calibration" };
}
function event(type, version, fields, symbol = "BTCUSDT") {
  return { eventType: `ASSET_CARD_${type}`, symbol, snapshotVersion: version, payload: { symbol, snapshotVersion: version, ...fields } };
}

// The explicit server display cohort, never snapshot presence, owns the renderer switch.
for (const flag of [false, undefined, "true"]) {
  const legacy = fixture();
  const assets = legacy.home.assets.map(asset => ({ ...asset, cardSignalDisplayEnabled: flag, cardSignal: snapshot(asset.rawSymbol),
    riskItems: [{ riskType: "EVENT_RISK", evidenceStatus: "AVAILABLE", severity: "HIGH", currentValue: "2",
      source: "fixture-legacy-event", observedAt: "2026-09-10T00:00:00Z", primaryEvidence: "legacy event evidence" }] }));
  legacy.setHome({ ...legacy.home, assets });
  let legacyLoads = 0;
  legacy.setHomeLoader(async () => { legacyLoads++; });
  for (const asset of assets) {
    const html = legacy.opportunityCard(asset, "BTCUSDT");
    assert.ok(html.includes('data-live-field="confidence">99%'), "disabled/SHADOW/outside-CANARY preserves the existing visible confidence");
    assert.ok(html.includes('data-live-field="price">$99,999'), "the non-cohort keeps its pre-switch price renderer");
    assert.equal((html.match(/data-live-field="confidence"/g) || []).length, 1);
    assert.ok(html.includes('data-desktop-hover="risk"'), "outside-cohort keeps its actual evidence risk interaction");
    assert.ok(legacy.riskDrawerForTrigger({ dataset: { desktopHover: "risk", riskSymbol: asset.rawSymbol } }).includes("fixture-legacy-event"));
    assert.ok(!html.includes("72%"), "an attached SHADOW result cannot select the new renderer");
    for (const group of ["PRICE", "SIGNAL", "RISK", "HEALTH"]) legacy.applyAssetCardEvent(event(group, 20, snapshot(asset.rawSymbol), asset.rawSymbol));
    assert.equal(legacy.snapshot(asset.rawSymbol), undefined, "non-cohort ignores all independent card SSE groups");
  }
  await legacy.lightweightHomeRefresh();
  assert.equal(legacy.requests.length, 0, "no card-only GET when no displayed symbol is in the cohort");
  assert.equal(legacyLoads, 1, "disabled/SHADOW retains the existing periodic Home read, not blank frozen legacy cards");
  legacy.setConnected(true); legacy.scheduleHomeFallbackPoll();
  assert.equal([...legacy.intervals.values()][0].delay, 60000);
  for (let i = 0; i < 3; i++) { [...legacy.intervals.values()][0].callback(); await Promise.resolve(); }
  assert.equal(legacyLoads, 4);
  legacy.setConnected(false); legacy.scheduleHomeFallbackPoll();
  assert.equal(legacy.intervals.size, 1);
  assert.equal([...legacy.intervals.values()][0].delay, 15000);
}
const cohort = fixture();
const cohortAssets = cohort.home.assets.map((asset, index) => ({ ...asset, cardSignalDisplayEnabled: index === 0 }));
cohort.setHome({ ...cohort.home, assets: cohortAssets });
cohort.mergeAssetCardSnapshot(snapshot("BTCUSDT", 2), false);
cohort.applyAssetCardEvent(event("PRICE", 4, { spotPrice: 444, latestPriceAt: "2026-09-10T00:00:04Z" }));
let mixedHomeReads = 0, mixedHtml;
cohort.setHomeLoader(async () => {
  mixedHomeReads++;
  mixedHtml = cohortAssets.map(asset => cohort.opportunityCard({ ...asset, cardSignal: snapshot(asset.rawSymbol, 1) }, "BTCUSDT")).join("");
});
cohort.setApi(async () => { throw new Error("mixed cohort must not issue a second concurrent card GET"); });
await cohort.lightweightHomeRefresh();
assert.equal(mixedHomeReads, 1);
assert.equal(cohort.snapshot("BTCUSDT").snapshotVersion, 4);
assert.equal(cohort.snapshot("BTCUSDT").spotPrice, 444, "a periodic legacy Home result cannot overwrite a newer card SSE field");
assert.equal(cohort.snapshot("ETHUSDT"), undefined, "an unexpected non-cohort GET row cannot patch legacy cards");
assert.ok(cohort.opportunityCard(cohortAssets[1], "BTCUSDT").includes('data-live-field="confidence">99%'));
assert.equal((mixedHtml.match(/data-live-field="confidence"/g) || []).length, 2, "mixed cohorts still have one visible confidence per card");
assert.deepEqual([...mixedHtml.matchAll(/data-symbol="([^"]+)"/g)].map(match => match[1]), ["BTCUSDT", "ETHUSDT"]);
for (const invalid of [null, { ...snapshot("BTCUSDT", 3), modelVersion: null },
  { ...snapshot("BTCUSDT", 4), signal: { direction: null, status: "INSUFFICIENT_DATA" } }]) {
  const switched = fixture();
  const asset = { ...switched.home.assets[0], cardSignalDisplayEnabled: true, cardSignal: invalid };
  const html = switched.opportunityCard(asset, "BTCUSDT");
  assert.ok(html.includes('data-live-field="confidence">—'));
  assert.ok(!html.includes("99%") && !html.includes("99,999"), "switched missing/invalid model is fail-closed, never a legacy fallback");
}
const changedCohort = fixture();
let finishPriorCohortRead;
changedCohort.setApi(() => new Promise(resolve => { finishPriorCohortRead = resolve; }));
const priorCohortRead = changedCohort.lightweightHomeRefresh();
changedCohort.setHome({ ...changedCohort.home, assets: changedCohort.home.assets.map(asset => ({ ...asset, cardSignalDisplayEnabled: false })) });
changedCohort.setHome(changedCohort.home);
finishPriorCohortRead([snapshot("BTCUSDT", 100)]);
await priorCohortRead;
assert.equal(changedCohort.snapshot("BTCUSDT"), undefined, "a request from a previous display cohort cannot revive its old model result after re-entry");
const reentry = fixture();
const reentryAsset = { ...reentry.home.assets[0], cardSignal: snapshot("BTCUSDT", 10) };
reentry.opportunityCard(reentryAsset, "BTCUSDT");
reentry.opportunityCard({ ...reentryAsset, cardSignalDisplayEnabled: false }, "BTCUSDT");
assert.ok(reentry.opportunityCard(reentryAsset, "BTCUSDT").includes('data-live-field="confidence">72%'),
  "a fresh complete cohort projection is not hidden by field watermarks from its previous membership");

const f = fixture();
const directions = { STRONG_LONG: "强偏多", LONG: "偏多", WEAK_LONG: "弱偏多", STRONG_SHORT: "强偏空",
  SHORT: "偏空", WEAK_SHORT: "弱偏空", RANGE: "震荡", WATCH: "观望" };
let version = 1;
for (const [direction, label] of Object.entries(directions)) {
  const card = snapshot("BTCUSDT", version++); card.signal.direction = direction;
  f.mergeAssetCardSnapshot(card, false);
  const html = f.opportunityCard(f.home.assets[0], "BTCUSDT");
  assert.ok(html.includes(label), direction);
  assert.ok(!html.includes("99%") && !html.includes("99,999"), "legacy confidence and Mark price must be unreachable");
  if (["RANGE", "WATCH"].includes(direction)) assert.ok(!html.includes("72%"));
}
const missing = snapshot("BTCUSDT", version++);
missing.signal = { direction: null, status: "INSUFFICIENT_DATA" }; missing.spotPrice = null;
f.mergeAssetCardSnapshot(missing, false);
let html = f.opportunityCard(f.home.assets[0], "BTCUSDT");
assert.ok(html.includes("数据不足") && !html.includes("观望") && !html.includes("99%") && !html.includes("99,999"));
const invalid = snapshot("BTCUSDT", version++); invalid.signal.status = "INVALIDATED";
f.mergeAssetCardSnapshot(invalid, false);
html = f.opportunityCard(f.home.assets[0], "BTCUSDT");
assert.ok(html.includes("偏多") && html.includes("已失效") && !html.includes("72%"));
const unvalidated = snapshot("BTCUSDT", version++); unvalidated.modelVersion = null;
f.mergeAssetCardSnapshot(unvalidated, false);
assert.ok(!f.opportunityCard(f.home.assets[0], "BTCUSDT").includes("72%"));
for (const status of ["SHADOW", "UNVALIDATED", "UNKNOWN"]) {
  const shadow = snapshot("BTCUSDT", version++); shadow.signal.status = status;
  f.mergeAssetCardSnapshot(shadow, false);
  const hiddenModel = f.opportunityCard(f.home.assets[0], "BTCUSDT");
  assert.ok(!hiddenModel.includes("72%") && hiddenModel.includes("数据不足"), status);
}
const metadata = fixture();
metadata.mergeAssetCardSnapshot(snapshot("BTCUSDT", 1), true);
metadata.applyAssetCardEvent(event("SIGNAL", 2, { signal: { ...snapshot().signal, direction: "SHORT", calibratedConfidence: 68 } }));
assert.equal(metadata.nodes.get("BTCUSDT").fields.get("confidence").textContent, "—", "a new SIGNAL without its own bundle metadata cannot borrow an old validated version");
metadata.applyAssetCardEvent(event("SIGNAL", 3, { signal: { ...snapshot().signal, direction: "SHORT", calibratedConfidence: 68 },
  featureVersion: "test-only-features-v2", modelVersion: "test-only-model-v2", calibrationVersion: "test-only-calibration-v2" }));
assert.equal(metadata.nodes.get("BTCUSDT").fields.get("confidence").textContent, "68%");
assert.equal(metadata.snapshot("BTCUSDT").modelVersion, "test-only-model-v2");
metadata.applyAssetCardEvent(event("PRICE", 4, { spotPrice: 104, modelVersion: "unrelated-price-metadata" }));
assert.equal(metadata.snapshot("BTCUSDT").modelVersion, "test-only-model-v2", "PRICE cannot update SIGNAL bundle provenance");

const base = snapshot("BTCUSDT", 100);
f.mergeAssetCardSnapshot(base, false);
const homeBefore = JSON.stringify(f.home), ethBefore = JSON.stringify(f.nodes.get("ETHUSDT"));
const clockBefore = f.snapshot("BTCUSDT").cardAsOf;
f.applyAssetCardEvent(event("PRICE", 104, { spotPrice: 104, latestPriceAt: "2026-09-10T00:00:04Z", cardAsOf: "2099-01-01T00:00:00Z", signal: { direction: "SHORT" } }));
f.applyAssetCardEvent(event("PRICE", 105, { spotPrice: 105, latestPriceAt: "2026-09-10T00:00:05Z" }));
assert.equal(f.timeouts.size, 1, "one throttled price render per symbol");
assert.ok([...f.timeouts.values()].every(timer => timer.delay >= 1000 && timer.delay <= 2000));
assert.equal(f.nodes.get("BTCUSDT").fields.get("price").textContent, "");
f.applyAssetCardEvent(event("SIGNAL", 102, { signal: { ...base.signal, direction: "SHORT", calibratedConfidence: 68 }, cardAsOf: "2026-09-10T00:00:02Z", spotPrice: 1 }));
assert.equal(f.snapshot("BTCUSDT").signal.direction, "SHORT", "price version must not suppress newer signal group");
assert.equal(f.snapshot("BTCUSDT").spotPrice, 105);
assert.equal(f.nodes.get("BTCUSDT").fields.get("direction").textContent, "偏空");
f.applyAssetCardEvent(event("SIGNAL", 101, { signal: base.signal }));
assert.equal(f.snapshot("BTCUSDT").signal.direction, "SHORT");
const signalBeforeHealth = JSON.stringify(f.snapshot("BTCUSDT").signal);
f.applyAssetCardEvent(event("HEALTH", 110, { health: { status: "STALE" }, signal: base.signal, cardAsOf: "2099-01-01T00:00:00Z" }));
assert.equal(JSON.stringify(f.snapshot("BTCUSDT").signal), signalBeforeHealth);
assert.equal(f.snapshot("BTCUSDT").cardAsOf, "2026-09-10T00:00:02Z");
f.applyAssetCardEvent(event("RISK", 103, { risk: { overallLevel: null, items: [{ type: "DATA", assessmentStatus: "UNKNOWN", level: "LOW" }] }, cardAsOf: "2026-09-10T00:00:03Z" }));
assert.equal(f.nodes.get("BTCUSDT").fields.get("risk").textContent, "—");
assert.equal(f.nodes.get("BTCUSDT").fields.get("risk-items").hidden, true);
f.applyAssetCardEvent(event("PRICE", 106, { spotPrice: 1 }, "SOLUSDT"));
f.applyAssetCardEvent({ ...event("PRICE", 106, { spotPrice: 1 }), payload: { symbol: "ETHUSDT", snapshotVersion: 106, spotPrice: 1 } });
assert.equal(f.snapshot("BTCUSDT").spotPrice, 105);
f.flushPrices();
assert.equal(f.nodes.get("BTCUSDT").fields.get("price").textContent, "$105");
assert.equal(JSON.stringify(f.home), homeBefore, "events must not mutate Home, slots, pins, plans, AI or positions");
assert.equal(JSON.stringify(f.nodes.get("ETHUSDT")), ethBefore, "events must not touch another card");
assert.equal(f.requests.length, 0, "card events never call Home or another endpoint");
assert.notEqual(f.snapshot("BTCUSDT").cardAsOf, clockBefore);
const latestBeforeRebuild = JSON.stringify(f.snapshot("BTCUSDT"));
f.opportunityCard({ ...f.home.assets[0], cardSignal: base }, "BTCUSDT");
assert.equal(JSON.stringify(f.snapshot("BTCUSDT")), latestBeforeRebuild, "an older full Home rebuild cannot overwrite live card groups");
for (const invalidVersion of [null, 0, -1, "invalid", Number.MAX_SAFE_INTEGER + 1]) {
  f.mergeAssetCardSnapshot({ ...base, snapshotVersion: invalidVersion, spotPrice: 1 }, true);
  assert.equal(JSON.stringify(f.snapshot("BTCUSDT")), latestBeforeRebuild);
}
const commonVersion = 120;
for (const [type, payload] of [["PRICE", { spotPrice: 120 }], ["SIGNAL", { signal: base.signal }],
  ["RISK", { risk: base.risk }], ["HEALTH", { health: base.health }]]) {
  f.applyHomeLiveEvent(event(type, commonVersion, payload));
}
assert.equal(f.snapshot("BTCUSDT").spotPrice, 120);
assert.equal(f.snapshot("BTCUSDT").signal.direction, "LONG");
assert.equal(f.snapshot("BTCUSDT").risk.overallLevel, "HIGH");
assert.equal(f.snapshot("BTCUSDT").health.status, "HEALTHY");
assert.equal(f.requests.length, 0);
f.flushPrices();
const streamOnly = fixture();
streamOnly.mergeAssetCardSnapshot(base, false);
for (let tick = 0; tick < 1000; tick++) {
  streamOnly.applyHomeLiveEvent(event("PRICE", 200 + tick, { spotPrice: 100 + tick }));
  streamOnly.applyHomeLiveEvent({ eventType: "ASSET_PRICE_UPDATED", symbol: "BTCUSDT", snapshotVersion: tick + 1 });
}
assert.equal(streamOnly.timeouts.size, 1, "a price burst never queues a full Home refresh");
assert.equal(streamOnly.requests.length, 0);
for (const type of ["ASSET_DIRECTION_UPDATED", "ASSET_RISK_UPDATED", "PLAN_STATE_CHANGED", "POSITION_MONITOR_UPDATED", "SYSTEM_STATUS_UPDATED", "DATA_SOURCE_STATUS_CHANGED"]) {
  const legacy = fixture();
  legacy.applyHomeLiveEvent({ eventType: type, symbol: "SYSTEM", eventId: "heartbeat-1", snapshotVersion: 1 });
  assert.equal(legacy.timeouts.size, 1, `${type} retains its existing Home refresh behavior`);
  assert.equal([...legacy.timeouts.values()][0].delay, 250);
}
const risks = snapshot("BTCUSDT", 121);
risks.risk = { overallLevel: "HIGH", items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "DATA"].map((type, index) => ({
  type, assessmentStatus: index === 4 ? "UNKNOWN" : "ASSESSED", level: index === 4 ? "LOW" : "HIGH",
  asOf: `2026-09-10T00:00:0${index}Z`, source: "test-only", evidenceValue: 2 })) };
f.mergeAssetCardSnapshot(risks, false);
html = f.opportunityCard(f.home.assets[0], "BTCUSDT");
assert.equal((html.match(/<span class="asset-card-risk-high">/g) || []).length, 3);
assert.ok(html.includes('data-desktop-hover="risk"') && html.includes('aria-haspopup="dialog"'), "real active risks retain hover/focus drawer access");
const drawer = f.assetCardRiskDrawer(f.snapshot("BTCUSDT"));
assert.equal((drawer.match(/risk-evidence-item/g) || []).length, 3);
assert.ok(drawer.includes("test-only") && drawer.includes("指标值：2"));
assert.equal(f.assetCardRiskDrawer({ risk: { items: [{ type: "DATA", assessmentStatus: "UNKNOWN", level: "HIGH" }] } }), "");
assert.equal(f.assetCardRiskDrawer({ risk: { items: [{ type: "DATA", assessmentStatus: "ASSESSED", level: "NONE" }] } }), "");
const orderedRisk = fixture();
const orderedSnapshot = snapshot();
orderedSnapshot.signal.status = "INVALIDATED";
orderedSnapshot.risk.items = [
  { type: "CHASE", assessmentStatus: "ASSESSED", level: "MEDIUM", invalidatesSignal: true, asOf: "2026-09-10T00:00:10Z" },
  { type: "SHOCK", assessmentStatus: "ASSESSED", level: "HIGH", invalidatesSignal: true, asOf: "2026-09-10T00:00:02Z" },
  { type: "EVENT", assessmentStatus: "ASSESSED", level: "HIGH", invalidatesSignal: false, asOf: "2026-09-10T00:00:03Z" },
  { type: "REVERSAL", assessmentStatus: "ASSESSED", level: "HIGH", invalidatesSignal: true, asOf: "2026-09-10T00:00:01Z" }
];
orderedRisk.mergeAssetCardSnapshot(orderedSnapshot, true);
const sortedRiskHtml = orderedRisk.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML;
assert.deepEqual([...sortedRiskHtml.matchAll(/>([^<]+)<\/span>/g)].map(match => match[1]), ["急涨急跌·高", "反转·高", "事件·高"],
  "card risk items are capped at three and ordered by severity, invalidation effect, then freshness");
orderedRisk.mergeAssetCardSnapshot({ ...orderedSnapshot, snapshotVersion: 2, risk: { overallLevel: "MEDIUM", items: [
  { type: "REVERSAL", assessmentStatus: "ASSESSED", level: "MEDIUM", invalidatesSignal: true, asOf: "2026-09-10T00:00:01Z" },
  { type: "EVENT", assessmentStatus: "ASSESSED", level: "MEDIUM", invalidatesSignal: false, asOf: "2026-09-10T00:00:03Z" }
] } }, true);
assert.deepEqual([...orderedRisk.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.matchAll(/>([^<]+)<\/span>/g)]
  .map(match => match[1]), ["事件·中", "反转·中"], "only HIGH explicit invalidators receive priority; medium items still use freshness");
assert.ok(!html.includes("数据·低") && !html.includes("更新于") && !html.includes("24h") && !html.includes("test-only"));
const clockAfterRisk = f.snapshot("BTCUSDT").cardAsOf;
f.mergeAssetCardSnapshot({ ...risks, snapshotVersion: 122, cardAsOf: "2099-01-01T00:00:00Z" }, false);
assert.equal(f.snapshot("BTCUSDT").cardAsOf, clockAfterRisk, "unchanged polling does not advance card clock");

f.setConnected(false); f.scheduleHomeFallbackPoll(); f.scheduleHomeFallbackPoll();
assert.equal(f.intervals.size, 1); assert.equal([...f.intervals.values()][0].delay, 15000);
f.setConnected(true); f.scheduleHomeFallbackPoll();
assert.equal(f.intervals.size, 1); assert.equal([...f.intervals.values()][0].delay, 60000);
await f.lightweightHomeRefresh();
assert.equal(f.requests.length, 1);
const url = new URL(f.requests[0].url, "https://test.invalid");
assert.equal(url.pathname, "/api/dashboard/runtime-snapshot");
assert.equal(url.searchParams.get("view"), "ASSET_CARDS");
assert.equal(url.searchParams.get("symbols"), "BTCUSDT,ETHUSDT");
f.document.hidden = true; f.stopHomeFallbackPoll(); f.scheduleHomeFallbackPoll();
assert.equal(f.intervals.size, 0);
const reconnect = fixture();
reconnect.connectHomeStream();
const stream = reconnect.stream();
assert.equal(stream.url, "/api/dashboard/stream");
for (const group of ["PRICE", "SIGNAL", "RISK", "HEALTH"]) assert.equal(typeof stream.listeners[`ASSET_CARD_${group}`], "function");
stream.onerror(); stream.onerror();
assert.equal(reconnect.intervals.size, 1);
assert.equal([...reconnect.intervals.values()][0].delay, 15000);
stream.onopen();
await Promise.resolve();
assert.equal(reconnect.intervals.size, 1, "SSE recovery replaces the disconnected timer, never adds another timer");
assert.equal([...reconnect.intervals.values()][0].delay, 60000);
assert.equal(reconnect.requests.length, 1, "SSE recovery performs one card-only reconciliation");
stream.listeners.ASSET_CARD_PRICE({ data: JSON.stringify(event("PRICE", 1, { spotPrice: 42 })) });
assert.equal(reconnect.snapshot("BTCUSDT").spotPrice, 42);
assert.equal(reconnect.timeouts.size, 1);
assert.equal([...reconnect.timeouts.values()][0].delay, 1500);
assert.equal(reconnect.requests.length, 1, "native card SSE listener does not load Home");
const reconciliation = fixture();
const pending = [];
reconciliation.setApi((url, options) => new Promise(resolve => pending.push({ url, options, resolve })));
const olderRead = reconciliation.lightweightHomeRefresh();
const newerRead = reconciliation.lightweightHomeRefresh();
assert.equal(pending[0].options.signal.aborted, true, "overlapping card reconciliations abort their predecessor");
pending[1].resolve([{ ...snapshot("BTCUSDT", 2), spotPrice: 202 }]);
await newerRead;
pending[0].resolve([{ ...snapshot("BTCUSDT", 1), spotPrice: 101 }]);
await olderRead;
assert.equal(reconciliation.snapshot("BTCUSDT").spotPrice, 202, "an old response cannot undo the newer card result even if abort is ignored");
const safety = fixture();
safety.mergeAssetCardSnapshot(snapshot("BTCUSDT", 200), true);
safety.mergeAssetCardSnapshot(snapshot("ETHUSDT", 200), true);
safety.flushPrices();
safety.applyAssetCardEvent(event("PRICE", 202, { spotPrice: 202, latestPriceAt: "2026-09-10T00:00:02Z" }));
const safeHomeBefore = JSON.stringify(safety.home), safeEthBefore = JSON.stringify(safety.nodes.get("ETHUSDT"));
const unavailable = version => ({ ...snapshot("BTCUSDT", version), spotPrice: null, latestPriceAt: null,
  signal: { ...base.signal, direction: "SHORT", status: "INVALIDATED", calibratedConfidence: null },
  risk: { overallLevel: null, items: [] }, health: { status: "SOURCE_UNAVAILABLE", reason: "test-only stale read" } });
safety.setApi(async () => [unavailable(201)]);
await safety.lightweightHomeRefresh();
assert.equal(safety.snapshot("BTCUSDT").signal.status, "VALID", "a stale complete read cannot downgrade a lagging field group");
assert.equal(safety.snapshot("BTCUSDT").spotPrice, 202);
safety.setApi(async () => [unavailable(202)]);
await safety.lightweightHomeRefresh();
assert.equal(safety.snapshot("BTCUSDT").spotPrice, null, "a latest same-version read can clear unsafe price without allocating a version");
assert.equal(safety.snapshot("BTCUSDT").signal.direction, "LONG", "safety downgrade retains existing direction, never adopts an opposite one");
assert.equal(safety.snapshot("BTCUSDT").signal.status, "INVALIDATED");
assert.equal(safety.snapshot("BTCUSDT").signal.calibratedConfidence, null);
assert.equal(safety.snapshot("BTCUSDT").signal.pLong, null);
assert.equal(safety.snapshot("BTCUSDT").signal.pShort, null);
assert.equal(safety.snapshot("BTCUSDT").risk.overallLevel, "HIGH", "read safety cannot erase a known high risk");
assert.equal(safety.snapshot("BTCUSDT").snapshotVersion, 202);
assert.equal(safety.snapshot("BTCUSDT").cardAsOf, base.cardAsOf, "read safety does not invent a newly published effective clock");
assert.equal(safety.snapshot("BTCUSDT").health.status, "SOURCE_UNAVAILABLE");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("price").textContent, "—");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("confidence").textContent, "—");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("status").textContent, "已失效");
assert.equal(JSON.stringify(safety.nodes.get("ETHUSDT")), safeEthBefore);
assert.equal(JSON.stringify(safety.home), safeHomeBefore);
const downgraded = JSON.stringify(safety.snapshot("BTCUSDT"));
safety.setApi(async () => [snapshot("BTCUSDT", 202)]);
await safety.lightweightHomeRefresh();
for (const version of [201, 202]) for (const [group, fields] of [["PRICE", { spotPrice: 999 }],
  ["SIGNAL", { signal: base.signal }], ["RISK", { risk: { overallLevel: null, items: [] } }], ["HEALTH", { health: base.health }]]) {
  safety.applyAssetCardEvent(event(group, version, fields));
}
assert.equal(JSON.stringify(safety.snapshot("BTCUSDT")), downgraded, "equal/old GET and SSE cannot restore safety-downgraded fields");
safety.flushPrices();
assert.equal(safety.nodes.get("BTCUSDT").fields.get("price").textContent, "—", "a queued older price render cannot revive the old value");
safety.setApi(async () => [snapshot("BTCUSDT", 203)]);
await safety.lightweightHomeRefresh();
assert.equal(safety.snapshot("BTCUSDT").signal.status, "VALID", "a higher published version may restore normal state");
assert.equal(safety.snapshot("BTCUSDT").spotPrice, 100);
const safetyPending = [];
safety.setApi((url, options) => new Promise(resolve => safetyPending.push(resolve)));
const supersededRead = safety.lightweightHomeRefresh(), currentRead = safety.lightweightHomeRefresh();
safetyPending[1]([snapshot("BTCUSDT", 203)]); await currentRead;
safetyPending[0]([unavailable(203)]); await supersededRead;
assert.equal(safety.snapshot("BTCUSDT").signal.status, "VALID", "an obsolete request sequence cannot apply even an equal-version safety downgrade");
const checkedAt = new Date().toISOString();
const modelBase = snapshot("BTCUSDT", 300);
modelBase.signal.signalAsOf = new Date(Date.now() - 5000).toISOString();
const revokedModel = version => ({ ...modelBase, snapshotVersion: version,
  signal: { direction: null, status: "UNVALIDATED", calibratedConfidence: null, pLong: null, pShort: null,
    oneHourState: "数据不足", fourHourTrend: "数据不足", signalAsOf: modelBase.signal.signalAsOf },
  health: { status: "MODEL_UNAVAILABLE", reason: "fixture-current-model-revoked", asOf: checkedAt } });
for (const readPath of ["HOME", "CARD_GET"]) {
  const modelSafety = fixture();
  modelSafety.mergeAssetCardSnapshot(modelBase, true);
  modelSafety.mergeAssetCardSnapshot(snapshot("ETHUSDT", 300), true);
  const beforeOther = JSON.stringify(modelSafety.snapshot("ETHUSDT"));
  if (readPath === "HOME") modelSafety.opportunityCard({ ...modelSafety.home.assets[0], cardSignal: revokedModel(300) }, "BTCUSDT");
  else { modelSafety.setApi(async () => [revokedModel(300)]); await modelSafety.lightweightHomeRefresh(); }
  const result = modelSafety.snapshot("BTCUSDT");
  assert.equal(result.signal.direction, null, readPath + " same-version current model revocation hides the direction");
  assert.equal(result.signal.calibratedConfidence, null);
  assert.equal(result.signal.pLong, null); assert.equal(result.signal.pShort, null);
  assert.equal(result.signal.status, "UNVALIDATED");
  assert.equal(result.spotPrice, modelBase.spotPrice, "model failure is not loss of real Spot price");
  assert.deepEqual(result.risk, modelBase.risk);
  assert.equal(result.cardAsOf, modelBase.cardAsOf);
  assert.equal(JSON.stringify(modelSafety.snapshot("ETHUSDT")), beforeOther);
  const html = modelSafety.opportunityCard({ ...modelSafety.home.assets[0], cardSignal: modelBase }, "BTCUSDT");
  assert.ok(!html.includes("72%") && !html.includes("99%"), "an equal-version normal Home cannot restore a revoked model or legacy confidence");
}
const staleModelHome = fixture();
staleModelHome.mergeAssetCardSnapshot({ ...modelBase, snapshotVersion: 290 }, false);
staleModelHome.applyAssetCardEvent(event("PRICE", 301, { spotPrice: 301 }));
staleModelHome.opportunityCard({ ...staleModelHome.home.assets[0], cardSignal: revokedModel(300) }, "BTCUSDT");
assert.equal(staleModelHome.snapshot("BTCUSDT").signal.status, "VALID", "old Home revocation cannot clear newer SSE state");
for (const asOf of [null, "not-a-time", "2099-01-01T00:00:00Z", new Date(Date.now() - 60000).toISOString()]) {
  const invalidClock = fixture(); invalidClock.mergeAssetCardSnapshot({ ...modelBase, snapshotVersion: 290 }, false);
  invalidClock.applyAssetCardEvent(event("PRICE", 300, { spotPrice: 300 }));
  invalidClock.opportunityCard({ ...invalidClock.home.assets[0], cardSignal: { ...revokedModel(300),
    health: { ...revokedModel(300).health, asOf } } }, "BTCUSDT");
  assert.equal(invalidClock.snapshot("BTCUSDT").signal.status, "VALID", "unknown/future/pre-signal model check time cannot downgrade a same-version model");
}
const modelHealth = fixture(); modelHealth.mergeAssetCardSnapshot(modelBase, false);
modelHealth.applyAssetCardEvent(event("HEALTH", 299, { health: revokedModel(299).health }));
assert.equal(modelHealth.snapshot("BTCUSDT").signal.status, "VALID");
modelHealth.applyAssetCardEvent(event("HEALTH", 300, { health: revokedModel(300).health }));
assert.equal(modelHealth.snapshot("BTCUSDT").signal.direction, null, "explicit equal-version MODEL_UNAVAILABLE health is a one-way safety action");
assert.equal(modelHealth.snapshot("BTCUSDT").spotPrice, 100);
modelHealth.applyAssetCardEvent(event("SIGNAL", 300, modelBase));
assert.equal(modelHealth.snapshot("BTCUSDT").signal.direction, null, "equal-version SIGNAL cannot undo model safety");
modelHealth.applyAssetCardEvent(event("HEALTH", 301, { health: revokedModel(301).health }));
modelHealth.applyAssetCardEvent(event("PRICE", 301, { spotPrice: 301 }));
modelHealth.applyAssetCardEvent(event("RISK", 301, { risk: modelBase.risk }));
assert.equal(modelHealth.snapshot("BTCUSDT").spotPrice, 301, "model safety does not suppress independent same-version real price");
assert.equal(modelHealth.snapshot("BTCUSDT").risk.overallLevel, "HIGH");
const combinedFailure = fixture(); combinedFailure.mergeAssetCardSnapshot(modelBase, false);
combinedFailure.opportunityCard({ ...combinedFailure.home.assets[0], cardSignal: { ...revokedModel(300),
  spotPrice: null, latestPriceAt: null, health: { status: "SOURCE_UNAVAILABLE", reason: "fixture-price-and-model-loss", asOf: null } } }, "BTCUSDT");
assert.equal(combinedFailure.snapshot("BTCUSDT").signal.direction, null, "explicit UNVALIDATED signal still clears the direction when source failure replaces model health");
assert.equal(combinedFailure.snapshot("BTCUSDT").spotPrice, null);
const shared = source.slice(0, source.indexOf("/* Desktop Home runtime */"));
assert.ok(!shared.includes("cardSignal") && !shared.includes("ASSET_CARD_"), "Pool shared semantics remain untouched");
const css = fs.readFileSync("src/main/resources/static/css/home.css", "utf8");
assert.ok(css.includes("height: 146px") && css.includes("grid-template-rows: 24px 23px 40px minmax(0,1fr)"));
if (!process.env.ASSET_CARD_CLOCK_CHILD) {
  for (const zone of ["UTC", "Asia/Shanghai", "America/New_York"]) {
    const child = spawnSync(process.execPath, [process.argv[1]], { encoding: "utf8", env: { ...process.env, TZ: zone, ASSET_CARD_CLOCK_CHILD: "1" } });
    assert.equal(child.status, 0, child.stderr);
  }
} else {
  const expected = { UTC: "00:00:00", "Asia/Shanghai": "08:00:00", "America/New_York": "20:00:00" }[process.env.TZ];
  assert.equal(f.assetCardClock("2026-09-10T00:00:00Z"), expected);
  assert.equal(f.assetCardClock(null), "—");
}
console.log("ASSET_CARD_RUNTIME_MATRIX: PASS (rendering, field isolation, versions, clocks, polling, frozen geometry)");
