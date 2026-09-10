(function () {
    "use strict";

    // Shared, read-only desktop presentation. No provider calls or business calculations.
    var riskNames = Object.freeze({ CHASE_RISK: "追高风险", RAPID_MOVE_RISK: "急涨急跌风险",
        TREND_REVERSAL_RISK: "趋势反转风险", CROWDING_RISK: "拥挤风险", LIQUIDATION_RISK: "清算风险",
        LIQUIDITY_RISK: "流动性风险", EVENT_RISK: "事件风险", DATA_RISK: "数据风险" });
    function escape(value) {
        return String(value == null ? "" : value).replace(/[&<>"']/g, function (c) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
        });
    }
    function utcDate(value) {
        if (!value) return null;
        // Legacy LocalDateTime fields are UTC on the server; do not interpret them as device time.
        var raw = String(value);
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(raw) && !/(Z|[+-]\d{2}:?\d{2})$/i.test(raw)) raw += "Z";
        var date = new Date(raw);
        return Number.isNaN(date.getTime()) ? null : date;
    }
    function beijingTime(value, short) {
        var date = utcDate(value);
        if (!date) return "尚无记录";
        var options = { timeZone: "Asia/Shanghai", hourCycle: "h23", hour: "2-digit", minute: "2-digit" };
        if (!short) { options.month = "2-digit"; options.day = "2-digit"; }
        return new Intl.DateTimeFormat("zh-CN", options).format(date);
    }
    function directionTimeLabel(value) {
        var date = utcDate(value);
        if (!date) return "";
        var parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai", year: "numeric",
            month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).formatToParts(date);
        var fields = {}; parts.forEach(function (part) { fields[part.type] = part.value; });
        return "方向计算时间：" + fields.year + "-" + fields.month + "-" + fields.day + " "
            + fields.hour + ":" + fields.minute + "（北京时间）";
    }
    function fullBeijingTime(value) {
        var label = directionTimeLabel(value);
        return label ? label.replace("方向计算时间：", "").replace("（北京时间）", " 北京时间 UTC+08:00") : "尚无记录";
    }
    function serviceTime(value) {
        var date = utcDate(value);
        return date ? '<time datetime="' + escape(date.toISOString()) + '" title="'
            + escape(fullBeijingTime(value)) + '">' + beijingTime(value) + '</time>' : '<span>尚无记录</span>';
    }
    function priceText(value) {
        if (value == null || value === "" || !Number.isFinite(Number(value))) return "价格待同步";
        // Preserve API precision; the payload does not yet provide the exchange tick size.
        return new Intl.NumberFormat("en-US", { maximumFractionDigits: 20 }).format(Number(value));
    }
    function confidenceText(asset) {
        var value = asset && asset.finalConfidence;
        return typeof value === "number" && Number.isFinite(value) && value >= 0 && value <= 100
            ? value + "%" : "—";
    }
    function pinnedObservationLabel(asset) {
        if (!asset || asset.homePinned !== true) return "";
        var states = [];
        var risk = { HIGH: "高风险", EXTREME: "极高风险" }[asset.riskLevel];
        if (risk) states.push(risk);
        var state = { HIGH_RISK: "高风险观察", INVALIDATED: "已失效", COOLING: "冷却中", CONFUSED: "信号混乱", BLOCKED: "已阻断" }[asset.opportunityState];
        if (state) states.push(state);
        if ((asset.finalPlanMode === "BLOCKED" || asset.planMode === "BLOCKED") && asset.opportunityState !== "BLOCKED") states.push("计划阻断");
        return "置顶观察" + (states.length ? " · " + states.join(" · ") : "");
    }
    function priceCaption(asset) {
        if (!asset || asset.latestPrice == null) return "";
        var basis = asset.priceBasis === "LIVE" ? "实时价" : asset.priceBasis === "CLOSED_5M" ? "最近闭线价" : "价格来源待确认";
        return '<small class="asset-price-caption" title="' + escape(basis + ' · ' + (asset.latestPriceSource || '')
            + ' · ' + fullBeijingTime(asset.latestPriceAt)) + '">' + basis
            + (asset.latestPriceAt ? ' <time datetime="' + escape(asset.latestPriceAt) + '">'
                + beijingTime(asset.latestPriceAt, true) + '</time>' : '') + '</small>';
    }
    function planPriceText(value, asset) {
        var precision = asset && asset.pricePrecision;
        if (!Number.isInteger(precision) || precision < 0 || precision > 20
                || !(Number(asset.tickSize) > 0) || !asset.priceMetadataSource) return String(value == null ? "" : value);
        // Format only price fields; never mutate the source plan or infer symbol-specific precision.
        return String(value).replace(/[0-9]+(?:\.[0-9]+)?/g, function (price) {
            return new Intl.NumberFormat("en-US", { minimumFractionDigits: precision,
                maximumFractionDigits: precision }).format(Number(price));
        });
    }
    function riskClass(level) {
        return { LOW: "risk-level-low", MEDIUM: "risk-level-medium", HIGH: "risk-level-high",
            EXTREME: "risk-level-extreme" }[String(level || "").toUpperCase()] || "risk-level-unknown";
    }
    function poolTimeframes(asset) {
        function timeframe(value, prefix) {
            var detail = String(value || prefix + "数据不足").replace(new RegExp("^" + prefix + "\\s*"), "");
            return prefix + " " + detail;
        }
        return timeframe(asset && asset.oneHourOpportunityLabel, "1小时") + " · "
            + timeframe(asset && asset.fourHourTrendLabel, "4小时");
    }
    function riskLevel(level) {
        return { LOW: "低", MEDIUM: "中", HIGH: "高", EXTREME: "极高" }[String(level || "").toUpperCase()] || "待评估";
    }
    function riskRows(asset) {
        var items = Array.isArray(asset && asset.riskItems) ? asset.riskItems : [];
        return Object.keys(riskNames).map(function (type) {
            var item = items.find(function (value) { return value.riskType === type; });
            return { type: type, name: riskNames[type], item: item || null };
        });
    }
    function riskSummary(asset) {
        var rank = { LOW: 1, MEDIUM: 2, HIGH: 3, EXTREME: 4 };
        var items = riskRows(asset).filter(function (row) {
            return row.item && row.item.evidenceStatus === "AVAILABLE" && rank[row.item.severity];
        }).sort(function (a, b) {
            return rank[b.item.severity] - rank[a.item.severity];
        });
        if (!items.length) {
            var completeWithoutRisk = riskRows(asset).every(function (row) {
                return row.item && row.item.evidenceStatus === "AVAILABLE" && row.item.severity === "NONE";
            });
            return completeWithoutRisk ? "" : '<span class="risk-pending-copy">风险待评估</span>';
        }
        var row = items[0];
        return '<span class="risk-summary-line"><span class="risk-type-copy">' + escape(row.name.replace(/风险$/, ""))
            + '</span><span class="risk-separator">·</span><span class="' + riskClass(row.item.severity) + '">'
            + escape(riskLevel(row.item.severity)) + '</span>'
            + (items.length > 1 ? '<span class="risk-count">+' + (items.length - 1) + '</span>' : '') + '</span>';
    }
    function hasConfirmedRisks(asset) {
        return riskRows(asset).some(function (row) {
            return row.item && row.item.evidenceStatus === "AVAILABLE"
                && ["LOW", "MEDIUM", "HIGH", "EXTREME"].indexOf(row.item.severity) >= 0;
        });
    }
    function riskDrawer(asset) {
        if (!hasConfirmedRisks(asset)) return "";
        var rows = riskRows(asset), confirmed = rows.filter(function (row) {
            return row.item && row.item.evidenceStatus === "AVAILABLE"
                && ["LOW", "MEDIUM", "HIGH", "EXTREME"].indexOf(row.item.severity) >= 0;
        });
        return '<h3>' + escape(asset.rawSymbol || asset.symbol) + ' · 风险详情</h3><p class="drawer-timezone">风险观察时间 · 北京时间 UTC+08:00</p>'
            + confirmed.map(function (row) {
                var item = row.item;
                return '<section class="risk-evidence-item"><header><strong class="risk-type-copy">'
                    + escape(row.name) + '</strong><span class="' + riskClass(item.severity) + '">'
                    + riskLevel(item.severity) + '</span></header><p>证据完整 · 指标值：'
                    + escape(item.currentValue == null ? "未记录" : item.currentValue) + '</p><p>'
                    + escape(item.primaryEvidence || "未提供证据说明") + '</p><small>'
                    + escape(item.source || "尚无来源记录") + ' · ' + beijingTime(item.observedAt) + '</small></section>';
            }).join("");
    }
    function riskDataStatus(asset) {
        if (hasConfirmedRisks(asset) || !riskSummary(asset)) return "";
        var reasons = [];
        riskRows(asset).forEach(function (row) {
            var reason = row.item && row.item.missingReason;
            if (reason && reasons.indexOf(reason) < 0) reasons.push(reason);
        });
        if (!reasons.length) reasons.push(asset && asset.analysisId
            ? "本轮尚未记录完整的独立风险证据" : "该资产尚无成功分析，风险无法判断");
        return '<h3>风险数据状态</h3><p>' + reasons.map(escape).join('；') + '</p>';
    }
    function coreProviders(home) {
        var providers = home && home.diagnostics && home.diagnostics.providerReadiness && home.diagnostics.providerReadiness.providers || [];
        var names = { BINANCE_PUBLIC_MARKET_DATA: "Binance", COINGLASS: "CoinGlass", OPENAI: "OpenAI/GPT", GEMINI: "Gemini", XAI: "xAI/Grok" };
        return Object.keys(names).map(function (name) {
            var provider = providers.find(function (item) { return item.name === name; }) || { name: name };
            var raw = String(provider.status || "").toUpperCase();
            var state = ["FAIL_CLOSED", "UNAVAILABLE", "ERROR", "AUTH_FAILED", "DISABLED", "NOT_CONFIGURED"].indexOf(raw) >= 0 ? "不可用"
                : ["STALE", "DEGRADED", "PARTIAL"].indexOf(raw) >= 0 || provider.freshness === "STALE" ? "延迟"
                : provider.connected === true || ["READY", "CONNECTED"].indexOf(raw) >= 0 ? "正常"
                : ["CHECKING", "RUNNING", "STARTED"].indexOf(raw) >= 0 ? "检查中"
                : provider.lastSuccessAt ? "延迟" : "尚未运行";
            if (name === "COINGLASS" && provider.runtimeState) {
                state = { NOT_STARTED: "尚未运行", RUNNING: "检查中", FRESH: "正常", STALE: "延迟",
                    ERROR: "不可用", RATE_LIMITED: "限流", DISABLED: "不可用" }[provider.runtimeState] || "尚未运行";
            }
            return { name: names[name], state: state, provider: provider };
        }).sort(function (a, b) {
            return ["不可用", "限流", "延迟", "检查中", "尚未运行", "正常"].indexOf(a.state)
                - ["不可用", "限流", "延迟", "检查中", "尚未运行", "正常"].indexOf(b.state);
        });
    }
    function serviceSummary(home) {
        var services = coreProviders(home);
        var abnormal = services.filter(function (row) { return row.state !== "正常"; });
        return (5 - abnormal.length) + "/5" + (abnormal.length ? " · " + abnormal.map(function (row) {
            return row.name.replace("OpenAI/", "").replace("xAI/", "") + row.state;
        }).join("、") : " · 正常");
    }
    function serviceDrawer(home) {
        return '<h3>核心服务</h3><p class="drawer-timezone">北京时间 UTC+08:00 · 只读</p>'
            + '<table class="desktop-service-table"><thead><tr><th>服务</th><th>状态</th><th>最近尝试 / 成功</th><th>数据时间 / 年龄</th><th>当前影响</th><th>下次检查</th></tr></thead><tbody>'
            + coreProviders(home).map(function (row) {
                var p = row.provider, dataTime = p.name === "COINGLASS" ? p.providerDataAt : p.lastSuccessAt;
                var date = utcDate(dataTime);
                var age = date ? Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000)) + "分钟" : "尚无数据时间";
                var color = row.state === "不可用" ? "risk-level-high" : ["延迟", "限流"].indexOf(row.state) >= 0
                    ? "risk-level-medium" : row.state === "正常" ? "risk-level-low" : "risk-level-unknown";
                return '<tr><th scope="row">' + row.name + '</th><td><span class="service-state ' + color + '">'
                    + row.state + '</span></td><td>' + serviceTime(p.lastAttemptAt) + '<br><small>成功 ' + serviceTime(p.lastSuccessAt)
                    + '</small></td><td>' + serviceTime(dataTime) + '<br><small>' + age + '</small></td><td>'
                    + escape(p.impact || "尚无影响记录") + '</td><td>' + (p.nextCheckAt ? serviceTime(p.nextCheckAt) : "尚未计划") + '</td></tr>';
            }).join("") + '</tbody></table>';
    }
    function installHoverDrawers(resolve) {
        if (window.__trineDesktopHoverController) return window.__trineDesktopHoverController;
        var opened = null, pending = null, openTimer = null, closeTimer = null;
        var drawer = document.createElement("section");
        drawer.className = "desktop-hover-drawer";
        drawer.id = "desktopHoverDrawer";
        drawer.hidden = true;
        drawer.tabIndex = -1;
        drawer.setAttribute("role", "dialog");
        document.body.appendChild(drawer);
        function cancelOpen() { clearTimeout(openTimer); openTimer = null; pending = null; }
        function close() {
            cancelOpen(); clearTimeout(closeTimer);
            if (opened) opened.setAttribute("aria-expanded", "false");
            opened = null; drawer.hidden = true;
        }
        function open(trigger) {
            cancelOpen(); clearTimeout(closeTimer);
            var content = resolve(trigger);
            if (!content) return;
            if (opened && opened !== trigger) opened.setAttribute("aria-expanded", "false");
            opened = trigger;
            drawer.innerHTML = content;
            drawer.dataset.kind = trigger.dataset.desktopHover;
            drawer.setAttribute("aria-label", trigger.dataset.desktopHover === "service" ? "核心服务状态" : "资产风险详情");
            trigger.setAttribute("aria-controls", drawer.id); trigger.setAttribute("aria-expanded", "true");
            drawer.hidden = false;
            var rect = trigger.getBoundingClientRect();
            drawer.style.left = Math.max(12, Math.min(rect.left, window.innerWidth - drawer.offsetWidth - 12)) + "px";
            drawer.style.top = Math.max(12, rect.bottom + 8 + drawer.offsetHeight <= window.innerHeight
                ? rect.bottom + 8 : rect.top - drawer.offsetHeight - 8) + "px";
        }
        function leave(event) {
            var related = event.relatedTarget;
            if (related && pending && pending.contains(related)) return;
            var region = opened;
            if (related && (drawer.contains(related) || region && region.contains(related))) return;
            cancelOpen(); clearTimeout(closeTimer); closeTimer = setTimeout(close, 300);
        }
        document.addEventListener("pointerover", function (event) {
            var trigger = event.target.closest("[data-desktop-hover]");
            var region = opened;
            if (drawer.contains(event.target) || region && region.contains(event.target)) clearTimeout(closeTimer);
            if (!trigger || trigger === opened || trigger === pending) return;
            cancelOpen(); pending = trigger; openTimer = setTimeout(function () { open(trigger); }, 250);
        });
        document.addEventListener("pointerout", function (event) {
            if (event.target.closest("[data-desktop-hover], .opportunity-card, .desktop-hover-drawer, .service-status-cell")) leave(event);
        });
        document.addEventListener("focusin", function (event) {
            var trigger = event.target.closest("[data-desktop-hover]");
            if (trigger) open(trigger);
        });
        document.addEventListener("focusout", leave);
        document.addEventListener("keydown", function (event) { if (event.key === "Escape") close(); });
        drawer.addEventListener("pointerenter", function () { clearTimeout(closeTimer); });
        document.addEventListener("click", function (event) {
            if (event.target.closest("[data-desktop-hover], .desktop-hover-drawer")) event.stopPropagation();
        }, true);
        document.addEventListener("keydown", function (event) {
            if ((event.key === "Enter" || event.key === " ") && event.target.closest("[data-desktop-hover], .desktop-hover-drawer")) event.stopPropagation();
        }, true);
        window.__trineDesktopHoverController = { close: close };
        return window.__trineDesktopHoverController;
    }
    window.TrineDesktopSemantics = Object.freeze({ refreshPolicy: Object.freeze({ reconcileMs: 60000, disconnectedMs: 15000 }), beijingTime: beijingTime, fullBeijingTime: fullBeijingTime, directionTimeLabel: directionTimeLabel, priceText: priceText, confidenceText: confidenceText, planPriceText: planPriceText, riskSummary: riskSummary,
        pinnedObservationLabel: pinnedObservationLabel, priceCaption: priceCaption, riskDrawer: riskDrawer, riskDataStatus: riskDataStatus, hasConfirmedRisks: hasConfirmedRisks, poolTimeframes: poolTimeframes, serviceSummary: serviceSummary, serviceDrawer: serviceDrawer, installHoverDrawers: installHoverDrawers });
})();

/* Desktop Home runtime */
(function () {
    "use strict";

    if (document.body.dataset.pageKey !== "home") return;
    var desktop = window.TrineDesktopSemantics;
    var homeCardSymbols = [];

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
    var assetAnalysisBusy = new Set();
    var activeClosePositionId = "";
    var activeArchivePositionId = "";
    var homeEventSource = null;
    var homeFallbackTimer = null;
    var homePollIntervalMs = 0;
    var homeStreamConnected = false;
    var homeRuntimeStarted = false;
    var homeLiveState = "连接中";
    var homeRequestFailed = false;
    var liveSnapshotVersions = new Map();
    var homeRequestSequence = 0;
    var homeAbortController = null;
    var homeRefreshQueued = false;
    var assetCardSnapshots = new Map();
    var assetCardFieldVersions = new Map();
    var assetCardPriceTimers = new Map();
    var assetCardAbortController = null;
    var assetCardRequestSequence = 0;
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
        CONNECTED: "已连接", CONFIGURED: "已配置", NOT_CONFIGURED: "未配置", FAIL_CLOSED: "已阻断",
        DISABLED: "数据源未启用", WAITING_SYNC: "等待同步", OK: "正常", UP: "正常", DEGRADED: "降级",
        FOUND: "已发现", NONE_FOUND: "未发现", INSUFFICIENT_DATA: "数据不足",
        SOURCE_UNAVAILABLE: "来源不可用", STALE: "数据已过期",
        COMPLETE: "覆盖完整", PARTIAL_COVERAGE: "覆盖部分", UNKNOWN: "等待评估",
        SYSTEM_PLAN_POSITION: "系统计划", MANUAL_POSITION: "独立录入", MANUAL_INDEPENDENT: "独立录入",
        BINANCE: "Binance",
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
        return /^[A-Z][A-Z0-9_]*$/.test(raw) ? (fallback || "当前不可查看") : raw;
    }
    function humanReason(value, fallback) {
        var raw = Array.isArray(value) ? value.join("；") : value;
        var normalized = String(raw || "").trim().toUpperCase();
        var reasons = {
            ANALYSIS_PREVIEW_NON_FINAL: "规则参考计划尚未通过 Final 校验，当前不可执行",
            VALIDATED_MARKET_BIAS_REQUIRED: "方向尚未通过完整规则校验",
            BOUNDARY_INCOMPLETE: "计划边界尚未完整形成",
            PLAN_BLOCKED: "当前风险或规则条件不允许执行",
            DERIVATIVES_STALE: "衍生品补充数据已过期，等待自动刷新",
            SOURCE_UNAVAILABLE: "数据来源暂不可用",
            INSUFFICIENT_DATA: "形成判断所需的数据不足",
            MULTI_TIMEFRAME_CONFLICT: "1小时与4小时方向相反",
            TIMEFRAME_CONFLICT: "1小时与4小时方向相反",
            BUDGET_EXHAUSTED: "今日AI额度已用完"
        };
        if (reasons[normalized]) return reasons[normalized];
        if (!normalized || /^[A-Z][A-Z0-9_]*(?:[,|:][A-Z0-9_]+)*$/.test(normalized)) {
            return fallback || "当前条件尚未满足，等待下一次完整更新";
        }
        return String(raw);
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
        return desktop.beijingTime(value, false);
    }
    function clockTime(value) {
        return desktop.beijingTime(value, true);
    }
    function symbolOf(asset) {
        var raw = text(asset && (asset.rawSymbol || asset.symbol), "").trim().toUpperCase();
        return /^[A-Z0-9][A-Z0-9._:/-]{1,31}$/.test(raw) ? raw : "";
    }
    function announce(message) { setText("homeLiveRegion", message || ""); }
    function reportHomeRequestFailure(error) {
        homeRequestFailed = true;
        announce("更新失败，正在重试。已保留上一份完整数据。"
            + (error && error.message ? " " + error.message : ""));
    }
    function clearHomeRequestFailure() {
        if (!homeRequestFailed) return;
        homeRequestFailed = false;
        announce("");
    }
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
        var payload = await response.json().catch(function () { return null; });
        if (!response.ok) {
            var failure = new Error(text(payload && payload.msg, "请求失败（" + response.status + "）"));
            failure.status = response.status;
            throw failure;
        }
        return apiData(payload);
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
    function semanticClass(value) {
        var normalized = String(value || "").trim().toUpperCase();
        if (normalized === "STRONG_BULLISH" || normalized === "STRONG_LONG") return " semantic-strong-bullish";
        if (["BULLISH", "LONG"].indexOf(normalized) >= 0) return " semantic-bullish";
        if (["WEAK_BULLISH", "WEAK_LONG"].indexOf(normalized) >= 0) return " semantic-weak-bullish";
        if (["WAIT", "RANGE", "NEUTRAL", "STALE", "SOURCE_UNAVAILABLE", "INSUFFICIENT_DATA", "UNKNOWN", "NEVER_SCANNED"].indexOf(normalized) >= 0) return " semantic-neutral";
        if (normalized === "STRONG_BEARISH" || normalized === "STRONG_SHORT") return " semantic-strong-bearish";
        if (["BEARISH", "SHORT"].indexOf(normalized) >= 0) return " semantic-bearish";
        if (["WEAK_BEARISH", "WEAK_SHORT"].indexOf(normalized) >= 0) return " semantic-weak-bearish";
        if (["ANALYZING", "STARTED", "QUEUED", "IN_PROGRESS", "RUNNING"].indexOf(normalized) >= 0) return " semantic-analyzing";
        return " semantic-neutral";
    }
    function directionSemanticClass(value) {
        var normalized = String(value || "").trim().toUpperCase();
        if (normalized === "STRONG_BULLISH" || normalized === "STRONG_LONG") return " semantic-strong-bullish";
        if (["BULLISH", "LONG"].indexOf(normalized) >= 0) return " semantic-bullish";
        if (["WEAK_BULLISH", "WEAK_LONG"].indexOf(normalized) >= 0) return " semantic-weak-bullish";
        if (["WAIT", "RANGE", "NEUTRAL"].indexOf(normalized) >= 0) return " semantic-neutral";
        if (normalized === "STRONG_BEARISH" || normalized === "STRONG_SHORT") return " semantic-strong-bearish";
        if (["BEARISH", "SHORT"].indexOf(normalized) >= 0) return " semantic-bearish";
        if (["WEAK_BEARISH", "WEAK_SHORT"].indexOf(normalized) >= 0) return " semantic-weak-bearish";
        if (["ANALYZING", "STARTED", "QUEUED", "IN_PROGRESS", "RUNNING"].indexOf(normalized) >= 0) return " semantic-analyzing";
        if (["UNKNOWN", "SOURCE_UNAVAILABLE", "INSUFFICIENT_DATA", "STALE", "NEVER_SCANNED", "TIMEFRAME_CONFLICT", "MULTI_TIMEFRAME_CONFLICT"].indexOf(normalized) >= 0) return " semantic-unavailable";
        return " semantic-unavailable";
    }
    function riskSemanticClass(value) {
        var normalized = String(value || "").trim().toUpperCase();
        if (normalized === "EXTREME") return " risk-level-extreme";
        if (normalized === "HIGH") return " risk-level-high";
        if (normalized === "MEDIUM") return " risk-level-medium";
        if (normalized === "LOW") return " risk-level-low";
        return " risk-level-unknown";
    }
    function toneText(value, raw) {
        return '<span class="semantic-value tone-' + semanticTone(raw) + semanticClass(raw) + '">' + escapeHtml(value) + "</span>";
    }
    function applySemanticClass(id, raw) {
        var node = document.getElementById(id);
        if (!node) return;
        Array.from(node.classList).filter(function (name) { return name.indexOf("semantic-") === 0; })
            .forEach(function (name) { node.classList.remove(name); });
        semanticClass(raw).trim().split(/\s+/).filter(Boolean).forEach(function (name) { node.classList.add(name); });
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
            && has(asset && asset.name)
            && slotType === "DECISION"
            && has(asset && (asset.opportunityId || asset.primaryOpportunityId))
            && has(asset && asset.analysisId)
            && eligibleOpportunity(asset)
            && asset.hasFinal === true
            && has(asset.finalMarketBias)
            && (["CONFIRMATION", "REDUCED", "PREPARATION"].indexOf(finalMode) >= 0
                || ["OBSERVATION", "BLOCKED"].indexOf(finalMode) >= 0)
            && has(asset.confidenceLevel)
            && has(asset.riskLevel);
    }
    function validObservationCard(asset) {
        var state = String(asset && (asset.opportunityState || asset.assetState) || "").toUpperCase();
        var slotType = String(asset && asset.slotType || "").toUpperCase();
        if (slotType === "DEFAULT_SLOT") return false;
        return symbolOf(asset)
            && has(asset && asset.assetId)
            && has(asset && asset.name)
            && asset.hasFinal !== true
            && ["OBSERVATION", "DECISION"].indexOf(slotType) >= 0
            && ["OBSERVING", "NO_QUALIFIED_OPPORTUNITY", "STALE", "NEVER_SCANNED", "RANGE", "WAIT",
                "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED", "HIGH_RISK", "BLOCKED", "CONFUSED", "INVALIDATED", "COOLING"].indexOf(state) >= 0;
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
        var updated = has(header.updatedAt) ? "更新于 " + clockTime(header.updatedAt) : "等待同步";
        setText("headerUpdatedAt", updated + " · " + homeLiveState);
    }

    function renderStatus(home) {
        var header = home.header || {};
        var state = home.systemState || {};
        setText("statusEnvironment", statusValue(state.marketTrend));
        var runtimeLabel = has(header.systemRuntimeLabel) ? header.systemRuntimeLabel : "状态未知";
        var completedScan = header.lastCompletedScanAt;
        setText("statusSystem", runtimeLabel
            + (has(completedScan) ? " · 上次扫描 " + clockTime(completedScan) : ""));
        applySemanticClass("statusSystem", header.systemRuntimeStatus || runtimeLabel);
        setText("statusData", has(state.dataQuality?.value) ? "更新于 " + clockTime(state.dataQuality.value) : "等待同步");
        applySemanticClass("statusData", has(state.dataQuality?.value) ? "READY" : "UNKNOWN");
        setText("statusService", desktop.serviceSummary(home));
        setText("statusAccount", statusValue(state.accountStatus, "等待同步"));
        setText("statusReset", statusValue(state.hotReset, "等待同步"));
        renderServicePopover(home);
        renderAiBudget(home);
    }

    function providerName(value) {
        return {
            BINANCE_PUBLIC_MARKET_DATA: "Binance 行情",
            KRAKEN_PUBLIC_MARKET_DATA: "Kraken 行情",
            LOCAL_REAL_MARKET_DATA: "实时行情",
            OPENAI: "GPT",
            GEMINI: "Gemini",
            XAI: "Grok",
            COINGLASS: "CoinGlass",
            MACRO_NEWS_CONTEXT: "宏观与新闻"
        }[String(value || "").toUpperCase()] || label(value, "未知服务");
    }
    function providerReady(provider) {
        return provider && (provider.connected === true
            || ["CONNECTED", "READY"].indexOf(String(provider.status || "").toUpperCase()) >= 0);
    }
    function sortedProviders(home) {
        var providers = home?.diagnostics?.providerReadiness?.providers;
        return (Array.isArray(providers) ? providers.slice() : []).sort(function (left, right) {
            return Number(providerReady(left)) - Number(providerReady(right));
        });
    }
    function renderServicePopover(home) {
        var target = document.getElementById("serviceStatusPopover");
        if (!target) return;
        target.innerHTML = desktop.serviceDrawer(home);
    }
    function renderAiBudget(home) {
        var summary = home && home.diagnostics && home.diagnostics.providerReadiness
            && home.diagnostics.providerReadiness.summary || {};
        var target = document.getElementById("aiBudgetSummary");
        if (!target) return;
        if (!has(summary.aiDailyLimitUsd)) {
            target.textContent = "AI额度当前不可查看；不影响规则方向、计划和持仓监控";
            return;
        }
        var exhausted = String(summary.aiBudgetStatus || "").toUpperCase() === "EXHAUSTED";
        target.textContent = (exhausted ? "今日AI额度已用完" : "AI额度可用")
            + " · 每日 $" + summary.aiDailyLimitUsd
            + " · 已用 $" + text(summary.aiUsedTodayUsd, "0")
            + " · 剩余 $" + text(summary.aiRemainingUsd, "0")
            + " · 重置 " + time(summary.aiBudgetResetAt)
            + "；仅影响按需三AI，不影响规则方向、风险、计划和持仓监控";
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
            var alertScope = has(alert.symbol) ? label(alert.symbol, "全局") : "全局";
            alertNode.querySelector("strong").textContent = alertScope + " · " + userFacingAlertMessage(alert.message);
            alertNode.querySelector("em").textContent = alertTokenLabel(alert.level, "高优先级");
            alertNode.querySelector("time").textContent = has(alert.time) ? time(alert.time) : "";
            alertNode.querySelector("time").title = has(alert.time) ? desktop.fullBeijingTime(alert.time) : "";
            alertNode.querySelector("time").setAttribute("datetime", alert.time || "");
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
        return '<span class="state-badge' + tone + semanticClass(asset.opportunityState || asset.assetState) + '">' + escapeHtml(visible) + "</span>";
    }
    function shortId(value) {
        var raw = text(value, "");
        if (!raw) return "未形成";
        return raw.length <= 14 ? raw : raw.slice(0, 8) + "…" + raw.slice(-4);
    }
    function assetProvenance(asset) {
        return {
            analysisId: has(asset && asset.analysisId) ? String(asset.analysisId) : null,
            analysisVersion: has(asset && asset.analysisVersion) ? Number(asset.analysisVersion) : null,
            configurationVersion: has(asset && asset.configurationVersion) ? String(asset.configurationVersion) : null,
            providerMatrixVersion: has(asset && asset.providerMatrixVersion) ? String(asset.providerMatrixVersion) : null,
            provider: has(asset && asset.provider) ? String(asset.provider) : null,
            sourceId: has(asset && asset.sourceId) ? String(asset.sourceId) : null,
            priceObservedAt: has(asset && asset.priceObservedAt) ? String(asset.priceObservedAt) : null,
            oneHourClosedAt: has(asset && asset.oneHourClosedAt) ? String(asset.oneHourClosedAt) : null,
            fourHourClosedAt: has(asset && asset.fourHourClosedAt) ? String(asset.fourHourClosedAt) : null,
            freshnessStatus: has(asset && asset.freshnessStatus) ? String(asset.freshnessStatus) : null,
            dataQualityScore: has(asset && asset.dataQualityScore) ? Number(asset.dataQualityScore) : null,
            directionMaturity: has(asset && asset.directionMaturity) ? String(asset.directionMaturity) : null,
            homeTier: has(asset && asset.homeTier) ? String(asset.homeTier) : null,
            decisionId: has(asset && asset.decisionId) ? String(asset.decisionId) : null,
            traceId: has(asset && asset.traceId) ? String(asset.traceId) : null,
            latestPriceAt: has(asset && asset.latestPriceAt) ? String(asset.latestPriceAt) : null,
            priceAtDecision: has(asset && asset.priceAtDecision) ? String(asset.priceAtDecision) : null,
            marketDataAsOf: has(asset && asset.marketDataAsOf) ? String(asset.marketDataAsOf) : null,
            directionCalculatedAt: has(asset && asset.directionCalculatedAt) ? String(asset.directionCalculatedAt) : null,
            decisionAgeSeconds: has(asset && asset.decisionAgeSeconds) ? String(asset.decisionAgeSeconds) : null,
            priceDriftPct: has(asset && asset.priceDriftPct) ? String(asset.priceDriftPct) : null,
            planInvalidationLevel: has(asset && asset.planInvalidationLevel) ? String(asset.planInvalidationLevel) : null,
            planState: has(asset && asset.planState) ? String(asset.planState) : null
        };
    }
    function provenanceAttributes(asset) {
        var provenance = assetProvenance(asset);
        return ' data-analysis-id="' + escapeHtml(provenance.analysisId || "")
            + '" data-snapshot-id="' + escapeHtml(asset.snapshotId || "")
            + '" data-analysis-version="' + escapeHtml(has(provenance.analysisVersion) ? provenance.analysisVersion : "")
            + '" data-direction-maturity="' + escapeHtml(provenance.directionMaturity || "")
            + '" data-home-tier="' + escapeHtml(provenance.homeTier || "")
            + '" data-decision-id="' + escapeHtml(provenance.decisionId || "")
            + '" data-trace-id="' + escapeHtml(provenance.traceId || "")
            + '" data-latest-price-at="' + escapeHtml(provenance.latestPriceAt || "")
            + '" data-price-at-decision="' + escapeHtml(provenance.priceAtDecision || "")
            + '" data-market-data-as-of="' + escapeHtml(provenance.marketDataAsOf || "")
            + '" data-direction-calculated-at="' + escapeHtml(provenance.directionCalculatedAt || "")
            + '" data-decision-age-seconds="' + escapeHtml(provenance.decisionAgeSeconds || "")
            + '" data-price-drift-pct="' + escapeHtml(provenance.priceDriftPct || "")
            + '" data-plan-invalidation-level="' + escapeHtml(provenance.planInvalidationLevel || "")
            + '" data-plan-state="' + escapeHtml(provenance.planState || "") + '"';
    }
    function assetTicker(asset) {
        var display = text(asset && asset.symbol, "").trim().toUpperCase();
        if (display.indexOf("/") > 0) return display.split("/")[0];
        var raw = symbolOf(asset);
        return raw.endsWith("USDT") ? raw.slice(0, -4) : raw;
    }
    // Appendix I belongs only to Home cards. Pool/Plan/AI continue using their existing projections.
    var assetCardDisplaySymbols = new Set();
    var assetCardDisplayEpoch = 0;
    function setAssetCardDisplay(symbol, enabled) {
        if (assetCardDisplaySymbols.has(symbol) !== enabled) {
            if (enabled) assetCardDisplaySymbols.add(symbol); else assetCardDisplaySymbols.delete(symbol);
            assetCardDisplayEpoch++;
            assetCardSnapshots.delete(symbol);
            ["PRICE", "SIGNAL", "RISK", "HEALTH"].forEach(function (group) { assetCardFieldVersions.delete(symbol + "|" + group); });
            if (assetCardPriceTimers.has(symbol)) {
                window.clearTimeout(assetCardPriceTimers.get(symbol));
                assetCardPriceTimers.delete(symbol);
            }
        }
        return enabled;
    }
    var assetCardDirections = Object.freeze({ STRONG_LONG: "强偏多", LONG: "偏多", WEAK_LONG: "弱偏多",
        STRONG_SHORT: "强偏空", SHORT: "偏空", WEAK_SHORT: "弱偏空", RANGE: "震荡", WATCH: "观望" });
    var assetCardRiskNames = Object.freeze({ CHASE: "追高", SHOCK: "急涨急跌", REVERSAL: "反转", CROWDING: "拥挤",
        LIQUIDATION: "清算", LIQUIDITY: "流动性", EVENT: "事件", DATA: "数据" });
    function assetCardDate(value) {
        if (!value) return null;
        var raw = String(value);
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(raw) && !/(Z|[+-]\d{2}:?\d{2})$/i.test(raw)) raw += "Z";
        var date = new Date(raw);
        return Number.isNaN(date.getTime()) ? null : date;
    }
    function assetCardClock(value) {
        var date = assetCardDate(value);
        return date ? new Intl.DateTimeFormat("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit",
            hourCycle: "h23" }).format(date) : "—";
    }
    function assetCardDirection(snapshot) {
        var signal = snapshot && snapshot.signal || {};
        if (["VALID", "INVALIDATED"].indexOf(signal.status) < 0) return "—";
        return Object.prototype.hasOwnProperty.call(assetCardDirections, signal.direction) ? assetCardDirections[signal.direction] : "—";
    }
    function assetCardConfidence(snapshot) {
        var signal = snapshot && snapshot.signal || {};
        var value = signal.calibratedConfidence;
        return signal.status === "VALID" && /^(STRONG_|WEAK_)?(LONG|SHORT)$/.test(signal.direction || "")
            && snapshot.modelVersion && snapshot.calibrationVersion && snapshot.featureVersion
            && Number.isInteger(value) && value >= 0 && value <= 100 ? value + "%" : "—";
    }
    function assetCardDirectionClass(snapshot) {
        var direction = snapshot && snapshot.signal && snapshot.signal.direction;
        return assetCardDirection(snapshot) === "—" ? "asset-card-unknown"
            : "asset-card-" + direction.toLowerCase().replace(/_/g, "-");
    }
    function assetCardStatus(snapshot) {
        var signal = snapshot && snapshot.signal || {};
        if (signal.status === "INVALIDATED") return "已失效";
        if (assetCardDirection(snapshot) === "—") return "数据不足";
        var state = String(snapshot && snapshot.health && snapshot.health.status || "").toUpperCase();
        if (["HEALTHY", "OK", "NORMAL", "READY", "UP"].indexOf(state) >= 0) return "";
        return state === "STALE" ? "数据过期" : state === "DEGRADED" ? "数据异常" : "数据不足";
    }
    function assetCardPrice(snapshot) {
        var price = snapshot && snapshot.spotPrice;
        return price != null && Number.isFinite(Number(price)) && Number(price) > 0
            ? "$" + desktop.priceText(price) : "—";
    }
    function assetCardTimeframes(snapshot) {
        var signal = snapshot && snapshot.signal || {};
        var oneHour = String(signal.oneHourState || "").replace(/^1小时/, "");
        var fourHour = String(signal.fourHourTrend || "").replace(/^4小时(?:趋势)?/, "");
        return { oneHour: "1小时" + ({ OPPORTUNITY: "机会", WATCH: "观察", OBSERVE: "观察", OBSERVATION: "观察",
            CONFLICT: "冲突", "机会": "机会", "观察": "观察", "冲突": "冲突" }[oneHour] || "数据不足"),
            fourHour: "4小时趋势" + ({ LONG: "偏多", SHORT: "偏空", RANGE: "震荡", "偏多": "偏多", "偏空": "偏空", "震荡": "震荡" }[fourHour] || "数据不足") };
    }
    function assetCardActiveRisks(snapshot) {
        var items = snapshot && snapshot.risk && snapshot.risk.items;
        var seen = new Set();
        return (Array.isArray(items) ? items : []).filter(function (item) {
            if (!item || !Object.prototype.hasOwnProperty.call(assetCardRiskNames, item.type) || seen.has(item.type)
                    || item.assessmentStatus !== "ASSESSED" || ["MEDIUM", "HIGH"].indexOf(item.level) < 0) return false;
            seen.add(item.type); return true;
        }).sort(function (left, right) {
            var rank = { HIGH: 2, MEDIUM: 1 };
            var severity = rank[right.level] - rank[left.level];
            var invalidation = Number(right.level === "HIGH" && right.invalidatesSignal === true)
                - Number(left.level === "HIGH" && left.invalidatesSignal === true);
            return severity || invalidation || (assetCardDate(right.asOf)?.getTime() || 0) - (assetCardDate(left.asOf)?.getTime() || 0);
        }).slice(0, 3);
    }
    function assetCardOverallRisk(snapshot) {
        var risk = snapshot && snapshot.risk || {};
        var items = Array.isArray(risk.items) ? risk.items : [];
        // Display an assessed high/medium honestly even if another item is unknown; never turn UNKNOWN into LOW.
        var assessed = items.filter(function (item) { return item && Object.prototype.hasOwnProperty.call(assetCardRiskNames, item.type) && item.assessmentStatus === "ASSESSED"; });
        if (assessed.some(function (item) { return item.level === "HIGH"; })) return "HIGH";
        if (assessed.some(function (item) { return item.level === "MEDIUM"; })) return "MEDIUM";
        if (risk.overallLevel === "HIGH" || risk.overallLevel === "MEDIUM") return risk.overallLevel;
        return risk.overallLevel === "LOW" && Object.keys(assetCardRiskNames).every(function (type) {
            return assessed.some(function (item) { return item.type === type && ["NONE", "LOW"].indexOf(item.level) >= 0; });
        }) ? "LOW" : "UNKNOWN";
    }
    function assetCardRiskLabel(level) {
        return { LOW: "低", MEDIUM: "中", HIGH: "高" }[level] || "—";
    }
    function assetCardRiskItemsHtml(snapshot) {
        return assetCardActiveRisks(snapshot).map(function (item) {
            return '<span class="asset-card-risk-' + item.level.toLowerCase() + '">' + escapeHtml(assetCardRiskNames[item.type])
                + '·' + assetCardRiskLabel(item.level) + '</span>';
        }).join("");
    }
    function assetCardRiskDrawer(snapshot) {
        var items = assetCardActiveRisks(snapshot);
        if (!items.length) return "";
        return '<h3>' + escapeHtml(snapshot.symbol) + ' · 风险详情</h3>' + items.map(function (item) {
            return '<section class="risk-evidence-item"><header><strong class="risk-type-copy">' + escapeHtml(assetCardRiskNames[item.type])
                + '</strong><span class="risk-level-' + item.level.toLowerCase() + '">' + assetCardRiskLabel(item.level)
                + '</span></header><p>指标值：' + escapeHtml(item.evidenceValue == null ? "未记录" : item.evidenceValue)
                + '</p><p>' + escapeHtml(item.reason || "未提供证据说明") + '</p><small>' + escapeHtml(item.source || "未记录来源")
                + ' · ' + escapeHtml(assetCardClock(item.asOf)) + '</small></section>';
        }).join("");
    }
    function assetCardRiskAttributes(symbol, active) {
        return active ? ' tabindex="0" data-desktop-hover="risk" data-risk-symbol="' + escapeHtml(symbol)
            + '" aria-haspopup="dialog" aria-expanded="false" aria-label="' + escapeHtml(symbol) + ' 风险详情"' : '';
    }
    function assetCardSignificant(group, value) {
        if (group === "SIGNAL") return JSON.stringify(value ? [value.direction, value.status, value.calibratedConfidence] : null);
        return JSON.stringify(value ? [value.overallLevel, (Array.isArray(value.items) ? value.items : []).map(function (item) {
            return item && [item.type, item.assessmentStatus, item.level, item.evidenceValue, item.source, item.reason];
        })] : null);
    }
    function mergeAssetCardGroup(symbol, version, group, payload) {
        if (homeCardSymbols.indexOf(symbol) < 0 || !assetCardDisplaySymbols.has(symbol)
                || !Number.isSafeInteger(version) || version <= 0) return false;
        var identity = symbol + "|" + group;
        if (version <= (assetCardFieldVersions.get(identity) || 0)) return false;
        var current = assetCardSnapshots.get(symbol) || { symbol: symbol, snapshotVersion: 0 };
        var next = Object.assign({}, current, { snapshotVersion: Math.max(version, current.snapshotVersion || 0) });
        var key = group.toLowerCase();
        if (group === "PRICE") {
            next.spotPrice = payload.spotPrice == null ? null : payload.spotPrice;
            next.latestPriceAt = payload.latestPriceAt || null;
        } else {
            next[key] = payload[key] && typeof payload[key] === "object" ? payload[key] : null;
            if (group === "SIGNAL") {
                ["featureVersion", "modelVersion", "calibrationVersion"].forEach(function (field) {
                    next[field] = typeof payload[field] === "string" && payload[field].trim() ? payload[field] : null;
                });
            }
            if ((group === "SIGNAL" || group === "RISK") && assetCardSignificant(group, current[key]) !== assetCardSignificant(group, next[key])) {
                var clock = assetCardDate(payload.cardAsOf), previousClock = assetCardDate(current.cardAsOf);
                if (clock && (!previousClock || clock >= previousClock)) next.cardAsOf = payload.cardAsOf;
            }
        }
        assetCardFieldVersions.set(identity, version);
        assetCardSnapshots.set(symbol, next);
        return true;
    }
    function patchAssetCardField(card, field, value, className) {
        var target = card && card.querySelector('[data-live-field="' + field + '"]');
        if (!target) return;
        if (target.textContent !== value) target.textContent = value;
        if (className != null) target.className = className;
    }
    function patchAssetCard(symbol, group) {
        var card = liveCard(symbol), snapshot = assetCardSnapshots.get(symbol);
        if (!card || !snapshot || homeCardSymbols.indexOf(symbol) < 0 || !assetCardDisplaySymbols.has(symbol)) return;
        if (group === "PRICE") {
            patchAssetCardField(card, "price", assetCardPrice(snapshot));
            card.setAttribute("data-card-price-at", snapshot.latestPriceAt || "");
        }
        if (group === "SIGNAL") {
            var frames = assetCardTimeframes(snapshot);
            patchAssetCardField(card, "direction", assetCardDirection(snapshot), assetCardDirectionClass(snapshot));
            patchAssetCardField(card, "confidence", assetCardConfidence(snapshot));
            patchAssetCardField(card, "one-hour", frames.oneHour);
            patchAssetCardField(card, "four-hour", frames.fourHour);
            card.setAttribute("aria-label", "查看 " + symbol + " 首页资产上下文；" + assetCardDirection(snapshot) + "；置信度 " + assetCardConfidence(snapshot));
        }
        if (group === "SIGNAL" || group === "HEALTH") {
            var status = assetCardStatus(snapshot);
            patchAssetCardField(card, "status", status);
            var statusNode = card.querySelector('[data-live-field="status"]');
            if (statusNode) statusNode.hidden = !status;
        }
        if (group === "RISK") {
            var level = assetCardOverallRisk(snapshot);
            patchAssetCardField(card, "risk", assetCardRiskLabel(level), "asset-card-risk-" + level.toLowerCase());
            var items = card.querySelector('[data-live-field="risk-items"]');
            if (items) {
                items.innerHTML = assetCardRiskItemsHtml(snapshot); items.hidden = !items.innerHTML;
                var attributes = { tabindex: "0", "data-desktop-hover": "risk", "data-risk-symbol": symbol,
                    "aria-haspopup": "dialog", "aria-expanded": "false", "aria-label": symbol + " 风险详情" };
                Object.keys(attributes).forEach(function (name) {
                    if (items.hidden) items.removeAttribute(name); else items.setAttribute(name, attributes[name]);
                });
            }
        }
        if (group === "SIGNAL" || group === "RISK") {
            patchAssetCardField(card, "card-time", assetCardClock(snapshot.cardAsOf));
            var clock = card.querySelector('[data-live-field="card-time"]');
            if (clock) { if (snapshot.cardAsOf) clock.setAttribute("datetime", snapshot.cardAsOf); else clock.removeAttribute("datetime"); }
        }
    }
    function scheduleAssetCardPrice(symbol) {
        if (assetCardPriceTimers.has(symbol)) return;
        assetCardPriceTimers.set(symbol, window.setTimeout(function () {
            assetCardPriceTimers.delete(symbol);
            if (!document.hidden) patchAssetCard(symbol, "PRICE");
        }, 1500));
    }
    function mergeAssetCardSnapshot(snapshot, renderFields) {
        if (!snapshot || typeof snapshot !== "object") return false;
        var symbol = String(snapshot.symbol || "").toUpperCase(), version = Number(snapshot.snapshotVersion);
        if (homeCardSymbols.indexOf(symbol) < 0 || !assetCardDisplaySymbols.has(symbol)
                || !Number.isSafeInteger(version) || version <= 0) return false;
        if (snapshot.health && snapshot.health.status === "MODEL_UNAVAILABLE"
                && !currentModelCheck(snapshot.health, assetCardSnapshots.get(symbol))) return false;
        var accepted = false;
        ["PRICE", "SIGNAL", "RISK", "HEALTH"].forEach(function (group) {
            if (!mergeAssetCardGroup(symbol, version, group, snapshot)) return;
            accepted = true;
            if (renderFields) { if (group === "PRICE") scheduleAssetCardPrice(symbol); else patchAssetCard(symbol, group); }
        });
        if (accepted && snapshot.assetName) {
            var current = assetCardSnapshots.get(symbol);
            assetCardSnapshots.set(symbol, Object.assign({}, current, { assetName: snapshot.assetName }));
        }
        return accepted;
    }
    function currentModelCheck(health, current) {
        var checkedAt = assetCardDate(health && health.asOf);
        var signalAt = assetCardDate(current && current.signal && current.signal.signalAsOf);
        var healthAt = assetCardDate(current && current.health && current.health.asOf);
        return checkedAt && checkedAt <= new Date() && (!signalAt || checkedAt >= signalAt) && (!healthAt || checkedAt >= healthAt);
    }
    function applyReadSafetyDowngrade(snapshot, sequence, renderFields) {
        var status = snapshot && snapshot.health && snapshot.health.status;
        if (status !== "SOURCE_UNAVAILABLE" && status !== "MODEL_UNAVAILABLE") return false;
        var symbol = String(snapshot && snapshot.symbol || "").toUpperCase();
        var version = Number(snapshot && snapshot.snapshotVersion);
        var current = assetCardSnapshots.get(symbol);
        if (sequence != null && sequence !== assetCardRequestSequence || renderFields !== false && document.hidden || homeCardSymbols.indexOf(symbol) < 0
                || !assetCardDisplaySymbols.has(symbol)
                || !current || !Number.isSafeInteger(version) || version <= 0 || version !== current.snapshotVersion
                || status === "MODEL_UNAVAILABLE" && !currentModelCheck(snapshot.health, current)) return false;
        var modelUnavailable = status === "MODEL_UNAVAILABLE" || snapshot.signal && snapshot.signal.status === "UNVALIDATED";
        var sourceUnavailable = status === "SOURCE_UNAVAILABLE";
        var signal = current.signal && Object.assign({}, current.signal, { calibratedConfidence: null, pLong: null, pShort: null });
        if (modelUnavailable) signal = Object.assign({}, signal, { direction: null, status: "UNVALIDATED",
            calibratedConfidence: null, pLong: null, pShort: null, oneHourState: "数据不足", fourHourTrend: "数据不足" });
        else if (signal && assetCardDirection(current) !== "—") signal.status = "INVALIDATED";
        // A read can fail closed without allocating a published version. It cannot manufacture a direction,
        // erase assessed risk, advance the effective card clock, or restore values at this same version.
        assetCardSnapshots.set(symbol, Object.assign({}, current, {
            spotPrice: sourceUnavailable ? null : current.spotPrice, latestPriceAt: sourceUnavailable ? null : current.latestPriceAt,
            signal: signal, health: Object.assign({}, snapshot.health) }));
        (sourceUnavailable ? ["PRICE", "SIGNAL", "RISK", "HEALTH"] : ["SIGNAL", "HEALTH"]).forEach(function (group) {
            assetCardFieldVersions.set(symbol + "|" + group, version);
        });
        if (renderFields !== false) {
            if (sourceUnavailable) patchAssetCard(symbol, "PRICE");
            patchAssetCard(symbol, "SIGNAL");
            patchAssetCard(symbol, "HEALTH");
        }
        return true;
    }
    function applyAssetCardEvent(event) {
        if (!event) return;
        var group = String(event.eventType || "").replace(/^ASSET_CARD_/, "");
        if (["PRICE", "SIGNAL", "RISK", "HEALTH"].indexOf(group) < 0) return;
        var payload = event.payload || event;
        var symbol = String(event.symbol || payload.symbol || "").toUpperCase();
        if (payload.symbol && String(payload.symbol).toUpperCase() !== symbol) return;
        var version = Number(event.snapshotVersion == null ? payload.snapshotVersion : event.snapshotVersion);
        if (payload.snapshotVersion != null && Number(payload.snapshotVersion) !== version) return;
        if (group === "HEALTH" && payload.health && payload.health.status === "MODEL_UNAVAILABLE") {
            var current = assetCardSnapshots.get(symbol);
            if (current && version < current.snapshotVersion || !currentModelCheck(payload.health, current)) return;
            if (!current || version > current.snapshotVersion) {
                if (!mergeAssetCardGroup(symbol, version, group, payload)) return;
            }
            applyReadSafetyDowngrade({ symbol: symbol, snapshotVersion: version, health: payload.health }, null);
            return;
        }
        if (!mergeAssetCardGroup(symbol, version, group, payload)) return;
        if (group === "PRICE") scheduleAssetCardPrice(symbol); else patchAssetCard(symbol, group);
    }
    // Pre-switch renderer from merged main. It is reachable only outside the explicit display cohort.
    function legacyOpportunityCard(asset, selected) {
        var symbol = symbolOf(asset);
        var isSelected = symbol === selected;
        var ticker = assetTicker(asset);
        var finalDirection = asset.hasFinal === true ? asset.finalMarketBias : asset.marketBias;
        var direction = has(asset.marketBiasLabel) ? text(asset.marketBiasLabel)
            : has(finalDirection) ? label(finalDirection, "待重新分析") : "待重新分析";
        var confidence = desktop.confidenceText(asset);
        var oneHour = text(asset.oneHourOpportunityLabel, "1小时数据不足");
        var fourHour = text(asset.fourHourTrendLabel, "4小时数据不足");
        var price = has(asset.latestPrice) ? "$" + desktop.priceText(asset.latestPrice) : "价格待同步";
        var provenance = assetProvenance(asset);
        var risk = desktop.riskSummary(asset);
        var timeLabel = desktop.directionTimeLabel(asset.directionCalculatedAt);
        return '<article class="opportunity-card' + (isSelected ? " is-selected" : "") + '" tabindex="0" role="button" aria-pressed="'
            + String(isSelected) + '" data-symbol="'
            + escapeHtml(symbol) + '"' + provenanceAttributes(asset) + ' aria-label="查看 '
            + escapeHtml(symbol + " 首页资产上下文；" + direction + "；置信度 " + confidence) + '"><header><div class="asset-identity"><strong>'
            + escapeHtml(ticker) + '</strong><span aria-hidden="true">/</span><small>'
            + escapeHtml(text(asset.name, "名称不可用"))
            + '</small></div><div class="asset-price-block"><strong class="opportunity-price" data-live-field="price">' + escapeHtml(price)
            + '</strong>' + desktop.priceCaption(asset) + '</div></header><div class="opportunity-final"><small>方向</small><b data-live-field="direction" class="semantic-value' + directionSemanticClass(finalDirection) + '">' + escapeHtml(direction)
            + '</b><span class="metric-separator">·</span><small>置信</small><strong data-live-field="confidence">' + escapeHtml(confidence)
            + '</strong></div>' + (risk || asset.homePinned === true ? '<div class="opportunity-facts">' : '')
            + (risk ? '<div class="opportunity-risk" data-live-field="risk"'
            + (desktop.hasConfirmedRisks(asset) ? ' tabindex="0" data-desktop-hover="risk" data-risk-symbol="' + escapeHtml(symbol)
            + '" aria-haspopup="dialog" aria-expanded="false" aria-label="' + escapeHtml(symbol) + ' 风险详情"'
            : '') + '>' + risk + '</div>' : '')
            + (asset.homePinned === true ? '<div class="pinned-observation-copy">' + escapeHtml(desktop.pinnedObservationLabel(asset)) + '</div>' : '')
            + (risk || asset.homePinned === true ? '</div>' : '')
            + '<div class="opportunity-context"><span>' + escapeHtml(oneHour)
            + '</span><span>' + escapeHtml(fourHour) + '</span>'
            + (timeLabel ? '<time class="opportunity-updated" datetime="' + escapeHtml(asset.directionCalculatedAt)
                + '" title="' + escapeHtml(timeLabel) + '" aria-label="' + escapeHtml(timeLabel) + '">'
                + escapeHtml(desktop.beijingTime(asset.directionCalculatedAt, true)) + '</time>' : '')
            + '</div></article>';
    }
    function opportunityCard(asset, selected) {
        var symbol = symbolOf(asset);
        if (!setAssetCardDisplay(symbol, asset.cardSignalDisplayEnabled === true)) return legacyOpportunityCard(asset, selected);
        var isSelected = symbol === selected;
        var ticker = assetTicker(asset);
        var current = assetCardSnapshots.get(symbol);
        if (asset.cardSignal && String(asset.cardSignal.symbol || "").toUpperCase() === symbol
                && (!current || Number(asset.cardSignal.snapshotVersion) >= current.snapshotVersion)) {
            // The Home loader already checks its own request sequence; this projection still checks the card version.
            if (!applyReadSafetyDowngrade(asset.cardSignal, null, false)) mergeAssetCardSnapshot(asset.cardSignal, false);
        }
        var snapshot = assetCardSnapshots.get(symbol) || {};
        var direction = assetCardDirection(snapshot), confidence = assetCardConfidence(snapshot);
        var frames = assetCardTimeframes(snapshot), status = assetCardStatus(snapshot);
        var level = assetCardOverallRisk(snapshot), riskItems = assetCardRiskItemsHtml(snapshot);
        return '<article class="opportunity-card' + (isSelected ? " is-selected" : "") + '" tabindex="0" role="button" aria-pressed="'
            + String(isSelected) + '" data-symbol="'
            + escapeHtml(symbol) + '"' + provenanceAttributes(asset) + ' aria-label="查看 '
            + escapeHtml(symbol + " 首页资产上下文；" + direction + "；置信度 " + confidence) + '"><header><div class="asset-identity"><strong>'
            + escapeHtml(ticker) + '</strong><span aria-hidden="true">/</span><small>'
            + escapeHtml(text(snapshot.assetName || asset.name, "名称不可用"))
            + '</small></div><div class="asset-price-block"><strong class="opportunity-price" data-live-field="price">' + escapeHtml(assetCardPrice(snapshot))
            + '</strong></div></header><div class="opportunity-final"><small>方向</small><b data-live-field="direction" class="' + assetCardDirectionClass(snapshot) + '">' + escapeHtml(direction)
            + '</b><span class="metric-separator">·</span><small>置信</small><strong data-live-field="confidence">' + escapeHtml(confidence)
            + '</strong></div><div class="opportunity-facts"><div class="asset-card-risk-summary"><span>风险 <strong data-live-field="risk" class="asset-card-risk-'
            + level.toLowerCase() + '">' + assetCardRiskLabel(level) + '</strong></span><small data-live-field="status"'
            + (status ? '' : ' hidden') + '>' + escapeHtml(status) + '</small></div><div class="asset-card-risk-items" data-live-field="risk-items"'
            + (riskItems ? assetCardRiskAttributes(symbol, true) : ' hidden') + '>' + riskItems + '</div></div>'
            + '<div class="opportunity-context"><span data-live-field="one-hour">' + escapeHtml(frames.oneHour)
            + '</span><span data-live-field="four-hour">' + escapeHtml(frames.fourHour) + '</span>'
            + '<time class="opportunity-updated" data-live-field="card-time"' + (snapshot.cardAsOf ? ' datetime="' + escapeHtml(snapshot.cardAsOf) + '"' : '') + '>'
            + escapeHtml(assetCardClock(snapshot.cardAsOf)) + '</time>'
            + '</div></article>';
    }
    function renderOpportunities(home) {
        var all = Array.isArray(home.assets) ? home.assets : [];
        var seen = new Set();
        var assets = all.filter(function (asset) {
            var identity = "asset:" + String(asset.assetId);
            var symbolIdentity = "symbol:" + symbolOf(asset);
            if (seen.has(identity) || seen.has(symbolIdentity)) return false;
            seen.add(identity);
            seen.add(symbolIdentity);
            return true;
        }).slice(0, 6);
        homeCardSymbols = assets.map(symbolOf);
        assetCardDisplaySymbols.forEach(function (symbol) {
            if (homeCardSymbols.indexOf(symbol) < 0) setAssetCardDisplay(symbol, false);
        });
        assetCardPriceTimers.forEach(function (timer, symbol) {
            if (homeCardSymbols.indexOf(symbol) < 0) { window.clearTimeout(timer); assetCardPriceTimers.delete(symbol); }
        });
        var grid = document.getElementById("opportunityGrid");
        var empty = document.getElementById("opportunityEmpty");
        var selected = symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || selectedSymbol;
        setText("opportunityHeading", "重点资产 · " + assets.length + "/6");
        grid.innerHTML = assets.map(function (asset) { return opportunityCard(asset, selected); }).join("");
        grid.hidden = assets.length === 0;
        empty.hidden = assets.length >= 6;
        if (!empty.hidden) empty.textContent = home.homeAssetShortfallReason
            || (home.snapshotComplete === true ? "暂无更多合格资产" : "完整资产快照尚未就绪");
        grid.querySelectorAll("[data-symbol]").forEach(function (card) {
            function select(event) {
                if (event && event.target.closest("[data-desktop-hover]")) return;
                selectedSymbol = card.dataset.symbol;
                if (typeof contract.replaceUrlParam === "function") contract.replaceUrlParam("asset", selectedSymbol);
                var asset = assets.find(function (item) { return symbolOf(item) === selectedSymbol; });
                currentHome = Object.assign({}, currentHome, {
                    selectedSymbol: selectedSymbol,
                    selectedAssetContext: asset,
                    executionSuggestion: { status: "UPDATING", blockedReason: "正在读取当前资产的同批次计划" },
                    aiDecision: { tabs: [], analysisId: asset && asset.analysisId, decisionId: asset && asset.decisionId }
                });
                renderHeader(currentHome);
                renderOpportunities(currentHome);
                renderPlan(currentHome);
                renderAi(currentHome);
                card.setAttribute("aria-busy", "true");
                loadHome(selectedSymbol).finally(function () {
                    card.removeAttribute("aria-busy");
                });
            }
            card.addEventListener("click", select);
            card.addEventListener("keydown", function (event) {
                if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(event); }
            });
        });
        return assets.length;
    }

    function trustedMonitor(position) {
        var trust = String(position && position.monitorTrustState || "SOURCE_UNAVAILABLE").toUpperCase();
        return position && trust === "VERIFIED_FRESH" && position.markPriceFresh === true
            && ["OPEN_MONITORING", "RISK_ESCALATED", "PLAN_INVALIDATED"].indexOf(String(position.dataState || "").toUpperCase()) >= 0;
    }
    function monitorPriceAvailable(position) {
        var trust = String(position && position.monitorTrustState || "").toUpperCase();
        return position && position.markPriceFresh === true
            && ["VERIFIED_FRESH", "BASE_PRICE_VERIFIED_OPTIONAL_CONTEXT_PENDING"].indexOf(trust) >= 0
            && has(position.markPrice || position.currentPrice);
    }
    function monitorJudgmentAvailable(position) {
        var trust = String(position && position.monitorTrustState || "").toUpperCase();
        return position && position.markPriceFresh === true
            && ["VERIFIED_FRESH", "BASE_PRICE_VERIFIED_OPTIONAL_CONTEXT_PENDING"].indexOf(trust) >= 0
            && has(position.riskLevel) && has(position.monitorConclusion) && has(position.suggestedAction);
    }
    function validPosition(position) {
        return position && symbolOf(position) && has(position.direction)
            && has(position.entryPrice) && has(position.openedAt);
    }
    function manualPositionArchiveVisible(position) {
        var source = String(position && position.sourceType || "").trim().toUpperCase();
        var status = String(position && position.status || "").trim().toUpperCase();
        return ["MANUAL", "MANUAL_POSITION", "MANUAL_INDEPENDENT"].indexOf(source) >= 0
            && status !== "ARCHIVED_MISTAKE";
    }
    function positionDetailLink(positionId) {
        var normalized = String(positionId || "").trim();
        return /^\d+$/.test(normalized) && Number(normalized) > 0
            ? '<a class="position-detail-link" href="/positions/' + encodeURIComponent(normalized) + '?returnTo=' + encodeURIComponent("/dashboard" + (selectedSymbol ? "?asset=" + selectedSymbol : "")) + '">查看详情</a>'
            : "";
    }
    function riskRank(value) { return { LOW: 1, MEDIUM: 2, HIGH: 3, EXTREME: 4 }[String(value || "").toUpperCase()] || 0; }
    function highestRisk(positions) {
        var trusted = positions.filter(monitorJudgmentAvailable).sort(function (a, b) { return riskRank(b.riskLevel) - riskRank(a.riskLevel); });
        return trusted.length ? text(trusted[0].riskLevelLabel, label(trusted[0].riskLevel, "暂无评估")) : "暂无评估";
    }
    function trustStateText(position) {
        var state = String(position && position.monitorTrustState || "SOURCE_UNAVAILABLE").toUpperCase();
        return {
            PENDING_FIRST_RUN: "等待首次监控",
            PENDING: "等待监控数据",
            PENDING_VERIFICATION: "等待监控数据",
            BASE_PRICE_VERIFIED_OPTIONAL_CONTEXT_PENDING: "基础价格监控已更新，完整监控待验证",
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
        var judgmentAvailable = monitorJudgmentAvailable(position);
        var priceAvailable = monitorPriceAvailable(position);
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
        var closeAction = /^\d+$/.test(String(position.positionId || ""))
            ? '<button class="position-close-button" type="button" data-close-position-id="' + escapeHtml(position.positionId) + '" data-close-position-symbol="' + escapeHtml(symbolOf(position)) + '">记录平仓</button>'
            : "";
        var auditAction = /^\d+$/.test(String(position.positionId || ""))
            ? '<button class="text-button" type="button" data-open-position-audit="' + escapeHtml(position.positionId) + '">审计详情</button>' : "";
        var archiveAction = /^\d+$/.test(String(position.positionId || "")) && manualPositionArchiveVisible(position)
            ? '<button class="text-button danger" type="button" data-archive-position-id="' + escapeHtml(position.positionId)
                + '" data-archive-position-symbol="' + escapeHtml(symbolOf(position)) + '">归档误录</button>' : "";
        var positionActions = '<div class="position-row-actions">' + detailLink + auditAction + closeAction + archiveAction + '</div>';
        var pnlCoverage = trusted && has(position.pnlCoverage)
            ? "盈亏仅含标记价格、开仓价和数量；费用、资金费率、部分成交及追加仓位覆盖未知"
            : "";
        var openingFacts = '<div class="position-facts">' + positionFact("开仓价", number(position.entryPrice), "UNKNOWN", "numeric")
            + positionFact("开仓时间", time(position.openedAt), "UNKNOWN", "numeric");
        if (priceAvailable) {
            openingFacts += positionFact("标记价格", number(position.markPrice), "STABLE", "numeric")
                + positionFact("盈亏", percent(position.pnlPercent), Number(position.pnlPercent) >= 0 ? "STABLE" : "INVALID", "numeric");
        }
        openingFacts += "</div>";
        var monitorColumns = judgmentAvailable
            ? '<div class="position-judgment">' + positionFact("行情来源", label(position.markPriceSource, "当前不可查看"), position.markPriceSource, "center")
                + positionFact("监控时间", time(position.lastMonitorAt || position.markPriceObservedAt), "STABLE", "center")
                + positionFact("主要风险", risk, position.riskLevel, "center")
                + positionFact("风险趋势", trend, position.riskTrend, "center") + "</div>"
                + '<div class="position-conclusion">' + positionFact("监控结论", conclusion, position.monitorConclusion, "narrative")
                + positionFact("建议动作", action, position.suggestedAction, "narrative")
                + positionActions + '</div>'
            : '<div class="position-trust-state" role="status"><strong>' + escapeHtml(unavailable) + '</strong>' + positionActions + '</div>';
        return '<article class="position-row' + (trusted ? " is-trusted" : judgmentAvailable ? " is-partial" : " is-untrusted")
            + '" data-position-id="' + escapeHtml(position.positionId || "") + '"'
            + ' aria-label="' + escapeHtml(symbolOf(position) + " " + text(position.directionLabel, label(position.direction)) + " "
                + (judgmentAvailable ? conclusion : unavailable) + (pnlCoverage ? " " + pnlCoverage : "")) + '">'
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
        var activeCount = Number.isInteger(aggregate.activeCount) ? aggregate.activeCount : "等待同步";
        var highestTrustedRisk = has(aggregate.highestTrustedRisk)
            ? label(aggregate.highestTrustedRisk, "等待评估") : "等待评估";
        var coverage = has(aggregate.coverageState)
            ? label(aggregate.coverageState, "等待评估") : "等待评估";
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
        var asset = home.selectedAssetContext || {};
        var bias = String(asset.marketBias || asset.finalMarketBias || "").toUpperCase();
        var directional = ["STRONG_BULLISH", "BULLISH", "WEAK_BULLISH", "WEAK_BEARISH", "BEARISH", "STRONG_BEARISH"].indexOf(bias) >= 0;
        var sameSnapshot = has(asset.analysisId) && has(asset.decisionId) && has(asset.traceId)
            && String(plan.sourceAnalysisId) === String(asset.analysisId)
            && String(plan.sourceDecisionId) === String(asset.decisionId)
            && String(plan.sourceTraceId) === String(asset.traceId);
        var targetText = plan.takeProfitRules || plan.targetZones;
        var targets = String(targetText || "").match(/^TP1\s+([0-9.]+)\s*[；;]\s*TP2\s+([0-9.]+)$/);
        var conditionalState = String(plan.planLifecycleState || "").toUpperCase();
        var validFrom = Date.parse(plan.validFrom), validUntil = Date.parse(plan.expiresAt);
        var validNow = Number.isFinite(validFrom) && Number.isFinite(validUntil) && validFrom <= Date.now() && Date.now() < validUntil;
        var currentFinal = conditionalState === "CURRENT" && plan.finalPlan === true
            && String(plan.validationStatus || "").toUpperCase() === "PASS"
            && String(plan.chainStatus || "").toUpperCase() === "FINAL_VALIDATED"
            && plan.needsRevalidation !== true && has(plan.sourceExecutionPlanId)
            && ["CONFIRMATION", "REDUCED", "PREPARATION"].indexOf(plan.finalPlanMode) >= 0;
        if (directional && sameSnapshot && currentFinal && access.visible && validNow
                && has(plan.entryZone) && has(plan.triggerCondition) && has(plan.stopLoss || plan.stopZone) && has(targetText)
                && has(plan.invalidCondition || plan.abandonCondition)) {
            target.innerHTML = '<div class="plan-status-layer"><strong>完整执行计划</strong><span>' + escapeHtml(plan.finalPlanMode) + ' · 非交易指令</span></div>'
                + '<div class="plan-key-layer">' + planField("入场 / 触发区间", desktop.planPriceText(plan.entryZone, asset))
                + planField("触发条件", plan.triggerCondition)
                + planField("止损", desktop.planPriceText(plan.stopLoss || plan.stopZone, asset))
                + (targets ? planField("TP1", desktop.planPriceText(targets[1], asset)) + planField("TP2", desktop.planPriceText(targets[2], asset))
                    : planField("止盈目标", desktop.planPriceText(targetText, asset)))
                + planField("失效条件", plan.invalidCondition || plan.abandonCondition)
                + planField("有效期（北京时间）", desktop.beijingTime(plan.validFrom, true) + " — " + desktop.beijingTime(plan.expiresAt, true)) + '</div>';
            link.hidden = true;
            return;
        }
        var sameDecision = has(asset.analysisId) && has(asset.decisionId)
            && String(plan.sourceAnalysisId) === String(asset.analysisId)
            && String(plan.sourceDecisionId) === String(asset.decisionId);
        if (sameDecision && /BLOCKED|REVALIDATION/.test(String(plan.status || ""))
                && (sameSnapshot || plan.finalPlan === false)) {
            var blockedReasons = humanReason(plan.blockedReason, "本轮未通过执行校验，具体原因尚未记录");
            target.innerHTML = '<div class="plan-empty"><strong>暂不执行</strong>'
                + blockedReasons.split("；").map(function (reason) { return '<span>原因：' + escapeHtml(reason) + '</span>'; }).join("")
                + '<span>下一步：' + escapeHtml(humanReason(plan.revalidationRule,
                    "等待下一根1小时K线闭合，使用新行情重新验证方向、未平仓量和风险条件")) + '</span></div>';
            link.hidden = true;
            return;
        }
        if (!directional || !sameSnapshot) {
            var unavailable = !sameSnapshot && directional ? "当前判断与计划的同批次关联尚未确认"
                : ["CONFLICT", "CONFUSED", "TIMEFRAME_CONFLICT", "MULTI_TIMEFRAME_CONFLICT"].indexOf(bias) >= 0 ? "周期冲突：" + text(asset.oneHourOpportunityLabel, "1小时方向未提供") + " / " + text(asset.fourHourTrendLabel, "4小时方向未提供")
                : ["RANGE", "WAIT", "NEUTRAL"].indexOf(bias) >= 0 ? "震荡 / 观望：当前没有方向性条件计划"
                : text(asset.marketBiasLabel, "方向状态未提供") + "：" + text(asset.oneHourOpportunityLabel, "1小时状态未提供") + " / " + text(asset.fourHourTrendLabel, "4小时状态未提供");
            target.innerHTML = '<div class="plan-empty"><strong>暂无完整执行计划</strong><span>原因：' + escapeHtml(unavailable)
                + '</span><span>当前动作：等待</span><span>下次评估：下一根1小时K线闭合后'
                + (home.nextOneHourCloseAt ? '，北京时间 ' + desktop.beijingTime(home.nextOneHourCloseAt, true) : '，闭线时间待同步') + '</span></div>';
            link.hidden = true;
            return;
        }
        var missing = [];
        if (!has(plan.entryZone)) missing.push("入场 / 触发区间");
        if (!has(plan.triggerCondition)) missing.push("触发条件");
        if (!has(plan.stopLoss || plan.stopZone)) missing.push("止损");
        if (!has(targetText)) missing.push("止盈目标");
        if (!has(plan.invalidCondition || plan.abandonCondition)) missing.push("失效条件");
        if (!currentFinal || !access.visible) missing.push("当前有效且完成最终校验的 Final 计划");
        if (!validNow) missing.push("当前有效期");
        var reason = humanReason(plan.blockedReason || plan.pauseReason || access.reason, "尚缺：" + missing.join("、"));
        target.innerHTML = '<div class="plan-empty"><strong>暂无完整执行计划</strong><span>原因：'
            + escapeHtml(reason) + '</span>' + (has(plan.recoveryCondition) ? '<span>恢复条件：' + escapeHtml(plan.recoveryCondition) + '</span>' : '') + '</div>';
        link.hidden = true;
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

    function scanRuntimeText(header) {
        var state = String(header && (header.scanState || header.scanTaskState) || "").toUpperCase();
        if (["QUEUED", "RUNNING", "SCANNING", "IN_PROGRESS"].indexOf(state) >= 0) return "扫描任务执行中";
        if (has(header && header.lastScanResult)) return label(header.lastScanResult, "最近一次扫描已完成");
        if (has(header && header.lastCompletedScanAt)) return "最近一次扫描已完成";
        return "尚无完成记录";
    }

    function providerReadinessText(home) {
        var active = sortedProviders(home).filter(function (provider) {
            return provider && (provider.enabled === true || provider.connected === true || provider.configured === true);
        });
        if (!active.length) return "当前不可查看";
        return active.map(function (provider) {
            var name = text(provider.name, "未知 Provider");
            var status = label(provider.status, text(provider.status, "状态未知"));
            var reason = humanReason(provider.reason, "等待自动恢复");
            return name + " · " + status + " · " + reason;
        }).join("；");
    }

    function providerDetail(home) {
        var providers = sortedProviders(home);
        if (!providers.length) return '<p class="muted">服务明细当前不可查看</p>';
        return '<div class="provider-detail-list">' + providers.map(function (provider) {
            return '<section class="provider-detail-item' + (providerReady(provider) ? ' is-ready' : ' is-unavailable') + '"><header><strong>'
                + escapeHtml(providerName(provider.name)) + '</strong><span>'
                + escapeHtml(providerReady(provider) ? "READY" : label(provider.status, "UNAVAILABLE"))
                + '</span></header>' + dl([
                    ["最后成功", time(provider.lastSuccessAt)],
                    ["最近状态", humanReason(provider.reason, "等待下一次健康检查")],
                    ["新鲜度 / 延迟", has(provider.freshness) ? label(provider.freshness) : has(provider.latencyMs) ? number(provider.latencyMs, 0) + "ms" : "当前不可查看"],
                    ["影响范围", text(provider.impact, "当前不可查看")],
                    ["恢复 / 重试", text(provider.retryStatus, "等待自动重试")]
                ]) + '</section>';
        }).join("") + '</div>';
    }

    async function openHomeStatus(trigger) {
        var dialog = document.getElementById("homeStatusDialog");
        var target = document.getElementById("homeStatusDetail");
        if (!dialog || !target) return;
        dialog.dataset.restoreFocusId = trigger && trigger.id || "";
        if (!dialog.open) dialog.showModal();
        var header = currentHome.header || {};
        target.innerHTML = dl([
            ["应用状态", "正在读取"],
            ["调度状态", text(header.systemRuntimeLabel, label(header.systemRuntimeState, "当前不可查看"))],
            ["调度心跳", time(header.schedulerHeartbeatAt)],
            ["本轮开始", time(header.scanStartedAt)],
            ["上次成功完成", time(header.lastCompletedScanAt)],
            ["下次计划扫描", time(header.nextScheduledScanAt)],
            ["上次扫描结果", label(header.lastScanResult, "尚无完成记录")],
            ["上次失败原因", label(header.lastScanFailureReason, "无失败记录")],
            ["数据来源", label(header.dataSourceText, "当前不可查看")],
            ["服务明细", providerReadinessText(currentHome)]
        ]);
        try {
            var response = await fetch("/api/system/runtime-readiness-guardrail-status", {
                credentials: "same-origin", headers: { Accept: "application/json" }
            });
            var readiness = await response.json();
            if (!response.ok) throw new Error("STATUS_UNAVAILABLE");
            target.innerHTML = dl([
                ["应用状态", label(readiness.status, "当前不可查看")],
                ["数据库", label(readiness.databaseStatus, "当前不可查看")],
                ["调度器", label(readiness.schedulerObservationStatus || header.systemRuntimeState, "当前不可查看")],
                ["扫描状态", scanRuntimeText(header)],
                ["调度心跳", time(header.schedulerHeartbeatAt)],
                ["本轮开始", time(header.scanStartedAt)],
                ["上次成功完成", time(header.lastCompletedScanAt)],
                ["下次计划扫描", time(header.nextScheduledScanAt)],
                ["上次扫描结果", label(header.lastScanResult, "尚无完成记录")],
                ["上次失败原因", label(header.lastScanFailureReason, "无失败记录")],
                ["数据来源", label(header.dataSourceText, "当前不可查看")],
                ["服务概览", providerReadinessText(currentHome)]
            ]) + providerDetail(currentHome)
                + '<div class="status-recovery-copy"><strong>恢复条件</strong><p>请先确认数据库、调度心跳和数据源恢复，再由 Owner 手动重试。此面板不执行恢复动作。</p></div>';
        } catch (_) {
            target.innerHTML = '<div class="status-recovery-copy"><strong>系统状态当前不可查看</strong><p>未返回可信运行状态；不会自动触发任何恢复动作。</p></div>';
        }
    }

    function bindHomeStatus() {
        document.addEventListener("click", function (event) {
            var open = event.target.closest("[data-open-home-status]");
            if (open) {
                event.preventDefault();
                openHomeStatus(open);
                return;
            }
            var close = event.target.closest("[data-close-home-status]");
            if (close) {
                event.preventDefault();
                close.closest("dialog")?.close();
            }
        });
        document.getElementById("homeStatusDialog")?.addEventListener("cancel", function (event) {
            event.preventDefault();
            event.currentTarget.close();
        });
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
        if (!candidateStateLegal(core.opportunityState, candidate.planMode)) {
            return '<div class="ai-unavailable"><strong>GPT 综合判断</strong><span>机会状态与候选参与方式不一致，当前不可查看</span></div>';
        }
        var context = '<div class="ai-context-line" aria-label="方向判断、机会进度、候选参与方式"><span>方向判断 · '
            + escapeHtml(text(core.direction || core.marketBias, "当前不可查看")) + '</span><span>机会进度 · '
            + escapeHtml(label(core.opportunityState, "当前不可查看")) + '</span><span>候选参与方式 · '
            + escapeHtml(label(candidate.planMode, "当前不可查看")) + '</span></div>';
        return context + aiReadableResult("GPT 综合判断 · 非最终计划",
            candidateConclusion(candidate.summary || core.text, core.opportunityState),
            role.supportingEvidence,
            itemText((role.opposingEvidence || [])[0]) || candidate.riskExplanation,
            candidate.invalidCondition || candidate.revalidationRule,
            candidate.recommendedAction || label(candidate.planMode, "等待"));
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
        var reasons = (role.logicConflicts || []).concat(role.evidenceGaps || []);
        return aiReadableResult("Gemini 冲突复核",
            "复核结果：" + label(reviewResult, "当前不可查看"), reasons,
            itemText((role.underestimatedRisks || [])[0]) || role.riskAdjustment,
            role.recoveryCondition || suggestion.recoveryCondition,
            role.planModeAdjustment || suggestion.after || "等待");
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
            return '<div class="failure-path-chain"><strong>' + escapeHtml(text(path.hypothesis, "失败路径")) + '</strong><ol aria-label="触发 → 演化 → 失效">'
                + '<li><small>触发</small><span>' + escapeHtml(text(path.triggerCondition, "当前不可查看")) + '</span></li>'
                + '<li><small>演化</small><span>' + escapeHtml(text(path.causalPath, "当前不可查看")) + '</span></li>'
                + '<li><small>失效</small><span>' + escapeHtml(text(path.invalidatingEvidence, "当前不可查看")) + '</span></li></ol></div>';
        }).join("");
    }
    function renderGrok(role) {
        var failurePath = failurePathStateView(role);
        var firstPath = failurePath.paths[0] || {};
        var reasons = (role.opposingScenarios || []).concat(role.watchIndicators || []);
        return aiReadableResult("Grok 反方挑战",
            role.challengeSummary || role.currentDirectionChallenge || failurePath.label,
            reasons,
            itemText((role.externalEventRisks || [])[0]) || itemText((role.microstructureRisks || [])[0]),
            firstPath.invalidatingEvidence || firstPath.triggerCondition,
            role.planModeImpact || "观察反方情景");
    }

    function aiReadableResult(roleName, conclusion, reasons, risk, invalidation, action) {
        var values = (Array.isArray(reasons) ? reasons : []).map(itemText).filter(Boolean).slice(0, 3);
        return '<div class="ai-readable-result"><header><strong>' + escapeHtml(roleName) + '</strong></header><section><h3>一句话结论</h3><p>'
            + escapeHtml(text(conclusion, "当前结论不可查看")) + '</p></section><section><h3>核心依据</h3>'
            + (values.length ? '<ol>' + values.map(function (value) { return '<li>' + escapeHtml(value) + '</li>'; }).join("") + '</ol>'
                : '<p>' + escapeHtml(roleName + " 尚未返回足够依据") + '</p>')
            + '</section><section><h3>最大风险或反方情景</h3><p>' + escapeHtml(text(risk, "当前未返回明确风险"))
            + '</p></section><section><h3>判断失效条件</h3><p>' + escapeHtml(text(invalidation, "等待下一次闭线或关键证据变化"))
            + '</p></section><section><h3>当前建议</h3><p>' + escapeHtml(label(action, text(action, "等待"))) + '</p></section></div>';
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
    function aiMatchesSelectedSnapshot(home, ai) {
        var asset = home && home.selectedAssetContext || {};
        var roles = typeof contract.normalizeAiTabs === "function"
            ? contract.normalizeAiTabs(ai && ai.tabs) : (Array.isArray(ai && ai.tabs) ? ai.tabs : []);
        var available = roles.filter(function (role) { return role && role.resultAvailable === true; });
        if (!has(ai && ai.analysisId) && !has(ai && ai.decisionId) && !available.length) return true;
        if (!has(asset.analysisId) || !has(asset.decisionId) || !has(asset.traceId)
                || !has(ai && ai.analysisId) || !has(ai && ai.decisionId)
                || String(asset.analysisId) !== String(ai.analysisId)
                || String(asset.decisionId) !== String(ai.decisionId)) return false;
        return available.every(function (role) {
            return has(role.analysisId) && has(role.decisionId) && has(role.traceId)
                && String(role.analysisId) === String(asset.analysisId)
                && String(role.decisionId) === String(asset.decisionId)
                && String(role.traceId) === String(asset.traceId);
        });
    }
    function currentAiCompleteForAsset(asset) {
        var ai = currentHome && currentHome.aiDecision || {};
        if (!asset || !aiMatchesSelectedSnapshot({ selectedAssetContext: asset }, ai)) return false;
        var roles = typeof contract.normalizeAiTabs === "function"
            ? contract.normalizeAiTabs(ai.tabs) : (Array.isArray(ai.tabs) ? ai.tabs : []);
        return roles.length === 3 && roles.every(function (role) {
            return role && role.resultAvailable === true
                && String(role.analysisId || "") === String(asset.analysisId || "")
                && String(role.decisionId || "") === String(asset.decisionId || "")
                && String(role.traceId || "") === String(asset.traceId || "");
        });
    }
    function renderAi(home) {
        var ai = home.aiDecision || {};
        var roles = typeof contract.normalizeAiTabs === "function" ? contract.normalizeAiTabs(ai.tabs) : (Array.isArray(ai.tabs) ? ai.tabs : []);
        var role = roles.find(function (item) { return item.role === activeRole; });
        var panel = document.getElementById("aiRolePanel");
        setText("aiContext", symbolOf(home.selectedAssetContext || { symbol: home.selectedSymbol }) || "等待分析上下文");
        if (!aiMatchesSelectedSnapshot(home, ai)) {
            panel.innerHTML = '<div class="empty-state"><strong>当前结果已过期，等待当前批次重新分析</strong>'
                + '<span>不会把其他 Analysis 或 Decision 的结果拼接到当前资产。</span></div>';
            setText("aiMetadata", "当前同批次审计链尚未形成");
            var staleAudit = document.getElementById("auditChainLink");
            staleAudit.removeAttribute("href");
            staleAudit.textContent = "当前同批次审计链尚未形成";
            staleAudit.setAttribute("aria-disabled", "true");
            renderConflict({ aiDecision: { consistency: {} } });
            return;
        }
        var roleContent;
        if (!role || role.resultAvailable !== true) roleContent = roleUnavailable(role);
        else if (activeRole === "GPT_FINAL") roleContent = renderGpt(role);
        else if (activeRole === "GEMINI_REVIEW") roleContent = renderGemini(role);
        else roleContent = renderGrok(role);
        panel.innerHTML = roleContent;
        var provenanceCopy = "AI Analysis " + shortId((role && role.analysisId) || ai.analysisId)
            + " · Decision " + shortId((role && role.decisionId) || ai.decisionId)
            + " · Trace " + shortId(role && role.traceId)
            + " · 生成时间 " + time(role && role.generatedAt);
        setText("aiMetadata", (role ? "本轮状态 " + text(role.runStatusLabel, role.statusMessage)
            + " · 模型来源 " + text(role.provider, "尚未记录") + " · " : "") + provenanceCopy);
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
        var sequence = ++homeRequestSequence;
        if (homeAbortController) homeAbortController.abort();
        var controller = new AbortController();
        homeAbortController = controller;
        try {
            var query = new URLSearchParams({ limit: "6" });
            if (symbol) query.set("selectedSymbol", symbol);
            var requestedPositionId = new URLSearchParams(window.location.search).get("positionId");
            if (requestedPositionId && /^\d+$/.test(requestedPositionId)) {
                query.set("positionId", requestedPositionId);
            }
            var fresh = await api("/api/dashboard/home?" + query.toString(), { signal: controller.signal });
            if (sequence !== homeRequestSequence) return;
            if (!fresh || fresh.snapshotComplete !== true || !fresh.snapshotId
                    || !Number.isSafeInteger(fresh.projectionVersion)) throw new Error("完整快照尚未就绪，正在重试");
            if (Number(currentHome.projectionVersion || 0) >= fresh.projectionVersion) return;
            if (!Array.isArray(fresh.assets) || !Array.isArray(fresh.assetPool)
                    || !Number.isInteger(fresh.homeAssetCount) || fresh.homeAssetCount !== fresh.assets.length
                    || fresh.homeAssetCount < 0 || fresh.homeAssetCount > 6) {
                throw new Error("重点资产完整集合尚未返回，保留上一份数据");
            }
            var available = fresh.assetPool;
            var cardSymbols = fresh.assets.map(symbolOf);
            if (new Set(cardSymbols).size !== cardSymbols.length) throw new Error("重点资产集合含重复项，保留上一份数据");
            fresh.assets.forEach(function (card) {
                var cardSymbol = symbolOf(card);
                var shared = available.find(function (asset) { return symbolOf(asset) === cardSymbol; });
                if (!shared || card.snapshotId !== fresh.snapshotId || JSON.stringify(card) !== JSON.stringify(shared)) {
                    throw new Error(cardSymbol + " 同批次快照未返回");
                }
            });
            if (fresh.selectedAssetContext) {
                var selectedContext = available.find(function (asset) {
                    return symbolOf(asset) === symbolOf(fresh.selectedAssetContext);
                });
                if (!selectedContext && fresh.selectedContextState === "EXITED_TOP6" && fresh.selectedContextExitReason) {
                    selectedContext = fresh.selectedAssetContext;
                }
                if (!selectedContext || selectedContext.snapshotId !== fresh.snapshotId
                        || symbol && symbolOf(selectedContext) !== symbol
                        || selectedContext.analysisId !== fresh.selectedAssetContext.analysisId
                        || selectedContext.decisionId !== fresh.selectedAssetContext.decisionId
                        || selectedContext.traceId !== fresh.selectedAssetContext.traceId) {
                    throw new Error("所选资产的快照关联未确认，保留上一份数据");
                }
                fresh.selectedAssetContext = selectedContext;
            }
            homeCardSymbols = cardSymbols;
            render(fresh);
            clearHomeRequestFailure();
        } catch (error) {
            if (error && error.name === "AbortError") return;
            if (sequence !== homeRequestSequence) return;
            if (Number(error.status) === 401 || Number(error.status) === 403) {
                announce("登录会话已失效，请重新登录后继续。当前页面数据已保留。");
                return;
            }
            reportHomeRequestFailure(error);
            if (!currentHome.snapshotId) {
                render({ states: { overall: "ERROR" }, diagnostics: {}, assets: [], positions: [], aiDecision: { tabs: [] } });
            }
        } finally {
            if (homeAbortController === controller) homeAbortController = null;
        }
    }

    function liveAsset(symbol) {
        var normalized = String(symbol || "").toUpperCase();
        return (Array.isArray(currentHome.assets) ? currentHome.assets : []).find(function (asset) {
            return symbolOf(asset) === normalized;
        });
    }
    function acceptLiveSnapshot(event) {
        var symbol = String(event && event.symbol || "SYSTEM").toUpperCase();
        var identity = symbol + "|" + String(event && event.eventType || "UNKNOWN").toUpperCase();
        var version = Number(event && event.snapshotVersion);
        if (!Number.isFinite(version) || version <= 0) return false;
        var current = Number(liveSnapshotVersions.get(identity) || 0);
        if (version <= current) return false;
        liveSnapshotVersions.set(identity, version);
        return true;
    }
    function liveCard(symbol) {
        var escaped = window.CSS && typeof window.CSS.escape === "function" ? window.CSS.escape(symbol) : symbol;
        return document.querySelector('.opportunity-card[data-symbol="' + escaped + '"]');
    }
    function sameLiveDecision(asset, payload) {
        if (!asset || !payload) return false;
        if (has(payload.analysisId) && has(asset.analysisId)
                && String(payload.analysisId) !== String(asset.analysisId)) return false;
        if (has(payload.decisionId) && has(asset.decisionId)
                && String(payload.decisionId) !== String(asset.decisionId)) return false;
        return true;
    }
    function applyHomeLiveEvent(event) {
        if (String(event && event.eventType || "").startsWith("ASSET_CARD_")) { applyAssetCardEvent(event); return; }
        // The shared futures Mark stream still serves monitoring, never the Spot-only card surface.
        if (event && event.eventType === "ASSET_PRICE_UPDATED") return;
        if (!event || !acceptLiveSnapshot(event)) return;
        if (homeRefreshQueued || homeAbortController) return;
        homeRefreshQueued = true;
        window.setTimeout(function () {
            homeRefreshQueued = false;
            if (!document.hidden) loadHome(selectedSymbol);
        }, 250);
    }
    async function lightweightHomeRefresh() {
        if (document.hidden || !homeCardSymbols.length) return;
        // Preserve the pre-switch periodic read while any legacy card is displayed. This is not an event fallback.
        if (homeCardSymbols.some(function (symbol) { return !assetCardDisplaySymbols.has(symbol); })) return loadHome(selectedSymbol);
        var symbols = homeCardSymbols.filter(function (symbol) { return assetCardDisplaySymbols.has(symbol); });
        if (!symbols.length) return;
        var displayEpoch = assetCardDisplayEpoch;
        var sequence = ++assetCardRequestSequence;
        if (assetCardAbortController) assetCardAbortController.abort();
        var controller = new AbortController();
        assetCardAbortController = controller;
        try {
            var query = new URLSearchParams({ view: "ASSET_CARDS", symbols: symbols.join(",") });
            var snapshots = await api("/api/dashboard/runtime-snapshot?" + query.toString(), { signal: controller.signal });
            if (sequence !== assetCardRequestSequence || displayEpoch !== assetCardDisplayEpoch || document.hidden) return;
            if (!Array.isArray(snapshots)) throw new Error("资产卡片快照未返回");
            snapshots.forEach(function (snapshot) {
                var symbol = String(snapshot && snapshot.symbol || "").toUpperCase();
                if (!snapshot || symbols.indexOf(symbol) < 0) return;
                var current = assetCardSnapshots.get(symbol);
                if (current && Number(snapshot.snapshotVersion) < current.snapshotVersion) return;
                if (!applyReadSafetyDowngrade(snapshot, sequence)) mergeAssetCardSnapshot(snapshot, true);
            });
        } catch (error) {
            if (!error || error.name !== "AbortError") throw error;
        } finally {
            if (assetCardAbortController === controller) assetCardAbortController = null;
        }
    }
    function reportAssetCardRequestFailure() {
        announce("资产卡片对账暂不可用，保留已有数据并等待重试");
    }
    function scheduleHomeFallbackPoll() {
        if (document.hidden) return;
        var policy = window.TrineDesktopSemantics.refreshPolicy;
        var delay = homeStreamConnected ? policy.reconcileMs : policy.disconnectedMs;
        if (homeFallbackTimer && homePollIntervalMs === delay) return;
        stopHomeFallbackPoll();
        homePollIntervalMs = delay;
        homeFallbackTimer = window.setInterval(function () {
            if (!document.hidden && !homeAbortController) lightweightHomeRefresh().catch(reportAssetCardRequestFailure);
        }, delay);
    }
    function stopHomeFallbackPoll() {
        if (!homeFallbackTimer) return;
        window.clearInterval(homeFallbackTimer);
        homeFallbackTimer = null;
        homePollIntervalMs = 0;
    }
    function connectHomeStream() {
        if (!window.EventSource) {
            scheduleHomeFallbackPoll();
            return;
        }
        if (homeEventSource) homeEventSource.close();
        homeEventSource = new EventSource("/api/dashboard/stream");
        ["ASSET_CARD_PRICE", "ASSET_CARD_SIGNAL", "ASSET_CARD_RISK", "ASSET_CARD_HEALTH"].forEach(function (type) {
            homeEventSource.addEventListener(type, function (message) {
                try { applyAssetCardEvent(Object.assign({}, JSON.parse(message.data), { eventType: type })); }
                catch (_) { announce("资产卡片实时内容格式异常，等待卡片对账"); }
            });
        });
        ["ASSET_PRICE_UPDATED", "ASSET_DIRECTION_UPDATED", "ASSET_RISK_UPDATED", "PLAN_STATE_CHANGED",
            "POSITION_MONITOR_UPDATED", "SYSTEM_STATUS_UPDATED", "DATA_SOURCE_STATUS_CHANGED"].forEach(function (type) {
            homeEventSource.addEventListener(type, function (message) {
                try { applyHomeLiveEvent(JSON.parse(message.data)); }
                catch (_) { announce("实时更新内容格式异常，等待完整对账"); }
            });
        });
        homeEventSource.onopen = function () {
            homeStreamConnected = true;
            scheduleHomeFallbackPoll();
            lightweightHomeRefresh().catch(reportAssetCardRequestFailure);
            homeLiveState = "实时已连接·60秒对账";
            renderHeader(currentHome);
            announce("首页实时更新已连接");
        };
        homeEventSource.onerror = function () {
            homeStreamConnected = false;
            homeLiveState = "重连中·15秒轮询";
            renderHeader(currentHome);
            scheduleHomeFallbackPoll();
        };
    }
    function startHomeLiveRuntime() {
        if (homeRuntimeStarted) return;
        homeRuntimeStarted = true;
        connectHomeStream();
        scheduleHomeFallbackPoll();
        document.addEventListener("visibilitychange", function () {
            if (document.hidden) {
                stopHomeFallbackPoll();
                if (assetCardAbortController) assetCardAbortController.abort();
                if (homeEventSource) homeEventSource.close();
                homeEventSource = null;
                homeStreamConnected = false;
                return;
            }
            loadHome(selectedSymbol);
            connectHomeStream();
            scheduleHomeFallbackPoll();
        });
        window.addEventListener("beforeunload", function () {
            if (homeEventSource) homeEventSource.close();
            stopHomeFallbackPoll();
            if (assetCardAbortController) assetCardAbortController.abort();
            assetCardPriceTimers.forEach(function (timer) { window.clearTimeout(timer); });
            assetCardPriceTimers.clear();
        });
    }

    function stableSubmissionId(prefix) {
        var value = window.crypto && typeof window.crypto.randomUUID === "function"
            ? window.crypto.randomUUID()
            : Date.now().toString(36) + "-" + Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
        return prefix + ":" + value;
    }
    function analysisPreviewKey(symbol, analysisId) {
        return "analysis-preview:" + String(symbol || "").trim().toUpperCase() + ":1h:"
            + String(analysisId || "search").trim();
    }
    function analysisPreviewSubmission(symbol, analysisId) {
        var key = analysisPreviewKey(symbol, analysisId);
        var saved = readDraft(key) || {};
        if (!saved.submissionId) saved.submissionId = stableSubmissionId("analysis-preview");
        saved.symbol = String(symbol || "").trim().toUpperCase();
        saved.timeframe = "1h";
        saved.sourceAnalysisId = analysisId || null;
        writeDraft(key, saved);
        return saved;
    }
    function rememberAnalysisPreview(symbol, current, result) {
        var saved = Object.assign({}, current || {}, {
            taskId: result && result.taskId,
            taskState: result && result.taskState,
            taskStage: result && result.taskStage,
            analysisId: result && result.analysisId,
            traceId: result && result.traceId
        });
        writeDraft(analysisPreviewKey(symbol, saved.sourceAnalysisId), saved);
        return saved;
    }
    function clearAnalysisPreview(symbol, analysisId) {
        removeDraft(analysisPreviewKey(symbol, analysisId));
    }
    async function recoverAnalysisPreviewTask(taskId) {
        if (!taskId) return null;
        for (var attempt = 0; attempt < 20; attempt++) {
            var tasks = await api("/api/workspace/tasks?limit=30");
            var task = (Array.isArray(tasks) ? tasks : []).find(function (item) {
                return item && item.taskId === taskId;
            });
            if (task && task.resultResourceId) return task;
            if (task && ["FAILED", "CANCELLED"].indexOf(String(task.state || "").toUpperCase()) >= 0) {
                throw new Error(text(task.errorMessage, "分析任务未完成"));
            }
            await new Promise(function (resolve) { window.setTimeout(resolve, 500); });
        }
        return null;
    }
    function openOrResumeAssetAnalysis(asset) {
        return (async function () {
            var symbol = symbolOf(asset);
            var analysisId = asset && asset.analysisId;
            if (!symbol || !analysisId) {
                announce("当前资产缺少可追溯分析，正在刷新");
                await loadHome(symbol || selectedSymbol);
                return;
            }
            var busyKey = symbol + ":" + analysisId;
            if (assetAnalysisBusy.has(busyKey)) return;
            assetAnalysisBusy.add(busyKey);
            announce(symbol + " 三 AI 分析启动中");
            try {
                await loadHome(symbol);
                var refreshedAsset = liveAsset(symbol);
                analysisId = refreshedAsset && refreshedAsset.analysisId || analysisId;
                if (!analysisId) throw new Error("当前资产缺少可追溯分析");
                if (currentAiCompleteForAsset(refreshedAsset)) {
                    announce(symbol + " 三 AI 分析已恢复");
                    return;
                }
                var previewState = analysisPreviewSubmission(symbol, analysisId);
                var result = await api("/api/asset-pool/search/" + encodeURIComponent(symbol)
                    + "/analysis-preview?timeframe=1h&submissionId="
                    + encodeURIComponent(previewState.submissionId), { method: "POST" });
                previewState = rememberAnalysisPreview(symbol, previewState, result);
                if ((!result || !result.analysisId) && result && result.taskId) {
                    var recovered = await recoverAnalysisPreviewTask(result.taskId);
                    if (recovered && recovered.resultResourceId) {
                        result.analysisId = recovered.resultResourceId;
                        result.traceId = recovered.traceId;
                        rememberAnalysisPreview(symbol, previewState, result);
                    }
                }
                if (!result || !result.analysisId) throw new Error("分析任务尚未返回结果标识");
                await loadHome(symbol);
                announce(symbol + " 三 AI 分析已在首页更新");
            } catch (error) {
                announce(error.message);
                await loadHome(symbol);
            } finally {
                assetAnalysisBusy.delete(busyKey);
            }
        })();
    }
    function readDraft(key) {
        try { return JSON.parse(window.sessionStorage.getItem(key) || "null"); }
        catch (_) { return null; }
    }
    function writeDraft(key, value) {
        try { window.sessionStorage.setItem(key, JSON.stringify(value)); }
        catch (_) { /* The server idempotency contract remains authoritative when storage is unavailable. */ }
    }
    function removeDraft(key) {
        try { window.sessionStorage.removeItem(key); }
        catch (_) { /* no-op */ }
    }
    function localDateTimeValue(date) {
        var offset = date.getTimezoneOffset() * 60000;
        return new Date(date.getTime() - offset).toISOString().slice(0, 16);
    }
    function formSnapshot(form) {
        return Object.fromEntries(new FormData(form).entries());
    }
    function restoreForm(form, values) {
        Object.entries(values || {}).forEach(function (entry) {
            if (form.elements[entry[0]]) form.elements[entry[0]].value = entry[1];
        });
    }
    function setFormStatus(id, message, tone) {
        var node = document.getElementById(id);
        if (!node) return;
        node.textContent = message || "";
        node.classList.toggle("is-error", tone === "error");
        node.classList.toggle("is-success", tone === "success");
    }
    function setSubmitBusy(form, busy, busyText, idleText) {
        var button = form && form.querySelector('button[type="submit"]');
        if (!button) return;
        button.disabled = busy;
        button.textContent = busy ? busyText : idleText;
    }
    function openEntryDialog(trigger) {
        var dialog = document.getElementById("homePositionEntryDialog");
        var form = document.getElementById("homePositionEntryForm");
        var key = "trine.position.openDraft";
        var draft = readDraft(key);
        form.reset();
        form.elements.sourceType.value = "MANUAL_INDEPENDENT";
        form.elements.submissionId.value = stableSubmissionId("position-open");
        form.elements.openedAt.value = localDateTimeValue(new Date());
        var freshSubmissionId = form.elements.submissionId.value;
        var freshOpenedAt = form.elements.openedAt.value;
        if (selectedSymbol) form.elements.assetSymbol.value = selectedSymbol;
        if (draft) restoreForm(form, draft);
        form.elements.sourceType.value = "MANUAL_INDEPENDENT";
        if (!has(form.elements.submissionId.value)) form.elements.submissionId.value = freshSubmissionId;
        if (!has(form.elements.openedAt.value)) form.elements.openedAt.value = freshOpenedAt;
        writeDraft(key, formSnapshot(form));
        dialog.dataset.restoreFocusId = trigger && trigger.id || "";
        setFormStatus("homePositionEntryStatus", draft ? "已恢复未提交内容" : "", "");
        dialog.showModal();
        form.querySelector("input, select")?.focus();
    }
    function openCloseDialog(positionId, symbol, trigger) {
        var dialog = document.getElementById("homePositionCloseDialog");
        var form = document.getElementById("homePositionCloseForm");
        var key = "trine.position.closeDraft." + positionId;
        var draft = readDraft(key);
        activeClosePositionId = positionId;
        form.reset();
        form.elements.submissionId.value = stableSubmissionId("position-close");
        form.elements.closedAt.value = localDateTimeValue(new Date());
        var freshSubmissionId = form.elements.submissionId.value;
        var freshClosedAt = form.elements.closedAt.value;
        if (draft) restoreForm(form, draft);
        if (!has(form.elements.submissionId.value)) form.elements.submissionId.value = freshSubmissionId;
        if (!has(form.elements.closedAt.value)) form.elements.closedAt.value = freshClosedAt;
        writeDraft(key, formSnapshot(form));
        document.getElementById("homePositionCloseHeading").textContent = "记录平仓 · " + symbol;
        dialog.dataset.restoreFocusId = trigger && trigger.id || "";
        setFormStatus("homePositionCloseStatus", draft ? "已恢复未提交内容" : "", "");
        dialog.showModal();
        form.querySelector("input")?.focus();
    }
    function openArchiveDialog(positionId, symbol, trigger) {
        var dialog = document.getElementById("homePositionArchiveDialog");
        var form = document.getElementById("homePositionArchiveForm");
        if (!dialog || !form || !positionId) return;
        var position = (Array.isArray(currentHome.positions) ? currentHome.positions : []).find(function (item) {
            return String(item.positionId) === String(positionId);
        }) || {};
        if (!manualPositionArchiveVisible(position)) return;
        activeArchivePositionId = String(positionId);
        form.reset();
        form.dataset.submitting = "false";
        form.elements.submissionId.value = stableSubmissionId("position-mistake-archive");
        document.getElementById("homePositionArchiveHeading").textContent = "删除误录 · " + symbol;
        dialog.dataset.restoreFocusId = trigger && trigger.id || "";
        setFormStatus("homePositionArchiveStatus",
            "请确认：" + symbol + " · 开仓时间 " + time(position.openedAt) + " · 开仓价 " + number(position.entryPrice)
                + "。仅移除本系统中的持仓监控记录，不会在交易所平仓或执行交易。", "");
        dialog.showModal();
        form.querySelector("textarea")?.focus();
    }
    function auditField(labelText, value, copyable) {
        var display = has(value) ? String(value) : "当前不可查看";
        return '<div class="audit-field"><small>' + escapeHtml(labelText) + '</small><code>' + escapeHtml(display)
            + '</code>' + (copyable && has(value) ? '<button type="button" data-copy-audit-value="' + escapeHtml(value) + '">复制</button>' : '') + '</div>';
    }
    function openAuditDialog(title, fields, trigger) {
        var dialog = document.getElementById("homeAuditDialog");
        var target = document.getElementById("homeAuditDetail");
        if (!dialog || !target) return;
        document.getElementById("homeAuditHeading").textContent = title;
        target.innerHTML = fields.join("");
        dialog.dataset.restoreFocusId = trigger && trigger.id || "";
        dialog.showModal();
    }
    function openAssetAudit(trigger) {
        var asset = liveAsset(selectedSymbol) || currentHome.selectedAssetContext || {};
        var provenance = assetProvenance(asset);
        var roles = currentHome.aiDecision && Array.isArray(currentHome.aiDecision.tabs)
            ? currentHome.aiDecision.tabs : [];
        var role = roles.find(function (item) { return item && item.role === activeRole; }) || {};
        var formation = role.candidateSummary && (role.candidateSummary.summary || role.candidateSummary.recommendedAction)
            || role.coreJudgment && role.coreJudgment.text || role.challengeSummary;
        var supportingItems = role.supportingEvidence || role.logicConflicts || role.opposingScenarios;
        var opposingItems = role.opposingEvidence || role.underestimatedRisks || role.externalEventRisks;
        var supporting = (Array.isArray(supportingItems) ? supportingItems : [])
            .map(itemText).filter(Boolean).join("；");
        var opposing = (Array.isArray(opposingItems) ? opposingItems : [])
            .map(itemText).filter(Boolean).join("；");
        openAuditDialog("资产审计详情 · " + (symbolOf(asset) || "当前资产"), [
            auditField("Analysis ID", provenance.analysisId, true),
            auditField("Decision ID", provenance.decisionId, true),
            auditField("Trace ID", provenance.traceId, true),
            auditField("方向计算时间", time(provenance.directionCalculatedAt)),
            auditField("行情批次时间", time(provenance.marketDataAsOf)),
            auditField("决策时价格", asset.priceAtDecision),
            auditField("当前价格", asset.latestPrice),
            auditField("价格更新时间", time(provenance.latestPriceAt)),
            auditField("计划状态", provenance.planState),
            auditField("失效位", provenance.planInvalidationLevel),
            auditField("形成原因", formation),
            auditField("支持证据", supporting),
            auditField("反对证据", opposing)
        ], trigger);
    }
    function openPositionAudit(positionId, trigger) {
        var position = (Array.isArray(currentHome.positions) ? currentHome.positions : []).find(function (item) {
            return String(item.positionId) === String(positionId);
        }) || {};
        openAuditDialog("持仓审计详情 · " + (symbolOf(position) || positionId), [
            auditField("Position ID", position.positionId, true),
            auditField("Analysis ID", position.analysisId, true),
            auditField("Decision ID", position.decisionId, true),
            auditField("Trace ID", position.traceId, true),
            auditField("行情来源", position.markPriceSource),
            auditField("监控时间", time(position.lastMonitorAt || position.markPriceObservedAt)),
            auditField("监控状态", position.monitorTrustState),
            auditField("完整监控证据", position.monitorEvidence || position.monitorConclusion)
        ], trigger);
    }
    function closePositionDialog(dialog) {
        if (dialog && dialog.open) dialog.close();
    }
    function bindPositionActions() {
        var entryForm = document.getElementById("homePositionEntryForm");
        var closeForm = document.getElementById("homePositionCloseForm");
        var archiveForm = document.getElementById("homePositionArchiveForm");
        preserveDateTimeDialogOnEscape(entryForm, "homePositionEntryStatus");
        preserveDateTimeDialogOnEscape(closeForm, "homePositionCloseStatus");
        document.addEventListener("click", function (event) {
            var openEntry = event.target.closest("[data-open-position-entry]");
            if (openEntry) { event.preventDefault(); openEntryDialog(openEntry); return; }
            var openClose = event.target.closest("[data-close-position-id]");
            if (openClose) { event.preventDefault(); openCloseDialog(openClose.dataset.closePositionId, openClose.dataset.closePositionSymbol, openClose); return; }
            var openArchive = event.target.closest("[data-archive-position-id]");
            if (openArchive) { event.preventDefault(); openArchiveDialog(openArchive.dataset.archivePositionId, openArchive.dataset.archivePositionSymbol, openArchive); return; }
            var positionAudit = event.target.closest("[data-open-position-audit]");
            if (positionAudit) { event.preventDefault(); openPositionAudit(positionAudit.dataset.openPositionAudit, positionAudit); return; }
            var assetAudit = event.target.closest("[data-open-selected-audit]");
            if (assetAudit) { event.preventDefault(); openAssetAudit(assetAudit); return; }
            var closeAudit = event.target.closest("[data-close-home-audit]");
            if (closeAudit) { event.preventDefault(); closePositionDialog(closeAudit.closest("dialog")); return; }
            var copy = event.target.closest("[data-copy-audit-value]");
            if (copy) {
                event.preventDefault();
                navigator.clipboard.writeText(copy.dataset.copyAuditValue).then(function () {
                    copy.textContent = "已复制";
                }).catch(function () { announce("复制失败，请手动选择编号"); });
                return;
            }
            var close = event.target.closest("[data-close-position-dialog]");
            if (close) { event.preventDefault(); closePositionDialog(close.closest("dialog")); }
        });
        [entryForm, closeForm].forEach(function (form) {
            form?.addEventListener("input", function () {
                var key = form === entryForm ? "trine.position.openDraft" : "trine.position.closeDraft." + activeClosePositionId;
                writeDraft(key, formSnapshot(form));
            });
            form?.closest("dialog")?.addEventListener("cancel", function (event) {
                event.preventDefault();
                setFormStatus(form === entryForm ? "homePositionEntryStatus" : "homePositionCloseStatus", "内容已保留；请使用取消或关闭按钮退出", "");
            });
        });
        entryForm?.addEventListener("submit", async function (event) {
            event.preventDefault();
            var values = formSnapshot(entryForm);
            if (values.openedAt) values.openedAt = new Date(values.openedAt).toISOString().slice(0, 19);
            setSubmitBusy(entryForm, true, "正在保存", "确认录入");
            setFormStatus("homePositionEntryStatus", "正在保存", "");
            try {
                await api("/api/user-positions/manual-open", { method: "POST", body: JSON.stringify(values) });
                removeDraft("trine.position.openDraft");
                setText("positionActionStatus", "持仓录入成功");
                announce("持仓录入成功");
                closePositionDialog(entryForm.closest("dialog"));
                entryForm.reset();
                await loadHome(selectedSymbol);
            } catch (error) {
                setFormStatus("homePositionEntryStatus", error.message, "error");
                announce(error.message);
            } finally { setSubmitBusy(entryForm, false, "正在保存", "确认录入"); }
        });
        closeForm?.addEventListener("submit", async function (event) {
            event.preventDefault();
            var positionId = activeClosePositionId;
            var values = formSnapshot(closeForm);
            if (values.closedAt) values.closedAt = new Date(values.closedAt).toISOString().slice(0, 19);
            setSubmitBusy(closeForm, true, "正在保存", "确认记录");
            setFormStatus("homePositionCloseStatus", "正在保存", "");
            try {
                await api("/api/user-positions/" + encodeURIComponent(positionId) + "/manual-close", { method: "POST", body: JSON.stringify(values) });
                removeDraft("trine.position.closeDraft." + positionId);
                setText("positionActionStatus", "平仓记录成功");
                announce("平仓记录成功");
                closePositionDialog(closeForm.closest("dialog"));
                closeForm.reset();
                activeClosePositionId = "";
                await loadHome(selectedSymbol);
            } catch (error) {
                setFormStatus("homePositionCloseStatus", error.message, "error");
                announce(error.message);
            } finally { setSubmitBusy(closeForm, false, "正在保存", "确认记录"); }
        });
        archiveForm?.addEventListener("submit", async function (event) {
            event.preventDefault();
            if (archiveForm.dataset.submitting === "true") return;
            archiveForm.dataset.submitting = "true";
            var positionId = activeArchivePositionId;
            var values = formSnapshot(archiveForm);
            setSubmitBusy(archiveForm, true, "正在归档", "确认归档误录");
            setFormStatus("homePositionArchiveStatus", "正在归档误录记录", "");
            try {
                await api("/api/user-positions/" + encodeURIComponent(positionId) + "/mistake-archive",
                    { method: "POST", body: JSON.stringify(values) });
                setText("positionActionStatus", "误录记录已归档");
                announce("误录记录已归档；未生成平仓或交易记录");
                closePositionDialog(archiveForm.closest("dialog"));
                archiveForm.reset();
                activeArchivePositionId = "";
                await loadHome(selectedSymbol);
            } catch (error) {
                setFormStatus("homePositionArchiveStatus", humanReason(error.message, "归档失败，请稍后重试"), "error");
                announce(humanReason(error.message, "归档失败，请稍后重试"));
            } finally {
                archiveForm.dataset.submitting = "false";
                setSubmitBusy(archiveForm, false, "正在归档", "确认归档误录");
            }
        });
        document.getElementById("homeAuditDialog")?.addEventListener("cancel", function (event) {
            event.preventDefault();
            event.currentTarget.close();
        });
    }

    function preserveDateTimeDialogOnEscape(form, statusId) {
        form?.querySelectorAll('input[type="datetime-local"]').forEach(function (input) {
            input.addEventListener("keydown", function (event) {
                if (event.key !== "Escape") return;
                event.preventDefault();
                event.stopImmediatePropagation();
                setFormStatus(statusId, "日期时间内容已保留；可继续选择或使用取消按钮退出", "");
            }, true);
        });
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
                var previewSymbol = symbolOf(selectedSearchAsset);
                var previewState = analysisPreviewSubmission(previewSymbol);
                var result = await api("/api/asset-pool/search/" + encodeURIComponent(previewSymbol)
                    + "/analysis-preview?timeframe=5m&submissionId="
                    + encodeURIComponent(previewState.submissionId), { method: "POST" });
                previewState = rememberAnalysisPreview(previewSymbol, previewState, result);
                if ((!result || !result.analysisId) && result && result.taskId) {
                    var recovered = await recoverAnalysisPreviewTask(result.taskId);
                    if (recovered && recovered.resultResourceId) {
                        result.analysisId = recovered.resultResourceId;
                        result.traceId = recovered.traceId;
                        rememberAnalysisPreview(previewSymbol, previewState, result);
                    }
                }
                if (!result || !result.analysisId) throw new Error("预览未返回分析标识");
                clearAnalysisPreview(previewSymbol);
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
                var roles = currentHome && currentHome.aiDecision
                    && Array.isArray(currentHome.aiDecision.tabs) ? currentHome.aiDecision.tabs : [];
                var role = roles.find(function (item) { return item && item.role === activeRole; });
                if (!aiMatchesSelectedSnapshot(currentHome, currentHome && currentHome.aiDecision)
                        || !role || role.resultAvailable !== true) {
                    openOrResumeAssetAnalysis(liveAsset(selectedSymbol));
                }
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

    desktop.installHoverDrawers(function (trigger) {
        if (trigger.dataset.desktopHover === "service") return desktop.serviceDrawer(currentHome);
        var symbol = String(trigger.dataset.riskSymbol || "").toUpperCase();
        if (!assetCardDisplaySymbols.has(symbol)) {
            var asset = liveAsset(symbol);
            return asset ? (trigger.dataset.desktopHover === "risk-status" ? desktop.riskDataStatus(asset) : desktop.riskDrawer(asset)) : null;
        }
        var snapshot = assetCardSnapshots.get(symbol);
        return snapshot ? assetCardRiskDrawer(snapshot) : null;
    });
    bindSearch();
    bindTabs();
    bindPositionActions();
    bindHomeStatus();
    var requested = typeof contract.readUrlParam === "function" ? contract.readUrlParam("asset") : new URLSearchParams(window.location.search).get("asset");
    loadHome(requested || "").finally(startHomeLiveRuntime);
})();
