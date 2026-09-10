import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";
import { spawnSync } from "node:child_process";

const source = fs.readFileSync("src/main/resources/static/js/home-runtime.js", "utf8");
assert.ok(source.includes("function applyAssetCardEvent("), "production Home must own a field-only asset-card event handler");
const bootstrap = "    desktop.installHoverDrawers(function (trigger) {";
assert.equal(source.split(bootstrap).length, 2);
const exported = `globalThis.cardTest = {
  opportunityCard, applyAssetCardEvent, mergeAssetCardSnapshot, lightweightHomeRefresh, assetCardRiskDrawer,
  scheduleHomeFallbackPoll, stopHomeFallbackPoll, connectHomeStream, assetCardClock,
  setHome(value) { currentHome = value; homeCardSymbols = value.assets.map(symbolOf); },
  setConnected(value) { homeStreamConnected = value; },
  stream() { return homeEventSource; },
  snapshot(symbol) { return assetCardSnapshots.get(symbol); },
  setApi(value) { api = value; },
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
