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
        HIGH_RISK: "高风险", COOLING: "冷却中", CONFUSED: "冲突待解",
        CONFIRMATION: "确认型", PREPARATION: "预备型", REDUCED: "缩减型", OBSERVATION: "观察", BLOCKED: "阻断",
        LEVEL_1_CONSISTENT: "一致", LEVEL_2_MINOR_DISAGREEMENT: "轻微分歧",
        LEVEL_3_SIGNIFICANT_DISAGREEMENT: "显著分歧", LEVEL_4_EXTREME_CONFLICT: "极端冲突",
        READY: "就绪", PARTIAL: "部分可用", FALLBACK: "规则路径降级", UNAVAILABLE: "当前不可用",
        DISABLED: "数据源未启用", WAITING_SYNC: "等待同步", OK: "正常", UP: "正常", DEGRADED: "降级",
        FOUND: "已发现", NONE_FOUND: "未发现", INSUFFICIENT_DATA: "数据不足",
        SOURCE_UNAVAILABLE: "来源不可用", STALE: "数据已过期",
        COMPLETE: "覆盖完整", PARTIAL_COVERAGE: "覆盖部分", UNKNOWN: "等待评估",
        SYSTEM_PLAN_POSITION: "系统计划录入", MANUAL_POSITION: "手动录入"
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
        return /^[A-Z][A-Z0-9_]*$/.test(raw) ? (fallback || "当前不可查看") : raw;
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
    function validOpportunity(asset) {
        return symbolOf(asset)
            && has(asset && (asset.opportunityId || asset.primaryOpportunityId))
            && has(asset && asset.analysisId)
            && has(asset && asset.opportunityScore)
            && !Number.isNaN(Number(asset.opportunityScore))
            && String(asset.slotType || "").toUpperCase() !== "DEFAULT_SLOT";
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
        setText("headerDataSource", "数据 · " + text(header.dataSourceText, label(header.dataStatus, "状态待同步"))
            + " · AI · " + text(header.aiStatusLabel, label(header.aiStatus, "状态待同步")));
        setText("headerUpdatedAt", has(header.updatedAt) ? "更新于 " + time(header.updatedAt) : "更新时间待同步");
    }

    function renderStatus(home, opportunityCount) {
        var state = home.systemState || {};
        setText("statusMarket", statusValue(state.marketTrend));
        setText("statusRisk", statusValue(state.riskLevel));
        setText("statusQuality", statusValue(state.dataQuality));
        setText("statusAi", text(home.header && home.header.aiStatusLabel, label(home.header && home.header.aiStatus, "等待同步")));
        setText("statusOpportunity", opportunityCount ? opportunityCount + " 个" : "暂无");
        setText("statusReset", statusValue(state.hotReset, "正常"));
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
            alertNode.querySelector("strong").textContent = text(alert.message, "风险状态发生变化");
            alertNode.querySelector("em").textContent = label(alert.level, "高优先级");
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
        var tone = view.tone === "danger" ? " danger" : view.tone === "warning" ? " warning" : view.tone === "muted" ? " muted" : "";
        return '<span class="state-badge' + tone + '">' + escapeHtml(view.label) + "</span>";
    }
    function opportunityCard(asset, finalAccess, selected) {
        var symbol = symbolOf(asset);
        var isSelected = symbol === selected;
        var finalVisible = isSelected && finalAccess.visible;
        var plan = finalAccess.plan;
        var finalBias = finalVisible ? label(plan.finalMarketBias || plan.direction, "—") : "—";
        var finalMode = finalVisible ? label(plan.finalPlanMode, "尚未形成") : "尚未形成";
        var confidence = text(asset.confidenceLabel, label(asset.confidenceLevel, "当前不可查看"));
        var risk = text(asset.riskLabel, label(asset.riskLevel, "当前不可查看"));
        var timeframe = text(asset.primaryTimeframe, "周期待同步");
        return '<article class="opportunity-card' + (isSelected ? " is-selected" : "") + '" tabindex="0" role="button" data-symbol="'
            + escapeHtml(symbol) + '" aria-label="查看 ' + escapeHtml(symbol) + ' 决策上下文"><header><div class="asset-identity"><strong>'
            + escapeHtml(text(asset.name, symbol.replace(/USDT$/, ""))) + "</strong><small>" + escapeHtml(symbol + " · " + timeframe)
            + "</small></div>" + stateBadge(asset) + '</header><div class="opportunity-metrics"><span><small>机会评分</small><strong>'
            + escapeHtml(number(asset.opportunityScore, 0)) + "</strong></span><span><small>置信度</small><strong>" + escapeHtml(confidence)
            + "</strong></span><span><small>风险</small><strong>" + escapeHtml(risk)
            + '</strong></span></div><div class="opportunity-final"><span><small>Final Bias</small><b>' + escapeHtml(finalBias)
            + "</b></span><span><small>Plan Mode</small><b>" + escapeHtml(finalMode) + "</b></span></div></article>";
    }
    function renderOpportunities(home) {
        var all = Array.isArray(home.assets) ? home.assets : [];
        var assets = all.filter(validOpportunity).slice(0, 6);
        var grid = document.getElementById("opportunityGrid");
        var empty = document.getElementById("opportunityEmpty");
        var finalAccess = selectedFinalAccess(home);
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || selectedSymbol;
        setText("opportunityHeading", "机会资产 · " + assets.length);
        grid.innerHTML = assets.map(function (asset) { return opportunityCard(asset, finalAccess, selected); }).join("");
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
        return position && position.markPriceFresh === true
            && ["OPEN_MONITORING", "RISK_ESCALATED", "PLAN_INVALIDATED"].indexOf(String(position.dataState || "").toUpperCase()) >= 0;
    }
    function validPosition(position) {
        return position && has(position.positionId) && symbolOf(position) && has(position.direction)
            && has(position.entryPrice) && has(position.openedAt);
    }
    function riskRank(value) { return { LOW: 1, MEDIUM: 2, HIGH: 3, EXTREME: 4 }[String(value || "").toUpperCase()] || 0; }
    function highestRisk(positions) {
        var trusted = positions.filter(trustedMonitor).sort(function (a, b) { return riskRank(b.riskLevel) - riskRank(a.riskLevel); });
        return trusted.length ? text(trusted[0].riskLevelLabel, label(trusted[0].riskLevel, "暂无评估")) : "暂无评估";
    }
    function fact(labelText, value) { return "<span><small>" + escapeHtml(labelText) + "</small><b>" + escapeHtml(value) + "</b></span>"; }
    function positionRow(position) {
        var trusted = trustedMonitor(position);
        var monitorState = label(position.dataState, "等待监控数据");
        var risk = trusted ? text(position.riskLevelLabel, label(position.riskLevel)) : "当前不可查看";
        var conclusion = trusted ? text(position.monitorConclusionLabel, label(position.monitorConclusion)) : monitorState;
        var action = trusted ? text(position.suggestedManualActionText, label(position.suggestedAction)) : "等待可信监控结果";
        return '<article class="position-row"><div class="position-primary"><strong>' + escapeHtml(symbolOf(position)) + "<b>"
            + escapeHtml(text(position.directionLabel, label(position.direction))) + "</b></strong><small>"
            + escapeHtml(label(position.sourceType, "已录入持仓")) + " · " + escapeHtml(time(position.openedAt))
            + '</small></div><div class="position-judgment">' + fact("持仓风险", risk) + fact("监控结论", conclusion) + fact("建议动作", action)
            + '</div><div class="position-facts">' + fact("开仓价", number(position.entryPrice))
            + fact("标记价格", trusted ? number(position.markPrice) : "当前不可查看")
            + fact("盈亏", trusted ? percent(position.pnlPercent) : "当前不可查看")
            + fact("开仓时间", time(position.openedAt))
            + '</div><div class="monitor-details">' + fact("入场逻辑状态", trusted ? text(position.entryLogicStatusLabel, label(position.entryLogicStatus)) : "等待监控")
            + fact("反转状态", trusted ? text(position.reversalStatusLabel, label(position.reversalStatus)) : "等待监控")
            + fact("风险变化原因", trusted ? text(position.riskReasonLabel, label(position.riskReason)) : "等待监控")
            + fact("最近监控时间", trusted ? time(position.lastMonitorTime || position.lastMonitorAt) : "等待首次监控")
            + '</div><a class="position-detail-link" href="/positions/' + encodeURIComponent(position.positionId) + '">详情</a></article>';
    }
    function renderPositions(home) {
        var positions = (Array.isArray(home.positions) ? home.positions : []).filter(validPosition);
        var shown = positions.slice(0, 3);
        var list = document.getElementById("positionList");
        var empty = document.getElementById("positionEmpty");
        list.innerHTML = shown.map(positionRow).join("");
        list.hidden = shown.length === 0;
        empty.hidden = shown.length !== 0;
        var coverage = label(home.diagnostics && home.diagnostics.accountRiskCoverageState, "等待评估");
        setText("positionAggregate", "活动 " + positions.length + " · 最高风险 " + highestRisk(positions) + " · " + coverage);
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
            target.innerHTML = '<div class="plan-empty"><strong>尚未形成</strong><span>机会状态 · '
                + escapeHtml(selectedOpportunityState(home)) + "</span><span>" + escapeHtml(text(access.reason, "当前没有通过规则校验的 Final Execution Plan。")) + "</span></div>";
            link.hidden = true;
            return;
        }
        var planId = plan.sourceExecutionPlanId;
        target.innerHTML = '<div class="plan-decision"><div><small>最终方向 · 计划模式</small><strong>'
            + escapeHtml(label(plan.finalMarketBias || plan.direction)) + " · " + escapeHtml(label(plan.finalPlanMode))
            + '</strong></div><span class="plan-state">' + escapeHtml(text(plan.statusLabel, "当前有效")) + '</span></div><div class="plan-conditions">'
            + planField("入场 / 触发", plan.entryZone || plan.triggerCondition)
            + planField("失效条件", plan.invalidCondition || plan.abandonCondition)
            + planField("止损", plan.stopZone || plan.stopLogic || plan.stopLoss)
            + planField("目标", plan.targetZones || plan.targetLogic || plan.takeProfitRules)
            + planField("杠杆", plan.leverageSuggestion) + planField("仓位", plan.positionSuggestion)
            + '</div><div class="plan-metadata">' + planField("有效期", plan.validPeriod || (has(plan.expiresAt) ? time(plan.expiresAt) : null))
            + planField("版本 / 来源", planId) + "</div>";
        link.href = "/plans/" + encodeURIComponent(planId);
        link.hidden = false;
    }

    function collectionLabel(state) {
        return typeof contract.collectionStateLabel === "function" ? contract.collectionStateLabel(state) : label(state, "来源不可用");
    }
    function itemText(item) {
        if (!has(item)) return "";
        if (typeof item !== "object") return label(item, text(item));
        return label(item.text || item.summary || item.hypothesis || item.currentValue || item.reason || item.description || item.source, "");
    }
    function list(items, emptyState) {
        var values = (Array.isArray(items) ? items : []).map(itemText).filter(Boolean).slice(0, 3);
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
    function renderGpt(role) {
        var core = role.coreJudgment || {};
        var candidate = role.candidateSummary || {};
        var multi = role.multiTimeframeExplanation || {};
        var why = text(core.text, text(role.decisionSummary, "当前形成原因不可查看"));
        return '<div class="ai-first-visual"><div class="primary"><small>GPT Candidate · 非 Final</small><strong>'
            + escapeHtml(text(candidate.summary, why)) + "</strong></div><div><small>Market Bias</small><strong>"
            + escapeHtml(label(core.marketBias, "当前不可查看")) + "</strong></div><div><small>Opportunity State · Candidate Mode</small><strong>"
            + escapeHtml(label(core.opportunityState, "当前不可查看") + " · " + label(candidate.planMode, "当前不可查看"))
            + '</strong></div></div><div class="ai-content-grid"><section class="ai-section"><h3>形成原因</h3><p>' + escapeHtml(why)
            + "</p>" + dl([["4h", label(multi["4h"], "暂无数据")], ["1h", label(multi["1h"], "暂无数据")], ["15m", label(multi["15m"], "暂无数据")], ["5m", label(multi["5m"], "暂无数据")]])
            + '</section><section class="ai-section"><h3>证据 · ' + escapeHtml(collectionLabel(role.supportingEvidenceState)) + "</h3>"
            + list(role.supportingEvidence, role.supportingEvidenceState) + '<h3>反对证据 · ' + escapeHtml(collectionLabel(role.opposingEvidenceState)) + "</h3>"
            + list(role.opposingEvidence, role.opposingEvidenceState) + '</section></div><div class="ai-summary-footer"><strong>Candidate 摘要</strong><span>'
            + escapeHtml(text(candidate.summary || candidate.recommendedAction, "当前候选摘要不可查看")) + "</span></div>";
    }
    function renderGemini(role) {
        var suggestion = role.downgradeSuggestion || {};
        var findings = [].concat(role.evidenceGaps || [], role.logicConflicts || [], role.underestimatedRisks || []);
        return '<div class="ai-first-visual"><div class="primary"><small>复核结果</small><strong>' + escapeHtml(label(role.reviewResult, "当前不可查看"))
            + "</strong></div><div><small>调整建议</small><strong>" + escapeHtml(label(role.planModeAdjustment || suggestion.after, "当前不可查看"))
            + "</strong></div><div><small>对 Candidate</small><strong>" + escapeHtml(text(suggestion.reason || role.finalDirectionImpact, "建议待同步"))
            + '</strong></div></div><div class="ai-content-grid gemini"><section class="ai-section"><h3>Before → After</h3>'
            + dl([["Before", label(suggestion.before, "当前不可查看")], ["After", label(suggestion.after, "当前不可查看")], ["置信度", label(role.confidenceAdjustment, "当前不可查看")], ["风险", label(role.riskAdjustment, "当前不可查看")]])
            + '</section><section class="ai-section"><h3>证据缺口 · 逻辑冲突 · 风险低估</h3>' + list(findings, role.evidenceGapsState || role.logicConflictsState || role.underestimatedRisksState)
            + '</section></div><div class="ai-summary-footer"><strong>恢复条件</strong><span>' + escapeHtml(text(role.recoveryCondition || suggestion.recoveryCondition, "当前无可验证恢复条件")) + "</span></div>";
    }
    function renderGrok(role) {
        return '<div class="ai-first-visual"><div class="primary"><small>失败路径</small><strong>' + escapeHtml(collectionLabel(role.failurePathState))
            + "</strong></div><div><small>当前方向挑战</small><strong>" + escapeHtml(text(role.currentDirectionChallenge, "当前不可查看"))
            + "</strong></div><div><small>计划模式影响</small><strong>" + escapeHtml(label(role.planModeImpact, "当前不可查看"))
            + '</strong></div></div><div class="ai-content-grid grok"><section class="ai-section"><h3>反向情景</h3>' + list(role.opposingScenarios, role.opposingScenariosState)
            + '<h3>外部事件风险</h3>' + list(role.externalEventRisks, role.externalEventRisksState)
            + '</section><section class="ai-section"><h3>微观结构风险</h3>' + list(role.microstructureRisks, role.microstructureRisksState)
            + '<h3>继续观察指标</h3>' + list(role.watchIndicators, role.watchIndicatorsState) + "</section></div>";
    }
    function renderConflict(home) {
        var consistency = home.aiDecision && home.aiDecision.consistency || {};
        var level = String(consistency.conflictLevel || "").toUpperCase();
        var ready = String(consistency.dataState || "").toUpperCase() === "READY";
        var show = ready && level && level !== "LEVEL_1_CONSISTENT";
        var target = document.getElementById("conflictSummary");
        var layout = document.getElementById("aiLayout");
        target.hidden = !show;
        layout.classList.toggle("has-conflict", show);
        if (!show) { target.innerHTML = ""; return; }
        target.innerHTML = '<h3>Conflict Summary</h3>' + dl([
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
        if (!role || role.resultAvailable !== true) panel.innerHTML = roleUnavailable(role);
        else if (activeRole === "GPT_FINAL") panel.innerHTML = renderGpt(role);
        else if (activeRole === "GEMINI_REVIEW") panel.innerHTML = renderGemini(role);
        else panel.innerHTML = renderGrok(role);
        setText("aiMetadata", role ? "角色状态 " + label(role.roleState, "当前不可用") + " · 生成时间 " + time(role.generatedAt) + " · 来源 " + text(role.provider, "当前不可查看") : "角色状态待同步");
        var trace = role && role.traceId;
        var analysis = role && role.analysisId;
        var audit = document.getElementById("auditChainLink");
        audit.href = trace ? "/audit/" + encodeURIComponent(trace) : analysis ? "/analysis/" + encodeURIComponent(analysis) : "/analysis";
        renderConflict(home);
    }

    function render(home) {
        currentHome = home || {};
        selectedSymbol = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || selectedSymbol;
        renderHeader(home);
        renderSignals(home);
        var count = renderOpportunities(home);
        renderStatus(home, count);
        renderPositions(home);
        renderPlan(home);
        renderAi(home);
    }
    async function loadHome(symbol) {
        try {
            var query = new URLSearchParams({ limit: "6" });
            if (symbol) query.set("selectedSymbol", symbol);
            render(await api("/api/dashboard/home?" + query.toString()));
        } catch (error) {
            announce(error.message);
            render({ states: { overall: "ERROR" }, diagnostics: {}, assets: [], positions: [], aiDecision: { tabs: [] } });
            setText("headerDataSource", "当前不可查看");
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
            stateNode.textContent = "请先从搜索结果中选择";
            previewButton.disabled = true;
            addButton.disabled = true;
            addButton.textContent = "加入观察资产池";
            return;
        }
        var symbol = symbolOf(selectedSearchAsset);
        var inPool = assetPoolSymbols.has(symbol);
        symbolNode.textContent = symbol;
        stateNode.textContent = text(selectedSearchAsset.baseAsset || selectedSearchAsset.name, symbol.replace(/USDT$/, ""))
            + " · " + (inPool ? "已在观察资产池" : "尚未加入观察资产池");
        previewButton.disabled = searchActionBusy;
        addButton.disabled = searchActionBusy || inPool;
        addButton.textContent = inPool ? "已在观察资产池" : "加入观察资产池";
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
                + "</small></span><em>" + (inPool ? "已在观察资产池" : "未加入") + "</em></button>";
        }).join("");
        if (!target.innerHTML) target.innerHTML = '<div class="search-result"><span><strong>未找到资产</strong><small>请尝试其他名称或交易对</small></span></div>';
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
            renderSearchSelection("正在创建按需分析…");
            try {
                var result = await api("/api/asset-pool/search/" + encodeURIComponent(symbolOf(selectedSearchAsset)) + "/analysis-preview?timeframe=5m", { method: "POST" });
                if (!result || !result.analysisId) throw new Error("预览未返回分析标识");
                window.location.assign("/analysis/" + encodeURIComponent(result.analysisId));
            } catch (error) {
                searchActionBusy = false;
                renderSearchSelection(error.message);
                announce(error.message);
            }
        });
        document.getElementById("homeAddAsset").addEventListener("click", async function () {
            if (!selectedSearchAsset || searchActionBusy || assetPoolSymbols.has(symbolOf(selectedSearchAsset))) return;
            searchActionBusy = true;
            renderSearchSelection("正在加入观察资产池…");
            try {
                await api("/api/asset-pool", { method: "POST", body: JSON.stringify({ symbol: symbolOf(selectedSearchAsset), focusEnabled: true }) });
                await loadAssetPoolMembership();
                searchActionBusy = false;
                renderSearchSelection("已加入观察资产池，将进入正常持续分析流程");
                renderSearchResults(searchResultItems);
                announce(symbolOf(selectedSearchAsset) + " 已加入观察资产池");
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
        document.querySelectorAll("[data-ai-role]").forEach(function (button) {
            button.addEventListener("click", function () {
                activeRole = button.dataset.aiRole;
                document.querySelectorAll("[data-ai-role]").forEach(function (item) {
                    var selected = item === button;
                    item.classList.toggle("is-active", selected);
                    item.setAttribute("aria-selected", String(selected));
                });
                renderAi(currentHome);
            });
        });
    }

    bindSearch();
    bindTabs();
    var requested = typeof contract.readUrlParam === "function" ? contract.readUrlParam("asset") : new URLSearchParams(window.location.search).get("asset");
    loadHome(requested || "");
})();
