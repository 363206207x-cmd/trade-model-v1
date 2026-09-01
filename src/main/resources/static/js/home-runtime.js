(function () {
    "use strict";

    var contract = window.TradeModelFrontendContract || {};
    var currentHome = {};
    var selectedSymbol = "";
    var activeRole = "GPT_FINAL";
    var searchTimer = null;
    var selectedSearchAsset = null;
    var searchResultItems = [];
    var activeSearchResultIndex = -1;
    var assetPoolSymbols = new Set();
    var assetPoolCount = 0;
    var searchActionBusy = false;
    var csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "";

    var labels = Object.freeze({
        LONG: "做多", SHORT: "做空",
        LOW: "低", MEDIUM: "中", HIGH: "高", EXTREME: "极高",
        STABLE: "稳定", INCREASED: "上升", SHARPLY_INCREASED: "显著上升",
        STILL_VALID: "仍成立", WEAKENED: "弱化", INVALIDATED: "失效",
        NO_REVERSAL: "无明显反转", WEAK_REVERSAL: "弱反转", STRONG_REVERSAL: "强反转",
        NO_CLEAR_RISK_FACTOR: "暂无明显风险因素", OPPOSING_EVIDENCE_INCREASED: "反向证据增加",
        STRUCTURE_CHANGED: "结构变化", EVENT_IMPACT: "事件冲击", DATA_QUALITY_DEGRADED: "数据质量下降",
        LOGIC_VALID: "逻辑仍成立", LOGIC_WEAKENED: "逻辑弱化", PLAN_INVALIDATED: "计划失效",
        NEAR_STOP_LOSS: "接近止损", NEAR_TAKE_PROFIT: "接近止盈",
        HIGH_RISK_OBSERVATION: "高风险观察", WAIT_USER_CONFIRM_CLOSE: "等待用户确认平仓",
        CONTINUE_HOLD: "继续持有", NO_ADD_POSITION: "暂不加仓", REDUCE_POSITION: "降低仓位",
        TIGHTEN_STOP: "收紧止损", MOVE_STOP: "移动止损", PARTIAL_TAKE_PROFIT: "分批止盈",
        WAIT_CONFIRMATION: "等待人工确认", RECORD_CLOSE_REVIEW: "记录平仓并进入复盘",
        OPEN_MONITORING: "持续监控", WAITING_MONITOR_DATA: "等待监控数据", RISK_ESCALATED: "风险升级",
        OBSERVING: "观察中", CANDIDATE: "候选", WAITING_TRIGGER: "等待触发", TRIGGERED: "已触发",
        HIGH_RISK: "高风险观察", COOLING: "冷却中", CONFUSED: "冲突待解",
        CONFIRMATION: "确认型", PREPARATION: "预备型", REDUCED: "缩减型", OBSERVATION: "观察", BLOCKED: "阻断",
        APPROVE: "通过", DOWNGRADE: "降级", REJECT_CANDIDATE: "拒绝候选", RISK_WARNING: "风险警告",
        UNCHANGED: "维持不变", SAME_FAMILY_DOWNGRADE: "方向不变，强度降低",
        RULE_REANALYSIS_REQUIRED: "需回到规则层重新分析", DOWNGRADE_ONE: "降一级", DOWNGRADE_TWO: "降两级",
        RAISE_ONE: "升一级", RAISE_TWO: "升两级", NEUTRAL: "中性",
        LEVEL_1_CONSISTENT: "一致", LEVEL_2_MINOR_DISAGREEMENT: "轻微分歧",
        LEVEL_3_SIGNIFICANT_DISAGREEMENT: "显著分歧", LEVEL_4_EXTREME_CONFLICT: "极端冲突",
        READY: "就绪", PARTIAL: "部分可用", FALLBACK: "规则路径降级", UNAVAILABLE: "当前不可用",
        DISABLED: "数据源未启用", WAITING_SYNC: "等待同步", OK: "正常", UP: "正常", DEGRADED: "降级",
        FOUND: "已发现", NONE_FOUND: "未发现", INSUFFICIENT_DATA: "数据不足",
        SOURCE_UNAVAILABLE: "来源不可用", STALE: "数据已过期",
        COMPLETE: "覆盖完整", PARTIAL_COVERAGE: "覆盖部分", UNKNOWN: "等待评估",
        SYSTEM_PLAN_POSITION: "系统计划", MANUAL_POSITION: "独立录入", MANUAL_INDEPENDENT: "独立录入",
        VERIFIED_FRESH: "已验证且新鲜", PENDING: "等待验证", INVALID: "来源无效",
        SOURCE_UNAVAILABLE: "来源不可用", CURRENT: "当前有效", NEEDS_REVALIDATION: "正在重验"
    });

    function has(value) { return value !== null && value !== undefined && value !== ""; }
    function text(value, fallback) { return has(value) ? String(value) : (fallback || "当前不可查看"); }
    function escapeHtml(value) {
        return text(value, "").replace(/[&<>'"]/g, function (character) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character];
        });
    }
    function label(value, fallback) {
        if (!has(value)) return fallback || "当前不可查看";
        var raw = String(value).trim();
        var mapped = labels[raw.toUpperCase()];
        if (mapped) return mapped;
        if (typeof contract.userFacingValue === "function") {
            var shared = contract.userFacingValue(raw);
            if (shared && shared !== raw) return shared;
        }
        return /^[A-Z][A-Z0-9_]*$/.test(raw) ? "—" : raw;
    }
    var alertTokenLabels = Object.freeze({
        HIGH: "高优先级", WARN: "需关注", ERROR: "读取失败", WAITING_SYNC: "等待同步",
        SOURCE_UNAVAILABLE: "数据来源不可用", NOT_CALLED: "尚未调用", STALE: "数据已过期",
        PARTIAL: "数据不完整", REGION_RESTRICTED: "当前区域不可用",
        DATA_QUALITY_INSUFFICIENT: "数据质量不足", DATA_QUALITY_DEGRADED: "数据质量下降",
        LEVEL_3_SIGNIFICANT_DISAGREEMENT: "显著分歧", LEVEL_4_EXTREME_CONFLICT: "极端冲突",
        WEAK: "较弱"
    });
    var alertMessagePrefixes = Object.freeze([
        "高风险决策", "数据质量不足", "收敛破裂：冲突升高且多周期弱收敛",
        "开仓被冲突阻断：冲突升高", "多模型冲突升高", "多周期收敛弱"
    ]);
    function alertTokenLabel(value, fallback) {
        if (!has(value)) return fallback || "当前不可查看";
        var raw = String(value).trim();
        var mapped = alertTokenLabels[raw.toUpperCase()];
        if (mapped) return mapped;
        if (typeof contract.userFacingValue === "function") {
            var shared = contract.userFacingValue(raw);
            if (shared && shared !== raw) return shared;
        }
        return /^[A-Z][A-Z0-9_]*$/.test(raw) ? (fallback || "当前不可查看") : raw;
    }
    function userFacingAlertMessage(value) {
        var raw = text(value, "风险状态发生变化").trim();
        if (/^[A-Z][A-Z0-9_]*$/.test(raw)) return alertTokenLabel(raw, "风险状态发生变化");
        var sourceDefined = alertMessagePrefixes.find(function (prefix) { return raw.indexOf(prefix) === 0; });
        if (sourceDefined) return sourceDefined;
        var sanitized = raw
            .replace(/[（(][^）)]*(?:[A-Za-z][A-Za-z0-9_]*\s*=|[A-Z][A-Z0-9_]{2,})[^）)]*[）)]/g, "")
            .replace(/\b(?:analysisId|traceId|symbol|riskLevel|dataQualityScore|aiConflictLevel|aiConflictScore|multiTfConvergence|isWorthOpening)\s*=\s*[^，,；;\s）)]+/g, "")
            .replace(/\b[A-Z][A-Z0-9_]*\b/g, function (token) {
                var mapped = alertTokenLabels[token];
                if (mapped) return mapped;
                if (typeof contract.userFacingValue === "function") {
                    var shared = contract.userFacingValue(token);
                    if (shared && shared !== token) return shared;
                }
                return token.indexOf("_") < 0 && token.length <= 4 ? token : "";
            })
            .replace(/\s*([，,；;：:])\s*([，,；;：:])/g, "$2")
            .replace(/[，,；;：:]\s*$/g, "")
            .replace(/\s{2,}/g, " ")
            .trim();
        return sanitized || "风险状态发生变化";
    }
    function setText(id, value) { var node = document.getElementById(id); if (node) node.textContent = value; }
    function number(value, fractionDigits) {
        if (!has(value) || Number.isNaN(Number(value))) return "当前不可查看";
        return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: fractionDigits === undefined ? 4 : fractionDigits }).format(Number(value));
    }
    function percent(value) {
        if (!has(value) || Number.isNaN(Number(value))) return "当前不可查看";
        var numeric = Number(value);
        return (numeric > 0 ? "+" : "") + number(numeric, 2) + "%";
    }
    function time(value) {
        if (!has(value)) return "当前不可查看";
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) return text(value);
        return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date);
    }
    function clockTime(value) {
        if (!has(value)) return "—";
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) return "—";
        return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(date);
    }
    function symbolOf(asset) {
        var raw = text(asset && (asset.rawSymbol || asset.symbol), "").trim().toUpperCase();
        return /^[A-Z0-9][A-Z0-9._:/-]{1,31}$/.test(raw) ? raw : "";
    }
    function announce(message) { setText("homeLiveRegion", message || ""); }
    function apiData(envelope) {
        if (typeof contract.parseApiEnvelope === "function") {
            var parsed = contract.parseApiEnvelope(envelope);
            if (!parsed.ok) throw new Error(parsed.message);
            return parsed.data;
        }
        if (!envelope || Number(envelope.code) !== 200) throw new Error(text(envelope && envelope.msg, "数据暂不可用"));
        return envelope.data;
    }
    async function api(url, options) {
        var request = Object.assign({ credentials: "same-origin", headers: { Accept: "application/json" } }, options || {});
        request.headers = Object.assign({}, request.headers || {});
        if (request.body && !(request.body instanceof FormData)) request.headers["Content-Type"] = "application/json";
        if (csrfToken && csrfHeader && request.method && request.method !== "GET") request.headers[csrfHeader] = csrfToken;
        var response = await fetch(url, request);
        if (!response.ok) throw new Error("请求失败（" + response.status + "）");
        return apiData(await response.json());
    }

    function statusValue(card, fallback) {
        if (!card) return fallback || "等待同步";
        return text(card.valueLabel, has(card.value) ? label(card.value, fallback) : label(card.status, fallback));
    }
    function semanticTone(value) {
        var normalized = String(value || "").trim().toUpperCase();
        if (["LOW", "STABLE", "STILL_VALID", "LOGIC_VALID", "NO_REVERSAL", "CURRENT", "READY", "VERIFIED_FRESH"].indexOf(normalized) >= 0) return "positive";
        if (["MEDIUM", "WEAKENED", "WEAK_REVERSAL", "INCREASED", "WAITING_TRIGGER", "PENDING", "STALE", "NEEDS_REVALIDATION"].indexOf(normalized) >= 0) return "warning";
        if (["HIGH", "EXTREME", "INVALIDATED", "STRONG_REVERSAL", "SHARPLY_INCREASED", "BLOCKED", "PLAN_INVALIDATED", "INVALID"].indexOf(normalized) >= 0) return "negative";
        return "unknown";
    }
    function toneText(value, raw) {
        return '<span class="semantic-value tone-' + semanticTone(raw) + '">' + escapeHtml(value) + "</span>";
    }
    function eligibleOpportunity(asset) {
        var state = String(asset && (asset.opportunityState || asset.assetState) || "").toUpperCase();
        return ["CANDIDATE", "WAITING_TRIGGER", "TRIGGERED", "HIGH_RISK"].indexOf(state) >= 0;
    }
    function validOpportunityCard(asset) {
        var slotType = String(asset && asset.slotType || "").toUpperCase();
        var finalMode = String(asset && asset.finalPlanMode || "").toUpperCase();
        if (slotType === "DEFAULT_SLOT") return false;
        return symbolOf(asset)
            && has(asset && asset.assetId)
            && slotType === "DECISION"
            && has(asset && (asset.opportunityId || asset.primaryOpportunityId))
            && has(asset && asset.analysisId)
            && eligibleOpportunity(asset)
            && asset.hasFinal === true
            && has(asset.finalMarketBias)
            && ["CONFIRMATION", "REDUCED", "PREPARATION"].indexOf(finalMode) >= 0
            && has(asset.confidenceLevel)
            && has(asset.riskLevel);
    }
    function validObservationCard(asset) {
        var state = String(asset && (asset.opportunityState || asset.assetState) || "").toUpperCase();
        var slotType = String(asset && asset.slotType || "").toUpperCase();
        if (slotType === "DEFAULT_SLOT") return false;
        return symbolOf(asset)
            && has(asset && asset.assetId)
            && String(asset && asset.slotType || "").toUpperCase() === "OBSERVATION"
            && ["OBSERVING", "NO_QUALIFIED_OPPORTUNITY", "STALE", "NEVER_SCANNED", "RANGE", "WAIT",
                "CANDIDATE", "HIGH_RISK", "BLOCKED", "CONFUSED", "INVALIDATED", "COOLING"].indexOf(state) >= 0;
    }
    function selectedFinalAccess(home) {
        var plan = home && home.executionSuggestion || {};
        var access = typeof contract.executionPlanAccess === "function"
            ? contract.executionPlanAccess(plan)
            : { visible: plan.finalPlan === true && String(plan.validationStatus || "").toUpperCase() === "PASS" };
        return { plan: plan, visible: access.visible === true, statusLabel: access.statusLabel, reason: access.reason };
    }

    function renderHeader(home) {
        var header = home.header || {};
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol });
        setText("selectedAssetContext", selected ? "当前资产 · " + selected : "尚未选择机会资产");
        setText("headerUpdatedAt", has(header.updatedAt) ? "更新于 " + clockTime(header.updatedAt) : "—");
    }

    function renderStatus(home) {
        var state = home.systemState || {};
        setText("statusEnvironment", statusValue(state.marketTrend));
        setText("statusSystem", statusValue(state.riskLevel));
        setText("statusData", has(state.dataQuality?.value) ? "更新于 " + clockTime(state.dataQuality.value) : "—");
        setText("statusService", statusValue(state.serviceAvailability, "—"));
        setText("statusAccount", statusValue(state.accountStatus, "—"));
        setText("statusReset", statusValue(state.hotReset, "—"));
    }

    function eventTime(value) {
        if (!has(value)) return "";
        if (typeof value === "string") return time(value);
        return time(value.startAt || value.start || value.from || value.observedAt);
    }
    function renderSignals(home) {
        var alert = Array.isArray(home.alerts) ? home.alerts[0] : null;
        var event = Array.isArray(home.events) ? home.events[0] : null;
        var alertNode = document.getElementById("homeAlert");
        var eventNode = document.getElementById("homeEvent");
        alertNode.hidden = !alert;
        eventNode.hidden = !event;
        if (alert) {
            alertNode.querySelector("strong").textContent = userFacingAlertMessage(alert.message);
            alertNode.querySelector("em").textContent = alertTokenLabel(alert.level, "高优先级");
            alertNode.querySelector("time").textContent = has(alert.time) ? time(alert.time) : "";
        }
        if (event) {
            eventNode.querySelector("strong").textContent = text(event.label, "重要事件");
            eventNode.querySelector("em").textContent = label(event.type, "事件");
            eventNode.querySelector("time").textContent = eventTime(event.timeWindow);
        }
        document.getElementById("signalEmpty").hidden = !!alert || !!event;
    }

    function stateBadge(asset) {
        var view = typeof contract.assetStateView === "function"
            ? contract.assetStateView(asset.opportunityState || asset.assetState, asset.assetStateLabel)
            : { label: label(asset.opportunityState || asset.assetState, "状态待同步"), tone: "neutral" };
        var revalidating = String(asset.opportunityState || asset.assetState || "").toUpperCase() === "TRIGGERED"
            && String(asset.finalPlanLifecycle || "").toUpperCase() === "NEEDS_REVALIDATION";
        var visible = revalidating ? "正在重验" : view.label;
        var tone = revalidating ? " warning" : view.tone === "danger" ? " danger" : view.tone === "warning" ? " warning" : view.tone === "muted" ? " muted" : "";
        return '<span class="state-badge' + tone + '">' + escapeHtml(visible) + "</span>";
    }
    function opportunityCard(asset, selected) {
        var symbol = symbolOf(asset);
        var isSelected = symbol === selected;
        var finalBias = label(asset.finalMarketBias, "—");
        var confidence = text(asset.confidenceLabel, label(asset.confidenceLevel, "当前不可查看"));
        var risk = text(asset.riskLabel, label(asset.riskLevel, "当前不可查看"));
        var oneHour = text(asset.oneHourOpportunityLabel, "1小时待验证");
        var fourHour = text(asset.fourHourTrendLabel, "4小时数据不足");
        var price = has(asset.latestPrice) ? "$" + number(asset.latestPrice, Number(asset.latestPrice) >= 100 ? 2 : 4) : "—";
        var dataNotice = opportunityDataNotice(asset);
        return '<article class="opportunity-card' + (isSelected ? " is-selected" : "") + '" tabindex="0" role="button" aria-pressed="'
            + String(isSelected) + '" data-symbol="'
            + escapeHtml(symbol) + '" aria-label="查看 ' + escapeHtml(symbol + " 最终决策上下文") + '"><header><div class="asset-identity"><strong>'
            + escapeHtml(text(asset.name, symbol.replace(/USDT$/, ""))) + "</strong><small>" + escapeHtml(symbol)
            + (dataNotice ? " · " + escapeHtml(dataNotice) : "")
            + '</small></div><strong class="opportunity-price">' + escapeHtml(price)
            + '</strong></header><div class="opportunity-final"><span><small>方向</small><b>' + escapeHtml(finalBias)
            + '</b></span></div><div class="opportunity-metrics"><span><small>置信</small><strong>' + escapeHtml(confidence)
            + '</strong></span><span><small>风险</small><strong>' + escapeHtml(risk)
            + '</strong></span></div><div class="opportunity-context"><span>' + escapeHtml(oneHour)
            + '</span><span>' + escapeHtml(fourHour) + '</span></div></article>';
    }
    function opportunityDataNotice(asset) {
        var state = String(asset && asset.dataFreshness || "").toUpperCase();
        if (state === "TIMEFRAME_CONFLICT") return "周期冲突";
        if (state === "STALE") return "数据过期";
        return "";
    }
    function observationStateText(asset) {
        var state = String(asset && (asset.opportunityState || asset.assetState) || "").toUpperCase();
        return {
            OBSERVING: "观察中",
            NO_QUALIFIED_OPPORTUNITY: "暂无合格机会",
            STALE: "数据过期",
            NEVER_SCANNED: "等待首次扫描",
            RANGE: "震荡",
            WAIT: "观望",
            CANDIDATE: "计划生成中",
            HIGH_RISK: "高风险观察",
            BLOCKED: "当前受限",
            CONFUSED: "当前受限",
            INVALIDATED: "已失效",
            COOLING: "冷却中"
        }[state] || "观察中";
    }
    function observationDataText(asset) {
        var state = String(asset && asset.dataFreshness || "").toUpperCase();
        if (state === "TIMEFRAME_CONFLICT") return "周期冲突";
        if (state === "STALE") return "数据过期";
        if (state === "NEVER_SCANNED") return "尚未扫描";
        if (state === "FRESH" || state === "READY") return "数据新鲜";
        return "状态待同步";
    }
    function observationCard(asset, selected) {
        var symbol = symbolOf(asset);
        var isSelected = symbol === selected;
        var state = observationStateText(asset);
        var direction = asset.hasFinal === true && has(asset.finalMarketBias)
            ? label(asset.finalMarketBias, "待验证")
            : state === "震荡" ? "震荡" : state === "观望" ? "观望" : "待验证";
        var oneHour = text(asset.oneHourOpportunityLabel, "1小时待验证");
        var fourHour = text(asset.fourHourTrendLabel, "4小时数据不足");
        var price = has(asset.latestPrice) ? "$" + number(asset.latestPrice, Number(asset.latestPrice) >= 100 ? 2 : 4) : "—";
        return '<article class="opportunity-card' + (isSelected ? " is-selected" : "")
            + '" tabindex="0" role="button" aria-pressed="' + String(isSelected)
            + '" data-symbol="' + escapeHtml(symbol)
            + '" aria-label="查看 ' + escapeHtml(symbol + " 观察状态，" + state) + '"><header><div class="asset-identity"><strong>'
            + escapeHtml(text(asset.name, symbol.replace(/USDT$/, ""))) + "</strong><small>" + escapeHtml(symbol)
            + '</small></div><strong class="opportunity-price">' + escapeHtml(price)
            + '</strong></header><div class="opportunity-final"><span><small>方向</small><b>' + escapeHtml(direction)
            + '</b></span></div><div class="opportunity-metrics"><span><small>状态</small><strong>' + escapeHtml(state)
            + '</strong></span><span><small>数据</small><strong>' + escapeHtml(observationDataText(asset))
            + '</strong></span></div><div class="opportunity-context"><span>' + escapeHtml(oneHour)
            + '</span><span>' + escapeHtml(fourHour) + '</span></div></article>';
    }
    function renderOpportunities(home) {
        var all = Array.isArray(home.assets) ? home.assets : [];
        var seen = new Set();
        var assets = all.filter(function (asset) {
            return validOpportunityCard(asset) || validObservationCard(asset);
        }).filter(function (asset) {
            var identity = "asset:" + String(asset.assetId);
            var symbolIdentity = "symbol:" + symbolOf(asset);
            if (seen.has(identity) || seen.has(symbolIdentity)) return false;
            seen.add(identity);
            seen.add(symbolIdentity);
            return true;
        }).slice(0, 6);
        var grid = document.getElementById("opportunityGrid");
        var empty = document.getElementById("opportunityEmpty");
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || selectedSymbol;
        setText("opportunityHeading", "资产");
        grid.innerHTML = assets.map(function (asset) {
            return String(asset.slotType || "").toUpperCase() === "OBSERVATION"
                ? observationCard(asset, selected)
                : opportunityCard(asset, selected);
        }).join("");
        grid.hidden = assets.length === 0;
        empty.hidden = assets.length !== 0;
        grid.querySelectorAll("[data-symbol]").forEach(function (card) {
            function select() {
                selectedSymbol = card.dataset.symbol;
                if (typeof contract.replaceUrlParam === "function") contract.replaceUrlParam("asset", selectedSymbol);
                loadHome(selectedSymbol);
            }
            card.addEventListener("click", select);
            card.addEventListener("keydown", function (event) {
                if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(); }
            });
        });
        return assets.length;
    }

    function trustedMonitor(position) {
        var trust = String(position && position.monitorTrustState || "SOURCE_UNAVAILABLE").toUpperCase();
        return position && trust === "VERIFIED_FRESH" && position.markPriceFresh === true
            && ["OPEN_MONITORING", "RISK_ESCALATED", "PLAN_INVALIDATED"].indexOf(String(position.dataState || "").toUpperCase()) >= 0;
    }
    function validPosition(position) {
        return position && symbolOf(position) && has(position.direction)
            && has(position.entryPrice) && has(position.openedAt);
    }
    function positionDetailLink(positionId) {
        var normalized = String(positionId || "").trim();
        return /^\d+$/.test(normalized) && Number(normalized) > 0
            ? '<a class="position-detail-link" href="/positions/' + encodeURIComponent(normalized) + '?returnTo=' + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : "")) + '">查看详情</a>'
            : "";
    }
    function riskRank(value) { return { LOW: 1, MEDIUM: 2, HIGH: 3, EXTREME: 4 }[String(value || "").toUpperCase()] || 0; }
    function highestRisk(positions) {
        var trusted = positions.filter(trustedMonitor).sort(function (a, b) { return riskRank(b.riskLevel) - riskRank(a.riskLevel); });
        return trusted.length ? text(trusted[0].riskLevelLabel, label(trusted[0].riskLevel, "暂无评估")) : "暂无评估";
    }
    function trustStateText(position) {
        var state = String(position && position.monitorTrustState || "SOURCE_UNAVAILABLE").toUpperCase();
        return {
            PENDING: "等待监控数据",
            PENDING_VERIFICATION: "等待监控数据",
            STALE: "监控数据已过期",
            INVALID: "当前不可查看",
            SOURCE_UNAVAILABLE: "监控来源不可用"
        }[state] || "等待监控数据";
    }
    function positionFact(labelText, value, raw, align) {
        return '<span class="position-fact ' + (align || "") + '"><small>' + escapeHtml(labelText) + "</small><b>"
            + toneText(value, raw) + "</b></span>";
    }
    function positionRow(position) {
        var trusted = trustedMonitor(position);
        var unavailable = trustStateText(position);
        var risk = text(position.riskLevelLabel, label(position.riskLevel));
        var logic = text(position.entryLogicStatusLabel, label(position.entryLogicStatus));
        var reversal = text(position.reversalStatusLabel, label(position.reversalStatus));
        var trend = label(position.riskTrend);
        var conclusion = text(position.monitorConclusionLabel, label(position.monitorConclusion));
        var action = text(position.suggestedManualActionText, label(position.suggestedAction));
        var source = typeof contract.positionSourceLabel === "function"
            ? contract.positionSourceLabel(position.sourceType) : label(position.sourceType, "来源不可用");
        var detailLink = positionDetailLink(position.positionId);
        var pnlCoverage = trusted && has(position.pnlCoverage)
            ? "盈亏仅含标记价格、开仓价和数量；费用、资金费率、部分成交及追加仓位覆盖未知"
            : "";
        var openingFacts = '<div class="position-facts">' + positionFact("开仓价", number(position.entryPrice), "UNKNOWN", "numeric")
            + positionFact("开仓时间", time(position.openedAt), "UNKNOWN", "numeric");
        if (trusted) {
            openingFacts += positionFact("标记价格", number(position.markPrice), "STABLE", "numeric")
                + positionFact("盈亏", percent(position.pnlPercent), Number(position.pnlPercent) >= 0 ? "STABLE" : "INVALID", "numeric");
        }
        openingFacts += "</div>";
        var monitorColumns = trusted
            ? '<div class="position-judgment">' + positionFact("入场逻辑", logic, position.entryLogicStatus, "center")
                + positionFact("反转状态", reversal, position.reversalStatus, "center")
                + positionFact("持仓风险", risk, position.riskLevel, "center")
                + positionFact("风险趋势", trend, position.riskTrend, "center") + "</div>"
                + '<div class="position-conclusion">' + positionFact("监控结论", conclusion, position.monitorConclusion, "narrative")
                + positionFact("建议动作", action, position.suggestedAction, "narrative")
                + detailLink + '</div>'
            : '<div class="position-trust-state" role="status"><strong>' + escapeHtml(unavailable) + '</strong>' + detailLink + '</div>';
        return '<article class="position-row' + (trusted ? " is-trusted" : " is-untrusted")
            + '"' + (pnlCoverage ? ' title="' + escapeHtml(pnlCoverage) + '"' : '')
            + ' aria-label="' + escapeHtml(symbolOf(position) + " " + text(position.directionLabel, label(position.direction)) + " "
                + (trusted ? conclusion : unavailable) + (pnlCoverage ? " " + pnlCoverage : "")) + '">'
            + '<div class="position-identity"><strong>' + escapeHtml(symbolOf(position)) + "</strong>"
            + '<span class="direction-label">' + escapeHtml(text(position.directionLabel, label(position.direction))) + "</span><small>" + escapeHtml(source) + "</small></div>"
            + openingFacts + monitorColumns + "</article>";
    }
    function renderPositions(home) {
        var positions = (Array.isArray(home.positions) ? home.positions : []).filter(validPosition);
        var shown = positions.slice(0, 3);
        var list = document.getElementById("positionList");
        var empty = document.getElementById("positionEmpty");
        list.innerHTML = shown.map(positionRow).join("");
        list.hidden = shown.length === 0;
        empty.hidden = shown.length !== 0;
        var aggregate = home.positionAggregate && typeof home.positionAggregate === "object"
            ? home.positionAggregate : {};
        var activeCount = Number.isInteger(aggregate.activeCount) ? aggregate.activeCount : "—";
        var highestTrustedRisk = has(aggregate.highestTrustedRisk)
            ? label(aggregate.highestTrustedRisk, "—") : "—";
        var coverage = has(aggregate.coverageState)
            ? label(aggregate.coverageState, "—") : "—";
        setText("positionAggregate", "活动 " + activeCount + " · 最高风险 " + highestTrustedRisk + " · " + coverage);
    }

    function planField(labelText, value) { return '<span><small>' + escapeHtml(labelText) + '</small><b>' + escapeHtml(text(value, "当前不可查看")) + "</b></span>"; }
    function selectedOpportunityState(home) {
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol });
        var asset = (Array.isArray(home.assets) ? home.assets : []).find(function (item) { return symbolOf(item) === selected; });
        return asset ? label(asset.opportunityState || asset.assetState, "状态待同步") : label(home.selectedContextState, "状态待同步");
    }
    function renderPlan(home) {
        var target = document.getElementById("planContent");
        var link = document.getElementById("planDetailLink");
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol });
        var access = selectedFinalAccess(home);
        var plan = access.plan;
        setText("planAsset", selected || "未选择资产");
        if (!access.visible) {
            var revalidating = String(plan.status || "").toUpperCase() === "REVALIDATION_REQUIRED";
            target.innerHTML = '<div class="plan-empty"><strong>' + (revalidating ? "正在重验" : "尚未形成") + '</strong><span>机会状态 · '
                + escapeHtml(selectedOpportunityState(home)) + "</span><span>" + escapeHtml(text(plan.revalidationReason || access.reason, "尚未形成有效计划")) + "</span>"
                + (revalidating ? '<span>恢复条件 · ' + escapeHtml(text(plan.revalidationRule, "当前无可验证恢复条件")) + "</span>" : "")
                + (revalidating ? '<span>最新重验状态 · ' + escapeHtml(label(plan.planLifecycleState, "等待重验")) + "</span>" : "") + "</div>";
            link.hidden = true;
            return;
        }
        var planId = plan.sourceExecutionPlanId;
        var lifecycle = plan.planLifecycleState || plan.status;
        target.innerHTML = '<div class="plan-status-layer"><div><small>最终偏向 / 计划模式</small><strong>'
            + escapeHtml(label(plan.finalMarketBias || plan.direction)) + " · " + escapeHtml(label(plan.finalPlanMode))
            + '</strong></div><span class="plan-state tone-' + semanticTone(lifecycle) + '">' + escapeHtml(label(lifecycle, text(plan.statusLabel, "当前有效"))) + '</span></div><div class="plan-key-layer">'
            + planField("入场 / 触发", plan.entryZone || plan.triggerCondition)
            + planField("止损", plan.stopZone || plan.stopLoss)
            + planField("失效条件", plan.invalidCondition || plan.abandonCondition)
            + planField("目标", plan.targetZones || plan.targetLogic || plan.takeProfitRules)
            + '</div><div class="plan-metadata-layer">' + planField("杠杆", plan.leverageSuggestion) + planField("仓位", plan.positionSuggestion)
            + planField("有效期", plan.validPeriod || (has(plan.expiresAt) ? time(plan.expiresAt) : null))
            + planField("版本", has(plan.planVersion) ? "v" + plan.planVersion : "当前不可查看") + "</div>";
        link.href = "/plans/" + encodeURIComponent(planId) + "?returnTo="
            + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : ""));
        link.hidden = false;
    }

    function collectionLabel(state) {
        return typeof contract.collectionStateLabel === "function" ? contract.collectionStateLabel(state) : label(state, "来源不可用");
    }
    function itemText(item) {
        if (!has(item)) return "";
        if (typeof item !== "object") return label(item, text(item));
        var value = label(item.text || item.summary || item.hypothesis || item.currentValue || item.reason || item.description || item.source, "");
        var typeValue = item.type || item.category;
        var changeValue = item.change || item.changeFromBaseline;
        var type = has(typeValue) ? label(typeValue) : "";
        var change = has(changeValue) ? label(changeValue) : "";
        var result = type && value && value.indexOf(type) !== 0 ? type + "：" + value : value || type;
        return change && result && result.indexOf(change) < 0 ? result + "（" + change + "）" : result;
    }
    function list(items, emptyState) {
        var values = (Array.isArray(items) ? items : []).map(itemText).filter(Boolean).slice(0, 2);
        return values.length ? "<ul>" + values.map(function (value) { return "<li>" + escapeHtml(value) + "</li>"; }).join("") + "</ul>"
            : "<p>" + escapeHtml(collectionLabel(emptyState)) + "</p>";
    }
    function dl(items) {
        return "<dl>" + items.map(function (item) { return "<div><dt>" + escapeHtml(item[0]) + "</dt><dd>" + escapeHtml(text(item[1], "当前不可查看")) + "</dd></div>"; }).join("") + "</dl>";
    }
    function roleUnavailable(role) {
        return '<div class="ai-unavailable"><strong>' + escapeHtml({ GPT_FINAL: "GPT 综合判断", GEMINI_REVIEW: "Gemini 冲突复核", GROK_CHALLENGE: "Grok 反方挑战" }[activeRole])
            + "</strong><span>" + escapeHtml(text(role && role.statusMessage, "当前角色结果不可查看")) + "</span></div>";
    }
    function candidateStateLegal(opportunityState, planMode) {
        var state = String(opportunityState || "").toUpperCase();
        var mode = String(planMode || "").toUpperCase();
        return state !== "WAITING_TRIGGER" || mode === "PREPARATION";
    }
    function candidateConclusion(summary, opportunityState) {
        var value = text(summary, "");
        if (String(opportunityState || "").toUpperCase() === "WAITING_TRIGGER"
                && value.indexOf("人工确认") >= 0) {
            return "等待触发；触发后重新校验，通过后再进入人工确认";
        }
        return value || "当前一句话结论不可查看";
    }
    function renderGpt(role) {
        var core = role.coreJudgment || {};
        var candidate = role.candidateSummary || {};
        var multi = role.multiTimeframeExplanation || {};
        var adjustment = role.biasAdjustment || {};
        if (!candidateStateLegal(core.opportunityState, candidate.planMode)) {
            return '<div class="ai-unavailable"><strong>GPT 综合判断</strong><span>机会状态与候选参与方式不一致，当前不可查看</span></div>';
        }
        var why = text(core.text, "当前形成原因不可查看");
        return '<div class="ai-first-visual"><div class="primary"><small>GPT 综合判断 · 非最终计划</small><strong>方向判断：'
            + escapeHtml(label(core.marketBias, "—")) + "</strong></div><div><small>机会进度</small><strong>"
            + escapeHtml(label(core.opportunityState, "—")) + "</strong></div><div><small>候选参与方式</small><strong>"
            + escapeHtml(label(candidate.planMode, "—"))
            + '</strong></div></div><div class="ai-content-grid"><section class="ai-section"><h3>形成原因</h3><p>' + escapeHtml(why)
            + "</p>" + dl([["4h", label(multi["4h"], "暂无数据")], ["1h", label(multi["1h"], "暂无数据")], ["15m", label(multi["15m"], "暂无数据")], ["5m", label(multi["5m"], "暂无数据")],
                ["偏向调整", label(adjustment.before, "当前不可查看") + " → " + label(adjustment.after, "当前不可查看")]])
            + '</section><section class="ai-section"><h3>支持证据 · ' + escapeHtml(collectionLabel(role.supportingEvidenceState)) + "</h3>"
            + list(role.supportingEvidence, role.supportingEvidenceState) + '<h3>反对证据 · ' + escapeHtml(collectionLabel(role.opposingEvidenceState)) + "</h3>"
            + list(role.opposingEvidence, role.opposingEvidenceState) + '</section></div><div class="ai-summary-footer"><strong>一句话结论</strong><span>'
            + escapeHtml(candidateConclusion(candidate.summary, core.opportunityState)) + "</span></div>";
    }
    function renderGemini(role) {
        var reviewResult = String(role.reviewResult || "").toUpperCase();
        if (["APPROVE", "DOWNGRADE", "REJECT_CANDIDATE", "RISK_WARNING"].indexOf(reviewResult) < 0) return roleUnavailable(role);
        var suggestion = role.downgradeSuggestion || {};
        var selectedState = String(currentHome && currentHome.selectedAssetContext
            && (currentHome.selectedAssetContext.opportunityState || currentHome.selectedAssetContext.assetState) || "").toUpperCase();
        if (selectedState === "WAITING_TRIGGER" && has(suggestion.before)
                && (String(suggestion.before).toUpperCase() === "CONFIRMATION"
                || String(suggestion.after || "").toUpperCase() !== "PREPARATION")) {
            return '<div class="ai-unavailable"><strong>Gemini 冲突复核</strong><span>复核前后状态与等待触发生命周期不一致，当前不可查看</span></div>';
        }
        var hasBeforeAfter = has(suggestion.before) && has(suggestion.after);
        var beforeAfter = hasBeforeAfter
            ? '<section class="ai-section"><h3>调整前 / 调整后</h3>'
                + dl([["调整前", label(suggestion.before)], ["调整后", label(suggestion.after)], ["调整原因", text(suggestion.reason, "当前不可查看")]]) + '</section>'
            : "";
        return '<div class="ai-first-visual is-single"><div class="primary"><small>Gemini 冲突复核</small><strong>复核结果：' + escapeHtml(label(reviewResult, "当前不可查看"))
            + '</strong></div></div><div class="ai-content-grid gemini">' + beforeAfter
            + '<section class="ai-section"><h3>证据缺口 · ' + escapeHtml(collectionLabel(role.evidenceGapsState)) + '</h3>' + list(role.evidenceGaps, role.evidenceGapsState)
            + '<h3>逻辑冲突 · ' + escapeHtml(collectionLabel(role.logicConflictsState)) + '</h3>' + list(role.logicConflicts, role.logicConflictsState)
            + '<h3>风险低估 · ' + escapeHtml(collectionLabel(role.underestimatedRisksState)) + '</h3>' + list(role.underestimatedRisks, role.underestimatedRisksState)
            + '</section></div><div class="ai-summary-footer"><strong>恢复条件</strong><span>' + escapeHtml(text(role.recoveryCondition || suggestion.recoveryCondition, "当前无可验证恢复条件")) + "</span></div>";
    }
    function completeFailurePath(path) {
        return path && has(path.triggerCondition) && has(path.causalPath) && has(path.invalidatingEvidence);
    }
    function failurePathStateView(role) {
        var state = String(role && role.failurePathState || "").toUpperCase();
        var paths = (Array.isArray(role && role.failurePaths) ? role.failurePaths : []).filter(completeFailurePath);
        if (state === "FOUND") {
            return paths.length ? { valid: true, label: "已发现可验证失败路径", paths: paths }
                : { valid: false, label: "失败路径状态不一致，当前不可查看", paths: [] };
        }
        if ((state === "NONE_FOUND" || state === "NO_VERIFIABLE_FAILURE_PATH")
                && (!role.failurePaths || role.failurePaths.length === 0)) {
            return { valid: true, label: "未发现可验证失败路径", paths: [] };
        }
        if (state === "INSUFFICIENT_DATA") return { valid: true, label: "数据不足，无法判断", paths: [] };
        if (state === "SOURCE_UNAVAILABLE") return { valid: true, label: "数据来源暂不可用", paths: [] };
        if (state === "STALE") return { valid: true, label: "数据已过期", paths: [] };
        return { valid: false, label: "失败路径状态不一致，当前不可查看", paths: [] };
    }
    function failurePathChain(paths, state, invalidStateLabel) {
        var rows = Array.isArray(paths) ? paths : [];
        if (!rows.length) return "<p>" + escapeHtml(invalidStateLabel || collectionLabel(state)) + "</p>";
        return rows.slice(0, 2).map(function (path) {
            return '<div class="failure-path-chain"><strong>' + escapeHtml(text(path.hypothesis, "失败路径")) + '</strong><ol>'
                + '<li><small>触发</small><span>' + escapeHtml(text(path.triggerCondition, "当前不可查看")) + '</span></li>'
                + '<li><small>演化</small><span>' + escapeHtml(text(path.causalPath, "当前不可查看")) + '</span></li>'
                + '<li><small>失效</small><span>' + escapeHtml(text(path.invalidatingEvidence, "当前不可查看")) + '</span></li></ol></div>';
        }).join("");
    }
    function renderGrok(role) {
        var failurePath = failurePathStateView(role);
        return '<div class="ai-first-visual is-single"><div class="primary"><small>Grok 反方挑战</small><strong>失败路径：' + escapeHtml(failurePath.label)
            + '</strong></div></div><div class="ai-content-grid grok"><section class="ai-section"><h3>失败路径 · 触发 → 演化 → 失效</h3>'
            + failurePathChain(failurePath.paths, role.failurePathState, failurePath.valid ? null : failurePath.label)
            + '<h3>反向情景</h3>' + list(role.opposingScenarios, role.opposingScenariosState)
            + '<h3>外部事件风险</h3>' + list(role.externalEventRisks, role.externalEventRisksState)
            + '</section><section class="ai-section"><h3>微观结构风险</h3>' + list(role.microstructureRisks, role.microstructureRisksState)
            + '<h3>继续观察指标</h3>' + list(role.watchIndicators, role.watchIndicatorsState)
            + '<h3>挑战摘要</h3><p>' + escapeHtml(text(role.challengeSummary, "—")) + "</p></section></div>";
    }

    function renderConflict(home) {
        var consistency = home.aiDecision && home.aiDecision.consistency || {};
        var level = String(consistency.conflictLevel || "").toUpperCase();
        var ready = String(consistency.dataState || "").toUpperCase() === "READY"
            && has(consistency.conflictLevel) && has(consistency.mainReason);
        var show = ready && level && level !== "LEVEL_1_CONSISTENT";
        var target = document.getElementById("conflictSummary");
        var layout = document.getElementById("aiLayout");
        target.hidden = !show;
        layout.classList.toggle("has-conflict", show);
        if (!show) { target.innerHTML = ""; return; }
        target.innerHTML = '<h3>冲突摘要</h3>' + dl([
            ["冲突等级", label(consistency.conflictLevel)], ["最终偏向", label(consistency.finalMarketBias)],
            ["最终计划", label(consistency.finalPlanMode)], ["主要原因", text(consistency.mainReason)],
            ["恢复条件", text(consistency.recoveryCondition)]
        ]);
    }
    function renderAi(home) {
        var ai = home.aiDecision || {};
        var roles = typeof contract.normalizeAiTabs === "function" ? contract.normalizeAiTabs(ai.tabs) : (Array.isArray(ai.tabs) ? ai.tabs : []);
        var role = roles.find(function (item) { return item.role === activeRole; });
        var panel = document.getElementById("aiRolePanel");
        setText("aiContext", symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || "等待分析上下文");
        var roleContent;
        if (!role || role.resultAvailable !== true) roleContent = roleUnavailable(role);
        else if (activeRole === "GPT_FINAL") roleContent = renderGpt(role);
        else if (activeRole === "GEMINI_REVIEW") roleContent = renderGemini(role);
        else roleContent = renderGrok(role);
        panel.innerHTML = roleContent;
        setText("aiMetadata", role ? "角色状态 " + label(role.roleState, "当前不可用") + " · 生成时间 " + time(role.generatedAt) + " · 来源 " + text(role.provider, "当前不可查看") : "角色状态待同步");
        var trace = role && role.traceId;
        var analysis = role && role.analysisId;
        var audit = document.getElementById("auditChainLink");
        if (trace) {
            audit.href = "/audit/" + encodeURIComponent(trace) + "?returnTo="
                + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : ""));
            audit.textContent = "查看完整审计链";
            audit.removeAttribute("aria-disabled");
        } else if (analysis) {
            audit.href = "/analysis/" + encodeURIComponent(analysis) + "?returnTo="
                + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : ""));
            audit.textContent = "查看分析详情";
            audit.removeAttribute("aria-disabled");
        } else {
            audit.removeAttribute("href");
            audit.textContent = "审计链尚未形成";
            audit.setAttribute("aria-disabled", "true");
        }
        renderConflict(home);
    }

    function render(home) {
        currentHome = home || {};
        selectedSymbol = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || selectedSymbol;
        renderHeader(home);
        renderSignals(home);
        renderOpportunities(home);
        renderStatus(home);
        renderPositions(home);
        renderPlan(home);
        renderAi(home);
    }
    async function loadHome(symbol) {
        try {
            var query = new URLSearchParams({ limit: "6" });
            if (symbol) query.set("selectedSymbol", symbol);
            var requestedPositionId = new URLSearchParams(window.location.search).get("positionId");
            if (requestedPositionId && /^\d+$/.test(requestedPositionId)) {
                query.set("positionId", requestedPositionId);
            }
            render(await api("/api/dashboard/home?" + query.toString()));
        } catch (error) {
            announce(error.message);
            render({ states: { overall: "ERROR" }, diagnostics: {}, assets: [], positions: [], aiDecision: { tabs: [] } });
        }
    }

    function setSearchPopoverOpen(open) {
        var input = document.getElementById("homeAssetSearch");
        var popover = document.getElementById("homeAssetSearchPopover");
        popover.hidden = !open;
        input.setAttribute("aria-expanded", String(open));
    }
    function renderSearchSelection(message) {
        var symbolNode = document.getElementById("homeSelectedSearchSymbol");
        var stateNode = document.getElementById("homeSelectedSearchState");
        var poolCountNode = document.getElementById("homeAssetPoolCount");
        var previewButton = document.getElementById("homePreviewAsset");
        var addButton = document.getElementById("homeAddAsset");
        var statusNode = document.getElementById("homeAssetSearchStatus");
        poolCountNode.textContent = "观察资产池 · " + assetPoolCount;
        statusNode.textContent = message || "";
        if (!selectedSearchAsset) {
            symbolNode.textContent = "尚未选择资产";
            stateNode.textContent = "未选择搜索结果";
            previewButton.textContent = "分析";
            previewButton.disabled = true;
            addButton.disabled = true;
            addButton.textContent = "添加";
            return;
        }
        var symbol = symbolOf(selectedSearchAsset);
        var inPool = assetPoolSymbols.has(symbol);
        symbolNode.textContent = symbol;
        stateNode.textContent = text(selectedSearchAsset.baseAsset || selectedSearchAsset.name, symbol.replace(/USDT$/, ""))
            + " · " + (inPool ? "已添加" : "未添加");
        previewButton.textContent = "分析";
        previewButton.disabled = searchActionBusy;
        addButton.disabled = searchActionBusy || inPool;
        addButton.textContent = inPool ? "已添加" : "添加";
    }
    function setActiveSearchResult(index, focusResult) {
        var buttons = Array.from(document.querySelectorAll("#homeAssetSearchResults [data-search-index]"));
        if (!buttons.length) { activeSearchResultIndex = -1; return; }
        activeSearchResultIndex = Math.max(0, Math.min(index, buttons.length - 1));
        buttons.forEach(function (button, buttonIndex) {
            var active = buttonIndex === activeSearchResultIndex;
            button.classList.toggle("is-active", active);
            button.setAttribute("aria-selected", String(active));
        });
        if (focusResult) buttons[activeSearchResultIndex].focus();
    }
    function selectSearchResult(index) {
        if (!searchResultItems[index]) return;
        selectedSearchAsset = searchResultItems[index];
        setActiveSearchResult(index, false);
        renderSearchSelection();
        setSearchPopoverOpen(true);
    }
    async function loadAssetPoolMembership() {
        var items = await api("/api/asset-pool");
        var values = Array.isArray(items) ? items : [];
        assetPoolSymbols = new Set(values.map(function (item) { return symbolOf(item); }).filter(Boolean));
        assetPoolCount = values.length;
        renderSearchSelection();
    }
    function renderSearchResults(items) {
        var target = document.getElementById("homeAssetSearchResults");
        searchResultItems = (Array.isArray(items) ? items : []).filter(function (asset) { return !!symbolOf(asset); }).slice(0, 8);
        activeSearchResultIndex = -1;
        target.innerHTML = searchResultItems.map(function (asset, index) {
            var symbol = symbolOf(asset);
            var inPool = assetPoolSymbols.has(symbol);
            return '<button class="search-result" type="button" role="option" aria-selected="false" data-search-index="' + index
                + '" data-search-symbol="' + escapeHtml(symbol)
                + '"><span><strong>' + escapeHtml(symbol) + "</strong><small>" + escapeHtml(text(asset.baseAsset || asset.name, "市场资产"))
                + "</small></span><em>" + (inPool ? "已添加" : "未添加") + "</em></button>";
        }).join("");
        if (!target.innerHTML) target.innerHTML = '<div class="search-result"><span><strong>未找到资产</strong><small>可更换名称或交易对</small></span></div>';
        setSearchPopoverOpen(true);
        target.querySelectorAll("[data-search-index]").forEach(function (button) {
            button.addEventListener("click", function () { selectSearchResult(Number(button.dataset.searchIndex)); });
        });
    }
    function bindSearch() {
        var input = document.getElementById("homeAssetSearch");
        var popover = document.getElementById("homeAssetSearchPopover");
        input.addEventListener("focus", function () { setSearchPopoverOpen(true); });
        input.addEventListener("input", function () {
            window.clearTimeout(searchTimer);
            var query = input.value.trim();
            selectedSearchAsset = null;
            renderSearchSelection();
            if (!query) { searchResultItems = []; document.getElementById("homeAssetSearchResults").innerHTML = ""; setSearchPopoverOpen(true); return; }
            searchTimer = window.setTimeout(async function () {
                try { renderSearchResults(await api("/api/asset-pool/search?query=" + encodeURIComponent(query) + "&limit=8")); }
                catch (error) { document.getElementById("homeAssetSearchResults").innerHTML = '<div class="search-result"><span><strong>搜索当前不可查看</strong><small>' + escapeHtml(error.message) + "</small></span></div>"; setSearchPopoverOpen(true); }
            }, 180);
        });
        input.addEventListener("keydown", function (event) {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                event.preventDefault();
                var step = event.key === "ArrowDown" ? 1 : -1;
                var start = activeSearchResultIndex < 0 ? (step > 0 ? 0 : searchResultItems.length - 1) : activeSearchResultIndex + step;
                setActiveSearchResult(start, false);
            } else if (event.key === "Enter" && activeSearchResultIndex >= 0) {
                event.preventDefault();
                selectSearchResult(activeSearchResultIndex);
            } else if (event.key === "Escape") {
                setSearchPopoverOpen(false);
                input.blur();
            }
        });
        document.getElementById("homeAssetSearchResults").addEventListener("keydown", function (event) {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                event.preventDefault();
                setActiveSearchResult(activeSearchResultIndex + (event.key === "ArrowDown" ? 1 : -1), true);
            } else if (event.key === "Enter" && activeSearchResultIndex >= 0) {
                event.preventDefault();
                selectSearchResult(activeSearchResultIndex);
                input.focus();
            } else if (event.key === "Escape") {
                setSearchPopoverOpen(false);
                input.focus();
            }
        });
        document.getElementById("homePreviewAsset").addEventListener("click", async function () {
            if (!selectedSearchAsset || searchActionBusy) return;
            searchActionBusy = true;
            renderSearchSelection("分析中");
            try {
                var result = await api("/api/asset-pool/search/" + encodeURIComponent(symbolOf(selectedSearchAsset)) + "/analysis-preview?timeframe=5m", { method: "POST" });
                if (!result || !result.analysisId) throw new Error("预览未返回分析标识");
                window.location.assign("/analysis/" + encodeURIComponent(result.analysisId) + "?returnTo="
                    + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : "")));
            } catch (error) {
                searchActionBusy = false;
                renderSearchSelection(error.message);
                announce(error.message);
            }
        });
        document.getElementById("homeAddAsset").addEventListener("click", async function () {
            if (!selectedSearchAsset || searchActionBusy || assetPoolSymbols.has(symbolOf(selectedSearchAsset))) return;
            searchActionBusy = true;
            renderSearchSelection("添加中");
            try {
                await api("/api/asset-pool", { method: "POST", body: JSON.stringify({ symbol: symbolOf(selectedSearchAsset), focusEnabled: true }) });
                await loadAssetPoolMembership();
                searchActionBusy = false;
                renderSearchSelection("已添加");
                renderSearchResults(searchResultItems);
                announce(symbolOf(selectedSearchAsset) + " 已添加");
            } catch (error) {
                searchActionBusy = false;
                renderSearchSelection(error.message);
                announce(error.message);
            }
        });
        document.addEventListener("click", function (event) { if (!event.target.closest(".asset-search")) setSearchPopoverOpen(false); });
        loadAssetPoolMembership().catch(function (error) {
            document.getElementById("homeAssetPoolCount").textContent = "观察资产池 · 当前不可查看";
            document.getElementById("homeAssetSearchStatus").textContent = error.message;
        });
        renderSearchSelection();
    }
    function bindTabs() {
        var tabs = Array.from(document.querySelectorAll("[data-ai-role]"));
        function activate(button, focus) {
            activeRole = button.dataset.aiRole;
            tabs.forEach(function (item) {
                var selected = item === button;
                item.classList.toggle("is-active", selected);
                item.setAttribute("aria-selected", String(selected));
                item.tabIndex = selected ? 0 : -1;
            });
            if (focus) button.focus();
            renderAi(currentHome);
        }
        tabs.forEach(function (button) {
            button.addEventListener("click", function () {
                activate(button, false);
            });
            button.addEventListener("keydown", function (event) {
                var index = tabs.indexOf(button);
                var next = index;
                if (event.key === "ArrowRight") next = (index + 1) % tabs.length;
                else if (event.key === "ArrowLeft") next = (index - 1 + tabs.length) % tabs.length;
                else if (event.key === "Home") next = 0;
                else if (event.key === "End") next = tabs.length - 1;
                else return;
                event.preventDefault();
                activate(tabs[next], true);
            });
        });
    }

    bindSearch();
    bindTabs();
    var requested = typeof contract.readUrlParam === "function" ? contract.readUrlParam("asset") : new URLSearchParams(window.location.search).get("asset");
    loadHome(requested || "");
})();
