#!/usr/bin/env python3
import json
import sys


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


def normalized(value):
    return value.strip().upper() if isinstance(value, str) else value


def is_zero(value):
    return not isinstance(value, bool) and isinstance(value, (int, float)) and value == 0


def require_no_directional_or_trade_text(value, field):
    if not isinstance(value, str):
        return
    upper_value = value.upper()
    forbidden = (
        "STRONG_BULLISH", "WEAK_BULLISH", "BULLISH", "STRONG_BEARISH",
        "WEAK_BEARISH", "BEARISH", "LONG", "SHORT", "PLAN_READY",
        "POSITION_MONITORING", "偏多", "偏空", "做多", "做空", "开仓",
        "可交易", "交易结论", "执行计划",
    )
    require(not any(token in upper_value for token in forbidden),
            f"empty dashboard {field} contains a directional/trading conclusion")


safe_empty_statuses = {
    None, "", "WAITING_SYNC", "DATA_INSUFFICIENT", "NOT_APPLICABLE",
    "FAIL_CLOSED", "UNKNOWN",
}
safe_empty_market_biases = safe_empty_statuses | {"WAIT", "RANGE", "NEUTRAL"}
safe_empty_asset_states = safe_empty_statuses | {"ANALYZING", "OBSERVING"}
asset_field_allowlist = {
    "slot", "slotType", "symbol", "rawSymbol", "marketBias", "marketBiasLabel",
    "compositeScore", "confidenceLevel", "confidenceLabel", "riskLevel", "riskLabel",
    "assetState", "assetStateLabel", "worthOpening", "latestPrice", "dataFreshness",
    "timeframeFreshness", "sourceProvider", "unavailableReason", "evidenceCount",
    "latestAnalysisTime", "currentConclusion",
}
safe_no_data_markers = {
    "NO_DATA", "UNAVAILABLE", "WAITING_SYNC", "DATA_INSUFFICIENT",
    "NOT_APPLICABLE", "FAIL_CLOSED", "UNKNOWN",
}
safe_empty_source_providers = safe_empty_statuses | {"NO_DATA", "UNAVAILABLE", "DISABLED"}

assets = dashboard.get("assets") or []
require(isinstance(assets, list), "dashboard assets is not a list")
for index, asset in enumerate(assets):
    require(isinstance(asset, dict), f"empty asset card {index} is not an object")
    unexpected_fields = sorted(set(asset) - asset_field_allowlist)
    require(not unexpected_fields,
            f"empty asset card {index} has unexpected fields: {', '.join(unexpected_fields)}")
    require(normalized(asset.get("slotType")) in {None, "DEFAULT_SLOT", "PLACEHOLDER", "EMPTY"},
            f"empty asset card {index} is not a placeholder")
    require(normalized(asset.get("assetState")) in safe_empty_asset_states,
            f"empty asset card {index} has unsafe assetState")
    require(normalized(asset.get("marketBias")) in safe_empty_market_biases,
            f"empty asset card {index} has directional marketBias")
    require(asset.get("worthOpening") is not True,
            f"empty asset card {index} has worthOpening=true")
    require(asset.get("latestPrice") is None,
            f"empty asset card {index} fabricated latestPrice")
    require(asset.get("compositeScore") is None or is_zero(asset.get("compositeScore")),
            f"empty asset card {index} fabricated compositeScore")
    require(normalized(asset.get("confidenceLevel")) in {
        None, "", "LOW", "NONE", "UNKNOWN", "NOT_APPLICABLE",
        "DATA_INSUFFICIENT", "WAITING_SYNC",
    }, f"empty asset card {index} has unsupported confidenceLevel")
    require(asset.get("evidenceCount") is None or is_zero(asset.get("evidenceCount")),
            f"empty asset card {index} has non-zero evidenceCount")
    require(asset.get("latestAnalysisTime") in {None, ""},
            f"empty asset card {index} fabricated latestAnalysisTime")
    require(normalized(asset.get("sourceProvider")) in safe_empty_source_providers,
            f"empty asset card {index} reports a connected sourceProvider")

    unavailable_reason = asset.get("unavailableReason")
    data_freshness = normalized(asset.get("dataFreshness"))
    has_unavailable_reason = (isinstance(unavailable_reason, str)
                              and bool(unavailable_reason.strip()))
    require(has_unavailable_reason or data_freshness in safe_no_data_markers,
            f"empty asset card {index} does not explain missing data")
    timeframe_freshness = asset.get("timeframeFreshness") or {}
    require(isinstance(timeframe_freshness, dict),
            f"empty asset card {index} timeframeFreshness is not an object")
    require(all(normalized(value) in safe_no_data_markers
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

system_state = dashboard.get("systemState") or {}
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
    for field in ("value", "valueLabel", "helper"):
        require_no_directional_or_trade_text(card.get(field), f"systemState.{name}.{field}")

market_trend = system_state["marketTrend"]
require(normalized(market_trend.get("status")) in safe_empty_statuses,
        "empty systemState.marketTrend status is not fail-closed")
require(normalized(market_trend.get("value")) in safe_empty_market_biases,
        "empty systemState.marketTrend is directional")

risk_level = system_state["riskLevel"]
require(normalized(risk_level.get("status")) in safe_empty_statuses,
        "empty systemState.riskLevel status is not fail-closed")
require(normalized(risk_level.get("value")) in safe_empty_statuses,
        "empty systemState.riskLevel reports an evidenced risk conclusion")

data_quality = system_state["dataQuality"]
require(normalized(data_quality.get("status")) in safe_empty_statuses,
        "empty systemState.dataQuality status is not fail-closed")
require(data_quality.get("value") is None or is_zero(data_quality.get("value")),
        "empty systemState.dataQuality reports valid quality data")
require(data_quality.get("score") is None or is_zero(data_quality.get("score")),
        "empty systemState.dataQuality fabricated a score")

ai_conflict = system_state["aiConflict"]
require(normalized(ai_conflict.get("status")) in safe_empty_statuses | {"DISABLED", "NOT_CALLED"},
        "empty systemState.aiConflict is not inapplicable/not-called")
require(normalized(ai_conflict.get("value")) in safe_empty_statuses,
        "empty systemState.aiConflict fabricated a conflict result")
require(ai_conflict.get("score") is None or is_zero(ai_conflict.get("score")),
        "empty systemState.aiConflict fabricated a score")

pending_review = system_state["pendingReview"]
require(is_zero(pending_review.get("value")),
        "empty systemState.pendingReview is not zero")
require(normalized(pending_review.get("status")) in safe_empty_statuses | {"CONNECTED"},
        "empty systemState.pendingReview status is unsupported")

confused = system_state["confused"]
require(confused.get("value") is None or is_zero(confused.get("value")),
        "empty systemState.confused is not zero/unknown")
require(normalized(confused.get("status")) in safe_empty_statuses | {"CONNECTED"},
        "empty systemState.confused status is unsupported")

hot_reset = system_state["hotReset"]
require(hot_reset.get("value") is None or hot_reset.get("value") is False,
        "empty systemState.hotReset is triggered")
require(normalized(hot_reset.get("status")) in safe_empty_statuses | {"CONNECTED"},
        "empty systemState.hotReset status is unsupported")
hot_reset_meta = hot_reset.get("meta") or {}
require(isinstance(hot_reset_meta, dict), "empty systemState.hotReset meta is not an object")
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
require(ai.get("runStatus") in {"DISABLED", "NOT_CALLED"},
        "AI state is not disabled/not-called")

diagnostics = dashboard.get("diagnostics") or {}
provider_values = [
    diagnostics.get("marketDataProvider"),
    diagnostics.get("aiProvider"),
    diagnostics.get("externalContextProvider"),
]
for provider in (diagnostics.get("providerReadiness") or {}).get("providers") or []:
    provider_values.append((provider or {}).get("status"))
require("CONNECTED" not in provider_values, "a provider is reported CONNECTED")
require((dashboard.get("pushInbox") or {}).get("telegramStatus") != "CONNECTED",
        "Telegram is reported CONNECTED")

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
require(recheck.get("totalCountWindow") == 0, "run baseline recheck total is not zero")
require(all(value == 0 for value in (recheck.get("statusCountsWindow") or {}).values()),
        "run baseline recheck status count is not zero")

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
