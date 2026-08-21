(function () {
    "use strict";

    const root = document.body;
    const pageKey = root.dataset.pageKey || "";
    const resourceId = root.dataset.resourceId || "";
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "";
    const liveRegion = document.getElementById("workspaceLiveRegion");
    const frontendContract = window.TradeModelFrontendContract || {};
    const userFacingSemantic = frontendContract.USER_FACING_SEMANTIC_MAPPER || {};
    const reviewResultLabel = frontendContract.reviewResultLabel;
    const failurePathView = frontendContract.failurePathView;
    const roleGate = frontendContract.roleGate;
    const analysisModeGate = frontendContract.analysisModeGate;
    let restoreFocus = null;
    let analysisAudit = null;
    let assetPoolItems = [];
    let latestTasks = [];
    let analysisSelectedAsset = null;
    let analysisPoolSymbols = new Set();
    let analysisMode = null;

    const labels = {
        LONG: "做多", SHORT: "做空",
        LOW: "低", MEDIUM: "中", HIGH: "高", EXTREME: "极高",
        STABLE: "稳定", INCREASED: "上升", SHARPLY_INCREASED: "显著上升",
        STILL_VALID: "仍成立", WEAKENED: "弱化", INVALIDATED: "失效",
        NO_REVERSAL: "无明显反转", WEAK_REVERSAL: "弱反转", STRONG_REVERSAL: "强反转",
        NO_CLEAR_RISK_FACTOR: "暂无明显风险因素",
        OPPOSING_EVIDENCE_INCREASED: "反向证据增加",
        STRUCTURE_CHANGED: "结构变化", EVENT_IMPACT: "事件冲击",
        DATA_QUALITY_DEGRADED: "数据质量下降",
        LOGIC_VALID: "逻辑仍成立", LOGIC_WEAKENED: "逻辑弱化",
        PLAN_INVALIDATED: "计划失效", NEAR_STOP_LOSS: "接近止损",
        NEAR_TAKE_PROFIT: "接近止盈", HIGH_RISK_OBSERVATION: "高风险观察",
        WAIT_USER_CONFIRM_CLOSE: "等待用户确认平仓",
        CONTINUE_HOLD: "继续持有", NO_ADD_POSITION: "暂不加仓",
        REDUCE_POSITION: "降低仓位", TIGHTEN_STOP: "收紧止损",
        MOVE_STOP: "移动止损", PARTIAL_TAKE_PROFIT: "分批止盈",
        WAIT_CONFIRMATION: "等待人工确认", RECORD_CLOSE_REVIEW: "记录平仓并进入复盘",
        SYSTEM_PLAN_POSITION: "系统计划", MANUAL_POSITION: "独立录入", MANUAL_INDEPENDENT: "独立录入",
        OPEN_MONITORING: "持续监控", WAITING_MONITOR_DATA: "等待监控数据",
        RISK_ESCALATED: "风险升级", CLOSED: "已平仓", NO_POSITION: "暂无持仓",
        CONFIRMATION: "确认型", PREPARATION: "预备型", REDUCED: "缩减型",
        OBSERVATION: "观察", BLOCKED: "禁止",
        CURRENT: "当前有效", NEEDS_REVALIDATION: "需要重新校验",
        SUPERSEDED: "已被新版本替代", TRACKING_STOPPED: "已停止跟踪",
        EXPIRED: "已过期",
        ANALYSIS_PREVIEW: "按需分析预览", OPPORTUNITY_DECISION: "机会决策",
        READY: "就绪", PARTIAL: "部分可用", FALLBACK: "规则路径降级",
        UNAVAILABLE: "当前不可用", ERROR: "运行错误",
        FOUND: "已发现", NONE_FOUND: "未发现", INSUFFICIENT_DATA: "数据不足",
        SOURCE_UNAVAILABLE: "来源不可用", STALE: "数据已过期",
        VERIFIED: "已验证", PENDING_VERIFICATION: "等待验证", INVALID: "无效",
        QUEUED: "排队中", RUNNING: "执行中", SUCCEEDED: "已完成",
        FAILED: "失败", CANCELLED: "已取消",
        COMPLETE: "完整", PARTIAL_COVERAGE: "部分覆盖", UNKNOWN: "等待评估",
        SYSTEM_DEFAULT: "系统默认", USER_CUSTOM: "用户自定义",
        UNBOUND: "未绑定", PENDING: "待验证", BOUND: "已绑定",
        READ: "已读", UNREAD: "未读",
        RUNTIME_READINESS_REVIEW_ONLY_READY: "运行状态可读",
        RUNTIME_READINESS_PARTIAL_REVIEW_ONLY: "运行状态部分可读",
        RUNTIME_READINESS_MISSING_FAIL_CLOSED: "运行状态缺失",
        RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED: "等待运行状态",
        SYSTEM_GUARDRAIL_REVIEW_ONLY_READY: "防护栏正常",
        SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY: "防护栏降级",
        SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED: "防护栏阻断",
        RUN_BASELINE_REVIEW_ONLY_READY: "运行基线可用",
        RUN_BASELINE_MISSING_FAIL_CLOSED: "运行基线缺失",
        RUNTIME_METRIC_REVIEW_ONLY_READY: "运行指标可用",
        RUNTIME_METRIC_MISSING_FAIL_CLOSED: "运行指标缺失",
        OK: "正常", UP: "正常", DOWN: "不可用", DEGRADED: "降级", RUNNING: "运行中",
        OBSERVING: "观察中", CANDIDATE: "候选", WAITING_TRIGGER: "等待触发",
        TRIGGERED: "已触发", HIGH_RISK: "高风险", COOLING: "冷却中", CONFUSED: "冲突待解",
        LEVEL_1_CONSISTENT: "一致", LEVEL_2_MINOR_DISAGREEMENT: "轻微分歧",
        LEVEL_3_SIGNIFICANT_DISAGREEMENT: "显著分歧", LEVEL_4_EXTREME_CONFLICT: "极端冲突",
        REVIEW_PASSED: "复核通过", REVIEW_WAITING: "等待人工复核",
        APPROVE: "通过", DOWNGRADE: "降级", REJECT_CANDIDATE: "拒绝候选", RISK_WARNING: "风险警告",
        DRIFTED_FROM_ENTRY_ZONE: "偏离入场区", DRIFTED: "发生偏移",
        RISK_BLOCKED: "风险阻断", CONFUSED_BLOCKED: "冲突阻断",
        GPT_FINAL: "GPT 综合判断", GEMINI_REVIEW: "Gemini 冲突复核",
        GROK_CHALLENGE: "Grok 反方挑战",
        NOT_CALLED_INPUT_GATE: "未调用（输入门禁）", INVALID_RESPONSE: "返回内容无效",
        MARKET_HEURISTIC: "市场启发式", SYSTEM_GENERATED: "系统生成",
        MODERATE_LEVERAGE: "适中杠杆"
    };

    function hasValue(value) {
        return value !== null && value !== undefined && value !== "";
    }

    function text(value, emptyText) {
        if (!hasValue(value)) return emptyText || "当前不可查看";
        return String(value);
    }

    function label(value, emptyText) {
        if (!hasValue(value)) return emptyText || "当前不可查看";
        const raw = String(value).trim();
        const normalized = raw.toUpperCase();
        if (labels[normalized]) return labels[normalized];
        if (typeof userFacingSemantic.value === "function") {
            const mapped = userFacingSemantic.value(raw);
            if (mapped && mapped !== raw) return mapped;
        }
        if (/^[A-Z][A-Z0-9_]*$/.test(raw)) return emptyText || "当前不可查看";
        return raw;
    }

    function fieldLabel(value) {
        if (typeof userFacingSemantic.field === "function") {
            return userFacingSemantic.field(value);
        }
        return "分析结果";
    }

    function roleLabel(role, mode) {
        return {
            GPT_FINAL: "GPT 综合判断",
            GEMINI_REVIEW: "Gemini 冲突复核",
            GROK_CHALLENGE: "Grok 反方挑战"
        }[String(role || "").toUpperCase()] || "分析";
    }

    function escapeHtml(value) {
        return text(value, "").replace(/[&<>'"]/g, function (character) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character];
        });
    }

    function formatNumber(value, options) {
        if (!hasValue(value) || Number.isNaN(Number(value))) return "当前不可查看";
        return new Intl.NumberFormat("zh-CN", options || { maximumFractionDigits: 4 }).format(Number(value));
    }

    function formatPercent(value) {
        if (!hasValue(value) || Number.isNaN(Number(value))) return "当前不可查看";
        const number = Number(value);
        return (number > 0 ? "+" : "") + formatNumber(number, { maximumFractionDigits: 2 }) + "%";
    }

    function formatTime(value) {
        if (!hasValue(value)) return "当前不可查看";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return text(value);
        return new Intl.DateTimeFormat("zh-CN", {
            month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
        }).format(date);
    }

    function announce(message) {
        if (liveRegion) liveRegion.textContent = message || "";
    }

    async function api(url, options) {
        const request = Object.assign({ credentials: "same-origin", headers: { Accept: "application/json" } }, options || {});
        request.headers = Object.assign({}, request.headers || {});
        if (request.body && !(request.body instanceof FormData)) request.headers["Content-Type"] = "application/json";
        if (csrfToken && csrfHeader && request.method && request.method !== "GET") request.headers[csrfHeader] = csrfToken;
        const response = await fetch(url, request);
        const payload = await response.json().catch(function () { return null; });
        if (!response.ok || (payload && hasValue(payload.code) && Number(payload.code) >= 400)) {
            throw new Error(payload?.msg || "请求未完成");
        }
        return payload && Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
    }

    function safeReturnTo(value, fallback) {
        if (!value || typeof value !== "string") return fallback;
        const candidate = value.trim();
        if (!candidate || candidate.includes("\\")
            || candidate.startsWith("//") || /^[a-z][a-z0-9+.-]*:/i.test(candidate)) return fallback;
        try {
            const parsed = new URL(candidate, window.location.origin);
            const allowed = [
                /^\/dashboard$/,
                /^\/messages$/,
                /^\/recheck\/[^/]+$/,
                /^\/plans\/[^/]+$/,
                /^\/positions(?:\/[^/]+)?$/,
                /^\/analysis(?:\/[^/]+)?$/,
                /^\/audit\/[^/]+$/
            ];
            if (parsed.origin !== window.location.origin
                || /%(?:2f|5c|25)/i.test(parsed.pathname)
                || !allowed.some(function (pattern) { return pattern.test(parsed.pathname); })) return fallback;
            return parsed.pathname + parsed.search + parsed.hash;
        } catch (_) {
            return fallback;
        }
    }

    function factGrid(items) {
        return '<dl class="fact-grid">' + items.map(function (item) {
            return '<div><dt>' + escapeHtml(item[0]) + '</dt><dd' + (item[2] ? ' class="' + escapeHtml(item[2]) + '"' : "") + '>' + escapeHtml(item[1]) + '</dd></div>';
        }).join("") + "</dl>";
    }

    function stateBadge(value) {
        const raw = text(value, "UNKNOWN").toUpperCase();
        return '<span class="state-badge" data-state="' + escapeHtml(raw.toLowerCase()) + '">' + escapeHtml(label(raw)) + "</span>";
    }

    function renderProviderStatus(provider) {
        const summary = factGrid([
            ["运行状态", label(provider?.runtimeReadinessStatus, "当前不可查看")],
            ["系统防护栏", label(provider?.systemGuardrailStatus, "当前不可查看")],
            ["数据源", label(provider?.sourceHealth, "当前不可查看")],
            ["数据库", label(provider?.databaseStatus, "当前不可查看")]
        ]);
        const explanation = '<p class="provider-status-message">' + escapeHtml(
            provider?.failClosed === true
                ? "当前状态仅供人工复核；所有下游动作保持关闭。"
                : "当前运行状态可读；这不是交易授权。"
        ) + "</p>";
        const audit = '<details class="audit-disclosure provider-audit"><summary>查看审计元数据</summary>' + factGrid([
            ["调度观察", label(provider?.schedulerObservationStatus, "当前不可查看")],
            ["运行基线", label(provider?.runBaselineStatus, "当前不可查看")],
            ["运行指标", label(provider?.runtimeMetricStatus, "当前不可查看")],
            ["观测窗口", hasValue(provider?.windowMinutes) ? provider.windowMinutes + " 分钟" : "当前不可查看"],
            ["采样数量", hasValue(provider?.runtimeMetricSampleCount) ? provider.runtimeMetricSampleCount : "当前不可查看"],
            ["生成时间", formatTime(provider?.generatedAt)],
            ["Fail Closed", provider?.failClosed === true ? "是" : "否"],
            ["交易授权", "否"]
        ]) + "</details>";
        return '<div class="provider-status-summary">' + summary + explanation + audit + "</div>";
    }

    function empty(target, title, description) {
        if (!target) return;
        target.innerHTML = '<div class="empty-state"><strong>' + escapeHtml(title) + '</strong><span>' + escapeHtml(description) + "</span></div>";
    }

    function openOverlay(name, trigger) {
        const dialog = document.getElementById(name.startsWith("overlay-") ? name : "overlay-" + name);
        if (!dialog || typeof dialog.showModal !== "function") return;
        restoreFocus = trigger || document.activeElement;
        if (!dialog.open) dialog.showModal();
        dialog.querySelector("input, select, button, a[href]")?.focus();
        if (dialog.id === "overlay-async-task-center") loadTasks();
        if (dialog.id === "overlay-status-recovery") loadSystemStatus();
    }

    function closeOverlay(dialog) {
        if (dialog?.open) dialog.close();
        if (restoreFocus && typeof restoreFocus.focus === "function") restoreFocus.focus();
        restoreFocus = null;
    }

    function bindOverlays() {
        document.addEventListener("click", function (event) {
            const opener = event.target.closest("[data-open-overlay]");
            if (opener) {
                event.preventDefault();
                openOverlay(opener.dataset.openOverlay, opener);
                return;
            }
            const closer = event.target.closest("[data-close-overlay]");
            if (closer) {
                event.preventDefault();
                closeOverlay(closer.closest("dialog"));
            }
        });
        document.querySelectorAll("dialog.overlay").forEach(function (dialog) {
            dialog.addEventListener("click", function (event) {
                if (event.target === dialog) closeOverlay(dialog);
            });
            dialog.addEventListener("cancel", function (event) {
                event.preventDefault();
                closeOverlay(dialog);
            });
        });
    }

    async function loadTasks() {
        const target = document.getElementById("asyncTaskList");
        try {
            const tasks = await api("/api/workspace/tasks?limit=30") || [];
            latestTasks = tasks;
            const activeTaskCount = tasks.filter(function (task) {
                return task.state === "QUEUED" || task.state === "RUNNING" || task.state === "PARTIAL";
            }).length;
            const taskIndicator = document.querySelector(".task-indicator");
            document.getElementById("workspaceTaskCount").textContent = String(activeTaskCount);
            if (taskIndicator) taskIndicator.hidden = activeTaskCount === 0;
            if (!target) return;
            if (!tasks.length) {
                empty(target, "暂无任务", "扫描、分析和重新校验任务会显示在这里。");
                updatePoolScanCta();
                return tasks;
            }
            target.innerHTML = tasks.map(function (task) {
                const retryable = task.state === "FAILED" || task.state === "PARTIAL";
                const cancellable = task.state === "QUEUED" || task.state === "RUNNING" || task.state === "PARTIAL";
                return '<article class="task-row"><div><strong>' + escapeHtml(label(task.taskType)) + '</strong><small>' + escapeHtml(formatTime(task.updatedAt || task.createdAt)) + '</small></div>' + stateBadge(task.state)
                    + (task.errorMessage ? '<p>' + escapeHtml(task.errorMessage) + "</p>" : "")
                    + (retryable ? '<button class="text-action" type="button" data-task-retry="' + escapeHtml(task.taskId) + '">重试</button>' : "")
                    + (cancellable ? '<button class="text-action" type="button" data-task-cancel="' + escapeHtml(task.taskId) + '">取消</button>' : "")
                    + "</article>";
            }).join("");
            target.querySelectorAll("[data-task-retry]").forEach(function (button) {
                button.addEventListener("click", async function () {
                    try { await api("/api/workspace/tasks/" + encodeURIComponent(button.dataset.taskRetry) + "/retry", { method: "POST" }); await loadTasks(); }
                    catch (error) { announce(error.message); }
                });
            });
            target.querySelectorAll("[data-task-cancel]").forEach(function (button) {
                button.addEventListener("click", async function () {
                    try { await api("/api/workspace/tasks/" + encodeURIComponent(button.dataset.taskCancel) + "/cancel", { method: "POST" }); await loadTasks(); }
                    catch (error) { announce(error.message); }
                });
            });
            updatePoolScanCta();
            return tasks;
        } catch (_) {
            if (target) empty(target, "任务当前不可查看", "系统没有返回可信任务状态。");
            latestTasks = [];
            const taskIndicator = document.querySelector(".task-indicator");
            if (taskIndicator) taskIndicator.hidden = true;
            updatePoolScanCta();
            return [];
        }
    }

    async function loadSystemStatus() {
        const target = document.getElementById("statusRecoveryContent");
        if (!target) return;
        try {
            const [home, readiness] = await Promise.all([
                api("/api/dashboard/home?limit=1"),
                api("/api/system/runtime-readiness-guardrail-status")
            ]);
            target.innerHTML = factGrid([
                ["数据状态", label(home?.header?.dataStatus, "等待同步")],
                ["AI 服务", text(home?.header?.aiStatusLabel, "等待同步")],
                ["运行就绪", label(readiness?.overallStatus || readiness?.status, "当前不可查看")],
                ["检查时间", formatTime(readiness?.checkedAt || readiness?.updatedAt)]
            ]);
        } catch (_) {
            empty(target, "系统状态当前不可查看", "请稍后重试；当前不会显示推测状态。");
        }
    }

    function renderAssetPoolRows(items) {
        const rows = document.getElementById("assetPoolRows");
        const emptyNode = document.getElementById("assetPoolEmpty");
        if (!rows) return;
        rows.innerHTML = "";
        emptyNode.hidden = items.length > 0;
        items.forEach(function (asset) {
            const row = document.createElement("tr");
            row.innerHTML = '<td><button class="table-link" type="button" data-pool-detail="' + escapeHtml(asset.symbol) + '"><strong>' + escapeHtml(asset.symbol) + '</strong><small>' + escapeHtml(text(asset.displayName || asset.name, "名称待同步")) + '</small></button></td><td>' + escapeHtml(label(asset.marketType)) + '</td><td>' + stateBadge(asset.watchStatus || (asset.focusEnabled ? "OBSERVING" : "PAUSED")) + '</td><td>' + escapeHtml(label(asset.sourceType || asset.source)) + '</td><td class="align-right"><button class="button button-quiet" type="button" data-remove-asset="' + escapeHtml(asset.symbol) + '">移除</button></td>';
            rows.appendChild(row);
        });
    }

    async function loadAssetPool() {
        try {
            const items = await api("/api/asset-pool") || [];
            assetPoolItems = items;
            renderAssetPoolRows(items);
            const batch = document.getElementById("poolBatchList");
            if (batch) batch.innerHTML = items.map(function (asset) {
                return '<label class="check-row"><input type="checkbox" value="' + escapeHtml(asset.symbol) + '"><span><strong>' + escapeHtml(asset.symbol) + '</strong><small>' + escapeHtml(text(asset.displayName || asset.name, "名称待同步")) + "</small></span></label>";
            }).join("") || '<p class="muted">暂无可管理资产</p>';
            updatePoolScanCta();
        } catch (_) {
            assetPoolItems = [];
            empty(document.getElementById("assetPoolRows")?.parentElement, "资产池当前不可查看", "未返回可信资产池数据。请稍后重试。");
            updatePoolScanCta(true);
        }
    }

    function updatePoolScanCta(loadFailed) {
        const scan = document.getElementById("scanAssetPool");
        const topUp = document.getElementById("topUpDefaultAssets");
        const status = document.getElementById("poolScanStatus");
        if (!scan) return;
        const poolEmpty = assetPoolItems.length === 0;
        const poolTasks = latestTasks.filter(function (task) { return task.taskType === "POOL_SCAN"; });
        const running = poolTasks.some(function (task) { return ["QUEUED", "RUNNING"].includes(task.state); });
        const failed = poolTasks.some(function (task) { return ["FAILED", "PARTIAL"].includes(task.state); });
        const completed = poolTasks.some(function (task) { return task.state === "SUCCEEDED"; });
        scan.classList.remove("button-primary");
        scan.classList.add("button-secondary");
        scan.disabled = poolEmpty || running || loadFailed === true;
        if (topUp) {
            topUp.classList.toggle("button-primary", poolEmpty);
            topUp.classList.toggle("button-secondary", !poolEmpty);
        }
        if (loadFailed === true) {
            scan.textContent = "当前不可扫描";
            if (status) status.textContent = "资产池状态当前不可查看";
        } else if (poolEmpty) {
            scan.textContent = "扫描资产池";
            if (status) status.textContent = "请先添加观察资产";
        } else if (running) {
            scan.textContent = "扫描中";
            if (status) status.textContent = "扫描任务执行中";
        } else if (failed) {
            scan.textContent = "重新扫描";
            scan.classList.remove("button-secondary");
            scan.classList.add("button-primary");
            if (status) status.textContent = "上次扫描未完成";
        } else if (!completed) {
            scan.textContent = "开始首次扫描";
            scan.classList.remove("button-secondary");
            scan.classList.add("button-primary");
            if (status) status.textContent = "尚未开始扫描";
        } else {
            scan.textContent = "扫描资产池";
            if (status) status.textContent = "机会排名已可用";
        }
    }

    function renderSearchResults(target, items, previewMode) {
        if (!target) return;
        if (!items.length) return empty(target, "未找到资产", "请尝试其他名称或交易对。"), undefined;
        target.innerHTML = items.map(function (asset) {
            return '<button class="search-result" type="button" data-search-symbol="' + escapeHtml(asset.symbol) + '" data-preview="' + (previewMode ? "true" : "false") + '"><span><strong>' + escapeHtml(asset.symbol) + '</strong><small>' + escapeHtml(text(asset.baseAsset, "市场资产")) + ' / ' + escapeHtml(text(asset.quoteAsset, "")) + '</small></span><em>' + (previewMode ? "分析" : "加入") + "</em></button>";
        }).join("");
    }

    async function searchAssets(query, target, previewMode) {
        if (!query.trim()) {
            if (target) target.innerHTML = "";
            return;
        }
        try {
            renderSearchResults(target, await api("/api/asset-pool/search?query=" + encodeURIComponent(query.trim()) + "&limit=20") || [], previewMode);
        } catch (_) {
            empty(target, "搜索当前不可用", "没有返回可信市场资产结果。");
        }
    }

    async function addAsset(symbol) {
        await api("/api/asset-pool", { method: "POST", body: JSON.stringify({ symbol: symbol, focusEnabled: true }) });
        announce(symbol + " 已加入资产池");
        await loadAssetPool();
    }

    async function previewAsset(symbol) {
        const result = await api("/api/asset-pool/search/" + encodeURIComponent(symbol) + "/analysis-preview?timeframe=5m", { method: "POST" });
        if (!result?.analysisId) throw new Error("预览未返回分析标识");
        window.location.assign("/analysis/" + encodeURIComponent(result.analysisId));
    }

    function selectedBatchSymbols() {
        return Array.from(document.querySelectorAll("#poolBatchList input:checked")).map(function (input) { return input.value; });
    }

    function updateBatchActions(message) {
        const symbols = selectedBatchSymbols();
        const status = document.getElementById("poolBatchStatus");
        const scan = document.getElementById("batchScanSelected");
        const remove = document.getElementById("batchRemoveSelected");
        if (status) status.textContent = message || (symbols.length ? "已选择 " + symbols.length + " 个资产" : "尚未选择资产");
        if (scan) scan.disabled = symbols.length === 0;
        if (remove) remove.disabled = symbols.length === 0;
    }

    function bindAssetPool() {
        const search = document.getElementById("assetPoolSearch");
        let searchTimer;
        search?.addEventListener("input", function () {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(function () { searchAssets(search.value, document.getElementById("assetSearchResults"), false); }, 180);
        });
        document.getElementById("quickAssetSearch")?.addEventListener("input", function (event) {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(function () { searchAssets(event.target.value, document.getElementById("quickAssetResults"), pageKey === "analysis"); }, 180);
        });
        document.getElementById("poolBatchList")?.addEventListener("change", function () { updateBatchActions(); });
        document.getElementById("batchScanSelected")?.addEventListener("click", async function (event) {
            const symbols = selectedBatchSymbols();
            if (!symbols.length) return;
            const button = event.currentTarget;
            button.disabled = true;
            document.getElementById("batchRemoveSelected").disabled = true;
            updateBatchActions("正在扫描 " + symbols.length + " 个资产…");
            try {
                const result = await api("/api/asset-pool/batch-scan", { method: "POST", body: JSON.stringify({ symbols: symbols, timeframe: "5m" }) }) || [];
                const completed = result.filter(function (item) { return item.status === "SUCCESS"; }).length;
                updateBatchActions("扫描完成：" + completed + " / " + result.length);
                await loadTasks();
            } catch (error) {
                updateBatchActions(error.message);
            } finally {
                updateBatchActions(document.getElementById("poolBatchStatus").textContent);
            }
        });
        document.getElementById("batchRemoveSelected")?.addEventListener("click", async function (event) {
            const symbols = selectedBatchSymbols();
            if (!symbols.length || !window.confirm("从观察资产池移除所选 " + symbols.length + " 个资产？历史记录会保留。")) return;
            const button = event.currentTarget;
            button.disabled = true;
            document.getElementById("batchScanSelected").disabled = true;
            updateBatchActions("正在移除 " + symbols.length + " 个资产…");
            try {
                await api("/api/asset-pool/batch-remove", { method: "POST", body: JSON.stringify({ symbols: symbols }) });
                announce("已移除 " + symbols.length + " 个观察资产");
                await loadAssetPool();
                updateBatchActions("移除完成");
            } catch (error) {
                updateBatchActions(error.message);
            }
        });
        document.addEventListener("click", async function (event) {
            const result = event.target.closest("[data-search-symbol]");
            const remove = event.target.closest("[data-remove-asset]");
            const detail = event.target.closest("[data-pool-detail]");
            try {
                if (result) {
                    result.disabled = true;
                    if (result.dataset.preview === "true") await previewAsset(result.dataset.searchSymbol);
                    else await addAsset(result.dataset.searchSymbol);
                } else if (remove && window.confirm("从观察资产池移除 " + remove.dataset.removeAsset + "？历史记录会保留。")) {
                    await api("/api/asset-pool/" + encodeURIComponent(remove.dataset.removeAsset), { method: "DELETE" });
                    announce(remove.dataset.removeAsset + " 已移除");
                    await loadAssetPool();
                } else if (detail) {
                    const target = document.getElementById("poolAssetDetailContent");
                    target.innerHTML = '<h3>' + escapeHtml(detail.dataset.poolDetail) + '</h3><p>该资产的历史分析与机会记录会独立保留。</p><a class="button button-secondary" href="/analysis?asset=' + encodeURIComponent(detail.dataset.poolDetail) + '">按需分析</a>';
                    openOverlay("pool-asset-detail", detail);
                }
            } catch (error) {
                announce(error.message);
            }
        });
        document.getElementById("topUpDefaultAssets")?.addEventListener("click", async function () {
            try {
                await api("/api/asset-pool/defaults/top-up", { method: "POST" });
                announce("默认资产已补齐");
                await loadAssetPool();
            } catch (error) { announce(error.message); }
        });
        document.getElementById("resetDefaultAssets")?.addEventListener("click", async function () {
            if (!window.confirm("确认将当前观察集合重置为系统默认？历史分析、机会、计划、复盘与持仓监控都会保留。")) return;
            try {
                await api("/api/asset-pool/defaults/reset", { method: "POST" });
                announce("观察集合已重置为默认资产池");
                await loadAssetPool();
            } catch (error) { announce(error.message); }
        });
        document.getElementById("scanAssetPool")?.addEventListener("click", async function (event) {
            const scanButton = event.currentTarget;
            scanButton.disabled = true;
            document.getElementById("poolScanStatus").textContent = "扫描任务执行中";
            try {
                const result = await api("/api/asset-pool/scan?timeframe=5m", { method: "POST" }) || [];
                const completed = result.filter(function (item) { return item.status === "SUCCESS"; }).length;
                document.getElementById("poolScanStatus").textContent = "扫描完成：" + completed + " / " + result.length;
                await loadTasks();
            } catch (error) {
                document.getElementById("poolScanStatus").textContent = error.message;
            } finally { scanButton.disabled = false; }
        });
        Promise.all([loadAssetPool(), loadTasks()]).then(function () { updatePoolScanCta(); });
        updateBatchActions();
    }

    function trustedMonitor(monitor) {
        return monitor && monitor.monitorTrustState === "VERIFIED_FRESH"
            && monitor.markPriceFresh === true
            && ["OPEN_MONITORING", "RISK_ESCALATED", "PLAN_INVALIDATED"].includes(monitor.dataState);
    }

    function monitorUnavailableText(monitor) {
        return {
            PENDING: "等待监控数据",
            PENDING_VERIFICATION: "等待监控数据",
            STALE: "监控数据已过期",
            INVALID: "当前不可查看",
            SOURCE_UNAVAILABLE: "监控来源不可用"
        }[String(monitor?.monitorTrustState || "SOURCE_UNAVAILABLE").toUpperCase()] || "等待监控数据";
    }

    function positionListReturnTo(tab) {
        const query = new URLSearchParams();
        query.set("tab", tab === "history" ? "history" : "active");
        if (tab !== "history") {
            const state = document.getElementById("positionStateFilter")?.value;
            const sort = document.getElementById("positionSort")?.value;
            if (state && state !== "ALL") query.set("state", state);
            if (sort && sort !== "RISK_DESC") query.set("sort", sort);
        }
        return "/positions?" + query.toString();
    }

    function renderPosition(userPosition, monitor) {
        const trusted = trustedMonitor(monitor);
        const positionId = userPosition.id || monitor?.positionId;
        const symbol = userPosition.assetSymbol || monitor?.symbol;
        const direction = userPosition.side || monitor?.direction;
        const facts = [["开仓价", formatNumber(userPosition.entryPrice)], ["开仓时间", formatTime(userPosition.openedAt)]];
        if (trusted) facts.splice(1, 0, ["标记价格", formatNumber(monitor.markPrice)], ["盈亏", formatPercent(monitor.pnlPercent)]);
        const judgment = trusted ? [
            ["入场逻辑状态", text(monitor.entryLogicStatusLabel, label(monitor.entryLogicStatus))],
            ["反转状态", text(monitor.reversalStatusLabel, label(monitor.reversalStatus))],
            ["持仓风险", text(monitor.riskLevelLabel, label(monitor.riskLevel))],
            ["风险趋势", label(monitor.riskTrend)]
        ] : [];
        const conclusion = trusted ? [
            ["监控结论", text(monitor.monitorConclusionLabel, label(monitor.monitorConclusion))],
            ["建议动作", text(monitor.suggestedManualActionText, label(monitor.suggestedAction))]
        ] : [];
        const detailHref = "/positions/" + encodeURIComponent(positionId) + "?returnTo=" + encodeURIComponent(positionListReturnTo("active"));
        const monitoring = trusted
            ? '<section class="position-judgement">' + factGrid(judgment) + '</section><section class="position-conclusion">' + factGrid(conclusion) + '<a class="text-action" href="' + detailHref + '">查看详情</a></section>'
            : '<section class="position-untrusted-state" role="status"><strong>' + escapeHtml(monitorUnavailableText(monitor)) + '</strong><a class="text-action" href="' + detailHref + '">查看详情</a></section>';
        return '<article class="position-card position-row' + (trusted ? " is-trusted" : " is-untrusted") + '" data-position-id="' + escapeHtml(positionId) + '">'
            + '<header class="position-identity"><div><strong>' + escapeHtml(symbol) + '</strong><span>' + escapeHtml(label(direction)) + '</span></div><small>' + escapeHtml(typeof frontendContract.positionSourceLabel === "function" ? frontendContract.positionSourceLabel(userPosition.sourceType) : label(userPosition.sourceType, "来源不可查看")) + '</small></header>'
            + '<section class="position-facts">' + factGrid(facts) + '</section>'
            + monitoring + "</article>";
    }

    let positionRows = [];
    function renderPositionRows() {
        const grid = document.getElementById("positionGrid");
        if (!grid) return;
        const stateFilter = document.getElementById("positionStateFilter")?.value || "ALL";
        const sort = document.getElementById("positionSort")?.value || "RISK_DESC";
        const riskRank = { LOW: 1, MEDIUM: 2, HIGH: 3, EXTREME: 4 };
        const rows = positionRows.filter(function (row) {
            if (stateFilter === "WAITING") return !trustedMonitor(row.monitor);
            if (stateFilter === "RISK") return trustedMonitor(row.monitor)
                && (row.monitor.riskTrend !== "STABLE" || ["HIGH", "EXTREME"].includes(row.monitor.riskLevel));
            return true;
        }).sort(function (left, right) {
            if (sort === "OPENED_DESC") return new Date(right.position.openedAt || 0) - new Date(left.position.openedAt || 0);
            return (riskRank[right.monitor?.riskLevel] || 0) - (riskRank[left.monitor?.riskLevel] || 0);
        });
        grid.innerHTML = rows.map(function (row) { return renderPosition(row.position, row.monitor); }).join("");
    }

    async function loadPositions() {
        const grid = document.getElementById("positionGrid");
        if (!grid) return;
        try {
            const data = await api("/api/workspace/positions/monitoring") || {};
            positionRows = data.positions || [];
            document.getElementById("positionEmpty").hidden = positionRows.length > 0;
            renderPositionRows();
            document.getElementById("accountRiskCoverage").textContent = label(data.accountRiskCoverageState, "等待评估");
        } catch (_) {
            empty(grid, "持仓当前不可查看", "未返回可信的用户持仓数据。");
        }
    }

    function renderHistoricalPosition(position) {
        const returnTo = positionListReturnTo("history");
        return '<article class="position-card position-row history-position-row" data-position-id="' + escapeHtml(position.id) + '">'
            + '<header class="position-identity"><div><strong>' + escapeHtml(position.assetSymbol) + '</strong><span>' + escapeHtml(label(position.side)) + '</span></div><small>'
            + escapeHtml(typeof frontendContract.positionSourceLabel === "function" ? frontendContract.positionSourceLabel(position.sourceType) : label(position.sourceType, "来源不可查看")) + '</small></header>'
            + '<section class="position-facts">' + factGrid([["开仓价", formatNumber(position.entryPrice)], ["开仓时间", formatTime(position.openedAt)]]) + '</section>'
            + '<section class="position-judgement">' + factGrid([["平仓价", formatNumber(position.closePrice)], ["平仓时间", formatTime(position.closedAt)]]) + '</section>'
            + '<section class="position-conclusion">' + factGrid([["结果说明", text(position.closeReason, "未记录说明")]])
            + '<a class="text-action" href="/positions/' + encodeURIComponent(position.id) + '?returnTo=' + encodeURIComponent(returnTo) + '">查看持仓详情</a></section></article>';
    }

    async function loadPositionHistory() {
        const grid = document.getElementById("positionHistoryGrid");
        if (!grid) return;
        try {
            const data = await api("/api/workspace/positions/history?limit=100") || {};
            const positions = data.positions || [];
            grid.innerHTML = positions.map(renderHistoricalPosition).join("");
            document.getElementById("positionHistoryEmpty").hidden = positions.length > 0;
        } catch (_) {
            empty(grid, "历史持仓当前不可查看", "未返回可信的已平仓持仓记录。");
        }
    }

    function selectPositionTab(tab) {
        const selected = tab === "history" ? "history" : "active";
        document.querySelectorAll("[data-position-tab]").forEach(function (button) {
            const active = button.dataset.positionTab === selected;
            button.classList.toggle("is-active", active);
            if (active) button.setAttribute("aria-current", "page");
            else button.removeAttribute("aria-current");
        });
        document.querySelectorAll("[data-position-panel]").forEach(function (panel) {
            panel.hidden = panel.dataset.positionPanel !== selected;
        });
        const url = new URL(window.location.href);
        url.searchParams.set("tab", selected);
        window.history.replaceState({}, "", url);
        if (selected === "history") loadPositionHistory();
        else loadPositions();
    }

    function syncPositionListUrl() {
        const url = new URL(window.location.href);
        const state = document.getElementById("positionStateFilter")?.value || "ALL";
        const sort = document.getElementById("positionSort")?.value || "RISK_DESC";
        if (state === "ALL") url.searchParams.delete("state");
        else url.searchParams.set("state", state);
        if (sort === "RISK_DESC") url.searchParams.delete("sort");
        else url.searchParams.set("sort", sort);
        window.history.replaceState({}, "", url);
    }

    document.getElementById("positionStateFilter")?.addEventListener("change", function () {
        syncPositionListUrl();
        renderPositionRows();
    });
    document.getElementById("positionSort")?.addEventListener("change", function () {
        syncPositionListUrl();
        renderPositionRows();
    });
    document.querySelectorAll("[data-position-tab]").forEach(function (button) {
        button.addEventListener("click", function () { selectPositionTab(button.dataset.positionTab); });
    });

    function formJson(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    function prepareManualPositionForm() {
        const form = document.getElementById("actualPositionForm");
        if (!form) return;
        form.reset();
        form.elements.sourceType.value = "MANUAL_INDEPENDENT";
        form.elements.finalPlanId.value = "";
    }

    function numericPlanValue(value) {
        const raw = String(value == null ? "" : value).trim();
        return /^\d+(?:\.\d+)?$/.test(raw) ? raw : "";
    }

    function sideFromMarketBias(value) {
        const bias = String(value || "").toUpperCase();
        if (bias.includes("BULLISH")) return "LONG";
        if (bias.includes("BEARISH")) return "SHORT";
        return "";
    }

    async function preparePlanPositionForm(plan) {
        const form = document.getElementById("actualPositionForm");
        if (!form || !plan?.planId || !plan?.analysisId) return;
        const analysis = await api("/api/analysis/runs/" + encodeURIComponent(plan.analysisId));
        form.reset();
        form.elements.finalPlanId.value = plan.planId;
        form.elements.sourceType.value = "SYSTEM_PLAN_POSITION";
        form.elements.assetSymbol.value = text(analysis.symbol, "");
        const side = sideFromMarketBias(plan.finalMarketBias);
        if (side) form.elements.side.value = side;
        form.elements.stopLoss.value = numericPlanValue(plan.stopLoss);
        form.elements.takeProfit.value = numericPlanValue(plan.takeProfitRules);
    }

    function bindPositionForms() {
        document.addEventListener("click", function (event) {
            const opener = event.target.closest('[data-open-overlay="actual-position"]');
            if (opener && opener.id !== "planActualPositionAction") prepareManualPositionForm();
        }, true);
        document.getElementById("actualPositionForm")?.addEventListener("submit", async function (event) {
            event.preventDefault();
            const values = formJson(event.currentTarget);
            values.sourceType = values.finalPlanId ? "SYSTEM_PLAN_POSITION" : "MANUAL_INDEPENDENT";
            if (values.openedAt) values.openedAt = new Date(values.openedAt).toISOString().slice(0, 19);
            try {
                await api("/api/user-positions/manual-open", { method: "POST", body: JSON.stringify(values) });
                closeOverlay(event.currentTarget.closest("dialog"));
                announce("真实持仓已录入");
                if (pageKey === "positions") await loadPositions();
            } catch (error) { announce(error.message); }
        });
        document.getElementById("closePositionForm")?.addEventListener("submit", async function (event) {
            event.preventDefault();
            if (!resourceId) return announce("缺少持仓标识");
            const values = formJson(event.currentTarget);
            if (values.closedAt) values.closedAt = new Date(values.closedAt).toISOString().slice(0, 19);
            try {
                await api("/api/user-positions/" + encodeURIComponent(resourceId) + "/manual-close", { method: "POST", body: JSON.stringify(values) });
                closeOverlay(event.currentTarget.closest("dialog"));
                window.location.assign("/positions?tab=history");
            } catch (error) { announce(error.message); }
        });
    }

    async function loadPositionDetail() {
        const card = document.getElementById("positionDetailCard");
        if (!card || !resourceId) return;
        try {
            const projection = await api("/api/workspace/positions/" + encodeURIComponent(resourceId) + "/monitoring");
            const position = projection.position;
            const monitor = projection.monitor;
            const closed = String(position.status || "").toUpperCase() === "CLOSED";
            card.innerHTML = closed ? renderHistoricalPositionDetail(position) : renderPosition(position, monitor);
            const returnTo = safeReturnTo(new URLSearchParams(window.location.search).get("returnTo"),
                closed ? "/positions?tab=history" : "/positions?tab=active");
            const returnLink = document.getElementById("positionDetailReturn");
            if (returnLink) returnLink.href = returnTo;
            const closeAction = document.getElementById("closePositionAction");
            if (closeAction) closeAction.hidden = closed;
            document.getElementById("actualPositionFacts").innerHTML = factGrid([
                ["资产", position.assetSymbol], ["方向", label(position.side)],
                ["开仓价", formatNumber(position.entryPrice)], ["数量", formatNumber(position.quantity)],
                ["杠杆", formatNumber(position.leverage)], ["开仓时间", formatTime(position.openedAt)]
            ]);
            if (position.finalPlanId) await loadOpeningPlan(position.finalPlanId);
            else empty(document.getElementById("openingPlanBaseline"), "独立手动持仓", "该持仓没有系统最终计划来源。仍可持续监控。 ");
            const timeline = document.getElementById("monitorTimeline");
            if (closed) {
                empty(timeline, "持仓已关闭", "活动监控已结束；历史监控不会冒充当前判断。");
                return;
            }
            const logs = await api("/api/review/positions/" + encodeURIComponent(resourceId) + "/monitor-logs?limit=30");
            if (!(logs || []).length) empty(timeline, "暂无监控记录", "等待首次可信监控。 ");
            else timeline.innerHTML = logs.map(function (item) {
                return '<article class="timeline-item"><time>' + escapeHtml(formatTime(item.observedAt || item.createdAt)) + '</time><strong>' + escapeHtml(label(item.monitorConclusion, "等待监控数据")) + '</strong><p>' + escapeHtml(label(item.riskReason, "暂无风险变化原因")) + "</p></article>";
            }).join("");
        } catch (_) { empty(card, "持仓详情当前不可查看", "未返回可信的持仓事实。"); }
    }

    function renderHistoricalPositionDetail(position) {
        return '<article class="position-card position-row history-position-row is-closed" data-position-id="' + escapeHtml(position.id) + '">'
            + '<header class="position-identity"><div><strong>' + escapeHtml(position.assetSymbol) + '</strong><span>' + escapeHtml(label(position.side)) + '</span></div><small>'
            + escapeHtml(typeof frontendContract.positionSourceLabel === "function" ? frontendContract.positionSourceLabel(position.sourceType) : label(position.sourceType, "来源不可查看")) + '</small></header>'
            + '<section class="position-facts">' + factGrid([["生命周期", "已关闭"], ["开仓价", formatNumber(position.entryPrice)], ["开仓时间", formatTime(position.openedAt)]]) + '</section>'
            + '<section class="position-judgement">' + factGrid([["平仓价", formatNumber(position.closePrice)], ["平仓时间", formatTime(position.closedAt)]]) + '</section>'
            + '<section class="position-conclusion">' + factGrid([["结果说明", text(position.closeReason, "当前不可查看")]]) + '</section></article>';
    }

    async function loadOpeningPlan(planId) {
        const target = document.getElementById("openingPlanBaseline");
        try {
            const plan = await api("/api/workspace/plans/" + encodeURIComponent(planId));
            target.innerHTML = factGrid([
                ["计划模式", label(plan.finalPlanMode || plan.planMode)],
                ["计划版本", text(plan.planVersion)],
                ["开仓时计划", plan.planId]
            ]) + '<a class="text-action" href="/plans/' + encodeURIComponent(plan.planId)
                + '?returnTo=' + encodeURIComponent(window.location.pathname + window.location.search) + '">查看开仓计划</a>';
        } catch (_) { empty(target, "开仓计划当前不可查看", "持仓事实仍保留，不使用其他计划替代。"); }
    }

    function reviewCard(kind, title, state, body, href) {
        return '<article class="review-card" data-review-kind="' + escapeHtml(kind) + '"><header><strong>' + escapeHtml(title) + '</strong>' + stateBadge(state) + '</header><p>' + escapeHtml(body) + '</p>' + (href ? '<a class="text-action" href="' + escapeHtml(href) + '">查看复盘</a>' : "") + "</article>";
    }

    async function loadReviews() {
        const target = document.getElementById("reviewList");
        try {
            const data = await api("/api/review/center") || {};
            const rows = [];
            (data.positionReviews || []).forEach(function (item) {
                rows.push(reviewCard("position", item.symbol + " · 持仓复盘", item.reviewStatus, text(item.monitorConclusion, "等待复盘结论"), item.reviewId ? "/reviews/" + encodeURIComponent(item.reviewId) : null));
            });
            (data.opportunityReviews || []).forEach(function (item) {
                rows.push(reviewCard("opportunity", item.symbol + " · 机会复盘", item.outcome, label(item.planMode, "等待后续结果"), item.reviewId ? "/reviews/" + encodeURIComponent(item.reviewId) : null));
            });
            target.innerHTML = rows.join("");
            document.getElementById("reviewEmpty").hidden = rows.length > 0;
        } catch (_) { empty(target, "复盘当前不可查看", "未返回可信复盘记录。"); }
    }

    function bindReviews() {
        document.querySelectorAll("[data-review-filter]").forEach(function (button) {
            button.addEventListener("click", function () {
                const filter = button.dataset.reviewFilter;
                document.querySelectorAll("[data-review-filter]").forEach(function (item) {
                    const selected = item === button;
                    item.classList.toggle("is-active", selected);
                    item.setAttribute("aria-selected", String(selected));
                });
                document.querySelectorAll("[data-review-kind]").forEach(function (card) {
                    card.hidden = filter !== "all" && card.dataset.reviewKind !== filter;
                });
            });
        });
        loadReviews();
    }

    async function loadReviewDetail() {
        if (!resourceId) return;
        try {
            const [aggregate, state] = await Promise.all([
                api("/api/review/aggregate/" + encodeURIComponent(resourceId)),
                api("/api/review/state/" + encodeURIComponent(resourceId))
            ]);
            document.getElementById("reviewAtTime").innerHTML = factGrid([
                ["分析时间", formatTime(aggregate?.run?.analysisTime)],
                ["当时方向", label(aggregate?.decision?.direction)],
                ["当时计划模式", label(aggregate?.plan?.planMode)],
                ["当时结论", text(aggregate?.reviewClosure?.decisionConclusion, "暂无当时结论")]
            ]);
            document.getElementById("reviewLater").innerHTML = factGrid([
                ["错过原因", label(state?.missedReason, "尚未记录")],
                ["后续结果", label(state?.laterOutcome, "尚未记录")],
                ["复盘结论", text(state?.summary, "尚未完成复盘")]
            ]);
            const chain = document.getElementById("responsibilityChain");
            const stages = [aggregate?.run, aggregate?.decision, aggregate?.plan].filter(Boolean);
            chain.innerHTML = stages.map(function (stage, index) {
                return '<article class="audit-step"><strong>' + escapeHtml(["分析运行", "决策结果", "最终计划"][index]) + '</strong><p>' + escapeHtml(text(stage.analysisId || stage.planId || stage.status, "记录存在")) + '</p>' + stateBadge(stage.status || "FOUND") + "</article>";
            }).join("");
        } catch (_) {
            empty(document.getElementById("reviewAtTime"), "复盘详情当前不可查看", "未返回可信的当时事实。");
            empty(document.getElementById("reviewLater"), "后续结果当前不可查看", "不会用当前状态覆盖历史事实。");
        }
    }

    function parseStructured(value) {
        if (!hasValue(value)) return null;
        if (typeof value === "object") return value;
        try { return JSON.parse(value); } catch (_) { return value; }
    }

    function renderStructured(value) {
        const parsed = parseStructured(value);
        if (!hasValue(parsed)) return '<p class="muted">暂无数据</p>';
        if (Array.isArray(parsed)) {
            if (!parsed.length) return '<p class="muted">暂无数据</p>';
            return '<ul class="structured-list">' + parsed.slice(0, 4).map(function (item) {
                return "<li>" + escapeHtml(aiItemText(item)) + "</li>";
            }).join("") + "</ul>";
        }
        if (typeof parsed === "object") {
            return factGrid(Object.entries(parsed).slice(0, 8).map(function (entry) {
                const value = typeof entry[1] === "object" ? aiItemText(entry[1]) : label(entry[1], text(entry[1]));
                return [fieldLabel(entry[0]), value];
            }));
        }
        return "<p>" + escapeHtml(label(parsed, text(parsed))) + "</p>";
    }

    function renderAnalysisScores(items) {
        if (!Array.isArray(items) || !items.length) return '<p class="muted">暂无数据</p>';
        return '<div class="analysis-score-list">' + items.slice(0, 8).map(function (item) {
            const direction = hasValue(item?.direction) ? label(item.direction, "") : "";
            return '<article class="analysis-score-item" data-analysis-score-item><div><strong>'
                + escapeHtml(label(item?.scoreType, "评分项")) + '</strong>'
                + (direction ? '<small>' + escapeHtml(direction) + '</small>' : "") + '</div><span>'
                + escapeHtml(hasValue(item?.scoreValue)
                    ? formatNumber(item.scoreValue, { maximumFractionDigits: 1 }) : "当前不可查看")
                + '</span></article>';
        }).join("") + "</div>";
    }

    function analysisEvidenceDescription(value) {
        const description = text(value, "暂无证据说明").trim();
        return /^[A-Z0-9_]+(?:\s*\|\s*[A-Z0-9_]+)*$/.test(description)
            ? "该证据当前不可用" : description;
    }

    function renderAnalysisEvidence(items) {
        if (!Array.isArray(items) || !items.length) return '<p class="muted">暂无数据</p>';
        return '<div class="analysis-evidence-list">' + items.slice(0, 20).map(function (item) {
            const currentValue = hasValue(item?.currentValue) ? label(item.currentValue, "当前不可查看") : "当前不可查看";
            const details = [
                label(item?.sourceProvider || item?.source, "来源当前不可查看"),
                label(item?.direction, "方向当前不可查看"),
                hasValue(item?.strength) ? "强度 " + formatNumber(item.strength, { maximumFractionDigits: 1 }) : null,
                hasValue(item?.confidence) ? "置信 " + formatNumber(item.confidence, { maximumFractionDigits: 1 }) : null
            ].filter(Boolean);
            return '<article class="analysis-evidence-item" data-analysis-evidence-item><header><strong>'
                + escapeHtml(label(item?.evidenceType, "证据")) + '</strong><span>'
                + escapeHtml(currentValue) + '</span></header><p>'
                + escapeHtml(analysisEvidenceDescription(item?.description)) + '</p><small>'
                + escapeHtml(details.join(" · ")) + '</small></article>';
        }).join("") + "</div>";
    }

    function renderRoleCollection(title, items, state) {
        const rows = Array.isArray(items) ? items : [];
        const content = rows.length ? '<ul class="ai-structured-list">' + rows.map(function (item) {
            return '<li>' + escapeHtml(text(item?.text || item?.currentValue || item?.reason || item?.hypothesis || item?.summary, "当前不可查看")) + '</li>';
        }).join("") + '</ul>' : '<p class="muted">' + escapeHtml(label(state, "当前不可查看")) + '</p>';
        return '<section class="ai-structured-section"><h4>' + escapeHtml(title) + '</h4>' + content + '</section>';
    }

    function collectionDescriptor(payload, key, stateKey) {
        const items = Array.isArray(payload?.[key]) ? payload[key] : [];
        return {
            state: payload?.[stateKey],
            size: items.length,
            failurePath: stateKey === "failurePathState"
        };
    }

    function roleCollections(role, payload, mode) {
        if (role === "GPT_FINAL") return [
            collectionDescriptor(payload, "supportingEvidence", "supportingEvidenceState"),
            collectionDescriptor(payload, "opposingEvidence", "opposingEvidenceState")
        ];
        if (role === "GEMINI_REVIEW") return [
            collectionDescriptor(payload, "evidenceGaps", "evidenceGapsState"),
            collectionDescriptor(payload, "logicConflicts", "logicConflictsState"),
            collectionDescriptor(payload, "underestimatedRisks", "underestimatedRisksState")
        ];
        const collections = [
            collectionDescriptor(payload, "opposingScenarios", "opposingScenariosState"),
            collectionDescriptor(payload, "externalEventRisks", "externalEventRisksState"),
            collectionDescriptor(payload, "microstructureRisks", "microstructureRisksState"),
            collectionDescriptor(payload, "watchIndicators", "watchIndicatorsState")
        ];
        if (mode === "OPPORTUNITY_DECISION") {
            collections.unshift(collectionDescriptor(payload, "failurePaths", "failurePathState"));
        }
        return collections;
    }

    function hasRoleIdentity(payload) {
        return hasValue(payload?.analysisId) && hasValue(payload?.traceId)
            && hasValue(payload?.roleState) && hasValue(payload?.generatedAt);
    }

    function roleResultAvailable(role, payload, mode) {
        if (!payload || payload.resultAvailable !== true || !hasRoleIdentity(payload)) return false;
        const state = typeof frontendContract.normalizeRoleState === "function"
            ? frontendContract.normalizeRoleState(payload.roleState) : "UNAVAILABLE";
        if (state !== "READY") return true;
        if (mode === "ANALYSIS_PREVIEW") {
            if (role === "GPT_FINAL") {
                return hasValue(payload.coreJudgment?.marketBias)
                    && hasValue(payload.coreJudgment?.text || payload.summary);
            }
            return true;
        }
        if (role === "GPT_FINAL") {
            return hasValue(payload.coreJudgment?.marketBias)
                && hasValue(payload.coreJudgment?.opportunityState)
                && hasValue(payload.candidateSummary?.planMode);
        }
        if (role === "GEMINI_REVIEW") {
            const review = String(payload.reviewResult || "").toUpperCase();
            const formal = ["APPROVE", "DOWNGRADE", "REJECT_CANDIDATE", "RISK_WARNING"].includes(review);
            if (!formal) return false;
            if (review === "APPROVE") return true;
            return hasValue(payload.downgradeSuggestion?.before)
                && hasValue(payload.downgradeSuggestion?.after)
                && hasValue(payload.downgradeSuggestion?.reason)
                && hasValue(payload.downgradeSuggestion?.recoveryCondition || payload.recoveryCondition);
        }
        return failurePathView(payload.failurePathState, payload.failurePaths).valid;
    }

    function renderRoleFailClosed(message) {
        return '<div class="empty-state ai-role-fail-closed"><strong>'
            + escapeHtml(message || "角色结果当前不可查看")
            + '</strong><span>不会使用旧输出、摘要或其他角色字段补齐。</span></div>';
    }

    function renderPartialRole(role, payload, mode) {
        const head = renderRoleMetadata(payload);
        if (role === "GPT_FINAL") {
            return head
                + renderRoleCollection("支持证据", payload.supportingEvidence, payload.supportingEvidenceState)
                + renderRoleCollection("反对证据", payload.opposingEvidence, payload.opposingEvidenceState);
        }
        if (role === "GEMINI_REVIEW") {
            return head
                + renderRoleCollection("证据缺口", payload.evidenceGaps, payload.evidenceGapsState)
                + renderRoleCollection("逻辑冲突", payload.logicConflicts, payload.logicConflictsState)
                + renderRoleCollection("风险低估", payload.underestimatedRisks, payload.underestimatedRisksState);
        }
        return head
            + (mode === "OPPORTUNITY_DECISION" ? renderFailurePaths(payload.failurePaths, payload.failurePathState) : "")
            + renderRoleCollection("反向情景", payload.opposingScenarios, payload.opposingScenariosState)
            + renderRoleCollection("外部事件风险", payload.externalEventRisks, payload.externalEventRisksState)
            + renderRoleCollection("微观结构风险", payload.microstructureRisks, payload.microstructureRisksState)
            + renderRoleCollection("继续观察指标", payload.watchIndicators, payload.watchIndicatorsState);
    }

    function renderFailurePaths(items, state) {
        const view = failurePathView(state, items);
        const rows = view.paths;
        if (!view.valid || !rows.length) return '<section class="ai-structured-section"><h4>失败路径</h4><p class="muted">'
            + escapeHtml(view.label) + '</p></section>';
        return '<section class="ai-structured-section"><h4>失败路径</h4><div class="failure-path-list">'
            + rows.map(function (item) {
                return '<article class="failure-path-item"><strong>' + escapeHtml(text(item?.hypothesis, "失败路径")) + '</strong>'
                    + factGrid([
                        ["触发", text(item?.triggerCondition)],
                        ["演化", text(item?.causalPath)],
                        ["失效", text(item?.invalidatingEvidence)]
                    ]) + '</article>';
            }).join("") + '</div></section>';
    }

    function renderRoleMetadata(payload) {
        return factGrid([
            ["角色状态", label(payload.roleState)],
            ["数据状态", label(payload.dataState)],
            ["生成时间", formatTime(payload.generatedAt)]
        ]);
    }

    function renderRolePrimary(title, value, note) {
        return '<section class="ai-role-primary"><span>' + escapeHtml(title) + '</span><strong>'
            + escapeHtml(value) + '</strong>' + (note ? '<small>' + escapeHtml(note) + '</small>' : "") + '</section>';
    }

    function renderPreviewRole(role, payload) {
        const head = renderRoleMetadata(payload);
        if (role === "GPT_FINAL") {
            return head + renderRolePrimary("方向假设", label(payload.coreJudgment?.marketBias), "按需分析预览 · 非 Opportunity")
                + factGrid([["形成依据", text(payload.coreJudgment?.text || payload.summary)]])
                + renderRoleCollection("支持证据", payload.supportingEvidence, payload.supportingEvidenceState)
                + renderRoleCollection("反对证据", payload.opposingEvidence, payload.opposingEvidenceState);
        }
        if (role === "GEMINI_REVIEW") {
            return head
                + renderRoleCollection("证据缺口", payload.evidenceGaps, payload.evidenceGapsState)
                + renderRoleCollection("逻辑冲突", payload.logicConflicts, payload.logicConflictsState)
                + renderRoleCollection("可信度复核", payload.underestimatedRisks, payload.underestimatedRisksState);
        }
        return head
            + renderRoleCollection("反向情景", payload.opposingScenarios, payload.opposingScenariosState)
            + renderRoleCollection("外部事件风险", payload.externalEventRisks, payload.externalEventRisksState)
            + renderRoleCollection("微观结构风险", payload.microstructureRisks, payload.microstructureRisksState)
            + renderRoleCollection("继续观察指标", payload.watchIndicators, payload.watchIndicatorsState);
    }

    function renderOpportunityRole(role, payload) {
        const head = renderRoleMetadata(payload);
        if (role === "GPT_FINAL") {
            const candidate = payload.candidateSummary;
            const candidateContent = candidate ? factGrid([
                ["候选参与方式", label(candidate.planMode)],
                ["候选置信度", text(candidate.confidence)],
                ["候选风险", label(candidate.riskLevel)],
                ["建议动作", text(candidate.recommendedAction)],
                ["摘要", text(candidate.summary)]
            ]) : '<p class="muted">Candidate 当前不可查看</p>';
            return head + renderRolePrimary("GPT Candidate · 非 Final",
                candidate ? label(candidate.planMode) : "当前不可查看", "候选参与方式") + factGrid([
                ["方向判断", label(payload.coreJudgment?.marketBias)],
                ["机会进度", label(payload.coreJudgment?.opportunityState)],
                ["计划边界", "Candidate · 非 Final"]
            ]) + renderRoleCollection("支持证据", payload.supportingEvidence, payload.supportingEvidenceState)
                + renderRoleCollection("反对证据", payload.opposingEvidence, payload.opposingEvidenceState)
                + '<section class="ai-structured-section"><h4>Candidate 摘要 · 非 Final</h4>' + candidateContent + '</section>';
        }
        if (role === "GEMINI_REVIEW") {
            const review = String(payload.reviewResult).toUpperCase();
            const suggestion = payload.downgradeSuggestion;
            const illegalWaitingConfirmation = String(analysisAudit?.opportunity?.state || "").toLowerCase() === "waiting_trigger"
                && String(suggestion?.before || "").toUpperCase() === "CONFIRMATION";
            const beforeAfter = review !== "APPROVE" && !illegalWaitingConfirmation
                ? '<section class="ai-structured-section"><h4>Before → After</h4>' + factGrid([
                    ["调整前", label(suggestion.before)], ["调整后", label(suggestion.after)],
                    ["调整原因", text(suggestion.reason)],
                    ["恢复条件", text(suggestion.recoveryCondition || payload.recoveryCondition)]
                ]) + '</section>' : review !== "APPROVE"
                    ? '<section class="ai-structured-section"><h4>Before → After</h4><p class="muted">调整前后当前不可查看</p></section>' : "";
            return head + renderRolePrimary("复核结果", reviewResultLabel(review), "审查对象：Candidate") + factGrid([
                ["对 Candidate 的影响", text(payload.finalDirectionImpact)],
                ["恢复条件", text(payload.recoveryCondition || suggestion?.recoveryCondition)]
            ]) + beforeAfter
                + renderRoleCollection("证据缺口", payload.evidenceGaps, payload.evidenceGapsState)
                + renderRoleCollection("逻辑冲突", payload.logicConflicts, payload.logicConflictsState)
                + renderRoleCollection("风险低估", payload.underestimatedRisks, payload.underestimatedRisksState);
        }
        const failureState = failurePathView(payload.failurePathState, payload.failurePaths);
        return head + renderRolePrimary("失败路径状态", failureState.label, "Grok 反方挑战") + factGrid([
            ["挑战摘要", text(payload.challengeSummary || payload.summary)],
            ["对当前方向的挑战", text(payload.currentDirectionChallenge)],
            ["重大反证", payload.majorCounterEvidence === true ? "是" : payload.majorCounterEvidence === false ? "否" : "当前不可查看"]
        ]) + renderFailurePaths(payload.failurePaths, payload.failurePathState)
            + renderRoleCollection("反向情景", payload.opposingScenarios, payload.opposingScenariosState)
            + renderRoleCollection("外部事件风险", payload.externalEventRisks, payload.externalEventRisksState)
            + renderRoleCollection("微观结构风险", payload.microstructureRisks, payload.microstructureRisksState)
            + renderRoleCollection("继续观察指标", payload.watchIndicators, payload.watchIndicatorsState);
    }

    function renderFormalRole(role, payload) {
        if (!payload) return '<div class="empty-state"><strong>该角色暂无结构化输出</strong><span>不会使用原始 JSON 推断结论。</span></div>';
        const gate = roleGate(payload.roleState, roleResultAvailable(role, payload, analysisMode),
            roleCollections(role, payload, analysisMode));
        if (!gate.allowed) return renderRoleFailClosed(gate.message);
        if (gate.renderMode === "PARTIAL") return renderPartialRole(role, payload, analysisMode);
        return analysisMode === "ANALYSIS_PREVIEW"
            ? renderPreviewRole(role, payload) : renderOpportunityRole(role, payload);
    }

    function renderAiRole(role) {
        const target = document.getElementById("analysisRoleContent");
        if (!target || !analysisAudit) return;
        if (!analysisMode) {
            empty(target, "当前分析模式暂不可用", "仅保留已验证的公共分析事实，不展示 Candidate、复核或失败路径。");
            return;
        }
        const trace = (analysisAudit.aiTraces || []).find(function (item) { return item.role === role; });
        const payload = analysisAudit.aiRoleResults?.roles?.[role];
        if (!trace && !payload) return empty(target, roleLabel(role, analysisMode) + " 暂无输出", "该角色没有返回可验证结果。"), undefined;
        const traceId = payload?.traceId || trace?.traceId;
        const auditEntry = hasValue(traceId)
            ? '<a class="text-action" href="/audit/' + encodeURIComponent(traceId) + '?returnTo=' + encodeURIComponent(window.location.pathname + window.location.search) + '">查看完整审计</a>'
            : '<span class="muted">审计链尚未形成</span>';
        target.innerHTML = '<header class="ai-role-head"><div><strong>' + escapeHtml(roleLabel(role, analysisMode)) + '</strong><small>' + escapeHtml(label(trace?.model, text(trace?.model, "模型未记录"))) + '</small></div>' + stateBadge(payload?.roleState || trace?.status) + '</header><div class="ai-output">' + renderFormalRole(role, payload) + '</div><details class="audit-disclosure"><summary>调用与责任链元数据</summary>' + factGrid([["Analysis", payload?.analysisId || trace?.analysisId], ["Trace", traceId], ["生成时间", formatTime(payload?.generatedAt || trace?.observedAt || trace?.createdAt)], ["耗时", hasValue(trace?.latencyMs) ? trace.latencyMs + " ms" : "当前不可查看"], ["降级", payload?.fallback === true || trace?.fallback === true ? "已进入规则路径" : "未触发"]]) + "</details>" + auditEntry;
    }

    function updateAnalysisRoleLabels(mode) {
        document.querySelectorAll("[data-ai-role]").forEach(function (tab) {
            tab.textContent = roleLabel(tab.dataset.aiRole, mode);
        });
    }

    function bindAiTabs() {
        document.querySelectorAll("[data-ai-role]").forEach(function (tab) {
            tab.addEventListener("click", function () {
                document.querySelectorAll("[data-ai-role]").forEach(function (candidate) {
                    const selected = candidate === tab;
                    candidate.classList.toggle("is-active", selected);
                    candidate.setAttribute("aria-selected", String(selected));
                    candidate.tabIndex = selected ? 0 : -1;
                });
                renderAiRole(tab.dataset.aiRole);
            });
        });
    }

    async function loadAnalysis() {
        if (!resourceId) return;
        try {
            analysisAudit = await api("/api/ai/audit-chain?analysisId=" + encodeURIComponent(resourceId));
            const analysis = analysisAudit.analysis || {};
            const modeView = analysisModeGate(analysis.analysisMode);
            analysisMode = modeView.mode;
            document.getElementById("analysisMode").textContent = modeView.label;
            document.getElementById("analysisModeBoundary").textContent = modeView.message;
            updateAnalysisRoleLabels(analysisMode);
            ["analysisTimeframesSection", "analysisEvidenceSection", "analysisScoresSection"].forEach(function (id) {
                const section = document.getElementById(id);
                if (section) section.hidden = !modeView.valid;
            });
            if (!modeView.valid) {
                document.getElementById("analysisDataQuality").innerHTML = renderRoleFailClosed(modeView.message);
                const conflictSummary = document.getElementById("analysisConflictSummary");
                if (conflictSummary) conflictSummary.hidden = true;
                document.getElementById("analysisAiLayout")?.classList.remove("has-conflict");
                renderAiRole("GPT_FINAL");
                return;
            }
            document.getElementById("analysisDataQuality").innerHTML = factGrid([
                ["数据质量", hasValue(analysis.dataQualityScore) ? formatNumber(analysis.dataQualityScore, { maximumFractionDigits: 0 }) : "当前不可查看"],
                ["分析周期", text(analysis.timeframe, "当前不可查看")],
                ["完成时间", formatTime(analysis.completedAt || analysis.analysisTime)]
            ]);
            document.getElementById("analysisScores").innerHTML = renderAnalysisScores(analysisAudit.scores || []);
            document.getElementById("analysisEvidence").innerHTML = renderAnalysisEvidence(analysisAudit.evidence || []);
            document.getElementById("analysisTimeframes").innerHTML = renderStructured(analysisAudit.decisionBundle?.multiTimeframeStates || analysisAudit.decisionBundle?.multiTimeframeState);
            const resolver = analysisMode === "OPPORTUNITY_DECISION" ? analysisAudit.conflictResolver : null;
            const conflictLevel = String(resolver?.conflictLevel || "").toUpperCase();
            const formalConflict = ["LEVEL_2_MINOR_DISAGREEMENT", "LEVEL_3_SIGNIFICANT_DISAGREEMENT", "LEVEL_4_EXTREME_CONFLICT"].includes(conflictLevel);
            const conflictSummary = document.getElementById("analysisConflictSummary");
            const aiLayout = document.getElementById("analysisAiLayout");
            if (conflictSummary) conflictSummary.hidden = !formalConflict;
            aiLayout?.classList.toggle("has-conflict", formalConflict);
            if (formalConflict) document.getElementById("analysisConflict").innerHTML = factGrid([
                ["冲突等级", label(conflictLevel)],
                ["主要原因", text(resolver.downgradeReason || resolver.ruleVetoReason)],
                ["计划模式", label(resolver.planModeAfter)]
            ]);
            const auxiliary = document.getElementById("analysisAuxiliaryPanels");
            if (auxiliary) {
                auxiliary.hidden = true;
                auxiliary.setAttribute("aria-hidden", "true");
            }
            const diffPanel = document.getElementById("analysisDecisionDiff");
            if (diffPanel) diffPanel.hidden = true;
            selectAnalysisAsset({ symbol: analysis.symbol, baseAsset: String(analysis.symbol || "").replace(/USDT$/, ""), quoteAsset: "USDT" }, { preserveMode: true });
            renderAiRole("GPT_FINAL");
        } catch (_) {
            empty(document.getElementById("analysisRoleContent"), "分析当前不可查看", "未返回可信分析链，当前不会构造 AI 输出。");
        }
    }

    function renderAnalysisSearchResults(items) {
        const target = document.getElementById("analysisAssetResults");
        if (!target) return;
        if (!items.length) return empty(target, "未找到资产", "请尝试其他名称、别名或交易对。"), undefined;
        target.innerHTML = items.map(function (asset) {
            const inPool = analysisPoolSymbols.has(String(asset.symbol || "").toUpperCase());
            return '<button class="search-result" type="button" data-analysis-symbol="' + escapeHtml(asset.symbol)
                + '" data-analysis-name="' + escapeHtml(asset.baseAsset || asset.symbol) + '" data-analysis-quote="'
                + escapeHtml(asset.quoteAsset || "") + '"><span><strong>' + escapeHtml(asset.symbol) + '</strong><small>'
                + escapeHtml(text(asset.baseAsset, "市场资产") + (asset.quoteAsset ? " / " + asset.quoteAsset : ""))
                + '</small></span><em>' + (inPool ? "已观察" : "选择") + '</em></button>';
        }).join("");
    }

    async function searchAnalysisAssets(query) {
        const target = document.getElementById("analysisAssetResults");
        if (!query.trim()) {
            if (target) target.innerHTML = "";
            return;
        }
        try {
            renderAnalysisSearchResults(await api("/api/asset-pool/search?query=" + encodeURIComponent(query.trim()) + "&limit=20") || []);
        } catch (_) {
            empty(target, "搜索当前不可用", "市场资产来源暂时不可用。请稍后重试。");
        }
    }

    function selectAnalysisAsset(asset, options) {
        if (!asset?.symbol) return;
        analysisSelectedAsset = asset;
        const selected = document.getElementById("analysisSelectedAsset");
        if (!selected) return;
        const symbol = String(asset.symbol).toUpperCase();
        const inPool = analysisPoolSymbols.has(symbol);
        selected.hidden = false;
        selected.querySelector("strong").textContent = symbol;
        selected.querySelector("small").textContent = text(asset.baseAsset, symbol.replace(/USDT$/, ""))
            + " · " + (inPool ? "已在观察资产中" : "尚未加入观察资产");
        const start = document.getElementById("startAnalysisPreview");
        if (start) start.disabled = false;
        const add = document.getElementById("addAnalysisAsset");
        if (add) add.hidden = inPool;
    }

    function bindAnalysis() {
        bindAiTabs();
        const returnValue = new URLSearchParams(window.location.search).get("returnTo");
        const returnLink = document.getElementById("analysisReturn");
        if (returnLink && returnValue) {
            returnLink.href = safeReturnTo(returnValue, "/dashboard");
            returnLink.hidden = false;
        }
        let timer;
        const search = document.getElementById("analysisAssetSearch");
        search?.addEventListener("input", function () {
            window.clearTimeout(timer);
            timer = window.setTimeout(function () { searchAnalysisAssets(search.value); }, 180);
        });
        document.getElementById("analysisSearchButton")?.addEventListener("click", function () { searchAnalysisAssets(search?.value || ""); });
        document.getElementById("analysisAssetResults")?.addEventListener("click", function (event) {
            const result = event.target.closest("[data-analysis-symbol]");
            if (!result) return;
            selectAnalysisAsset({ symbol: result.dataset.analysisSymbol, baseAsset: result.dataset.analysisName, quoteAsset: result.dataset.analysisQuote });
        });
        document.getElementById("startAnalysisPreview")?.addEventListener("click", async function (event) {
            if (!analysisSelectedAsset?.symbol) return;
            event.currentTarget.disabled = true;
            event.currentTarget.textContent = "分析中";
            try { await previewAsset(analysisSelectedAsset.symbol); }
            catch (error) { announce(error.message); event.currentTarget.disabled = false; event.currentTarget.textContent = "开始预览"; }
        });
        document.getElementById("addAnalysisAsset")?.addEventListener("click", async function () {
            if (!analysisSelectedAsset?.symbol) return;
            try {
                await addAsset(analysisSelectedAsset.symbol);
                analysisPoolSymbols.add(String(analysisSelectedAsset.symbol).toUpperCase());
                selectAnalysisAsset(analysisSelectedAsset);
            } catch (error) { announce(error.message); }
        });
        api("/api/asset-pool").then(function (items) {
            analysisPoolSymbols = new Set((items || []).map(function (item) { return String(item.symbol || "").toUpperCase(); }));
            const requested = new URLSearchParams(window.location.search).get("asset");
            if (requested && !resourceId) searchAnalysisAssets(requested).then(function () {
                const match = document.querySelector('[data-analysis-symbol="' + CSS.escape(requested.toUpperCase()) + '"]');
                if (match) match.click();
            });
        }).catch(function () { analysisPoolSymbols = new Set(); });
        loadAnalysis();
    }

    async function loadMessages() {
        const target = document.getElementById("messageList");
        try {
            const messages = (await api("/api/workspace/messages?limit=50") || []).filter(function (item) {
                return String(item.sourceMode || item.analysisMode || "").toUpperCase() !== "ANALYSIS_PREVIEW";
            });
            const groups = {
                OPPORTUNITY_PLAN: ["OPPORTUNITY", "PLAN", "FINAL_PLAN", "OPPORTUNITY_PLAN"],
                POSITION_RISK: ["POSITION", "POSITION_RISK", "RISK"],
                SYSTEM: ["SYSTEM", "DATA", "PROVIDER", "HOT_RESET"]
            };
            function groupFor(item) {
                const category = String(item.category || item.targetType || "SYSTEM").toUpperCase();
                return Object.keys(groups).find(function (group) {
                    return groups[group].some(function (token) { return category.indexOf(token) >= 0; });
                }) || "SYSTEM";
            }
            const requestedGroup = new URLSearchParams(window.location.search).get("group");
            const activeGroup = requestedGroup || document.querySelector("[data-message-group].is-active")?.dataset.messageGroup || "OPPORTUNITY_PLAN";
            document.querySelectorAll("[data-message-group]").forEach(function (button) {
                button.classList.toggle("is-active", button.dataset.messageGroup === activeGroup);
            });
            const visible = messages.filter(function (item) { return groupFor(item) === activeGroup; });
            document.getElementById("messageUnreadCount").textContent = String(messages.filter(function (item) { return String(item.readState || "").toUpperCase() !== "READ"; }).length);
            document.getElementById("messageHighPriorityCount").textContent = String(messages.filter(function (item) { return ["HIGH", "EXTREME", "P0", "P1"].includes(String(item.priority || item.severity || "").toUpperCase()); }).length);
            document.getElementById("messageRecheckCount").textContent = String(messages.filter(function (item) {
                return String(item.sourceType || "").toUpperCase() === "PUSH_SNAPSHOT";
            }).length);
            document.getElementById("messageEmpty").hidden = visible.length > 0;
            target.innerHTML = visible.map(function (item) {
                const targetType = String(item.targetType || item.category || "").toUpperCase();
                const messageReturn = "/messages?group=" + encodeURIComponent(activeGroup);
                const sourceType = String(item.sourceType || "").toUpperCase();
                const rawSourceId = String(item.sourceId || "");
                const pushSnapshotId = sourceType === "PUSH_SNAPSHOT" && rawSourceId
                    ? (rawSourceId.startsWith("push-snapshot-") ? rawSourceId : "push-snapshot-" + rawSourceId) : "";
                const href = targetType.indexOf("POSITION") >= 0 && item.positionId ? "/positions/" + encodeURIComponent(item.positionId) + "?returnTo=" + encodeURIComponent(messageReturn)
                    : item.planId ? "/plans/" + encodeURIComponent(item.planId) + "?returnTo=" + encodeURIComponent(messageReturn)
                    : item.analysisId ? "/analysis/" + encodeURIComponent(item.analysisId) + "?returnTo=" + encodeURIComponent(messageReturn) : "";
                const primaryAction = pushSnapshotId
                    ? '<button class="text-action" type="button" data-open-recheck="' + escapeHtml(pushSnapshotId)
                        + '" data-message-id="' + escapeHtml(item.messageId) + '" data-return-to="' + escapeHtml(messageReturn) + '">查看复核</button>'
                    : href ? '<a class="text-action" href="' + escapeHtml(href) + '">查看</a>'
                        : '<button class="text-action" type="button" data-read-message="' + escapeHtml(item.messageId) + '">标为已读</button>';
                return '<article class="message-item" data-message-id="' + escapeHtml(item.messageId) + '"><div><strong>' + escapeHtml(text(item.title, label(item.category))) + '</strong><p>' + escapeHtml(text(item.body, "暂无补充说明")) + '</p><small>' + escapeHtml(formatTime(item.createdAt)) + ' · 站内消息</small></div><div><span class="state-badge">' + escapeHtml(label(item.readState)) + '</span>' + primaryAction + "</div></article>";
            }).join("");
        } catch (_) { empty(target, "消息当前不可查看", "业务消息 owner 未返回可信记录。"); }
        if (!target.dataset.actionsBound) {
            document.addEventListener("click", async function (event) {
                const button = event.target.closest("[data-read-message]");
                if (!button) return;
                try { await api("/api/workspace/messages/" + encodeURIComponent(button.dataset.readMessage) + "/read", { method: "POST" }); await loadMessages(); }
                catch (error) { announce(error.message); }
            });
            document.addEventListener("click", async function (event) {
                const button = event.target.closest("[data-open-recheck]");
                if (!button || button.disabled) return;
                button.disabled = true;
                try {
                    const snapshotId = button.dataset.openRecheck;
                    await api("/api/workspace/rechecks/" + encodeURIComponent(snapshotId) + "/open", {
                        method: "POST", body: JSON.stringify({ messageId: button.dataset.messageId })
                    });
                    window.location.assign("/recheck/" + encodeURIComponent(snapshotId)
                        + "?messageId=" + encodeURIComponent(button.dataset.messageId)
                        + "&returnTo=" + encodeURIComponent(button.dataset.returnTo || "/messages"));
                } catch (error) {
                    button.disabled = false;
                    announce(error.message);
                }
            });
            document.querySelectorAll("[data-message-group]").forEach(function (button) {
                button.addEventListener("click", function () {
                    document.querySelectorAll("[data-message-group]").forEach(function (item) { item.classList.toggle("is-active", item === button); });
                    const url = new URL(window.location.href);
                    url.searchParams.set("group", button.dataset.messageGroup);
                    window.history.replaceState({}, "", url);
                    loadMessages();
                });
            });
            target.dataset.actionsBound = "true";
        }
    }

    function renderRecheck(result) {
        const snapshot = document.getElementById("originalSnapshot");
        const original = result.originalSnapshot || {};
        const current = result.currentResult || {};
        snapshot.innerHTML = factGrid([
            ["资产", text(original.symbol)], ["原计划模式", label(original.planModeSnapshot)],
            ["原入场区", text(original.entryZoneJson)], ["发送时间", formatTime(original.pushCreateTime)],
            ["到期时间", formatTime(original.expiresAt)]
        ]);
        document.getElementById("recheckProgress").innerHTML = factGrid([
            ["快照", result.pushSnapshotId], ["当前行情", hasValue(current.currentPrice) ? "已获取" : "当前不可查看"],
            ["执行环境", label(current.executionStatus, "等待复核")], ["风险", current.currentAccountRiskAllowed === false ? "已阻断" : current.currentAccountRiskAllowed === true ? "允许人工复核" : "当前不可查看"],
            ["状态", label(result.resultState)]
        ]);
        document.getElementById("recheckResult").textContent = label(result.resultState);
        document.getElementById("recheckReason").textContent = text(result.reason, "暂无原因说明");
        document.getElementById("recheckCurrentMetrics").innerHTML = factGrid([
            ["当前价格", formatNumber(current.currentPrice)], ["价格偏移", formatPercent(current.priceDriftRatio == null ? null : Number(current.priceDriftRatio) * 100)],
            ["滑点估算", formatPercent(current.currentSlippageEstimation == null ? null : Number(current.currentSlippageEstimation) * 100)],
            ["数据质量（原快照）", hasValue(original.dataQualityScoreSnapshot) ? text(original.dataQualityScoreSnapshot) : "当前不可查看"],
            ["Opportunity", text(original.pushStatus)], ["Risk", current.currentAccountRiskAllowed === false ? "阻断" : "当前不可查看"],
            ["Confused（原快照）", hasValue(original.confusedScoreSnapshot) ? text(original.confusedScoreSnapshot) : "当前不可查看"]
        ]);
        document.getElementById("recheckDiff").innerHTML = factGrid([
            ["原触发价格", formatNumber(original.triggerPrice)], ["当前价格", formatNumber(current.currentPrice)],
            ["原状态", label(original.pushStatus)], ["复核状态", label(result.resultState)]
        ]);
        document.getElementById("recheckAudit").innerHTML = factGrid([
            ["触发来源", text(current.triggerSource, "尚未形成")], ["Message ID", result.messageId],
            ["Push Snapshot ID", result.pushSnapshotId], ["Push ID", result.pushId],
            ["Recheck ID", text(result.recheckId, "尚未形成")], ["Analysis ID", result.analysisId],
            ["Plan ID", text(result.planId, "当前不可查看")], ["Trace ID", text(result.traceId, "当前不可查看")],
            ["复核时间", formatTime(current.recheckTime)], ["来源状态", label(current.executionStatus, "等待复核")]
        ]);
        const retry = document.getElementById("retryPushRecheck");
        if (retry) retry.hidden = result.retryAvailable !== true;
        const planLink = document.getElementById("recheckPlanLink");
        if (planLink) {
            planLink.hidden = !result.planId;
            if (result.planId) planLink.href = "/plans/" + encodeURIComponent(result.planId) + "?returnTo=" + encodeURIComponent(window.location.pathname + window.location.search);
        }
    }

    function recheckContext() {
        const query = new URLSearchParams(window.location.search);
        return {
            messageId: query.get("messageId"),
            returnTo: safeReturnTo(query.get("returnTo"), "/messages")
        };
    }

    async function loadRecheck(open) {
        const snapshot = document.getElementById("originalSnapshot");
        if (!snapshot || !resourceId) return;
        const context = recheckContext();
        const returnLink = document.getElementById("recheckReturn");
        if (returnLink) returnLink.href = context.returnTo;
        if (!context.messageId) {
            empty(snapshot, "原始快照当前不可查看", "缺少消息与推送快照的所有权上下文。");
            return;
        }
        try {
            const endpoint = "/api/workspace/rechecks/" + encodeURIComponent(resourceId);
            const result = open
                ? await api(endpoint + "/open", { method: "POST", body: JSON.stringify({ messageId: context.messageId }) })
                : await api(endpoint + "?messageId=" + encodeURIComponent(context.messageId));
            renderRecheck(result);
        } catch (_) {
            empty(snapshot, "原始快照当前不可查看", "缺少可验证的用户消息与推送快照关联。");
            document.getElementById("recheckResult").textContent = "当前不可查看";
            document.getElementById("recheckReason").textContent = "当前复核结果不可用，请稍后重试";
        }
    }

    function bindRecheck() {
        document.getElementById("requestPushRecheck")?.addEventListener("click", function () {
            loadRecheck(false);
            announce("正在刷新当前复核结果；该操作不是交易授权");
        });
        document.getElementById("retryPushRecheck")?.addEventListener("click", async function () {
            const context = recheckContext();
            try {
                renderRecheck(await api("/api/workspace/rechecks/" + encodeURIComponent(resourceId) + "/retry", {
                    method: "POST", body: JSON.stringify({ messageId: context.messageId })
                }));
            } catch (error) { announce(error.message); }
        });
        document.getElementById("reanalyzePush")?.addEventListener("click", async function () {
            const context = recheckContext();
            try {
                const result = await api("/api/workspace/rechecks/" + encodeURIComponent(resourceId) + "/reanalyze", {
                    method: "POST", body: JSON.stringify({ messageId: context.messageId })
                });
                if (result.analysisId) {
                    const returnTo = window.location.pathname + window.location.search;
                    window.location.assign("/analysis/" + encodeURIComponent(result.analysisId)
                        + "?returnTo=" + encodeURIComponent(returnTo));
                } else announce("重新分析请求未形成可信结果");
            } catch (error) { announce(error.message); }
        });
        loadRecheck(false);
    }

    async function loadPlan() {
        const revalidation = document.getElementById("requestPlanRevalidation");
        const actualPositionAction = document.getElementById("planActualPositionAction");
        if (actualPositionAction) actualPositionAction.remove();
        if (revalidation) { revalidation.hidden = true; revalidation.disabled = true; }
        if (!resourceId) {
            empty(document.getElementById("finalPlanDetail"), "最终计划当前不可查看", "缺少有效 Final 标识。");
            return;
        }
        const planReturn = document.getElementById("planReturn");
        if (planReturn) planReturn.href = safeReturnTo(new URLSearchParams(window.location.search).get("returnTo"), "/analysis");
        try {
            const plan = await api("/api/workspace/plans/" + encodeURIComponent(resourceId));
            const lifecycle = String(plan.planLifecycleState || "").toUpperCase();
            document.getElementById("planMode").textContent = label(plan.finalPlanMode || plan.planMode);
            document.getElementById("planLifecycle").textContent = label(lifecycle);
            document.getElementById("planSummary").innerHTML = factGrid([["推荐动作", text(plan.recommendedAction)], ["最终方向", label(plan.finalMarketBias)], ["计划版本", text(plan.planVersion)], ["有效期", formatTime(plan.validUntil)]]);
            document.getElementById("planEntry").innerHTML = factGrid([["入场区", text(plan.entryZone)], ["触发条件", text(plan.triggerCondition)], ["触发周期", text(plan.triggerTimeframe)]]);
            document.getElementById("planInvalidation").innerHTML = factGrid([["失效条件", text(plan.invalidCondition)], ["止损逻辑", text(plan.stopLogic)], ["止损", text(plan.stopLoss)]]);
            document.getElementById("planTargets").innerHTML = factGrid([["止盈规则", text(plan.takeProfitRules)], ["目标逻辑", text(plan.targetLogic)], ["持有周期", text(plan.holdingHorizon)]]);
            document.getElementById("planRisk").innerHTML = factGrid([["杠杆建议", text(plan.leverageSuggestion)], ["仓位建议", text(plan.positionSuggestion)], ["风险解释", text(plan.riskExplanation)], ["重新校验规则", text(plan.revalidationRule)]]);
            document.getElementById("planDrawerContent").innerHTML = document.getElementById("finalPlanDetail").innerHTML;
            const canRevalidate = ["CURRENT", "NEEDS_REVALIDATION"].includes(lifecycle);
            if (revalidation) { revalidation.hidden = !canRevalidate; revalidation.disabled = !canRevalidate; revalidation.dataset.lifecycle = lifecycle; }
            const canOpen = ["CONFIRMATION", "REDUCED"].includes(plan.finalPlanMode || plan.planMode) && lifecycle === "CURRENT";
            if (canOpen) {
                const actions = document.querySelector(".workspace-page .section-header");
                const button = document.createElement("button");
                button.id = "planActualPositionAction";
                button.type = "button";
                button.className = "button button-secondary";
                button.dataset.openOverlay = "actual-position";
                button.textContent = "录入实际持仓";
                actions.appendChild(button);
                button.addEventListener("click", function () {
                    preparePlanPositionForm(plan).catch(function () {
                        prepareManualPositionForm();
                        announce("计划上下文当前不可查看，未预填系统计划");
                    });
                }, true);
            }
        } catch (_) {
            empty(document.getElementById("finalPlanDetail"), "最终计划当前不可查看", "仅允许展示通过规则校验的 Final，不使用 Candidate 替代。");
            document.getElementById("planMode").textContent = "当前不可查看";
            document.getElementById("planLifecycle").textContent = "当前不可查看";
            if (revalidation) { revalidation.hidden = true; revalidation.disabled = true; revalidation.removeAttribute("data-lifecycle"); }
        }
    }

    function bindPlan() {
        document.getElementById("requestPlanRevalidation")?.addEventListener("click", async function (event) {
            if (event.currentTarget.hidden || !["CURRENT", "NEEDS_REVALIDATION"].includes(event.currentTarget.dataset.lifecycle || "")) return;
            event.currentTarget.disabled = true;
            try {
                await api("/api/workspace/plan-revalidations", { method: "POST", body: JSON.stringify({ planId: resourceId, triggerType: "MANUAL_REVALIDATION", reason: "USER_REQUESTED" }) });
                announce("重新校验任务已排队");
                await loadTasks();
            } catch (error) { announce(error.message); }
            finally { event.currentTarget.disabled = false; }
        });
        loadPlan();
    }

    async function loadCalendar() {
        const target = document.getElementById("eventList");
        try {
            const data = await api("/api/workspace/events?limit=40") || {};
            const events = (data.macro || []).map(function (item) { return Object.assign({ eventType: "MACRO" }, item); })
                .concat((data.industryAndProject || []).map(function (item) { return Object.assign({ eventType: item.eventType || "INDUSTRY_OR_PROJECT" }, item); }));
            if (!events.length) return empty(target, "暂无事件", "没有返回可信事件窗口。"), undefined;
            target.innerHTML = events.map(function (item, index) {
                return '<article class="event-item" tabindex="0" role="button" aria-label="查看事件详情" data-event-index="' + index + '"><time>' + escapeHtml(formatTime(item.eventTime || item.publishedAt || item.occurredAt)) + '</time><div><strong>' + escapeHtml(text(item.title || item.eventName, "未命名事件")) + '</strong><p>' + escapeHtml(text(item.summary || item.description, "暂无摘要")) + '</p></div>' + stateBadge(item.impactLevel || item.eventType) + "</article>";
            }).join("");
            target._events = events;
            target.addEventListener("click", function (event) {
                const item = event.target.closest("[data-event-index]");
                if (!item) return;
                const record = events[Number(item.dataset.eventIndex)];
                document.getElementById("eventDetailContent").innerHTML = renderStructured(record);
                document.getElementById("eventRelations").innerHTML = renderStructured(data.assetRelations || []);
                openOverlay("event-detail", item);
            });
            target.addEventListener("keydown", function (event) {
                const item = event.target.closest("[data-event-index]");
                if (item && (event.key === "Enter" || event.key === " ")) {
                    event.preventDefault();
                    item.click();
                }
            });
        } catch (_) { empty(target, "事件当前不可查看", "没有返回可信事件来源。"); }
    }

    function renderAuditChain(audit) {
        const target = document.getElementById("auditChain");
        const rows = audit?.orderedStages || [];
        if (!rows.length) return empty(target, "审计链当前不可查看", "没有找到该 Trace 的责任链。"), undefined;
        target.innerHTML = rows.map(function (stage) {
            return '<article class="audit-step"><strong>' + escapeHtml(label(stage.stage)) + '</strong><p>' + escapeHtml(text(stage.owner, "Owner 未记录")) + '</p>' + stateBadge(stage.status) + "</article>";
        }).join("");
        const metadata = JSON.stringify({
            analysisId: audit.analysis?.analysisId,
            candidateId: audit.candidate?.candidateId,
            traceIds: (audit.aiTraces || []).map(function (item) { return item.traceId; }),
            candidateFinalIsolated: audit.candidateFinalIsolated,
            resolverOwnedSeparately: audit.resolverOwnedSeparately,
            ruleValidationOwnedSeparately: audit.ruleValidationOwnedSeparately,
            notTradeInstruction: audit.notTradeInstruction
        }, null, 2);
        document.getElementById("auditMetadata").textContent = metadata;
        document.getElementById("auditDrawerContent").textContent = metadata;
    }

    async function loadAudit() {
        if (!resourceId) return;
        const returnValue = new URLSearchParams(window.location.search).get("returnTo");
        const returnLink = document.getElementById("auditReturn");
        if (returnLink && returnValue) {
            returnLink.href = safeReturnTo(returnValue, "/dashboard");
            returnLink.hidden = false;
        }
        try { renderAuditChain(await api("/api/ai/audit-chain?traceId=" + encodeURIComponent(resourceId))); }
        catch (_) { empty(document.getElementById("auditChain"), "审计链当前不可查看", "没有找到该 Trace 的可信责任链。"); }
    }

    async function loadSettings() {
        try {
            const config = await api("/api/user-config");
            const form = document.getElementById("riskPreferenceForm");
            if (form) {
                form.elements.riskPreference.value = config.riskPreference || "BALANCED";
                form.elements.defaultPoolMode.value = config.defaultPoolMode || "SYSTEM_DEFAULT";
                form.dataset.initialSettings = JSON.stringify(formJson(form));
                syncSettingsSaveState(form, document.getElementById("saveSettings"));
            }
        } catch (_) { announce("设置当前不可查看"); }
        try {
            const provider = await api("/api/system/runtime-readiness-guardrail-status");
            document.getElementById("providerStatus").innerHTML = renderProviderStatus(provider);
        } catch (_) { empty(document.getElementById("providerStatus"), "数据源状态当前不可查看", "不会强制显示就绪。"); }
    }

    function syncSettingsSaveState(form, save) {
        if (!form || !save) return;
        const dirty = JSON.stringify(formJson(form)) !== form.dataset.initialSettings;
        save.disabled = !dirty;
        save.hidden = !dirty;
        save.classList.toggle("is-dirty", dirty);
    }

    function bindSettings() {
        const form = document.getElementById("riskPreferenceForm");
        const save = document.getElementById("saveSettings");
        form?.addEventListener("change", function () {
            syncSettingsSaveState(form, save);
        });
        save?.addEventListener("click", async function () {
            const form = document.getElementById("riskPreferenceForm");
            save.disabled = true;
            try {
                await api("/api/user-config", { method: "PUT", body: JSON.stringify(formJson(form)) });
                form.dataset.initialSettings = JSON.stringify(formJson(form));
                syncSettingsSaveState(form, save);
                announce("设置已保存");
            } catch (error) {
                announce(error.message);
                syncSettingsSaveState(form, save);
            }
        });
        loadSettings();
    }

    function initialize() {
        bindOverlays();
        bindPositionForms();
        loadTasks();
        if (pageKey === "asset-pool") bindAssetPool();
        if (pageKey === "positions") {
            const query = new URLSearchParams(window.location.search);
            const state = query.get("state");
            const sort = query.get("sort");
            const stateFilter = document.getElementById("positionStateFilter");
            const sortControl = document.getElementById("positionSort");
            if (stateFilter && ["ALL", "WAITING", "RISK"].includes(state)) stateFilter.value = state;
            if (sortControl && ["RISK_DESC", "OPENED_DESC"].includes(sort)) sortControl.value = sort;
            selectPositionTab(query.get("tab"));
        }
        if (pageKey === "position-detail") loadPositionDetail();
        if (pageKey === "reviews") bindReviews();
        if (pageKey === "review-detail") loadReviewDetail();
        if (pageKey === "analysis") bindAnalysis();
        if (pageKey === "messages") loadMessages();
        if (pageKey === "recheck") bindRecheck();
        if (pageKey === "plan") bindPlan();
        if (pageKey === "calendar") loadCalendar();
        if (pageKey === "audit") loadAudit();
        if (pageKey === "me") bindSettings();
    }

    initialize();
})();
