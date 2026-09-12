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
  scheduleHomeFallbackPoll, stopHomeFallbackPoll, connectHomeStream, startHomeLiveRuntime, assetCardClock,
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

const priceBase = Date.now() - 2000;
function fixture(initialNow = null) {
  let nextTimer = 1;
  let controlledNow = initialNow;
  class RuntimeDate extends Date {
    constructor(...args) { super(...(args.length ? args : [RuntimeDate.now()])); }
    static now() { return controlledNow == null ? Date.now() : controlledNow; }
  }
  const timeouts = new Map(), expiryTimeouts = new Map(), intervals = new Map(), requests = [], nodes = new Map(), documentListeners = new Map();
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
    getElementById() { return null; }, addEventListener(name, callback) { documentListeners.set(name, callback); } };
  const window = { CSS: { escape: value => value }, TradeModelFrontendContract: {},
    setTimeout(callback, delay) { const id = nextTimer++; (callback.name === "expireAssetCardPrices" ? expiryTimeouts : timeouts)
      .set(id, { callback, delay, at: RuntimeDate.now() + delay }); return id; },
    clearTimeout(id) { timeouts.delete(id); expiryTimeouts.delete(id); },
    setInterval(callback, delay) { const id = nextTimer++; intervals.set(id, { callback, delay }); return id; },
    clearInterval(id) { intervals.delete(id); }, addEventListener() {}, location: { search: "" } };
  class EventSource {
    constructor(url) { this.url = url; this.listeners = {}; }
    addEventListener(type, callback) { this.listeners[type] = callback; }
    close() { this.closed = true; }
  }
  window.EventSource = EventSource;
  const context = vm.createContext({ window, document, console, URLSearchParams, AbortController,
    EventSource, Intl, Date: RuntimeDate, setTimeout: window.setTimeout, clearTimeout: window.clearTimeout });
  vm.runInContext(source.replace(bootstrap, exported + bootstrap), context);
  const home = { assets: ["BTCUSDT", "ETHUSDT"].map((symbol, index) => ({ assetId: index + 1, rawSymbol: symbol,
    name: symbol, slot: index + 1, homePinned: true, finalConfidence: 99, latestPrice: 99999,
    cardSignalDisplayEnabled: true, hasFinal: true, marketBiasLabel: "强偏空",
    finalMarketBias: "STRONG_BEARISH", riskLevel: "LOW" })), selectedSymbol: "BTCUSDT",
    positions: [{ positionId: "123", markPrice: 101 }], executionSuggestion: { status: "BLOCKED" },
    aiDecision: { tabs: [{ role: "GPT_FINAL" }] } };
  context.cardTest.setHome(home);
  context.cardTest.setApi(async (url, options) => { requests.push({ url, options }); return []; });
  return { ...context.cardTest, context, window, document, home, nodes, intervals, requests, timeouts, expiryTimeouts,
    setNow(value) { controlledNow = value; },
    visible(value) { document.hidden = !value; documentListeners.get("visibilitychange")?.(); },
    advanceTo(value) { controlledNow = value; for (let pass = 0; pass < 20; pass++) {
      const due = [...expiryTimeouts].filter(([, timer]) => timer.at <= value); if (!due.length) return;
      for (const [id, timer] of due) { expiryTimeouts.delete(id); timer.callback(); }
    } throw new Error("expiration scheduler must not spin"); },
    flushPrices() { for (const [id, timer] of [...timeouts]) { timeouts.delete(id); timer.callback(); } } };
}
function priceInstant(tradeId) { return new Date(priceBase + tradeId).toISOString(); }
function priceExpiry(observed) { return new Date(Date.parse(observed) + 10000).toISOString(); }
function snapshot(symbol = "BTCUSDT", version = 1) {
  return { symbol, assetName: "Bitcoin", spotPrice: 100, latestPriceAt: priceInstant(version), priceTradeId: version,
    priceValidUntil: priceExpiry(priceInstant(version)),
    signal: { direction: "LONG", status: "VALID", calibratedConfidence: 72, pLong: .72, pShort: .2,
      oneHourState: "OPPORTUNITY", fourHourTrend: "LONG", signalAsOf: "2026-09-10T00:00:00Z" },
    risk: { overallLevel: "HIGH", items: [{ type: "CHASE", assessmentStatus: "ASSESSED", level: "HIGH",
      evidenceValue: 2, source: "SPOT", asOf: "2026-09-10T00:00:00Z", reason: "结构延伸" }], riskAsOf: "2026-09-10T00:00:00Z",
      riskBasisSide: "LONG", riskBasisDirection: "LONG", riskBasisSignalAsOf: "2026-09-10T00:00:00Z",
      riskMarketAsOf: "2026-09-10T00:00:00Z", riskVersion: "test-only-risk" },
    health: { status: "HEALTHY" }, cardAsOf: "2026-09-10T00:00:00Z", snapshotVersion: version,
    featureVersion: "test-only-features", modelVersion: "test-only-model", calibrationVersion: "test-only-calibration", thresholdVersion: "test-only-threshold" };
}
function event(type, version, fields, symbol = "BTCUSDT") {
  return { eventType: `ASSET_CARD_${type}`, symbol, snapshotVersion: version, payload: {
    featureVersion: "test-only-features", modelVersion: "test-only-model", calibrationVersion: "test-only-calibration", thresholdVersion: "test-only-threshold",
    ...(type === "PRICE" ? { priceTradeId: version, latestPriceAt: priceInstant(version),
      priceValidUntil: priceExpiry(fields.latestPriceAt || priceInstant(version)) } : {}),
    symbol, snapshotVersion: version, ...fields } };
}

// V42 fixtures are isolated public market projections; they do not create a model, request or owner row.
function boundSnapshot(direction = "LONG", version = 1) {
  const value = snapshot("BTCUSDT", version);
  value.thresholdVersion = "test-only-threshold";
  value.signal.direction = direction;
  value.signal.calibratedConfidence = /SHORT$/.test(direction) ? 20 : /LONG$/.test(direction) ? 72 : null;
  value.risk = { ...value.risk, riskBasisSide: /SHORT$/.test(direction) ? "SHORT" : /LONG$/.test(direction) ? "LONG" : "NON_DIRECTIONAL",
    riskBasisDirection: direction, riskBasisSignalAsOf: value.signal.signalAsOf,
    riskMarketAsOf: value.latestPriceAt, riskVersion: "test-only-risk" };
  return value;
}
function boundEvent(type, version, value, overrides = {}) {
  return event(type, version, { ...value, snapshotVersion: version, riskVersion: value.risk.riskVersion,
    riskBasisSide: value.risk.riskBasisSide, riskBasisDirection: value.risk.riskBasisDirection,
    riskBasisSignalAsOf: value.risk.riskBasisSignalAsOf, ...overrides });
}
// Real price recovers independently; risk recovery requires a newly committed atomic snapshot.
function spotRecoveryFixture(version = 10, directional = false, independentRisk = false) {
  const runtime = fixture(Date.parse("2026-09-12T08:00:12Z")), failure = boundSnapshot("LONG", version);
  failure.signal = { ...failure.signal, direction: directional ? "LONG" : null, status: directional ? "INVALIDATED" : "SHADOW",
    calibratedConfidence: null, pLong: null, pShort: null };
  failure.risk = { ...failure.risk, riskBasisSide: directional ? "LONG" : "NON_DIRECTIONAL",
    riskBasisDirection: failure.signal.direction, riskAsOf: "2026-09-12T08:00:10Z", items:
      ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"].map(type => type === "DATA"
        ? { type, assessmentStatus: "ASSESSED", level: "HIGH", evidenceValue: "SPOT_SOURCE_UNAVAILABLE", unit: "SOURCE_STATE",
          source: "BINANCE_SPOT_AGG_TRADE", asOf: "2026-09-12T08:00:10Z", reason: "Test Spot connection lost", hardInvalidation: true }
        : type === "EVENT" && independentRisk
          ? { type, assessmentStatus: "ASSESSED", level: "HIGH", evidenceValue: "TEST_EVENT", unit: "EVENT_STATE",
            source: "ISOLATED_EVENT", asOf: "2026-09-12T08:00:09Z", reason: "Independent event evidence" }
          : { type, assessmentStatus: "UNKNOWN", level: null, reason: "Test missing evidence" }) };
  failure.health = { status: "SOURCE_UNAVAILABLE", asOf: "2026-09-12T08:00:10Z" };
  Object.assign(failure, { spotPrice: null, latestPriceAt: null, priceTradeId: null, priceValidUntil: null });
  if (version > 0) runtime.mergeAssetCardSnapshot({ ...failure, spotPrice: 100, priceTradeId: 100,
    latestPriceAt: "2026-09-12T08:00:09Z", priceValidUntil: "2026-09-12T08:00:19Z", health: { status: "HEALTHY" } }, true);
  runtime.mergeAssetCardSnapshot(failure, true); runtime.flushPrices();
  const recovered = { ...failure, spotPrice: 101, priceTradeId: 101, latestPriceAt: "2026-09-12T08:00:11Z",
    priceValidUntil: "2026-09-12T08:00:21Z", health: { status: "HEALTHY", asOf: "2026-09-12T08:00:11Z" },
    risk: { ...failure.risk, overallLevel: independentRisk ? "HIGH" : null, items: failure.risk.items.map(item => item.type === "DATA"
      ? { type: "DATA", assessmentStatus: "UNKNOWN", level: null, reason: "Spot recovered; other evidence pending" } : item) } };
  return { runtime, failure, recovered };
}
for (const path of ["PRICE", "CARD_GET", "HOME", "RISK_HEALTH_THEN_PRICE"]) for (const version of [0, 10]) {
  for (const independentRisk of [false, true]) {
    const { runtime, failure, recovered } = spotRecoveryFixture(version, version > 0, independentRisk);
    const before = runtime.snapshot("BTCUSDT"), analysis = JSON.stringify(before.signal), clock = before.cardAsOf;
    const committedRisk = JSON.stringify(before.risk);
    if (path === "CARD_GET") { runtime.setApi(async () => [recovered]); await runtime.lightweightHomeRefresh(); }
    else if (path === "HOME") {
      const html = runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: recovered }, "BTCUSDT");
      // The real Home renderer installs this returned HTML; the narrow fixture models those two nodes.
      runtime.nodes.get("BTCUSDT").fields.get("risk").textContent = html.match(/data-live-field="risk"[^>]*>([^<]*)/)[1];
      runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML = html.match(/class="asset-card-risk-items"[^>]*>(.*?)<\/div>/)[1];
      runtime.nodes.get("BTCUSDT").fields.get("status").textContent = html.match(/data-live-field="status"[^>]*>([^<]*)/)[1];
    }
    else {
      if (path === "RISK_HEALTH_THEN_PRICE") {
        runtime.applyAssetCardEvent(boundEvent("RISK", version, recovered));
        runtime.applyAssetCardEvent(boundEvent("HEALTH", version, recovered));
      }
      runtime.applyAssetCardEvent(event("PRICE", version, recovered));
    }
    runtime.flushPrices();
    const assertPriceRecoveredRiskPending = () => {
      const value = runtime.snapshot("BTCUSDT"), data = value.risk.items.find(item => item.type === "DATA");
      assert.equal(JSON.stringify(value.risk), committedRisk, `${path}/v${version}: fresh PRICE cannot rewrite risk before durable CAS`);
      assert.equal(value.spotPrice, 101);
      assert.equal(data.assessmentStatus, "ASSESSED"); assert.equal(data.level, "HIGH");
      assert.equal(data.evidenceValue, "SPOT_SOURCE_UNAVAILABLE");
      assert.equal(value.risk.items.length, 8); assert.equal(value.risk.overallLevel, "HIGH");
      assert.equal(value.snapshotVersion, version); assert.equal(JSON.stringify(value.signal), analysis); assert.equal(value.cardAsOf, clock);
      assert.equal(runtime.nodes.get("BTCUSDT").fields.get("status").textContent,
        version > 0 ? "已失效" : "风险恢复待保存", "recovery status cannot hide model invalidation or claim committed risk recovery");
      if (independentRisk) assert.equal(value.risk.items.find(item => item.type === "EVENT"), failure.risk.items.find(item => item.type === "EVENT"));
      assert.ok(runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("数据·高"), `${path}/v${version}: retain last committed evidence and its observation time`);
    };
    assertPriceRecoveredRiskPending();
    runtime.applyAssetCardEvent(boundEvent("RISK", version, recovered));
    runtime.applyAssetCardEvent(boundEvent("HEALTH", version, failure));
    runtime.mergeAssetCardSnapshot(failure, true); runtime.flushPrices(); assertPriceRecoveredRiskPending();
    const committedRecovery = { ...recovered, snapshotVersion: version + 1 };
    runtime.applyAssetCardEvent(boundEvent("RISK", version + 1, committedRecovery));
    const restored = runtime.snapshot("BTCUSDT");
    assert.equal(restored.snapshotVersion, version + 1);
    assert.equal(JSON.stringify(restored.risk), JSON.stringify(committedRecovery.risk), "only the next committed RISK changes the displayed evidence");
    assert.equal(restored.risk.items.find(item => item.type === "DATA").assessmentStatus, "UNKNOWN");
    assert.equal(restored.risk.overallLevel, independentRisk ? "HIGH" : null);
    assert.equal(JSON.stringify(restored.signal), analysis);
    assert.equal(runtime.nodes.get("BTCUSDT").fields.get("card-time").textContent,
      runtime.assetCardClock(restored.signal.signalAsOf), "risk commit does not change the analysis timestamp");
    assert.notEqual(runtime.nodes.get("BTCUSDT").fields.get("status").textContent, "风险恢复待保存");
    assert.ok(!runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("数据·高"));
    runtime.applyAssetCardEvent(boundEvent("RISK", version, failure));
    runtime.applyAssetCardEvent(boundEvent("HEALTH", version, failure));
    runtime.mergeAssetCardSnapshot(failure, true);
    assert.equal(runtime.snapshot("BTCUSDT").risk, restored.risk, "old failure cannot return after committed recovery");
    const risk = runtime.snapshot("BTCUSDT").risk, target = runtime.nodes.get("BTCUSDT").fields.get("risk-items");
    let html = target.innerHTML, paints = 0;
    Object.defineProperty(target, "innerHTML", { get: () => html, set(value) { html = value; paints++; } });
    runtime.applyAssetCardEvent(event("PRICE", version, recovered)); // Duplicate trade remains rejected.
    runtime.applyAssetCardEvent(event("PRICE", version, { ...recovered, priceTradeId: 102, spotPrice: 102,
      latestPriceAt: "2026-09-12T08:00:11.500Z", priceValidUntil: "2026-09-12T08:00:21.500Z" }));
    runtime.flushPrices(); assert.equal(runtime.snapshot("BTCUSDT").risk, risk); assert.equal(paints, 0);
    assert.equal(runtime.requests.length, 0); assert.equal(runtime.intervals.size, 0);
  }
}
for (const mutation of [
  value => { value.priceTradeId = 100; }, value => { value.priceTradeId = 0; },
  value => { value.latestPriceAt = "2026-09-12T08:00:10Z"; },
  value => { value.priceValidUntil = "2026-09-12T08:00:12Z"; },
  value => { value.priceTradeId = null; }, value => { value.spotPrice = -1; }
]) {
  const { runtime, recovered } = spotRecoveryFixture(); mutation(recovered);
  runtime.applyAssetCardEvent(event("PRICE", 10, recovered)); runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").risk.items.find(item => item.type === "DATA").level, "HIGH", "unaccepted trade must not withdraw DATA");
}
for (const mutateRisk of [
  risk => { risk.items.find(item => item.type === "DATA").evidenceValue = "DEPTH_SOURCE_UNAVAILABLE"; },
  risk => { risk.items.find(item => item.type === "DATA").asOf = "2026-09-12T08:00:11.500Z"; },
  risk => { risk.items.find(item => item.type === "DATA").asOf = "2026-09-12T08:00:11Z"; },
  risk => { risk.items.find(item => item.type === "DATA").unit = "UNVERIFIED_UNIT"; },
  risk => { risk.riskBasisSide = "SHORT"; }, risk => { risk.riskVersion = "unmatched-risk-version"; }
]) {
  const { runtime, recovered } = spotRecoveryFixture(), risk = runtime.snapshot("BTCUSDT").risk;
  mutateRisk(risk); runtime.applyAssetCardEvent(event("PRICE", 10, recovered)); runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").risk, risk, "different/newer/unbound evidence is not disproved by a Spot trade");
}
{
  const { runtime, recovered } = spotRecoveryFixture(10, true);
  const next = boundSnapshot("SHORT", 11); next.signal.signalAsOf = "2026-09-12T08:00:11Z";
  next.risk = { ...next.risk, riskVersion: "test-next-risk", riskBasisSignalAsOf: next.signal.signalAsOf };
  runtime.applyAssetCardEvent(boundEvent("SIGNAL", 11, next));
  const current = runtime.snapshot("BTCUSDT"), signal = current.signal, risk = current.risk;
  runtime.applyAssetCardEvent(boundEvent("RISK", 10, recovered));
  runtime.applyAssetCardEvent(boundEvent("HEALTH", 10, recovered));
  runtime.mergeAssetCardSnapshot(recovered, true);
  assert.equal(runtime.snapshot("BTCUSDT").signal, signal); assert.equal(runtime.snapshot("BTCUSDT").risk, risk);
  assert.equal(runtime.snapshot("BTCUSDT").snapshotVersion, 11);
}
{
  const { runtime, recovered } = spotRecoveryFixture();
  // No replacement Home snapshot is supplied; return must use the already accepted independent fields.
  runtime.setHomeLoader(async () => {});
  runtime.startHomeLiveRuntime(); runtime.visible(false);
  runtime.applyAssetCardEvent(event("PRICE", 10, recovered)); runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").risk.items.find(item => item.type === "DATA").assessmentStatus, "ASSESSED");
  assert.ok(runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("数据·高"), "hidden price paint is deferred");
  runtime.visible(true);
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("price").textContent, "$101");
  assert.ok(runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("数据·高"), "visibility cannot invent durable risk recovery");
  runtime.applyAssetCardEvent(boundEvent("RISK", 11, recovered));
  assert.ok(!runtime.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("数据·高"));
}
// Regression: a newer complete safety projection must revoke the old price on every read/event path.
for (const path of ["CARD_GET", "HOME", "SSE"]) for (const shadow of [false, true]) {
  const runtime = fixture(), original = boundSnapshot("LONG", 10);
  original.risk = { ...original.risk, overallLevel: "MEDIUM", items: [{ ...original.risk.items[0], level: "MEDIUM" }] };
  runtime.mergeAssetCardSnapshot(original, true); runtime.flushPrices();
  const signal = { ...original.signal, direction: shadow ? null : "SHORT", status: shadow ? "SHADOW" : "INVALIDATED",
    calibratedConfidence: null, pLong: null, pShort: null, signalAsOf: "2026-09-10T00:05:00Z" };
  const failure = { ...original, snapshotVersion: 11, spotPrice: null, latestPriceAt: null, priceValidUntil: null, priceTradeId: null,
    signal, cardAsOf: "2026-09-10T00:05:01Z", modelVersion: shadow ? null : "test-only-model-v2",
    calibrationVersion: shadow ? null : "test-only-calibration-v2", thresholdVersion: shadow ? null : "test-only-threshold-v2",
    health: { status: "SOURCE_UNAVAILABLE", asOf: priceInstant(30) },
    risk: { ...original.risk, overallLevel: "HIGH", riskBasisSide: shadow ? "NON_DIRECTIONAL" : "SHORT",
      riskBasisDirection: signal.direction, riskBasisSignalAsOf: signal.signalAsOf, riskVersion: "test-only-risk-v2",
      items: [{ type: "DATA", assessmentStatus: "ASSESSED", level: "HIGH", asOf: priceInstant(30),
        evidenceValue: "SOURCE_LOST", source: "BINANCE_SPOT_AGG_TRADE", unit: "SOURCE_STATE", reason: "TEST_NEW_ANALYSIS_SOURCE_LOST" }] } };
  if (path === "CARD_GET") { runtime.setApi(async () => [failure]); await runtime.lightweightHomeRefresh(); }
  else if (path === "HOME") runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: failure }, "BTCUSDT");
  else runtime.applyAssetCardEvent(boundEvent("HEALTH", 11, failure));
  assert.equal(runtime.snapshot("BTCUSDT").signal.direction, signal.direction, `${path} new source-loss snapshot owns its actual direction`);
  assert.equal(runtime.snapshot("BTCUSDT").signal.signalAsOf, signal.signalAsOf, `${path} new analysis time cannot inherit the old signal`);
  assert.equal(runtime.snapshot("BTCUSDT").risk.riskBasisSide, failure.risk.riskBasisSide);
  assert.equal(runtime.snapshot("BTCUSDT").risk.riskBasisSignalAsOf, signal.signalAsOf);
  assert.equal(runtime.snapshot("BTCUSDT").risk.riskVersion, failure.risk.riskVersion);
  assert.equal(runtime.snapshot("BTCUSDT").risk.overallLevel, "HIGH");
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, null);
  runtime.applyAssetCardEvent(boundEvent("SIGNAL", 11, failure));
  assert.equal(runtime.snapshot("BTCUSDT").signal.signalAsOf, signal.signalAsOf, "same-version SIGNAL preserves the complete already accepted failure identity");
  const partial = fixture(); partial.mergeAssetCardSnapshot(original, true);
  const incomplete = { ...failure, risk: { ...failure.risk, riskBasisSide: "LONG" } };
  if (path === "CARD_GET") { partial.setApi(async () => [incomplete]); await partial.lightweightHomeRefresh(); }
  else if (path === "HOME") partial.opportunityCard({ ...partial.home.assets[0], cardSignal: incomplete }, "BTCUSDT");
  else partial.applyAssetCardEvent(boundEvent("HEALTH", 11, incomplete));
  assert.equal(partial.snapshot("BTCUSDT").spotPrice, null, "unmatched analysis still revokes stale price");
  partial.applyAssetCardEvent(boundEvent("SIGNAL", 11, failure));
  assert.equal(partial.snapshot("BTCUSDT").signal.signalAsOf, signal.signalAsOf,
    "price revocation cannot advance an unaccepted SIGNAL watermark and discard the later complete same-version signal");
  assert.equal(partial.snapshot("BTCUSDT").risk.overallLevel, "HIGH");
}
for (const path of ["CARD_GET", "HOME", "SSE"]) for (const version of [11, 10]) {
  const runtime = fixture(), original = boundSnapshot("LONG", 10);
  runtime.mergeAssetCardSnapshot(original, true); runtime.flushPrices();
  const riskBefore = JSON.stringify(runtime.snapshot("BTCUSDT").risk);
  const failure = { ...original, snapshotVersion: version, spotPrice: null, latestPriceAt: null,
    priceValidUntil: null, priceTradeId: null,
    signal: { ...original.signal, status: "INVALIDATED", calibratedConfidence: null, pLong: null, pShort: null },
    health: { status: "SOURCE_UNAVAILABLE", reason: "ISOLATED_STALE_SOURCE", asOf: priceInstant(30) } };
  let renderedHome;
  if (path === "CARD_GET") { runtime.setApi(async () => [failure]); await runtime.lightweightHomeRefresh(); }
  else if (path === "HOME") renderedHome = runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: failure }, "BTCUSDT");
  else runtime.applyAssetCardEvent(boundEvent("HEALTH", version, failure));
  runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, null, `${path} v${version} revokes the old v10 price`);
  assert.equal(runtime.snapshot("BTCUSDT").latestPriceAt, null);
  assert.equal(runtime.snapshot("BTCUSDT").priceValidUntil, null);
  if (renderedHome) assert.ok(renderedHome.includes('data-live-field="price">—</strong>'), "full Home renders revoked price, not stale DOM");
  assert.equal(runtime.snapshot("BTCUSDT").cardAsOf, original.cardAsOf);
  assert.equal(JSON.stringify(runtime.snapshot("BTCUSDT").risk), riskBefore, "price safety preserves independently bound known risk");
  runtime.mergeAssetCardSnapshot(original, true); runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, null, "old complete response cannot restore revoked data");
  runtime.applyAssetCardEvent(event("PRICE", 0, { priceTradeId: 99, latestPriceAt: priceInstant(29), spotPrice: 999 }));
  runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, null, "a late pre-failure trade cannot restore price even with a higher trade id");
  runtime.applyAssetCardEvent(event("PRICE", 0, { priceTradeId: 31, latestPriceAt: priceInstant(31), spotPrice: 101 }));
  runtime.flushPrices();
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, 101, "new real trade restores price without signal or risk fallback");
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("card-time").textContent, runtime.assetCardClock(original.signal.signalAsOf));
  if (path === "CARD_GET") { runtime.setApi(async () => [failure]); await runtime.lightweightHomeRefresh(); }
  else if (path === "HOME") runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: failure }, "BTCUSDT");
  else runtime.applyAssetCardEvent(boundEvent("HEALTH", version, failure));
  assert.equal(runtime.snapshot("BTCUSDT").spotPrice, 101, `${path} late failure cannot erase a newer actual trade`);
}

// Price expiry is independent of SSE, reconciliation, rendering throttles and the configured TTL value.
for (const ttl of [3000, 10000]) {
  const at = Date.now(), runtime = fixture(at), original = boundSnapshot("LONG", 10);
  Object.assign(original, { latestPriceAt: new Date(at).toISOString(), priceValidUntil: new Date(at + ttl).toISOString() });
  runtime.mergeAssetCardSnapshot(original, true); runtime.flushPrices();
  const beforeRisk = JSON.stringify(runtime.snapshot("BTCUSDT").risk), beforeSignal = JSON.stringify(runtime.snapshot("BTCUSDT").signal);
  runtime.setApi(async () => { throw new Error("ISOLATED_OFFLINE"); });
  await assert.rejects(runtime.lightweightHomeRefresh(), /ISOLATED_OFFLINE/);
  assert.equal(runtime.expiryTimeouts.size, 1, "one local expiry scheduler; it sends no requests");
  runtime.advanceTo(at + ttl - 1);
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("price").textContent, "$100");
  runtime.advanceTo(at + ttl);
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("price").textContent, "—", `expiry is exact for server TTL ${ttl}`);
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("status").textContent, "价格过期");
  assert.equal(JSON.stringify(runtime.snapshot("BTCUSDT").risk), beforeRisk);
  assert.equal(JSON.stringify(runtime.snapshot("BTCUSDT").signal), beforeSignal);
  assert.equal(runtime.snapshot("BTCUSDT").cardAsOf, original.cardAsOf);
  assert.equal(runtime.requests.length, 0);
  runtime.mergeAssetCardSnapshot({ ...original, snapshotVersion: 100 }, true); runtime.flushPrices();
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("price").textContent, "—", "a larger DB version does not extend the original trade deadline");
  const recovered = { priceTradeId: 11, latestPriceAt: new Date(at + ttl).toISOString(),
    priceValidUntil: new Date(at + ttl * 2).toISOString(), spotPrice: 102 };
  runtime.applyAssetCardEvent(event("PRICE", 0, recovered)); runtime.flushPrices();
  assert.equal(runtime.nodes.get("BTCUSDT").fields.get("price").textContent, "$102");
  assert.equal(runtime.expiryTimeouts.size, 1);
}
const suspended = fixture(Date.now()), suspendAt = Date.now(); suspended.setNow(suspendAt);
const suspendPrice = { ...boundSnapshot("LONG", 10), latestPriceAt: new Date(suspendAt).toISOString(),
  priceValidUntil: new Date(suspendAt + 10000).toISOString() };
suspended.mergeAssetCardSnapshot(suspendPrice, true); suspended.flushPrices();
suspended.setHomeLoader(async () => undefined);
suspended.startHomeLiveRuntime(); suspended.startHomeLiveRuntime();
suspended.visible(false); suspended.setNow(suspendAt + 11000);
// Background timers may be frozen: becoming visible must clear stale DOM before any request completes.
suspended.visible(true);
assert.equal(suspended.nodes.get("BTCUSDT").fields.get("price").textContent, "—");
assert.equal(suspended.expiryTimeouts.size, 0);
const analysisClock = fixture(), analysisOnly = boundSnapshot("LONG", 10);
analysisOnly.cardAsOf = "2026-09-10T00:05:00Z";
analysisClock.mergeAssetCardSnapshot(analysisOnly, true);
assert.equal(analysisClock.nodes.get("BTCUSDT").fields.get("card-time").textContent,
  analysisClock.assetCardClock(analysisOnly.signal.signalAsOf), "displayed time is analysis time, not risk/connection update time");
const analysisHtml = analysisClock.opportunityCard({ ...analysisClock.home.assets[0], cardSignal: analysisOnly }, "BTCUSDT");
assert.ok(analysisHtml.includes('data-live-field="card-time" datetime="' + analysisOnly.signal.signalAsOf + '"'));
assert.ok(!analysisHtml.includes('datetime="' + analysisOnly.cardAsOf + '"'));
for (const badExpiry of [undefined, null, "not-a-time", priceInstant(9), priceInstant(10)]) {
  const malformed = fixture();
  malformed.mergeAssetCardSnapshot({ ...boundSnapshot("LONG", 10), priceValidUntil: badExpiry }, true);
  const html = malformed.opportunityCard(malformed.home.assets[0], "BTCUSDT");
  assert.ok(html.includes('data-live-field="price">—</strong>'), "missing/non-positive expiry never becomes a fresh price");
  assert.equal(malformed.expiryTimeouts.size, 0);
}

// A current read-time field failure must only downgrade its own field, even without a new durable version.
for (const path of ["SSE", "CARD_GET", "HOME"]) {
  for (const status of ["RISK_UNAVAILABLE", "SIGNAL_FAILED", "SIGNAL_AND_RISK_UNAVAILABLE"]) {
    const runtime = fixture(), original = boundSnapshot("LONG", 10);
    runtime.mergeAssetCardSnapshot(original, true);
    const riskFailed = status !== "SIGNAL_FAILED", signalFailed = status !== "RISK_UNAVAILABLE";
    const failed = { ...original, health: { status, reason: "ISOLATED_TEST_FIELD_FAILURE", asOf: priceInstant(30) },
      signal: signalFailed ? { ...original.signal, status: "FAILED", calibratedConfidence: null, pLong: null, pShort: null } : original.signal,
      risk: riskFailed ? { ...original.risk, overallLevel: null,
        items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"].map(type => ({
          type, assessmentStatus: "UNKNOWN", level: null, reason: "ISOLATED_TEST_FIELD_FAILURE" })) } : original.risk };
    if (path === "SSE") runtime.applyAssetCardEvent(boundEvent("HEALTH", 10, failed));
    else if (path === "CARD_GET") { runtime.setApi(async () => [failed]); await runtime.lightweightHomeRefresh(); }
    else runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: failed }, "BTCUSDT");
    const result = runtime.snapshot("BTCUSDT");
    assert.equal(result.spotPrice, original.spotPrice, `${path}/${status}: normal PRICE is isolated`);
    assert.equal(result.signal.calibratedConfidence, signalFailed ? null : 72, `${path}/${status}: signal downgrade`);
    assert.equal(result.risk.overallLevel, riskFailed ? null : "HIGH", `${path}/${status}: risk downgrade`);
    assert.equal(result.snapshotVersion, 10);
    runtime.applyAssetCardEvent(boundEvent("SIGNAL", 10, original));
    runtime.mergeAssetCardSnapshot(original, true);
    assert.equal(runtime.snapshot("BTCUSDT").signal.calibratedConfidence, signalFailed ? null : 72, "same-version restoration is not allowed");
    assert.equal(runtime.snapshot("BTCUSDT").risk.overallLevel, riskFailed ? null : "HIGH");
    for (const changed of [
      { thresholdVersion: "wrong" }, { modelVersion: "wrong" }, { calibrationVersion: "wrong" }, { featureVersion: "wrong" },
      { signal: { ...failed.signal, signalAsOf: priceInstant(1) } },
      { risk: { ...failed.risk, riskVersion: "wrong" } },
      { risk: { ...failed.risk, riskBasisSide: "SHORT" } },
      { health: { ...failed.health, asOf: "2099-01-01T00:00:00Z" } },
      { health: { ...failed.health, asOf: "2026-09-09T23:59:59Z" } }
    ]) {
      const safe = fixture(); safe.mergeAssetCardSnapshot(original, true);
      safe.applyAssetCardEvent(boundEvent("HEALTH", 10, { ...failed, ...changed }));
      assert.equal(safe.snapshot("BTCUSDT").signal.calibratedConfidence, 72, "mismatched identity/time cannot clear current signal");
      assert.equal(safe.snapshot("BTCUSDT").risk.overallLevel, "HIGH", "mismatched identity/time cannot replace current risk");
    }
  }
}

const v42 = fixture();
v42.mergeAssetCardSnapshot(boundSnapshot(), true);
const missingThreshold = fixture();
missingThreshold.mergeAssetCardSnapshot({ ...boundSnapshot(), thresholdVersion: null }, true);
assert.equal(missingThreshold.nodes.get("BTCUSDT").fields.get("confidence").textContent, "—", "V42 confidence needs the exact threshold bundle identity");
const shortFrame = boundSnapshot("SHORT", 2);
shortFrame.signal.signalAsOf = "2026-09-10T00:05:00Z";
shortFrame.risk.riskBasisSignalAsOf = shortFrame.signal.signalAsOf;
v42.applyAssetCardEvent(boundEvent("SIGNAL", 2, shortFrame));
assert.equal(v42.snapshot("BTCUSDT").signal.direction, "SHORT");
assert.equal(v42.snapshot("BTCUSDT").risk.riskBasisSide, "SHORT", "SIGNAL and its directional risk become visible atomically");
assert.ok(v42.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("追空"));
const shortBefore = JSON.stringify(v42.snapshot("BTCUSDT"));
for (const mismatch of [
  boundEvent("RISK", 10, boundSnapshot("LONG", 10)),
  boundEvent("RISK", 10, shortFrame, { thresholdVersion: "other-threshold" }),
  boundEvent("RISK", 10, shortFrame, { modelVersion: "other-model" }),
  boundEvent("RISK", 10, shortFrame, { riskVersion: "other-risk" }),
  boundEvent("RISK", 10, shortFrame, { riskBasisSignalAsOf: "2026-09-10T00:00:00Z" })
]) {
  v42.applyAssetCardEvent(mismatch);
  assert.equal(JSON.stringify(v42.snapshot("BTCUSDT")), shortBefore, "a new event sequence cannot legitimize mismatched risk provenance");
}
const pendingRisk = fixture(); pendingRisk.mergeAssetCardSnapshot(boundSnapshot(), true);
pendingRisk.applyAssetCardEvent(boundEvent("RISK", 2, shortFrame));
assert.equal(pendingRisk.snapshot("BTCUSDT").risk.riskBasisSide, "LONG", "risk arriving before its signal cannot mix sides");
pendingRisk.applyAssetCardEvent(boundEvent("SIGNAL", 2, shortFrame));
assert.equal(pendingRisk.snapshot("BTCUSDT").risk.riskBasisSide, "SHORT", "atomic SIGNAL recovers without requiring a repeated risk event");
const unbound = boundSnapshot("SHORT", 3); unbound.risk = { overallLevel: "HIGH", items: snapshot().risk.items };
unbound.signal.signalAsOf = shortFrame.signal.signalAsOf;
v42.applyAssetCardEvent(boundEvent("SIGNAL", 3, unbound));
assert.equal(v42.nodes.get("BTCUSDT").fields.get("risk").textContent, "—");
assert.ok(!v42.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML.includes("追高"));
v42.applyAssetCardEvent(boundEvent("PRICE", 4, shortFrame, { spotPrice: 42 }));
assert.equal(v42.timeouts.size, 1);
assert.ok([...v42.timeouts.values()].every(timer => timer.delay <= 250), "card-only paint scheduling leaves margin inside the V42 two-second end-to-end target");
assert.equal(v42.requests.length, 0);
const riskClock = fixture(); riskClock.mergeAssetCardSnapshot(boundSnapshot(), false);
const riskOnlyChange = boundSnapshot("LONG", 2);
riskOnlyChange.risk.items[0].level = "MEDIUM"; riskOnlyChange.risk.overallLevel = "MEDIUM";
riskOnlyChange.cardAsOf = "2026-09-10T00:00:02Z";
riskClock.mergeAssetCardSnapshot(riskOnlyChange, true);
assert.equal(riskClock.snapshot("BTCUSDT").cardAsOf, riskOnlyChange.cardAsOf, "a complete atomic risk change advances its actual published card clock even when signal is unchanged");
const neutral = boundSnapshot("RANGE", 4);
neutral.risk.items = []; neutral.risk.overallLevel = null; neutral.risk.reason = "当前中性方向尚无独立历史分布";
riskClock.mergeAssetCardSnapshot(neutral, true);
const neutralHtml = riskClock.opportunityCard(riskClock.home.assets[0], "BTCUSDT");
assert.ok(neutralHtml.includes('data-live-field="confidence">—'));
assert.ok(riskClock.assetCardRiskDrawer(riskClock.snapshot("BTCUSDT")).includes("当前中性方向"));
assert.ok(!riskClock.assetCardRiskDrawer(riskClock.snapshot("BTCUSDT")).includes("risk-evidence-item"));
const coldHealth = fixture();
const coldSource = { ...boundSnapshot(), signal: { direction: null, status: "INSUFFICIENT_DATA", signalAsOf: "2026-09-10T00:00:00Z" },
  featureVersion: null, modelVersion: null, calibrationVersion: null, thresholdVersion: null,
  health: { status: "SOURCE_UNAVAILABLE", reason: "fixture-real-price-source-loss", asOf: "2026-09-10T00:00:01Z" } };
coldSource.risk = { ...coldSource.risk, riskBasisSide: "NON_DIRECTIONAL", riskBasisDirection: null,
  items: [{ type: "DATA", assessmentStatus: "ASSESSED", level: "HIGH", hardInvalidation: true,
    evidenceValue: "SOURCE_LOST", source: "SPOT_HEALTH", reason: "fixture-real-price-source-loss", asOf: "2026-09-10T00:00:01Z" }] };
coldHealth.applyAssetCardEvent(boundEvent("HEALTH", 1, coldSource));
assert.equal(coldHealth.snapshot("BTCUSDT").risk.overallLevel, "HIGH", "first source-loss SSE retains its independently bound DATA evidence without needing an earlier Home snapshot");
assert.equal(coldHealth.snapshot("BTCUSDT").signal.direction, null);
assert.equal(coldHealth.snapshot("BTCUSDT").signal.signalAsOf, coldSource.signal.signalAsOf);
assert.equal(coldHealth.requests.length, 0);
const sourceRiskMismatch = fixture();
const oldLowRiskSnapshot = boundSnapshot("LONG", 10);
oldLowRiskSnapshot.risk = { ...oldLowRiskSnapshot.risk, overallLevel: "LOW",
  items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"]
    .map(type => ({ ...oldLowRiskSnapshot.risk.items[0], type, level: "LOW" })) };
sourceRiskMismatch.mergeAssetCardSnapshot(oldLowRiskSnapshot, true);
assert.equal(sourceRiskMismatch.nodes.get("BTCUSDT").fields.get("risk").textContent, "低");
const newIdentitySourceFailure = { ...oldLowRiskSnapshot, spotPrice: null, latestPriceAt: null, priceTradeId: null,
  signal: { ...oldLowRiskSnapshot.signal, status: "INVALIDATED", calibratedConfidence: null, pLong: null, pShort: null },
  health: { status: "SOURCE_UNAVAILABLE", reason: "fixture-source-loss", asOf: priceInstant(20) },
  risk: { ...oldLowRiskSnapshot.risk, riskVersion: "test-only-risk-v2", overallLevel: "HIGH",
    items: [{ ...oldLowRiskSnapshot.risk.items[0], type: "DATA", level: "HIGH", evidenceValue: "SOURCE_LOST",
      source: "BINANCE_SPOT_AGG_TRADE", reason: "fixture-source-loss", asOf: priceInstant(20) }] } };
sourceRiskMismatch.applyAssetCardEvent(boundEvent("HEALTH", 10, newIdentitySourceFailure));
assert.equal(sourceRiskMismatch.snapshot("BTCUSDT").health.status, "SOURCE_UNAVAILABLE");
assert.equal(sourceRiskMismatch.snapshot("BTCUSDT").spotPrice, null);
assert.equal(sourceRiskMismatch.snapshot("BTCUSDT").risk.overallLevel, null,
  "source loss with unverified incoming risk identity must become UNKNOWN, not retain a previously assessed LOW");
assert.equal(sourceRiskMismatch.nodes.get("BTCUSDT").fields.get("risk").textContent, "—");
assert.equal(sourceRiskMismatch.requests.length, 0);
for (const path of ["SSE", "CARD_GET", "HOME"]) {
  for (const safeStatus of ["INSUFFICIENT_DATA", "SHADOW"]) {
  const zeroHealth = fixture();
  const zeroSource = { ...coldSource, snapshotVersion: 0, spotPrice: null, latestPriceAt: null, priceTradeId: null,
    signal: { ...coldSource.signal, status: safeStatus, calibratedConfidence: null, pLong: null, pShort: null } };
  let zeroHtml;
  if (path === "SSE") zeroHealth.applyAssetCardEvent(boundEvent("HEALTH", 0, zeroSource));
  else if (path === "CARD_GET") { zeroHealth.setApi(async () => [zeroSource]); await zeroHealth.lightweightHomeRefresh(); }
  else zeroHtml = zeroHealth.opportunityCard({ ...zeroHealth.home.assets[0], cardSignal: zeroSource }, "BTCUSDT");
  const zeroResult = zeroHealth.snapshot("BTCUSDT");
  assert.equal(zeroResult?.snapshotVersion, 0, path + " first real source failure is visible even when the card DB has never allocated a version");
  assert.equal(zeroResult.signal.direction, null);
  assert.equal(zeroResult.signal.calibratedConfidence, null);
  assert.equal(zeroResult.risk.overallLevel, "HIGH", path + " keeps only backend-bound independent DATA evidence");
  assert.equal(zeroResult.risk.riskBasisSide, "NON_DIRECTIONAL");
  assert.ok((zeroHtml || zeroHealth.nodes.get("BTCUSDT").fields.get("risk-items").innerHTML).includes("数据·高"));
  const cleared = JSON.stringify(zeroResult);
  zeroHealth.applyAssetCardEvent(boundEvent("SIGNAL", 0, boundSnapshot()));
  assert.equal(JSON.stringify(zeroHealth.snapshot("BTCUSDT")), cleared, "version zero can never publish a directional signal");
  }
}
for (const override of [
  { signal: { ...coldSource.signal, direction: "LONG" } },
  { signal: { ...coldSource.signal, calibratedConfidence: 72 } },
  { signal: { ...coldSource.signal, pLong: .72 } },
  { signal: { ...coldSource.signal, pShort: .2 } },
  { signal: { ...coldSource.signal, status: "VALID" } },
  { health: { ...coldSource.health, asOf: null } },
  { health: { ...coldSource.health, asOf: "2099-01-01T00:00:00Z" } }
]) {
  const invalidZero = fixture();
  invalidZero.applyAssetCardEvent(boundEvent("HEALTH", 0, { ...coldSource, ...override }));
  assert.equal(invalidZero.snapshot("BTCUSDT"), undefined, "untrusted zero-version state cannot initialize the visible cohort");
}
const zeroLegacy = fixture();
zeroLegacy.setHome({ ...zeroLegacy.home, assets: zeroLegacy.home.assets.map(asset => ({ ...asset, cardSignalDisplayEnabled: false })) });
zeroLegacy.applyAssetCardEvent(boundEvent("HEALTH", 0, coldSource));
assert.equal(zeroLegacy.snapshot("BTCUSDT"), undefined, "cold failure must not activate a legacy/SHADOW card");
const priceDomain = fixture(); priceDomain.mergeAssetCardSnapshot(boundSnapshot("LONG", 10), true);
const beforePriceDomain = priceDomain.snapshot("BTCUSDT");
priceDomain.applyAssetCardEvent(event("PRICE", 0, { priceTradeId: 50, latestPriceAt: priceInstant(50), spotPrice: 123 }));
assert.equal(priceDomain.snapshot("BTCUSDT").spotPrice, 123, "real public trade does not depend on writable card database or allocated snapshot version");
assert.equal(priceDomain.snapshot("BTCUSDT").snapshotVersion, 10, "PRICE cannot allocate/advance the persisted signal/risk identity");
assert.equal(priceDomain.snapshot("BTCUSDT").signal, beforePriceDomain.signal);
assert.equal(priceDomain.snapshot("BTCUSDT").risk, beforePriceDomain.risk);
for (const stalePrice of [
  { priceTradeId: 49, latestPriceAt: priceInstant(49) },
  { priceTradeId: 50, latestPriceAt: priceInstant(51) },
  { priceTradeId: 51, latestPriceAt: priceInstant(49) },
  { priceTradeId: null, latestPriceAt: priceInstant(52) },
  { priceTradeId: 1000000, latestPriceAt: "2099-01-01T00:00:00Z" }
]) priceDomain.applyAssetCardEvent(event("PRICE", 999, { ...stalePrice, spotPrice: 1 }));
assert.equal(priceDomain.snapshot("BTCUSDT").spotPrice, 123);
const oldFailure = { ...boundSnapshot("LONG", 11), health: { status: "SOURCE_UNAVAILABLE", asOf: priceInstant(49) } };
priceDomain.applyAssetCardEvent(boundEvent("HEALTH", 11, oldFailure));
assert.equal(priceDomain.snapshot("BTCUSDT").spotPrice, 123, "delayed source-failure observation cannot erase a later real trade");
priceDomain.applyAssetCardEvent(boundEvent("HEALTH", 12, { ...oldFailure, health: { ...oldFailure.health, asOf: priceInstant(52) } }));
assert.equal(priceDomain.snapshot("BTCUSDT").spotPrice, null);
priceDomain.applyAssetCardEvent(event("PRICE", 0, { priceTradeId: 53, latestPriceAt: priceInstant(53), spotPrice: 125 }));
assert.equal(priceDomain.snapshot("BTCUSDT").spotPrice, 125, "new actual trade restores only price after source failure");
assert.equal(priceDomain.snapshot("BTCUSDT").signal.status, "INVALIDATED");
assert.equal(priceDomain.requests.length, 0);

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
    assert.ok(html.includes('data-live-field="price">—'), "a legacy Mark/closed-bar value without Spot trade identity cannot become the main price after source-caption removal");
    assert.ok(!html.includes("asset-price-caption") && !html.includes("pinned-observation-copy"));
    assert.ok(!html.includes("最近闭线价") && !html.includes("实时价") && !html.includes("价格来源待确认") && !html.includes("置顶观察"));
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
// Cosmetic legacy cleanup never enables the private renderer or borrows its probabilities.
for (const caption of ["暂不可判断", "待重新分析", "周期冲突", "UNKNOWN", "", null]) {
  const legacy = fixture();
  const asset = { ...legacy.home.assets[0], cardSignalDisplayEnabled: false, marketBiasLabel: caption,
    finalMarketBias: "UNDETERMINED", finalConfidence: 60, latestPriceSource: "BINANCE_MARK_PRICE_WEBSOCKET",
    priceBasis: "LIVE", latestPriceAt: priceInstant(1), cardSignal: boundSnapshot("LONG", 10) };
  const html = legacy.opportunityCard(asset, "BTCUSDT");
  assert.match(html, /data-live-field="direction"[^>]*>—<\/b>/);
  assert.ok(html.includes('data-live-field="confidence">—</strong>') && !html.includes("60%") && !html.includes("72%"));
  assert.ok(html.includes('data-live-field="price">—</strong>') && !html.includes("99,999"));
  assert.equal(legacy.snapshot("BTCUSDT"), undefined);
}
for (const basis of ["CLOSED_5M", "LIVE", "UNKNOWN"]) {
  const legacy = fixture(), asset = { ...legacy.home.assets[0], cardSignalDisplayEnabled: false,
    priceBasis: basis, latestPriceAt: priceInstant(1), directionCalculatedAt: "2026-09-10T00:00:00Z" };
  const html = legacy.opportunityCard(asset, "BTCUSDT");
  assert.ok(html.includes('data-live-field="confidence">99%'), "valid legacy direction/confidence stays outside the new cohort");
  assert.equal((html.match(/<time\b/g) || []).length, 1, "legacy cards have only the analysis clock, never a price-caption clock");
  assert.equal(html.match(/class="opportunity-updated"[^>]*>([^<]*)<\/time>/)[1],
    legacy.assetCardClock(asset.directionCalculatedAt));
  const field = legacy.nodes.get("BTCUSDT").fields.get("price"); field.textContent = "—";
  legacy.applyHomeLiveEvent({ eventType: "ASSET_PRICE_UPDATED", symbol: "BTCUSDT", snapshotVersion: 20,
    payload: { price: 88888, latestPrice: 88888, source: "BINANCE_MARK_PRICE_WEBSOCKET", latestPriceAt: priceInstant(20) } });
  assert.equal(field.textContent, "—", "shared Mark SSE cannot reintroduce a revoked legacy main price");
  assert.equal(legacy.timeouts.size, 0); assert.equal(legacy.requests.length, 0);
  assert.ok(legacy.opportunityCard({ ...asset, latestPrice: 88888 }, "BTCUSDT").includes('data-live-field="price">—'));
}
for (const raw of ["UNDETERMINED", null, "BEARISH"]) {
  const legacy = fixture(), asset = { ...legacy.home.assets[0], cardSignalDisplayEnabled: false,
    marketBiasLabel: "偏多", finalMarketBias: raw, finalConfidence: 60 };
  const html = legacy.opportunityCard(asset, "BTCUSDT");
  assert.match(html, /data-live-field="direction"[^>]*>—<\/b>/);
  assert.ok(html.includes('data-live-field="confidence">—') && !html.includes("60%"), "a translated label cannot override an absent or different legacy machine direction");
}
// A server-authorized SHADOW Owner sees actual independent fields, never private model probabilities.
const ownerPreview = fixture();
const previewFrame = { ...snapshot("BTCUSDT", 10),
  signal: { direction: null, status: "SHADOW", calibratedConfidence: null, pLong: null, pShort: null,
    oneHourState: "OPPORTUNITY", fourHourTrend: "LONG", signalAsOf: "2026-09-10T00:00:00Z" },
  risk: { overallLevel: "HIGH", items: [{ type: "DATA", assessmentStatus: "ASSESSED", level: "HIGH",
    evidenceValue: "STALE", source: "BINANCE_SPOT", asOf: "2026-09-10T00:00:00Z", reason: "真实来源已过期", unit: "STATE" }],
    riskAsOf: "2026-09-10T00:00:00Z", riskBasisSide: "NON_DIRECTIONAL", riskBasisDirection: null,
    riskBasisSignalAsOf: "2026-09-10T00:00:00Z", riskMarketAsOf: "2026-09-10T00:00:00Z", riskVersion: "test-only-risk" } };
const previewHtml = ownerPreview.opportunityCard({ ...ownerPreview.home.assets[0], cardSignal: previewFrame }, "BTCUSDT");
assert.ok(previewHtml.includes('data-live-field="price">$100'));
assert.ok(previewHtml.includes('data-live-field="confidence">—'));
assert.ok(previewHtml.includes('data-desktop-hover="risk"'), "independently evidenced DATA risk remains inspectable without a model");
assert.ok(!previewHtml.includes("99%") && !previewHtml.includes("72%"));
assert.equal((previewHtml.match(/data-live-field="confidence"/g) || []).length, 1);
assert.equal(ownerPreview.requests.length, 0);
const failedPreview = { ...previewFrame, signal: { ...previewFrame.signal, status: "FAILED" },
  health: { status: "SIGNAL_AND_RISK_UNAVAILABLE", reason: "TEST_OWNER_FIELD_FAILURE", asOf: priceInstant(30) },
  risk: { ...previewFrame.risk, overallLevel: null,
    items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"].map(type => ({
      type, assessmentStatus: "UNKNOWN", level: null, reason: "TEST_OWNER_FIELD_FAILURE" })) } };
ownerPreview.applyAssetCardEvent(boundEvent("HEALTH", 10, failedPreview));
assert.equal(ownerPreview.snapshot("BTCUSDT").signal.status, "FAILED");
assert.equal(ownerPreview.snapshot("BTCUSDT").risk.overallLevel, null);
assert.equal(ownerPreview.snapshot("BTCUSDT").spotPrice, 100);
assert.equal(ownerPreview.snapshot("BTCUSDT").signal.calibratedConfidence, null);

const cohort = fixture();
const cohortAssets = cohort.home.assets.map((asset, index) => ({ ...asset, cardSignalDisplayEnabled: index === 0 }));
cohort.setHome({ ...cohort.home, assets: cohortAssets });
cohort.mergeAssetCardSnapshot(snapshot("BTCUSDT", 2), false);
cohort.applyAssetCardEvent(event("PRICE", 4, { spotPrice: 444, latestPriceAt: priceInstant(4) }));
let mixedHomeReads = 0, mixedHtml;
cohort.setHomeLoader(async () => {
  mixedHomeReads++;
  mixedHtml = cohortAssets.map(asset => cohort.opportunityCard({ ...asset, cardSignal: snapshot(asset.rawSymbol, 1) }, "BTCUSDT")).join("");
});
cohort.setApi(async () => { throw new Error("mixed cohort must not issue a second concurrent card GET"); });
await cohort.lightweightHomeRefresh();
assert.equal(mixedHomeReads, 1);
assert.equal(cohort.snapshot("BTCUSDT").snapshotVersion, 2, "independent price cannot advance persisted signal/risk version");
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
  card.signal.calibratedConfidence = /SHORT$/.test(direction) ? 20 : /LONG$/.test(direction) ? 72 : null;
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
assert.match(html, /data-live-field="direction"[^>]*>—<\/b>/);
assert.ok(html.includes('data-live-field="confidence">—</strong>') && html.includes("已失效") && !html.includes("72%"));
assert.equal(f.snapshot("BTCUSDT").signal.direction, "LONG", "invalidated direction remains an internal audit/risk basis, not a visible current prediction");
assert.equal(f.snapshot("BTCUSDT").risk.riskBasisSide, "LONG");
const unvalidated = snapshot("BTCUSDT", version++); unvalidated.modelVersion = null;
f.mergeAssetCardSnapshot(unvalidated, false);
assert.ok(!f.opportunityCard(f.home.assets[0], "BTCUSDT").includes("72%"));
for (const status of ["SHADOW", "UNVALIDATED", "UNKNOWN"]) {
  const shadow = snapshot("BTCUSDT", version++); shadow.signal.status = status;
  f.mergeAssetCardSnapshot(shadow, false);
  const hiddenModel = f.opportunityCard(f.home.assets[0], "BTCUSDT");
  assert.ok(!hiddenModel.includes("72%") && hiddenModel.includes("数据不足"), status);
  assert.match(hiddenModel, /data-live-field="direction"[^>]*>—<\/b>/);
}
for (const mutation of [
  value => { value.signal.signalAsOf = null; },
  value => { value.signal.signalAsOf = "not-a-time"; },
  value => { value.signal.signalAsOf = "2026-09-10T00:00:00"; },
  value => { value.signal.signalAsOf = "2099-01-01T00:00:00Z"; },
  value => { value.signal.direction = "CONFLICT"; },
  ...[null, NaN, -1, 101, 72.5, 71].map(value => card => { card.signal.calibratedConfidence = value; }),
  ...["pLong", "pShort"].flatMap(key => [null, NaN, -0.1, 1.1, ".72"].map(value => card => { card.signal[key] = value; })),
  ...["featureVersion", "modelVersion", "calibrationVersion", "thresholdVersion"].map(key => value => { value[key] = " "; })
]) {
  const runtime = fixture(), value = boundSnapshot("LONG", 10); mutation(value);
  const rendered = runtime.opportunityCard({ ...runtime.home.assets[0], cardSignal: value }, "BTCUSDT");
  assert.match(rendered, /data-live-field="direction"[^>]*>—<\/b>/);
  assert.ok(rendered.includes('data-live-field="confidence">—</strong>') && !rendered.includes("72%") && !rendered.includes("99%"),
    "a current model prediction requires its own complete, time-valid identity on both initial render and patch");
}
for (const [direction, expected] of [["LONG", "追高风险"], ["SHORT", "追空风险"], ["RANGE", "位置风险"]]) {
  const runtime = fixture(), value = boundSnapshot(direction, 10);
  value.risk.items = [{ ...value.risk.items[0], level: "HIGH" }, ...["SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"]
    .map(type => ({ type, assessmentStatus: "UNKNOWN", level: null, reason: "TEST_NO_INDEPENDENT_EVIDENCE" }))];
  const rendered = runtime.opportunityCard({ ...runtime.home.assets[0], riskLevel: "LOW", cardSignal: value }, "BTCUSDT");
  assert.ok(rendered.includes(expected + "·高") && !rendered.includes('asset-card-risk-low">低'));
  assert.equal((runtime.assetCardRiskDrawer(runtime.snapshot("BTCUSDT")).match(/risk-evidence-item/g) || []).length, 1);
  assert.equal(runtime.snapshot("BTCUSDT").risk.items.length, 8, "partial unknowns cannot discard another independently known high risk");
}
for (const overallLevel of ["HIGH", "MEDIUM"]) {
  const runtime = fixture(), value = boundSnapshot("LONG", 10);
  value.risk = { ...value.risk, overallLevel, items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "LIQUIDATION", "LIQUIDITY", "EVENT", "DATA"]
    .map(type => ({ type, assessmentStatus: "UNKNOWN", level: null, reason: "TEST_NO_INDEPENDENT_EVIDENCE" })) };
  const rendered = runtime.opportunityCard({ ...runtime.home.assets[0], riskLevel: "HIGH", cardSignal: value }, "BTCUSDT");
  assert.match(rendered, /data-live-field="risk" class="asset-card-risk-unknown"[^>]*>—/);
  assert.ok(!runtime.assetCardRiskDrawer(runtime.snapshot("BTCUSDT")).includes("risk-evidence-item"), "aggregate-only or canonical HIGH cannot substitute for independent card risk evidence");
}
const metadata = fixture();
metadata.mergeAssetCardSnapshot(snapshot("BTCUSDT", 1), true);
metadata.applyAssetCardEvent(event("SIGNAL", 2, { signal: { ...snapshot().signal, direction: "SHORT", calibratedConfidence: 68, pShort: .68 },
  featureVersion: null, modelVersion: null, calibrationVersion: null, thresholdVersion: null }));
assert.equal(metadata.nodes.get("BTCUSDT").fields.get("confidence").textContent, "—", "a new SIGNAL without its own bundle metadata cannot borrow an old validated version");
metadata.applyAssetCardEvent(event("SIGNAL", 3, { signal: { ...snapshot().signal, direction: "SHORT", calibratedConfidence: 68, pShort: .68 },
  featureVersion: "test-only-features-v2", modelVersion: "test-only-model-v2", calibrationVersion: "test-only-calibration-v2" }));
assert.equal(metadata.nodes.get("BTCUSDT").fields.get("confidence").textContent, "68%");
assert.equal(metadata.snapshot("BTCUSDT").modelVersion, "test-only-model-v2");
metadata.applyAssetCardEvent(event("PRICE", 4, { spotPrice: 104, modelVersion: "unrelated-price-metadata" }));
assert.equal(metadata.snapshot("BTCUSDT").modelVersion, "test-only-model-v2", "PRICE cannot update SIGNAL bundle provenance");

const base = snapshot("BTCUSDT", 100);
f.mergeAssetCardSnapshot(base, false);
// The real SSE transport orders PRICE by the exchange trade ID, not the durable signal/risk CAS.
const independentTransport = fixture();
independentTransport.mergeAssetCardSnapshot(snapshot("BTCUSDT", 10), false);
for (const trade of [901, 902, 903]) {
  independentTransport.applyAssetCardEvent(event("PRICE", trade, {
    transportVersion: trade, snapshotVersion: 10, priceTradeId: String(trade),
    latestPriceAt: priceInstant(trade), spotPrice: trade
  }));
}
independentTransport.flushPrices();
assert.equal(independentTransport.snapshot("BTCUSDT").spotPrice, 903, "consecutive real PRICE events cannot be discarded for an unchanged durable version");
assert.equal(independentTransport.snapshot("BTCUSDT").snapshotVersion, 10);
assert.ok(independentTransport.nodes.get("BTCUSDT").fields.get("price").textContent.includes("903"));
independentTransport.applyAssetCardEvent(event("PRICE", 904, {
  transportVersion: 905, snapshotVersion: 10, priceTradeId: "904", latestPriceAt: priceInstant(904), spotPrice: 1
}));
assert.equal(independentTransport.snapshot("BTCUSDT").spotPrice, 903, "mismatched transport identity fails closed");
independentTransport.applyAssetCardEvent(event("PRICE", 905, {
  transportVersion: 905, snapshotVersion: 10, priceTradeId: "906", latestPriceAt: priceInstant(905), spotPrice: 1
}));
assert.equal(independentTransport.snapshot("BTCUSDT").spotPrice, 903, "PRICE transport identity must be its actual trade ID");
const homeBefore = JSON.stringify(f.home), ethBefore = JSON.stringify(f.nodes.get("ETHUSDT"));
const clockBefore = f.snapshot("BTCUSDT").cardAsOf;
f.applyAssetCardEvent(event("PRICE", 104, { spotPrice: 104, latestPriceAt: priceInstant(104), cardAsOf: "2099-01-01T00:00:00Z", signal: { direction: "SHORT" } }));
f.applyAssetCardEvent(event("PRICE", 105, { spotPrice: 105, latestPriceAt: priceInstant(105) }));
assert.equal(f.timeouts.size, 1, "one throttled price render per symbol");
assert.ok([...f.timeouts.values()].every(timer => timer.delay > 0 && timer.delay <= 250));
assert.equal(f.nodes.get("BTCUSDT").fields.get("price").textContent, "");
f.applyAssetCardEvent(event("SIGNAL", 102, { signal: { ...base.signal, direction: "SHORT", calibratedConfidence: 68, pShort: .68 }, cardAsOf: "2026-09-10T00:00:02Z", spotPrice: 1 }));
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
for (const [type, payload] of [["PRICE", { spotPrice: 120 }], ["SIGNAL", { signal: base.signal, risk: base.risk }],
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
risks.risk = { ...risks.risk, overallLevel: "HIGH", items: ["CHASE", "SHOCK", "REVERSAL", "CROWDING", "DATA"].map((type, index) => ({
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
orderedRisk.mergeAssetCardSnapshot({ ...orderedSnapshot, snapshotVersion: 2, risk: { ...orderedSnapshot.risk, overallLevel: "MEDIUM", items: [
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
assert.equal([...reconnect.timeouts.values()][0].delay, 250);
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
safety.applyAssetCardEvent(event("PRICE", 202, { spotPrice: 202, latestPriceAt: priceInstant(202) }));
const safeHomeBefore = JSON.stringify(safety.home), safeEthBefore = JSON.stringify(safety.nodes.get("ETHUSDT"));
const unavailable = version => ({ ...snapshot("BTCUSDT", version), spotPrice: null, latestPriceAt: null,
  signal: { ...base.signal, direction: "SHORT", status: "INVALIDATED", calibratedConfidence: null },
  risk: { overallLevel: null, items: [] }, health: { status: "SOURCE_UNAVAILABLE", reason: "test-only stale read", asOf: priceInstant(202) } });
safety.setApi(async () => [unavailable(199)]);
await safety.lightweightHomeRefresh();
assert.equal(safety.snapshot("BTCUSDT").signal.status, "VALID", "a stale complete read cannot downgrade a lagging field group");
assert.equal(safety.snapshot("BTCUSDT").spotPrice, 202);
safety.setApi(async () => [unavailable(200)]);
await safety.lightweightHomeRefresh();
assert.equal(safety.snapshot("BTCUSDT").spotPrice, null, "a latest same-version read can clear unsafe price without allocating a version");
assert.equal(safety.snapshot("BTCUSDT").signal.direction, "LONG", "safety downgrade retains existing direction, never adopts an opposite one");
assert.equal(safety.snapshot("BTCUSDT").signal.status, "INVALIDATED");
assert.equal(safety.snapshot("BTCUSDT").signal.calibratedConfidence, null);
assert.equal(safety.snapshot("BTCUSDT").signal.pLong, null);
assert.equal(safety.snapshot("BTCUSDT").signal.pShort, null);
assert.equal(safety.snapshot("BTCUSDT").risk.overallLevel, "HIGH", "read safety cannot erase a known high risk");
assert.equal(safety.snapshot("BTCUSDT").snapshotVersion, 200);
assert.equal(safety.snapshot("BTCUSDT").cardAsOf, base.cardAsOf, "read safety does not invent a newly published effective clock");
assert.equal(safety.snapshot("BTCUSDT").health.status, "SOURCE_UNAVAILABLE");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("price").textContent, "—");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("confidence").textContent, "—");
assert.equal(safety.nodes.get("BTCUSDT").fields.get("status").textContent, "已失效");
assert.equal(JSON.stringify(safety.nodes.get("ETHUSDT")), safeEthBefore);
assert.equal(JSON.stringify(safety.home), safeHomeBefore);
const downgraded = JSON.stringify(safety.snapshot("BTCUSDT"));
safety.setApi(async () => [snapshot("BTCUSDT", 200)]);
await safety.lightweightHomeRefresh();
for (const version of [199, 200]) for (const [group, fields] of [["PRICE", { spotPrice: 999 }],
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
modelBase.risk.riskBasisSignalAsOf = modelBase.signal.signalAsOf;
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
  assert.equal(result.risk.riskBasisSide, "NON_DIRECTIONAL");
  assert.equal(result.risk.overallLevel, null, "an unavailable model cannot retain the prior directional CHASE grade");
  assert.equal(result.risk.items.length, 0);
  assert.ok(!modelSafety.assetCardRiskDrawer(result).includes("risk-evidence-item"), "unknown reason is compact, never eight empty risk rows");
  assert.ok(modelSafety.assetCardRiskDrawer(result).includes("当前模型不可用"));
  assert.equal(result.cardAsOf, modelBase.cardAsOf);
  assert.equal(JSON.stringify(modelSafety.snapshot("ETHUSDT")), beforeOther);
  const html = modelSafety.opportunityCard({ ...modelSafety.home.assets[0], cardSignal: modelBase }, "BTCUSDT");
  assert.ok(!html.includes("72%") && !html.includes("99%"), "an equal-version normal Home cannot restore a revoked model or legacy confidence");
}
const staleModelHome = fixture();
staleModelHome.mergeAssetCardSnapshot({ ...modelBase, snapshotVersion: 290 }, false);
staleModelHome.applyAssetCardEvent(event("PRICE", 301, { spotPrice: 301 }));
staleModelHome.opportunityCard({ ...staleModelHome.home.assets[0], cardSignal: revokedModel(300) }, "BTCUSDT");
assert.equal(staleModelHome.snapshot("BTCUSDT").signal.status, "UNVALIDATED", "model revocation is compared to persisted signal state, not the independent price trade id");
assert.equal(staleModelHome.snapshot("BTCUSDT").spotPrice, 301, "model revocation must preserve the later real trade");
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
assert.equal(modelHealth.snapshot("BTCUSDT").risk.overallLevel, null, "old LONG risk cannot repopulate a model-unavailable NON_DIRECTIONAL card");
const combinedFailure = fixture(); combinedFailure.mergeAssetCardSnapshot(modelBase, false);
combinedFailure.opportunityCard({ ...combinedFailure.home.assets[0], cardSignal: { ...revokedModel(300),
  spotPrice: null, latestPriceAt: null, health: { status: "SOURCE_UNAVAILABLE", reason: "fixture-price-and-model-loss", asOf: priceInstant(301) } } }, "BTCUSDT");
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
  const localClock = fixture(), value = snapshot(); localClock.mergeAssetCardSnapshot(value, true);
  const target = localClock.nodes.get("BTCUSDT").fields.get("card-time");
  assert.equal(target.textContent, expected);
  assert.equal(target.attributes.datetime, value.signal.signalAsOf);
  localClock.applyAssetCardEvent(event("PRICE", 2, { spotPrice: 102 })); localClock.flushPrices();
  assert.equal(target.textContent, expected, "real price updates never change the user's analysis clock");
  assert.equal(target.attributes.datetime, value.signal.signalAsOf);
}
console.log("ASSET_CARD_RUNTIME_MATRIX: PASS (rendering, field isolation, versions, clocks, polling, frozen geometry)");
