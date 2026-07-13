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
SCENARIOS = {
    "normal",
    "low-quality",
    "ai-disabled-blocked",
    "ai-timeout",
    "plan-expired",
    "trace-mismatch",
    "position-monitored",
    "position-waiting",
    "placeholder",
    "home-failure",
    "detail-late",
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
        "currentConclusion": conclusion,
        "sourceProvider": "离线视觉验收 fixture",
        "dataFreshness": "FIXTURE_ONLY",
        "evidenceCount": 4,
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
        "positions": [],
        "selectedSymbol": selected_symbol,
        "executionSuggestion": {
            "status": "INCOMPLETE_BOUNDARY",
            "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "边界不足，等待结构确认",
            "positionMode": False,
        },
        "aiDecision": ai_decision_success(),
        "pushInbox": {
            "telegramStatus": "NOT_CONNECTED",
            "hasOpenPosition": False,
            "mode": "OPPORTUNITY_ONLY",
            "counts": {"executable": 0, "waiting": 2, "invalidated": 0, "positionRisk": 0},
            "items": [],
        },
        "diagnostics": {"fixture": True, "scenario": "normal"},
        "safety": {"reviewOnly": True, "notExecutable": True},
    }


def monitored_position(symbol: str, with_monitor: bool) -> dict[str, object]:
    position = {
        "positionId": 9001,
        "symbol": symbol,
        "direction": "LONG",
        "directionLabel": "做多持仓",
        "entryPrice": "63000.00",
        "currentPrice": "64218.40",
        "floatingPnl": "1218.40",
        "pnlPct": "1.93",
        "accountImpactPct": "0.42",
        "leverage": "2",
        "positionSize": "1",
        "positionStatus": "OPEN",
        "positionStatusLabel": "持仓中",
        "userStopLoss": "61200.00",
        "userTakeProfit": "67500.00",
        "systemSuggestedStopLoss": None,
        "systemSuggestedTakeProfit": None,
        "openedAt": "2026-07-13T09:10:00",
    }
    if with_monitor:
        position.update({
            "monitorConclusion": "LOGIC_VALID",
            "entryLogicStatus": "LOGIC_VALID",
            "entryLogicStatusLabel": "入场逻辑仍成立",
            "directionSupportStatus": "SUPPORTED",
            "directionSupportStatusLabel": "当前方向仍获支持",
            "reversalStatus": "NO_REVERSAL_SIGNAL",
            "reversalStatusLabel": "暂无反转信号",
            "riskLevel": "MEDIUM",
            "riskLevelLabel": "中",
            "suggestedManualAction": "HOLD",
            "suggestedManualActionText": "人工继续观察",
            "lastMonitorAt": "2026-07-13T11:58:00",
            "nextMonitorAt": None,
        })
    return position


def scenario_home(scenario: str, selected_symbol: str) -> dict[str, object]:
    home = base_home(selected_symbol)
    home["diagnostics"]["scenario"] = scenario
    selected_asset = next((item for item in home["assets"] if item["rawSymbol"] == selected_symbol), home["assets"][0])

    if scenario == "low-quality":
        selected_asset.update({
            "assetState": "HIGH_RISK", "assetStateLabel": "高风险观察", "marketBias": "WAIT",
            "marketBiasLabel": "观望", "confidenceLevel": "LOW", "confidenceLabel": "低",
            "riskLevel": "HIGH", "riskLabel": "高", "worthOpening": False,
            "currentConclusion": "数据质量不足，暂不形成执行建议",
        })
        home["systemState"]["dataQuality"] = status_card("不足", "关键行情窗口尚未齐备", "BLOCKED")
        home["executionSuggestion"] = {
            "status": "DATA_QUALITY_BLOCKED", "statusLabel": "当前暂无完整执行计划",
            "blockedReason": "数据质量不足，暂不形成执行建议", "positionMode": False,
        }
        abstain = ai_role("GPT_FINAL", "最终裁决官")
        abstain.update({
            "stance": "ABSTAIN", "direction": None, "finalMarketBias": None, "finalPlanMode": None,
            "resultAvailable": True, "reviewConclusion": "证据不足，暂不判断",
        })
        home["aiDecision"]["tabs"][0] = abstain

    elif scenario == "ai-disabled-blocked":
        home["header"].update({"aiStatus": "DISABLED", "aiStatusLabel": "已禁用"})
        home["aiDecision"] = unavailable_ai("DISABLED", "已禁用", "AI 复核未启用", True)
        home["systemState"]["aiConflict"] = status_card("不适用", "AI 复核未启用", "PARTIAL")
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

    elif scenario in {"position-monitored", "position-waiting"}:
        position = monitored_position(selected_symbol, scenario == "position-monitored")
        home["positions"] = [position]
        home["pushInbox"]["hasOpenPosition"] = True
        home["pushInbox"]["counts"]["positionRisk"] = 1 if scenario == "position-monitored" else 0
        home["executionSuggestion"] = {
            "status": "POSITION_MONITOR", "statusLabel": "持仓监控", "positionMode": True,
            "positionMonitor": copy.deepcopy(position),
            "originalPlanLabel": "原执行计划，仅用于持仓复核和复盘对照",
            "direction": "BULLISH", "entryZone": "62800 - 63200", "stopLoss": "61200",
            "takeProfitRules": "第一目标 65500；第二目标 67500", "leverageSuggestion": "不高于 2 倍",
            "positionSuggestion": "仅供人工复核", "validPeriod": "有效至 07-14 12:00",
            "invalidCondition": "结构破坏后重新分析",
        }

    elif scenario == "placeholder":
        home["assets"] = home["assets"][:-1]

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

        if parsed.path in {"/", "/dashboard"}:
            scenario = query.get("scenario", ["normal"])[0]
            if scenario not in SCENARIOS:
                self._json({"error": "unknown fixture scenario"}, HTTPStatus.BAD_REQUEST)
                return
            with SCENARIO_LOCK:
                ACTIVE_SCENARIO = scenario
                HOME_REQUEST_COUNTS[scenario] = 0
            html = TEMPLATE.read_text(encoding="utf-8").replace("REFRESH_MS = 30000;", "REFRESH_MS = 0;").encode("utf-8")
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Set-Cookie", f"dashboard_visual_scenario={scenario}; Path=/; SameSite=Strict")
            self.end_headers()
            self.wfile.write(html)
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
            if scenario == "home-failure":
                with SCENARIO_LOCK:
                    home_request_count = HOME_REQUEST_COUNTS.get(scenario, 0)
                    HOME_REQUEST_COUNTS[scenario] = home_request_count + 1
                if home_request_count > 0:
                    self._json({"success": False, "message": "fixture home failure"}, HTTPStatus.SERVICE_UNAVAILABLE)
                    return
            symbol = query.get("selectedSymbol", ["BTCUSDT"])[0].upper()
            home_scenario = "normal" if scenario == "detail-late" else scenario
            self._json({"success": True, "data": scenario_home(home_scenario, symbol)})
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
            self._json({"success": True, "data": {}, "status": "NOT_APPLICABLE", "statusLabel": "不适用"})
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
    args = parser.parse_args()
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
