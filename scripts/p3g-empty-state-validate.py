#!/usr/bin/env python3
import json
import sys


MISSING = object()


def load_data(path):
    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    if isinstance(payload, dict) and isinstance(payload.get("data"), dict):
        return payload["data"]
    return payload


def require(condition, message):
    if not condition:
        raise SystemExit("FAIL " + message)


if len(sys.argv) != 4:
    raise SystemExit("FAIL expected dashboard, review, and run-baseline JSON files")

dashboard = load_data(sys.argv[1])
review = load_data(sys.argv[2])
baseline = load_data(sys.argv[3])

require(isinstance(dashboard, dict), "dashboard payload is not an object")
require(isinstance(review, dict), "review payload is not an object")
require(isinstance(baseline, dict), "run-baseline payload is not an object")


def normalized(value):
    return value.strip().upper() if isinstance(value, str) else value


def is_allowed(value, allowed):
    try:
        return value in allowed
    except TypeError:
        return False


def is_zero(value):
    return not isinstance(value, bool) and isinstance(value, (int, float)) and value == 0


def require_no_directional_or_trade_text(value, field):
    if not isinstance(value, str):
        return
    upper_value = value.upper()
    forbidden = (
        "STRONG_BULLISH", "WEAK_BULLISH", "BULLISH", "STRONG_BEARISH",
        "WEAK_BEARISH", "BEARISH", "RANGE", "NEUTRAL", "LONG", "SHORT",
        "PLAN_READY", "POSITION_MONITORING", "CANDIDATE", "TRIGGERED",
        "HIGH_RISK", "COOLING", "CONFUSED", "偏多", "偏空", "做多",
        "做空", "开仓", "可交易", "交易结论", "执行计划", "震荡",
        "观望机会", "建议关注", "低风险", "中风险", "高风险",
        "极高风险", "候选", "等待触发", "已触发", "冷却",
    )
    require(not any(token in upper_value for token in forbidden),
            f"empty dashboard {field} contains a directional/trading conclusion")


FORMAL_MARKET_BIASES = {
    "STRONG_BULLISH", "BULLISH", "WEAK_BULLISH", "RANGE",
    "WEAK_BEARISH", "BEARISH", "STRONG_BEARISH", "WAIT",
}
EMPTY_ALLOWED_MARKET_BIASES = {None, "", "WAIT"}
FORMAL_ASSET_STATES = {
    "OBSERVING", "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED",
    "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED",
}
EMPTY_ALLOWED_ASSET_STATES = {None, ""}
GENERIC_EMPTY_STATUSES = {
    None, "", "WAITING_SYNC", "DATA_INSUFFICIENT", "NOT_APPLICABLE",
    "FAIL_CLOSED", "UNKNOWN",
}
EMPTY_ALLOWED_AI_STATUSES = {
    None, "", "WAITING_SYNC", "NOT_APPLICABLE", "NOT_CALLED", "DISABLED",
    "FAIL_CLOSED", "UNKNOWN",
}
EMPTY_ALLOWED_PROVIDER_STATUSES = {
    None, "", "WAITING_SYNC", "NOT_CONFIGURED", "UNAVAILABLE", "DISABLED",
    "CONFIGURED", "FAIL_CLOSED", "UNKNOWN", "MODEL_UNAVAILABLE",
}
EMPTY_ALLOWED_ASSET_SOURCE_PROVIDERS = {
    None, "", "WAITING_SYNC", "NOT_CONFIGURED", "UNAVAILABLE", "DISABLED",
    "NO_DATA",
}
EMPTY_ALLOWED_COUNTER_STATUSES = {
    None, "", "WAITING_SYNC", "CONNECTED", "FAIL_CLOSED", "UNKNOWN",
}
EMPTY_ALLOWED_DATA_FRESHNESS = {"NO_DATA", "UNAVAILABLE"}
EMPTY_ALLOWED_TIMEFRAME_FRESHNESS = {"NO_DATA"}
EMPTY_ALLOWED_RISK_LEVELS = {None, ""}
EMPTY_ALLOWED_CONFIDENCE_LEVELS = {None, ""}
EMPTY_ALLOWED_NO_CONCLUSION_LABELS = {
    None, "", "等待同步", "暂无数据", "数据不足", "暂无结论", "不适用",
}
EMPTY_ALLOWED_MARKET_BIAS_LABELS = EMPTY_ALLOWED_NO_CONCLUSION_LABELS | {"观望"}
EXPECTED_TIMEFRAMES = {"5m", "15m", "1h", "4h"}
ASSET_FIELD_ALLOWLIST = {
    "slot", "slotType", "symbol", "rawSymbol", "marketBias", "marketBiasLabel",
    "compositeScore", "confidenceLevel", "confidenceLabel", "riskLevel", "riskLabel",
    "assetState", "assetStateLabel", "worthOpening", "latestPrice", "dataFreshness",
    "timeframeFreshness", "sourceProvider", "unavailableReason", "evidenceCount",
    "latestAnalysisTime", "currentConclusion",
}

assets = dashboard.get("assets", MISSING)
require(assets is not MISSING, "dashboard assets is missing")
require(isinstance(assets, list), "dashboard assets is not a list")
for index, asset in enumerate(assets):
    require(isinstance(asset, dict), f"empty asset card {index} is not an object")
    unexpected_fields = sorted(set(asset) - ASSET_FIELD_ALLOWLIST)
    require(not unexpected_fields,
            f"empty asset card {index} has unexpected fields: {', '.join(unexpected_fields)}")
    require(normalized(asset.get("slotType")) == "DEFAULT_SLOT",
            f"empty asset card {index} has unsupported slotType")

    asset_state = normalized(asset.get("assetState"))
    require(is_allowed(asset_state, {None, ""}) or is_allowed(asset_state, FORMAL_ASSET_STATES),
            f"empty asset card {index} assetState is not a formal AssetStateEnum value")
    require(is_allowed(asset_state, EMPTY_ALLOWED_ASSET_STATES),
            f"empty asset card {index} has non-empty assetState")

    market_bias = normalized(asset.get("marketBias"))
    require(is_allowed(market_bias, {None, ""}) or is_allowed(market_bias, FORMAL_MARKET_BIASES),
            f"empty asset card {index} marketBias is not a formal MarketBiasEnum value")
    require(is_allowed(market_bias, EMPTY_ALLOWED_MARKET_BIASES),
            f"empty asset card {index} has an empty-database marketBias conclusion")
    require(is_allowed(asset.get("marketBiasLabel"), EMPTY_ALLOWED_MARKET_BIAS_LABELS),
            f"empty asset card {index} marketBiasLabel is not a no-conclusion label")

    require(asset.get("worthOpening") is None or asset.get("worthOpening") is False,
            f"empty asset card {index} has worthOpening=true")
    require(asset.get("latestPrice") is None,
            f"empty asset card {index} fabricated latestPrice")
    require(asset.get("compositeScore") is None or is_zero(asset.get("compositeScore")),
            f"empty asset card {index} fabricated compositeScore")
    require(is_allowed(normalized(asset.get("confidenceLevel")), EMPTY_ALLOWED_CONFIDENCE_LEVELS),
            f"empty asset card {index} has a confidence conclusion")
    require(is_allowed(asset.get("confidenceLabel"), EMPTY_ALLOWED_NO_CONCLUSION_LABELS),
            f"empty asset card {index} confidenceLabel is not a no-conclusion label")
    require(is_allowed(normalized(asset.get("riskLevel")), EMPTY_ALLOWED_RISK_LEVELS),
            f"empty asset card {index} has a risk conclusion")
    require(is_allowed(asset.get("riskLabel"), EMPTY_ALLOWED_NO_CONCLUSION_LABELS),
            f"empty asset card {index} riskLabel is not a no-conclusion label")
    require(is_allowed(asset.get("assetStateLabel"), EMPTY_ALLOWED_NO_CONCLUSION_LABELS),
            f"empty asset card {index} assetStateLabel is not a no-conclusion label")
    require(is_allowed(asset.get("currentConclusion"), EMPTY_ALLOWED_NO_CONCLUSION_LABELS),
            f"empty asset card {index} currentConclusion is not fail-closed")
    require(asset.get("evidenceCount") is None or is_zero(asset.get("evidenceCount")),
            f"empty asset card {index} has non-zero evidenceCount")
    require(is_allowed(asset.get("latestAnalysisTime"), {None, ""}),
            f"empty asset card {index} fabricated latestAnalysisTime")
    require(is_allowed(normalized(asset.get("sourceProvider")),
                       EMPTY_ALLOWED_ASSET_SOURCE_PROVIDERS),
            f"empty asset card {index} reports a connected sourceProvider")

    unavailable_reason = asset.get("unavailableReason")
    data_freshness = normalized(asset.get("dataFreshness"))
    require(unavailable_reason is None or isinstance(unavailable_reason, str),
            f"empty asset card {index} unavailableReason is not text/null")
    require(is_allowed(data_freshness, EMPTY_ALLOWED_DATA_FRESHNESS),
            f"empty asset card {index} dataFreshness is not a no-data state")

    timeframe_freshness = asset.get("timeframeFreshness", MISSING)
    require(timeframe_freshness is not MISSING,
            f"empty asset card {index} timeframeFreshness is missing")
    require(isinstance(timeframe_freshness, dict),
            f"empty asset card {index} timeframeFreshness is not an object")
    require(set(timeframe_freshness) == EXPECTED_TIMEFRAMES,
            f"empty asset card {index} timeframeFreshness keys are not exact")
    require(all(is_allowed(normalized(value), EMPTY_ALLOWED_TIMEFRAME_FRESHNESS)
                for value in timeframe_freshness.values()),
            f"empty asset card {index} reports fresh timeframe data")
    for field in (
        "marketBiasLabel", "confidenceLabel", "riskLabel", "assetStateLabel",
        "unavailableReason", "currentConclusion",
    ):
        require_no_directional_or_trade_text(asset.get(field), f"assets[{index}].{field}")
    confidence_text = " ".join(str(asset.get(field) or "")
                               for field in ("confidenceLevel", "confidenceLabel")).upper()
    require(not any(token in confidence_text
                    for token in ("HIGH", "高置信", "高可信", "高质量")),
            f"empty asset card {index} reports high confidence")

system_state = dashboard.get("systemState", MISSING)
require(system_state is not MISSING, "dashboard systemState is missing")
require(isinstance(system_state, dict), "dashboard systemState is not an object")
system_keys = {
    "marketTrend", "riskLevel", "dataQuality", "aiConflict",
    "pendingReview", "confused", "hotReset",
}
require(system_keys.issubset(system_state), "empty systemState is missing required cards")
require(not (set(system_state) - system_keys), "empty systemState has unexpected cards")
status_card_fields = {"key", "label", "value", "valueLabel", "helper", "status", "score", "meta"}
for name in system_keys:
    card = system_state.get(name)
    require(isinstance(card, dict), f"empty systemState.{name} is not an object")
    require(not (set(card) - status_card_fields),
            f"empty systemState.{name} has unexpected fields")
    require(card.get("score") is None or is_zero(card.get("score")),
            f"empty systemState.{name} fabricated a score")
    if name != "hotReset":
        meta = card.get("meta", MISSING)
        require(meta is MISSING or meta is None or meta == {},
                f"empty systemState.{name} contains hidden meta evidence")
    for field in ("value", "valueLabel", "helper"):
        require_no_directional_or_trade_text(card.get(field), f"systemState.{name}.{field}")

market_trend = system_state["marketTrend"]
require(is_allowed(normalized(market_trend.get("status")), GENERIC_EMPTY_STATUSES),
        "empty systemState.marketTrend status is not fail-closed")
market_trend_value = normalized(market_trend.get("value"))
require(is_allowed(market_trend_value, {None, ""})
        or is_allowed(market_trend_value, FORMAL_MARKET_BIASES),
        "empty systemState.marketTrend is not a formal MarketBiasEnum value")
require(is_allowed(market_trend_value, EMPTY_ALLOWED_MARKET_BIASES),
        "empty systemState.marketTrend has a market conclusion")
require(is_allowed(market_trend.get("valueLabel"), EMPTY_ALLOWED_MARKET_BIAS_LABELS),
        "empty systemState.marketTrend valueLabel is not a no-conclusion label")

risk_level = system_state["riskLevel"]
require(is_allowed(normalized(risk_level.get("status")), GENERIC_EMPTY_STATUSES),
        "empty systemState.riskLevel status is not fail-closed")
require(is_allowed(normalized(risk_level.get("value")), EMPTY_ALLOWED_RISK_LEVELS),
        "empty systemState.riskLevel reports an evidenced risk conclusion")
require(is_allowed(risk_level.get("valueLabel"), EMPTY_ALLOWED_NO_CONCLUSION_LABELS),
        "empty systemState.riskLevel valueLabel is not a no-conclusion label")

data_quality = system_state["dataQuality"]
require(is_allowed(normalized(data_quality.get("status")), GENERIC_EMPTY_STATUSES),
        "empty systemState.dataQuality status is not fail-closed")
require(data_quality.get("value") is None or is_zero(data_quality.get("value")),
        "empty systemState.dataQuality reports valid quality data")

ai_conflict = system_state["aiConflict"]
require(is_allowed(normalized(ai_conflict.get("status")), EMPTY_ALLOWED_AI_STATUSES),
        "empty systemState.aiConflict is not inapplicable/not-called")
require(is_allowed(normalized(ai_conflict.get("value")), {None, ""}),
        "empty systemState.aiConflict fabricated a conflict result")

pending_review = system_state["pendingReview"]
require(is_zero(pending_review.get("value")),
        "empty systemState.pendingReview is not zero")
require(is_zero(pending_review.get("score")),
        "empty systemState.pendingReview score is not zero")
require(is_allowed(normalized(pending_review.get("status")), EMPTY_ALLOWED_COUNTER_STATUSES),
        "empty systemState.pendingReview status is unsupported")

confused = system_state["confused"]
require(is_zero(confused.get("value")),
        "empty systemState.confused is not zero")
require(is_zero(confused.get("score")),
        "empty systemState.confused score is not zero")
require(is_allowed(normalized(confused.get("status")), EMPTY_ALLOWED_COUNTER_STATUSES),
        "empty systemState.confused status is unsupported")

hot_reset = system_state["hotReset"]
require(hot_reset.get("value") is False,
        "empty systemState.hotReset is triggered")
require(is_allowed(normalized(hot_reset.get("status")), EMPTY_ALLOWED_COUNTER_STATUSES),
        "empty systemState.hotReset status is unsupported")
hot_reset_meta = hot_reset.get("meta", MISSING)
require(hot_reset_meta is MISSING or hot_reset_meta is None
        or isinstance(hot_reset_meta, dict),
        "empty systemState.hotReset meta is not an object/null")
if hot_reset_meta is MISSING or hot_reset_meta is None:
    hot_reset_meta = {}
require(all(value is None or value == "" for value in hot_reset_meta.values()),
        "empty systemState.hotReset contains trigger evidence")

require(dashboard.get("positions") == [], "empty dashboard positions are not empty")

suggestion = dashboard.get("executionSuggestion") or {}
require(suggestion.get("status") == "NO_COMPLETE_PLAN",
        "empty dashboard did not fail closed to NO_COMPLETE_PLAN")
for field in (
    "sourceAnalysisId",
    "sourceExecutionPlanId",
    "sourceTraceId",
    "direction",
    "entryZone",
    "stopLoss",
    "takeProfitRules",
    "leverageSuggestion",
    "positionSuggestion",
    "validPeriod",
    "validFrom",
    "expiresAt",
    "invalidCondition",
):
    require(suggestion.get(field) is None, f"empty dashboard fabricated {field}")

safety = dashboard.get("safety") or {}
for field in (
    "reviewOnly",
    "manualReviewOnly",
    "notTradeInstruction",
    "notExecutable",
    "notAutoTrading",
    "notOrderExecution",
    "notPushSend",
    "notExternalChannel",
    "notUserPositionCreation",
    "notUserPositionMutation",
):
    require(safety.get(field) is True, f"dashboard safety.{field} is not true")

ai = dashboard.get("aiDecision") or {}
require(is_allowed(ai.get("runStatus"), {"DISABLED", "NOT_CALLED"}),
        "AI state is not disabled/not-called")

diagnostics = dashboard.get("diagnostics") or {}
provider_values = [
    diagnostics.get("marketDataProvider"),
    diagnostics.get("aiProvider"),
    diagnostics.get("externalContextProvider"),
]
for provider in (diagnostics.get("providerReadiness") or {}).get("providers") or []:
    require(isinstance(provider, dict), "provider readiness item is not an object")
    require(provider.get("connected") is not True,
            "a provider readiness item claims a live connection")
    provider_values.append(provider.get("status"))
require(all(is_allowed(normalized(value), EMPTY_ALLOWED_PROVIDER_STATUSES)
            for value in provider_values),
        "a provider reports an unsupported/connected empty-state status")
require(is_allowed(normalized((dashboard.get("pushInbox") or {}).get("telegramStatus")),
        EMPTY_ALLOWED_PROVIDER_STATUSES),
        "Telegram reports an unsupported/connected empty-state status")

for field in ("positionReviews", "opportunityReviews", "pushReviews", "ruleFeedback"):
    require(review.get(field) == [], f"empty review center {field} is not empty")

summary = review.get("summary") or {}
for field in (
    "positionReviewCount",
    "opportunityReviewCount",
    "pushReviewCount",
    "ruleFeedbackCount",
):
    require(summary.get(field) == 0, f"empty review center {field} is not zero")

alerts = baseline.get("alertSummary") or {}
for field in (
    "openCountWindow",
    "suppressedCountWindow",
    "dataQualityOpenCountWindow",
    "dataQualitySuppressedCountWindow",
):
    require(alerts.get(field) == 0, f"run baseline {field} is not zero")

data_quality = baseline.get("dataQualitySummary") or {}
for field in ("analysisRunCountWindow", "lowQualityCountWindow"):
    require(data_quality.get(field) == 0, f"run baseline {field} is not zero")

recheck = baseline.get("recheckSummary") or {}
require(recheck.get("availabilityStatus") == "PRIVATE_SOURCE_UNAVAILABLE",
        "run baseline recheck private source is not fail-closed")
require(recheck.get("totalCountWindow") is None,
        "run baseline exposed a global private recheck total")
require(recheck.get("statusCountsWindow") is None,
        "run baseline exposed global private recheck status counts")

hot_reset = baseline.get("hotResetSummary") or {}
require(hot_reset.get("eventCountWindow") == 0,
        "run baseline hot-reset count is not zero")
require(not (hot_reset.get("triggerTypeCountsWindow") or {}),
        "run baseline hot-reset trigger counts are not empty")

print("EMPTY_DASHBOARD_FAIL_CLOSED: PASS")
print("EMPTY_ASSET_CARDS_FAIL_CLOSED: PASS")
print("EMPTY_SYSTEM_STATE_FAIL_CLOSED: PASS")
print("EMPTY_REVIEW_CENTER_FAIL_CLOSED: PASS")
print("EMPTY_RUN_BASELINE_FAIL_CLOSED: PASS")
print("FAKE_ASSET_CONCLUSIONS: NONE")
print("FAKE_POSITION_PLAN_RECORDS: NONE")
print("ASSET_ENUM_CONTRACT: PASS_EXACT_FORMAL_VALUES")
print("MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY")
print("ASSET_JSON_SHAPE: PASS_STRICT")
