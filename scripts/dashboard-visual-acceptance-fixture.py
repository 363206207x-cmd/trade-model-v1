#!/usr/bin/env python3
"""Serve the real Dashboard template with deterministic, offline visual fixtures."""

from __future__ import annotations

import argparse
import copy
import json
import threading
import time
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "src/main/resources/templates/dashboard.html"
MOBILE_TEMPLATE = ROOT / "src/main/resources/templates/dashboard-mobile.html"
FRONTEND_CONTRACT = ROOT / "src/main/resources/static/js/frontend-contract.js"
MOBILE_SCRIPT = ROOT / "src/main/resources/static/js/dashboard-mobile.js"
MOBILE_STYLE = ROOT / "src/main/resources/static/css/dashboard-mobile.css"
ALERT_EXPLAIN = ROOT / "src/main/resources/static/js/alert-explain.js"
SCENARIOS = {
    "normal",
    "partial",
    "empty",
    "missing",
    "retry",
    "asset-switch-failure",
    "exact-plan",
    "top3-independent",
    "low-quality",
    "data-quality-60",
    "data-quality-69",
    "data-quality-70",
    "ai-disabled-blocked",
    "ai-all-abstain",
    "ai-timeout",
    "plan-expired",
    "trace-mismatch",
    "plan-blocked-position",
    "plan-needs-revalidation",
    "plan-partial",
    "plan-missing",
    "position-monitored",
    "position-waiting",
    "position-high-stable",
    "position-risk-escalated",
    "position-stale",
    "multi-position",
    "placeholder",
    "home-failure",
    "detail-late",
    "long-content",
}
REQUEST_LOG: list[dict[str, object]] = []
REQUEST_LOCK = threading.Lock()
SCENARIO_LOCK = threading.Lock()
ACTIVE_SCENARIO = "normal"
HOME_REQUEST_COUNTS: dict[str, int] = {}


def status_card(value: object, helper: str, status: str = "OK") -> dict[str, object]:
    return {"valueLabel": value, "helper": helper, "status": status}


def asset(
    slot: int,
    symbol: str,
    state: str,
    state_label: str,
    bias: str,
    bias_label: str,
    confidence: str,
    confidence_label: str,
    risk: str,
    risk_label: str,
    worth_opening: bool,
    conclusion: str,
    price: str,
) -> dict[str, object]:
    return {
        "slot": slot,
        "slotType": "ANALYZED",
        "symbol": symbol.replace("USDT", "/USDT"),
        "rawSymbol": symbol,
        "analysisId": f"analysis-{symbol.removesuffix('USDT').lower()}-asset",
        "assetState": state,
        "assetStateLabel": state_label,
        "marketBias": bias,
        "marketBiasLabel": bias_label,
        "confidenceLevel": confidence,
        "confidenceLabel": confidence_label,
        "riskLevel": risk,
        "riskLabel": risk_label,
        "worthOpening": worth_opening,
        "latestPrice": price,
        "compositeScore": 94 - slot * 3,
        "currentConclusion": conclusion,
        "sourceProvider": "离线视觉验收 fixture",
        "dataFreshness": "FRESH",
        "dataQuality": "GOOD",
        "multiTimeframeState": "CONFLICTED" if state == "CONFUSED" else "ALIGNED",
        "confused": state == "CONFUSED",
        "updatedAt": "2026-07-13T12:00:00Z",
        "moduleState": "READY",
        "fieldSourceStatus": {
            "symbol": "REAL",
            "latestPrice": "REAL",
            "direction": "DERIVED",
            "score": "DERIVED",
            "confidence": "DERIVED",
            "riskLevel": "DERIVED",
            "assetState": "REAL",
            "dataQuality": "DERIVED",
            "multiTimeframeState": "DERIVED",
            "confused": "DERIVED",
            "updatedAt": "REAL",
        },
        "evidenceCount": 4,
        "latestAnalysisTime": "2026-07-13T12:00:00Z",
    }


def assets_fixture() -> list[dict[str, object]]:
    return [
        asset(1, "BTCUSDT", "OBSERVING", "观察", "BULLISH", "偏多", "HIGH", "高", "LOW", "低", False,
              "结构仍在观察，等待规则条件进一步确认", "64218.40"),
        asset(2, "ETHUSDT", "CANDIDATE", "候选", "WEAK_BULLISH", "弱偏多", "MEDIUM", "中", "MEDIUM", "中", True,
              "已进入候选，仍需人工复核关键边界", "3521.18"),
        asset(3, "SOLUSDT", "HIGH_RISK", "高风险观察", "WAIT", "观望", "LOW", "低", "HIGH", "高", False,
              "波动与流动性风险偏高，暂不形成执行建议", "148.72"),
        asset(4, "BNBUSDT", "CONFUSED", "冲突状态", "RANGE", "震荡", "LOW", "低", "HIGH", "高", False,
              "多周期证据冲突，等待结构重新收敛", "592.36"),
        asset(5, "XRPUSDT", "COOLING", "冷却", "WEAK_BEARISH", "弱偏空", "MEDIUM", "中", "MEDIUM", "中", False,
              "冷却窗口内仅保留观察，不形成新计划", "0.5218"),
        asset(6, "DOGEUSDT", "INVALIDATED", "已失效", "BEARISH", "偏空", "LOW", "低", "HIGH", "高", False,
              "原结构已经失效，等待重新分析", "0.1284"),
    ]


def ai_role(role: str, label: str) -> dict[str, object]:
    if role == "GPT_FINAL":
        return {
            "role": role,
            "roleLabel": label,
            "runStatus": "SUCCESS",
            "runStatusLabel": "复核成功",
            "resultAvailable": True,
            "stance": "SUPPORT",
            "direction": "BULLISH",
            "finalMarketBias": "BULLISH",
            "finalConfidence": "MEDIUM",
            "finalRiskLevel": "MEDIUM",
            "finalPlanMode": "REVIEW_ONLY",
            "worthOpening": "等待人工复核",
            "finalConclusion": "规则方向可继续观察，但边界确认前不形成执行动作",
            "decisionSummary": "规则方向与主要证据基本一致，保留人工复核",
            "coreSupportingEvidence": ["多周期结构未破坏", "风险仍在可复核范围"],
            "coreCounterEvidence": ["短周期波动仍偏高"],
            "downgradeReason": "暂无降级原因",
        }
    if role == "GEMINI_REVIEW":
        return {
            "role": role,
            "roleLabel": label,
            "runStatus": "SUCCESS",
            "runStatusLabel": "复核成功",
            "resultAvailable": True,
            "stance": "SUPPORT",
            "reviewConclusion": "证据链基本一致",
            "reviewVerdict": "保留人工复核",
            "manualReviewRequired": "是",
            "detectedContradictions": ["短周期动量略有分歧"],
            "weakEvidence": ["外部上下文仅作辅助"],
            "logicGaps": [],
            "downgradeRecommendation": "暂不降级",
            "riskAdjustmentSuggestion": "继续观察风险变化",
        }
    return {
        "role": role,
        "roleLabel": label,
        "runStatus": "SUCCESS",
        "runStatusLabel": "复核成功",
        "resultAvailable": True,
        "stance": "CHALLENGE",
        "challengeThesis": "短周期流动性变化可能削弱当前结构",
        "challengeConclusion": "尚未形成足以推翻规则方向的反证",
        "eventRisks": ["突发事件可能放大波动"],
        "sentimentReversalRisks": ["情绪快速反转"],
        "microstructureTraps": ["短周期插针"],
        "liquidityRisks": ["深度下降"],
        "counterEvidence": ["成交延续性仍需确认"],
    }


def ai_decision_success() -> dict[str, object]:
    return {
        "schemaVersion": "AI_ROLE_RESULTS_SCHEMA_V1",
        "runStatus": "SUCCESS",
        "runStatusLabel": "正常",
        "decisionMode": "REVIEW_ONLY",
        "decisionModeLabel": "仅供人工复核",
        "activeTab": "GPT_FINAL",
        "tabs": [
            ai_role("GPT_FINAL", "最终裁决官"),
            ai_role("GEMINI_REVIEW", "冲突复核官"),
            ai_role("GROK_CHALLENGE", "反方挑战官"),
        ],
        "consistency": {
            "level": "LEVEL_2_LIGHT_DIVERGENCE",
            "consistencyLevel": "轻微分歧",
            "consistencyScore": 78,
            "consistencySummary": "三角色结论大体一致，保留短周期风险复核",
            "confused": False,
            "aiApplicable": True,
            "directionalPushBlocked": False,
            "downgradeReason": "暂无降级原因",
        },
    }


def ai_decision_all_abstain() -> dict[str, object]:
    tabs = []
    for role, role_label in (
        ("GPT_FINAL", "最终裁决官"),
        ("GEMINI_REVIEW", "冲突复核官"),
        ("GROK_CHALLENGE", "反方挑战官"),
    ):
        tabs.append({
            "role": role,
            "roleLabel": role_label,
            "runStatus": "SUCCESS",
            "runStatusLabel": "复核成功",
            "resultAvailable": True,
            "stance": "ABSTAIN",
            "reviewConclusion": "证据不足，暂不判断",
        })
    return {
        "schemaVersion": "AI_ROLE_RESULTS_SCHEMA_V1",
        "runStatus": "SUCCESS",
        "runStatusLabel": "正常",
        "decisionMode": "NOT_APPLICABLE",
        "decisionModeLabel": "不适用",
        "activeTab": "GPT_FINAL",
        "tabs": tabs,
        "consistency": {
            "level": None,
            "consistencyLevel": "不适用",
            "consistencyScore": None,
            "consistencySummary": "AI 成功返回，但所有角色均因证据不足而弃权",
            "confused": False,
            "aiApplicable": False,
            "directionalPushBlocked": False,
            "downgradeReason": "暂无降级原因",
        },
    }


def unavailable_ai(status: str, label: str, message: str, directional_blocked: bool = False) -> dict[str, object]:
    tabs = []
    for role, role_label in (
        ("GPT_FINAL", "最终裁决官"),
        ("GEMINI_REVIEW", "冲突复核官"),
        ("GROK_CHALLENGE", "反方挑战官"),
    ):
        tabs.append({
            "role": role,
            "roleLabel": role_label,
            "runStatus": status,
            "runStatusLabel": label,
            "resultAvailable": False,
            "statusMessage": message,
        })
    return {
        "runStatus": status,
        "runStatusLabel": label,
        "decisionMode": "NOT_APPLICABLE",
        "decisionModeLabel": "不适用",
        "activeTab": "GPT_FINAL",
        "tabs": tabs,
        "consistency": {
            "level": "不适用",
            "consistencyLevel": "不适用",
            "consistencySummary": "等待 AI 三角色结果同步后生成一致性结论",
            "confused": False,
            "aiApplicable": False,
            "directionalPushBlocked": directional_blocked,
            "downgradeReason": "暂无降级原因",
        },
    }


def base_home(selected_symbol: str) -> dict[str, object]:
    assets = assets_fixture()
    chosen = next((item for item in assets if item["rawSymbol"] == selected_symbol), assets[0])
    return {
        "header": {
            "pageTitle": "首页总览",
            "dataStatus": "READY",
            "aiStatus": "SUCCESS",
            "aiStatusLabel": "正常",
            "dataSourceText": "离线视觉验收数据",
            "updatedAt": "2026-07-13T12:00:00",
        },
        "systemState": {
            "marketTrend": status_card(chosen["marketBiasLabel"], "当前选择资产的规则方向"),
            "riskLevel": status_card(chosen["riskLabel"], "当前风险分层"),
            "dataQuality": status_card("92", "确定性 fixture 数据完整"),
            "aiConflict": status_card("轻微分歧", "三角色结果仅供人工复核"),
            "pendingReview": status_card("2", "待复核数量"),
            "confused": status_card("否", "未进入冲突阻断"),
            "hotReset": status_card("未触发", "当前无需热重置"),
        },
        "alerts": [
            {"level": "中", "message": "短周期波动上升，继续人工观察", "symbol": selected_symbol, "time": "12:00"},
        ],
        "events": [
            {"label": "暂无高影响外部事件", "impactLevel": "低", "timeWindow": "当前窗口"},
        ],
        "assets": assets,
        "positions": [
            monitored_position("BTC/USDT", True, 9001),
            monitored_position("ETH/USDT", True, 9002),
            monitored_position("SOL/USDT", True, 9003),
        ],
        "selectedSymbol": selected_symbol,
        "executionSuggestion": {
            "status": "INCOMPLETE_BOUNDARY",
            "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "边界不足，等待结构确认",
            "positionMode": False,
            "moduleState": "PARTIAL",
        },
        "aiDecision": ai_decision_success(),
        "pushInbox": {
            "telegramStatus": "NOT_CONNECTED",
            "hasOpenPosition": True,
            "mode": "OPPORTUNITY_ONLY",
            "counts": {"executable": 0, "waiting": 2, "invalidated": 0, "positionRisk": 3},
            "items": [],
        },
        "diagnostics": {"fixture": True, "scenario": "normal"},
        "safety": {"reviewOnly": True, "notExecutable": True},
        "states": {
            "overall": "READY",
            "assets": "READY",
            "executionPlan": "READY",
            "positions": "READY",
            "ai": "READY",
            "consistency": "READY",
        },
    }


def monitored_position(
    symbol: str,
    with_monitor: bool,
    position_id: int = 9001,
    risk_level: str = "MEDIUM",
    risk_trend: str = "STABLE",
) -> dict[str, object]:
    position = {
        "positionId": position_id,
        "symbol": symbol,
        "direction": "LONG",
        "entryPrice": "63000.00",
        "leverage": "2",
        "positionSize": "1",
        "positionStatus": "OPEN",
        "positionStatusLabel": "持仓中",
        "userStopLoss": "61200.00",
        "userTakeProfit": "67500.00",
        "systemSuggestedStopLoss": None,
        "systemSuggestedTakeProfit": None,
        "openedAt": "2026-07-13T09:10:00",
        "updatedAt": "2026-07-13T11:55:00",
        "warningState": "NONE" if with_monitor else "MISSING",
        "moduleState": "READY" if with_monitor else "PARTIAL",
    }
    if with_monitor:
        risk_escalated = risk_trend in {"INCREASED", "SHARPLY_INCREASED"}
        data_state = "RISK_ESCALATED" if risk_escalated else "OPEN_MONITORING"
        position.update({
            "markPrice": "64218.40",
            "markPriceFresh": True,
            "pnlAmount": "1218.40",
            "pnlPercent": "1.93",
            "monitorConclusion": "HIGH_RISK_OBSERVATION" if risk_escalated else "LOGIC_VALID",
            "entryLogicStatus": "STILL_VALID",
            "reversalStatus": "NO_REVERSAL",
            "riskLevel": risk_level,
            "riskTrend": risk_trend,
            "riskReason": "OPPOSING_EVIDENCE_INCREASED" if risk_escalated else "NO_CLEAR_RISK_FACTOR",
            "suggestedAction": "TIGHTEN_STOP" if risk_escalated else "CONTINUE_HOLD",
            "lastMonitorTime": "2026-07-13T11:58:00",
            "dataState": data_state,
        })
    else:
        position.update({
            "markPrice": None,
            "markPriceFresh": False,
            "pnlAmount": None,
            "pnlPercent": None,
            "monitorConclusion": None,
            "entryLogicStatus": None,
            "reversalStatus": None,
            "riskLevel": None,
            "riskTrend": None,
            "riskReason": None,
            "suggestedAction": None,
            "lastMonitorTime": None,
            "dataState": "WAITING_MONITOR_DATA",
        })
    return position


def asset_execution_suggestion(symbol: str) -> dict[str, object]:
    marker = symbol.removesuffix("USDT") or symbol
    levels = {
        "BTC": ("63600 - 64200", "61200", "目标一 66000；目标二 67500", "1 : 2.3", "4H 收盘跌破 61200"),
        "ETH": ("3460 - 3525", "3340", "目标一 3650；目标二 3780", "1 : 2.1", "4H 收盘跌破 3340"),
        "SOL": ("145.0 - 149.0", "138.0", "目标一 158.0；目标二 166.0", "1 : 2.0", "4H 收盘跌破 138.0"),
        "BNB": ("584 - 594", "566", "目标一 618；目标二 636", "1 : 2.2", "4H 收盘跌破 566"),
        "XRP": ("0.510 - 0.525", "0.488", "目标一 0.552；目标二 0.578", "1 : 2.0", "4H 收盘跌破 0.488"),
        "DOGE": ("0.124 - 0.130", "0.117", "目标一 0.139；目标二 0.147", "1 : 2.1", "4H 收盘跌破 0.117"),
    }
    entry_zone, stop_loss, take_profit, risk_reward, invalid_condition = levels.get(
        marker,
        ("--", "--", "--", "--", "当前计划边界不可验证"),
    )
    return {
        "status": "USABLE_REVIEW_PLAN",
        "statusLabel": "资产执行计划，仅供人工复核",
        "moduleState": "READY",
        "positionMode": False,
        "positionMonitor": None,
        "sourceAnalysisId": f"analysis-{marker.lower()}-asset",
        "sourceExecutionPlanId": f"plan-{marker.lower()}-asset",
        "sourceTraceId": f"trace-{marker.lower()}-asset",
        "direction": "BULLISH",
        "entryZone": entry_zone,
        "stopLoss": stop_loss,
        "takeProfitRules": take_profit,
        "riskRewardRatio": risk_reward,
        "leverageSuggestion": "不高于 2 倍",
        "positionSuggestion": "仅供人工复核",
        "validFrom": "2026-07-13T12:00:00Z",
        "expiresAt": "2026-07-14T12:00:00Z",
        "validPeriod": "2026-07-13 12:00 至 2026-07-14 12:00",
        "invalidCondition": invalid_condition,
    }


def unavailable_asset_execution_suggestion(
    symbol: str,
    status: str,
    status_label: str,
    blocked_reason: str,
) -> dict[str, object]:
    marker = symbol.removesuffix("USDT") or symbol
    return {
        "status": status,
        "statusLabel": status_label,
        "moduleState": "MISSING" if "MISSING" in status else "PARTIAL",
        "blockedReason": blocked_reason,
        "positionMode": False,
        "positionMonitor": None,
        "sourceAnalysisId": f"analysis-{marker.lower()}-asset",
    }


def apply_data_quality_boundary(
    home: dict[str, object],
    selected_asset: dict[str, object],
    score: int,
) -> None:
    passes_minimum_gate = score >= 70
    home["systemState"]["dataQuality"] = status_card(
        str(score),
        "已通过最低数据质量门槛；仍需其他正式门禁"
        if passes_minimum_gate
        else "低于 70，数据质量断路器已触发",
        "OK" if passes_minimum_gate else "BLOCKED",
    )
    home["diagnostics"]["dataQualityScore"] = score
    home["diagnostics"]["dataQualityMinimumGatePassed"] = passes_minimum_gate
    if passes_minimum_gate:
        selected_asset["dataQuality"] = "GOOD"
        selected_asset["fieldSourceStatus"]["dataQuality"] = "DERIVED"
        home["diagnostics"]["allOtherPlanGatesValid"] = True
        return

    selected_asset.update({
        "assetState": "HIGH_RISK",
        "assetStateLabel": "高风险观察",
        "marketBias": "WAIT",
        "marketBiasLabel": "观望",
        "confidenceLevel": "LOW",
        "confidenceLabel": "低",
        "riskLevel": "HIGH",
        "riskLabel": "高",
        "worthOpening": False,
        "currentConclusion": "数据质量不足，暂不交易 / 事件观望",
        "dataQuality": "PARTIAL",
        "moduleState": "PARTIAL",
    })
    selected_asset["fieldSourceStatus"]["dataQuality"] = "DERIVED"
    home["executionSuggestion"] = {
        "status": "DATA_QUALITY_BLOCKED",
        "statusLabel": "当前暂无完整执行计划",
        "blockedReason": "数据质量不足，暂不交易 / 事件观望",
        "positionMode": False,
        "positionMonitor": None,
        "moduleState": "PARTIAL",
    }
    home["aiDecision"] = unavailable_ai(
        "NOT_CALLED",
        "未调用",
        "数据质量不足，暂不交易 / 事件观望",
    )


def apply_module_states(home: dict[str, object]) -> None:
    assets = home.get("assets") or []
    selected_symbol = home.get("selectedSymbol")
    selected_asset = next(
        (item for item in assets if item.get("rawSymbol") == selected_symbol),
        None,
    )
    asset_state_override = home.pop("_assetStateOverride", None)
    asset_state = str(asset_state_override) if asset_state_override else (
        selected_asset.get("moduleState", "MISSING") if selected_asset else "MISSING"
    )
    positions = home.get("positions") or []
    if not positions:
        position_state = "EMPTY"
    elif any(item.get("moduleState") == "ERROR" for item in positions):
        position_state = "ERROR"
    elif any(item.get("moduleState") != "READY" for item in positions):
        position_state = "PARTIAL"
    else:
        position_state = "READY"
    execution = home.get("executionSuggestion") or {}
    execution_state = execution.get("moduleState")
    if not execution_state:
        status = str(execution.get("status") or "").upper()
        if status == "USABLE_REVIEW_PLAN":
            execution_state = "READY"
        elif status in {"PLAN_MISSING", "PLAN_IDENTITY_MISSING"}:
            execution_state = "MISSING"
        elif status in {"PLAN_INVALID", "PLAN_BLOCKED", "PLAN_IDENTITY_ERROR"}:
            execution_state = "ERROR"
        else:
            execution_state = "PARTIAL"
    ai_status = str((home.get("aiDecision") or {}).get("runStatus") or "").upper()
    if ai_status == "SUCCESS":
        ai_state = "READY"
    elif ai_status in {"PARTIAL_SUCCESS", "STARTED"}:
        ai_state = "PARTIAL"
    elif ai_status in {"DISABLED", "NOT_CONFIGURED", "NOT_CALLED", ""}:
        ai_state = "MISSING"
    else:
        ai_state = "ERROR"
    consistency_state = "READY" if ai_state == "READY" else (
        "ERROR" if ai_state == "ERROR" else "PARTIAL"
    )
    if asset_state in {"ERROR", "MISSING", "EMPTY"}:
        overall = asset_state
    elif any(state in {"ERROR", "PARTIAL", "MISSING"} for state in (
        execution_state, position_state, ai_state, consistency_state
    )):
        overall = "PARTIAL"
    else:
        overall = "READY"
    home["states"] = {
        "overall": overall,
        "assets": asset_state,
        "executionPlan": execution_state,
        "positions": position_state,
        "ai": ai_state,
        "consistency": consistency_state,
    }
    home["header"]["dataStatus"] = overall


def scenario_home(
    scenario: str,
    selected_symbol: str,
    selected_position_id: int | None = None,
) -> dict[str, object]:
    home = base_home(selected_symbol)
    home["diagnostics"]["scenario"] = scenario
    selected_asset = next((item for item in home["assets"] if item["rawSymbol"] == selected_symbol), home["assets"][0])
    home["executionSuggestion"] = asset_execution_suggestion(selected_symbol)

    if scenario == "partial":
        selected_asset.update({
            "compositeScore": 61,
            "confidenceLevel": "LOW",
            "confidenceLabel": "低",
            "dataQuality": "PARTIAL",
            "multiTimeframeState": None,
            "moduleState": "PARTIAL",
        })
        selected_asset["fieldSourceStatus"].update({
            "score": "FALLBACK",
            "confidence": "FALLBACK",
            "multiTimeframeState": "MISSING",
        })
        home["executionSuggestion"] = unavailable_asset_execution_suggestion(
            selected_symbol,
            "PLAN_INCOMPLETE",
            "当前暂无完整执行计划",
            "执行计划字段不完整",
        )

    elif scenario == "empty":
        home["assets"] = []
        home["positions"] = []
        home["_assetStateOverride"] = "EMPTY"
        home["executionSuggestion"] = {
            "status": "NO_COMPLETE_PLAN",
            "statusLabel": "暂无执行计划",
            "blockedReason": "当前合法集合无结果",
            "positionMode": False,
            "moduleState": "EMPTY",
        }
        home["aiDecision"] = unavailable_ai("NOT_CALLED", "未调用", "当前无可复核资产")

    elif scenario == "missing":
        home["assets"] = []
        home["executionSuggestion"] = {
            "status": "PLAN_IDENTITY_MISSING",
            "statusLabel": "当前不可查看",
            "blockedReason": "当前资产或精确计划身份缺失",
            "positionMode": False,
            "moduleState": "MISSING",
        }
        home["aiDecision"] = unavailable_ai("NOT_CALLED", "未调用", "当前资产身份缺失")

    elif scenario == "exact-plan":
        marker = selected_symbol.removesuffix("USDT").lower() or selected_symbol.lower()
        home["executionSuggestion"]["sourceExecutionPlanId"] = f"plan-{marker}-exact"
        home["diagnostics"]["decoyLatestExecutionPlanId"] = f"plan-{marker}-latest"

    elif scenario in {"low-quality", "data-quality-60", "data-quality-69", "data-quality-70"}:
        score = {
            "low-quality": 69,
            "data-quality-60": 60,
            "data-quality-69": 69,
            "data-quality-70": 70,
        }[scenario]
        apply_data_quality_boundary(home, selected_asset, score)

    elif scenario == "ai-disabled-blocked":
        home["header"].update({"aiStatus": "DISABLED", "aiStatusLabel": "已禁用"})
        home["aiDecision"] = unavailable_ai("DISABLED", "已禁用", "AI 复核未启用", True)
        home["systemState"]["aiConflict"] = status_card("不适用", "AI 复核未启用", "PARTIAL")
        home["systemState"]["confused"] = status_card("否", "AI 一致性不适用")

    elif scenario == "ai-all-abstain":
        home["aiDecision"] = ai_decision_all_abstain()
        home["systemState"]["aiConflict"] = status_card(
            "不适用", "本轮未形成可裁决 AI 意见", "NOT_APPLICABLE")
        home["systemState"]["confused"] = status_card("否", "AI 一致性不适用")

    elif scenario == "ai-timeout":
        home["header"].update({"aiStatus": "TIMEOUT", "aiStatusLabel": "调用超时"})
        home["aiDecision"] = unavailable_ai("TIMEOUT", "调用超时", "AI 复核超时，本轮未采纳该角色")
        home["systemState"]["aiConflict"] = status_card("不适用", "本轮 AI 结果不可用", "PARTIAL")

    elif scenario == "plan-expired":
        home["executionSuggestion"] = {
            "status": "EXPIRED", "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "计划已失效，等待重新分析", "positionMode": False,
            "validFrom": "2026-07-12T00:00:00Z", "expiresAt": "2026-07-12T12:00:00Z",
        }

    elif scenario == "trace-mismatch":
        home["executionSuggestion"] = {
            "status": "STATE_SNAPSHOT_MISMATCH", "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "状态已更新，原计划需重新分析", "positionMode": False,
        }

    elif scenario == "plan-blocked-position":
        home["executionSuggestion"] = unavailable_asset_execution_suggestion(
            selected_symbol,
            "PLAN_BLOCKED",
            "当前执行计划已阻断",
            "执行计划未通过来源或风险门控",
        )
        position = monitored_position(selected_symbol, True)
        home["positions"] = [position]
        home["pushInbox"]["hasOpenPosition"] = True
        home["pushInbox"]["counts"]["positionRisk"] = 1
        home["selectedPositionId"] = None
        home["positionSelectionStatus"] = "POSITION_SELECTION_REQUIRED"
        home["matchingPositionCount"] = 1

    elif scenario == "plan-needs-revalidation":
        home["executionSuggestion"] = unavailable_asset_execution_suggestion(
            selected_symbol,
            "REVALIDATION_REQUIRED",
            "执行计划需要重新验证",
            "极端价格波动触发重新验证",
        )

    elif scenario == "plan-partial":
        home["executionSuggestion"] = unavailable_asset_execution_suggestion(
            selected_symbol,
            "PLAN_INCOMPLETE",
            "当前暂无完整执行计划",
            "执行计划状态、来源或边界信息不完整",
        )

    elif scenario == "plan-missing":
        home["executionSuggestion"] = {
            "status": "PLAN_MISSING",
            "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "执行计划不存在或当前不可查看",
            "positionMode": False,
            "positionMonitor": None,
        }

    elif scenario in {
        "position-monitored",
        "position-waiting",
        "position-high-stable",
        "position-risk-escalated",
        "position-stale",
    }:
        if scenario == "position-waiting":
            position = monitored_position(selected_symbol, False)
        elif scenario == "position-risk-escalated":
            position = monitored_position(
                selected_symbol,
                True,
                risk_level="HIGH",
                risk_trend="INCREASED",
            )
        else:
            risk_level = "HIGH" if scenario == "position-high-stable" else "MEDIUM"
            position = monitored_position(selected_symbol, True, risk_level=risk_level)
            if scenario == "position-stale":
                position["markPriceFresh"] = False
        home["positions"] = [position]
        home["pushInbox"]["hasOpenPosition"] = True
        home["pushInbox"]["counts"]["positionRisk"] = 1 if scenario != "position-waiting" else 0
        home["selectedPositionId"] = None
        home["positionSelectionStatus"] = "POSITION_SELECTION_REQUIRED"
        home["matchingPositionCount"] = 1

    elif scenario == "multi-position":
        position_a = monitored_position(
            "BTC/USDT", True, 9101, risk_level="HIGH", risk_trend="STABLE"
        )
        position_b = monitored_position(
            "ETH/USDT", True, 9102, risk_level="MEDIUM", risk_trend="INCREASED"
        )
        position_a.update({"entryPrice": "61000.00", "sourceExecutionPlanId": "plan-POSITION-A"})
        position_b.update({"entryPrice": "62000.00", "sourceExecutionPlanId": "plan-POSITION-B"})
        home["positions"] = [position_a, position_b]
        home["pushInbox"]["hasOpenPosition"] = True
        home["matchingPositionCount"] = 2
        if selected_position_id == 9101:
            home["selectedPositionId"] = 9101
            home["positionSelectionStatus"] = "EXACT_POSITION_SELECTED"
        elif selected_position_id == 9102:
            home["selectedPositionId"] = 9102
            home["positionSelectionStatus"] = "EXACT_POSITION_SELECTED"
        else:
            home["selectedPositionId"] = None
            home["positionSelectionStatus"] = (
                "POSITION_SELECTION_REQUIRED" if selected_position_id is None else "POSITION_NOT_FOUND"
            )

    elif scenario == "placeholder":
        home["assets"] = home["assets"][:-1]

    elif scenario == "long-content":
        selected_asset["currentConclusion"] = (
            "中周期结构仍保持偏多，但短周期流动性、成交延续性与外部事件窗口尚未完全收敛，"
            "当前只保留人工复核，不形成自动执行动作。"
        )
        home["alerts"][0]["message"] = (
            "短周期波动与盘口价差同步上升，需等待成交延续性恢复后再复核原计划边界"
        )
        home["events"][0]["label"] = (
            "未来两小时存在可能影响当前资产风险收益结构的外部事件窗口"
        )
        home["executionSuggestion"]["invalidCondition"] = (
            "4H 收盘跌破关键结构、数据质量降至最低门槛以下，或多周期状态转为冲突阻断"
        )
        home["aiDecision"]["tabs"][0]["finalConclusion"] = (
            "规则方向仍可观察，但在成交延续、流动性与风险边界同时完成复核前，不应把该结论解释为交易授权。"
        )
        home["aiDecision"]["consistency"]["consistencySummary"] = (
            "三角色对规则方向大体一致，同时保留短周期流动性和外部事件窗口两项明确异议。"
        )

    apply_module_states(home)
    return home


def detail_fixture(symbol: str) -> dict[str, object]:
    return {
        "decision": {
            "symbol": symbol,
            "marketBiasHierarchy": "WAIT",
            "assetState": "OBSERVING",
            "confidenceLevel": "LOW",
            "riskLevel": "MEDIUM",
            "isWorthOpening": False,
            "recommendedAction": "等待人工复核",
            "hasOpenPosition": False,
        },
        "marketEnvironmentMini": {
            "marketEnvironmentLabel": "离线 fixture",
            "sourceProviderLabel": "视觉验收数据",
        },
        "evidenceTopItems": [],
        "scoreTopItems": [],
    }


def render_static_fixture(scenario: str, output: Path) -> None:
    """Build a self-contained fixture for browser environments that cannot bind localhost."""
    home_payload = {"code": 200, "msg": "success", "data": scenario_home(scenario, "BTCUSDT")}
    detail_payload = detail_fixture("BTCUSDT")
    fixtures = json.dumps({
        "home": home_payload,
        "detail": detail_payload,
        "localReal": {"mode": "VISUAL_ACCEPTANCE_FIXTURE", "status": "OFFLINE"},
        "summary": {"systemHealth": {"status": "离线视觉验收"}, "decisions": []},
    }, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c")
    interceptor = f"""<script id=\"dashboard-static-fixture\">
(() => {{
  const fixtures = {fixtures};
  window.fetch = function(input, options) {{
    const method = String((options && options.method) || "GET").toUpperCase();
    const url = String(input || "");
    if (method !== "GET") {{
      return Promise.resolve(new Response(JSON.stringify({{success:false,message:"离线视觉验收 fixture 拒绝所有写请求"}}), {{status:405,headers:{{"Content-Type":"application/json"}}}}));
    }}
    let payload = {{code:200,msg:"success",data:{{}},status:"NOT_APPLICABLE",statusLabel:"不适用"}};
    if (url.indexOf("/api/dashboard/home") >= 0) payload = fixtures.home;
    else if (url.indexOf("/api/dashboard/detail") >= 0) payload = fixtures.detail;
    else if (url.indexOf("/api/local-real/status") >= 0) payload = fixtures.localReal;
    else if (url.indexOf("/api/dashboard/summary") >= 0) payload = fixtures.summary;
    return Promise.resolve(new Response(JSON.stringify(payload), {{status:200,headers:{{"Content-Type":"application/json"}}}}));
  }};
}})();
</script>"""
    html = TEMPLATE.read_text(encoding="utf-8").replace("REFRESH_MS = 30000;", "REFRESH_MS = 0;")
    html = html.replace(
        '<script th:src="@{/js/frontend-contract.js}" src="/js/frontend-contract.js"></script>',
        "<script>" + FRONTEND_CONTRACT.read_text(encoding="utf-8") + "</script>",
    )
    html = html.replace(
        '<script src="/js/alert-explain.js"></script>',
        "<script>" + ALERT_EXPLAIN.read_text(encoding="utf-8") + "</script>",
    )
    html = html.replace("</head>", interceptor + "\n</head>", 1)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8")
    print(f"DASHBOARD_STATIC_FIXTURE: {output.resolve()}", flush=True)
    print("DASHBOARD_VISUAL_FIXTURE_EXTERNAL_CALLS: 0", flush=True)
    print("DASHBOARD_VISUAL_FIXTURE_WRITES: REJECTED", flush=True)


class FixtureHandler(BaseHTTPRequestHandler):
    server_version = "DashboardVisualFixture/1.0"

    def log_message(self, _format: str, *_args: object) -> None:
        return

    def _scenario(self) -> str:
        with SCENARIO_LOCK:
            scenario = ACTIVE_SCENARIO
        return scenario if scenario in SCENARIOS else "normal"

    def _record(self, parsed) -> None:
        with REQUEST_LOCK:
            REQUEST_LOG.append({
                "time": datetime.now(timezone.utc).isoformat(),
                "method": self.command,
                "path": parsed.path,
                "query": parse_qs(parsed.query),
                "scenario": self._scenario(),
            })

    def _json(self, payload: object, status: HTTPStatus = HTTPStatus.OK) -> None:
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _javascript(self, path: Path) -> None:
        body = path.read_bytes()
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "application/javascript; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _stylesheet(self, path: Path) -> None:
        body = path.read_bytes()
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "text/css; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _empty(self, status: HTTPStatus) -> None:
        self.send_response(status)
        self.send_header("Content-Length", "0")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()

    def do_GET(self) -> None:
        global ACTIVE_SCENARIO
        parsed = urlparse(self.path)
        self._record(parsed)
        query = parse_qs(parsed.query)

        if parsed.path in {"/", "/dashboard", "/dashboard-mobile"}:
            scenario = query.get("scenario", ["normal"])[0]
            if scenario not in SCENARIOS:
                self._json({"error": "unknown fixture scenario"}, HTTPStatus.BAD_REQUEST)
                return
            with SCENARIO_LOCK:
                ACTIVE_SCENARIO = scenario
                HOME_REQUEST_COUNTS[scenario] = 0
            source_template = MOBILE_TEMPLATE if parsed.path == "/dashboard-mobile" else TEMPLATE
            html_text = source_template.read_text(encoding="utf-8").replace(
                "REFRESH_MS = 30000;", "REFRESH_MS = 0;"
            )
            if parsed.path == "/dashboard-mobile":
                html_text = html_text.replace(
                    "data-mobile-home-root",
                    "data-mobile-home-root data-client-home-bootstrap",
                    1,
                )
            visual_theme = query.get("visualTheme", [""])[0].lower()
            if visual_theme in {"light", "dark"}:
                theme_override = f"""<script id="dashboard-visual-theme">
window.addEventListener("load", function () {{
  var root = document.documentElement;
  if ("{visual_theme}" === "dark") root.setAttribute("data-theme", "dark");
  else root.removeAttribute("data-theme");
}});
</script>"""
                html_text = html_text.replace("</body>", theme_override + "\n</body>", 1)
            html = html_text.encode("utf-8")
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Set-Cookie", f"dashboard_visual_scenario={scenario}; Path=/; SameSite=Strict")
            self.end_headers()
            self.wfile.write(html)
            return

        if parsed.path == "/js/frontend-contract.js":
            self._javascript(FRONTEND_CONTRACT)
            return

        if parsed.path == "/js/dashboard-mobile.js":
            self._javascript(MOBILE_SCRIPT)
            return

        if parsed.path == "/css/dashboard-mobile.css":
            self._stylesheet(MOBILE_STYLE)
            return

        if parsed.path == "/js/alert-explain.js":
            self._javascript(ALERT_EXPLAIN)
            return

        if parsed.path == "/__fixture__/scenario":
            scenario = query.get("name", [""])[0]
            if scenario not in SCENARIOS:
                self._json({"error": "unknown fixture scenario"}, HTTPStatus.BAD_REQUEST)
                return
            with SCENARIO_LOCK:
                ACTIVE_SCENARIO = scenario
            self._json({"status": "SCENARIO_SET", "scenario": scenario})
            return

        if parsed.path == "/__fixture__/requests":
            with REQUEST_LOCK:
                self._json({"requests": copy.deepcopy(REQUEST_LOG)})
            return

        if parsed.path == "/__fixture__/reset":
            with REQUEST_LOCK:
                REQUEST_LOG.clear()
            self._json({"status": "CLEARED"})
            return

        if parsed.path == "/api/dashboard/home":
            scenario = self._scenario()
            if scenario in {"home-failure", "retry"}:
                with SCENARIO_LOCK:
                    home_request_count = HOME_REQUEST_COUNTS.get(scenario, 0)
                    HOME_REQUEST_COUNTS[scenario] = home_request_count + 1
                should_fail = home_request_count > 0 if scenario == "home-failure" else home_request_count == 0
                if should_fail:
                    self._json({"code": 503, "msg": "fixture home failure", "data": None}, HTTPStatus.SERVICE_UNAVAILABLE)
                    return
            symbol = query.get("selectedSymbol", ["BTCUSDT"])[0].upper()
            if scenario == "asset-switch-failure" and symbol != "BTCUSDT":
                self._json({"code": 503, "msg": "fixture asset switch failure", "data": None}, HTTPStatus.SERVICE_UNAVAILABLE)
                return
            home_scenario = "normal" if scenario in {
                "detail-late", "home-failure", "retry", "asset-switch-failure",
                "top3-independent"
            } else scenario
            raw_position_id = query.get("positionId", [None])[0]
            try:
                selected_position_id = int(raw_position_id) if raw_position_id is not None else None
            except ValueError:
                selected_position_id = -1
            self._json({
                "code": 200,
                "msg": "success",
                "data": scenario_home(home_scenario, symbol, selected_position_id),
            })
            return

        if parsed.path == "/api/dashboard/detail":
            if self._scenario() == "detail-late":
                time.sleep(1.5)
            symbol = query.get("symbol", ["BTCUSDT"])[0].upper()
            self._json(detail_fixture(symbol))
            return

        if parsed.path == "/api/local-real/status":
            self._json({"mode": "VISUAL_ACCEPTANCE_FIXTURE", "status": "OFFLINE"})
            return

        if parsed.path == "/api/dashboard/summary":
            self._json({"systemHealth": {"status": "离线视觉验收"}, "decisions": []})
            return

        if parsed.path == "/favicon.ico":
            self._empty(HTTPStatus.NO_CONTENT)
            return

        if parsed.path.startswith("/api/"):
            self._json({
                "code": 200,
                "msg": "success",
                "data": {},
                "status": "NOT_APPLICABLE",
                "statusLabel": "不适用",
            })
            return

        self._empty(HTTPStatus.NOT_FOUND)

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        self._record(parsed)
        self._json(
            {"success": False, "message": "视觉验收 fixture 拒绝所有写请求"},
            HTTPStatus.METHOD_NOT_ALLOWED,
        )


def main() -> None:
    parser = argparse.ArgumentParser(description="Offline Dashboard visual acceptance fixture server")
    parser.add_argument("--port", type=int, default=18081)
    parser.add_argument("--scenario", choices=sorted(SCENARIOS), default="normal")
    parser.add_argument("--static-output", type=Path)
    args = parser.parse_args()
    if args.static_output:
        render_static_fixture(args.scenario, args.static_output)
        return
    server = ThreadingHTTPServer(("127.0.0.1", args.port), FixtureHandler)
    print(f"DASHBOARD_VISUAL_FIXTURE_URL: http://127.0.0.1:{args.port}/dashboard?scenario=normal", flush=True)
    print("DASHBOARD_VISUAL_FIXTURE_EXTERNAL_CALLS: 0", flush=True)
    print("DASHBOARD_VISUAL_FIXTURE_WRITES: REJECTED", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
