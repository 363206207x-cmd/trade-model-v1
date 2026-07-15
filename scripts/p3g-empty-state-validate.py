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
print("EMPTY_REVIEW_CENTER_FAIL_CLOSED: PASS")
print("EMPTY_RUN_BASELINE_FAIL_CLOSED: PASS")
print("FAKE_ASSET_POSITION_PLAN_RECORDS: NONE")
