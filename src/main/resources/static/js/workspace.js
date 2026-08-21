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
    let restoreFocus = null;
    let analysisAudit = null;
    let assetPoolItems = [];
    let latestTasks = [];
    let analysisSelectedAsset = null;
    let analysisPoolSymbols = new Set();
    let analysisMode = "ANALYSIS_PREVIEW";

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
        const monitoring = trusted
            ? '<section class="position-judgement">' + factGrid(judgment) + '</section><section class="position-conclusion">' + factGrid(conclusion) + '<a class="text-action" href="/positions/' + encodeURIComponent(positionId) + '">查看详情</a></section>'
            : '<section class="position-untrusted-state" role="status"><strong>' + escapeHtml(monitorUnavailableText(monitor)) + '</strong></section>';
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
            const [positions, home] = await Promise.all([
                api("/api/user-positions/open"),
                api("/api/dashboard/home?limit=6")
            ]);
            const active = positions || [];
            document.getElementById("positionEmpty").hidden = active.length > 0;
            positionRows = active.map(function (position) {
                const monitor = (home.positions || []).find(function (item) { return String(item.positionId) === String(position.id); });
                return { position, monitor };
            });
            renderPositionRows();
            const coverage = home?.diagnostics?.accountRiskCoverageState || home?.safety?.accountRiskCoverageState;
            document.getElementById("accountRiskCoverage").textContent = label(coverage, "等待评估");
        } catch (_) {
            empty(grid, "持仓当前不可查看", "未返回可信的用户持仓数据。");
        }
    }

    document.getElementById("positionStateFilter")?.addEventListener("change", renderPositionRows);
    document.getElementById("positionSort")?.addEventListener("change", renderPositionRows);

    function formJson(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    function bindPositionForms() {
        document.getElementById("actualPositionForm")?.addEventListener("submit", async function (event) {
            event.preventDefault();
            const values = formJson(event.currentTarget);
            values.sourceType = values.finalPlanId ? "SYSTEM_PLAN_POSITION" : "MANUAL_POSITION";
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
            try {
                await api("/api/user-positions/" + encodeURIComponent(resourceId) + "/manual-close", { method: "POST", body: JSON.stringify(formJson(event.currentTarget)) });
                closeOverlay(event.currentTarget.closest("dialog"));
                window.location.assign("/reviews");
            } catch (error) { announce(error.message); }
        });
    }

    async function loadPositionDetail() {
        const card = document.getElementById("positionDetailCard");
        if (!card || !resourceId) return;
        try {
            const [position, home, logs] = await Promise.all([
                api("/api/user-positions/" + encodeURIComponent(resourceId)),
                api("/api/dashboard/home?limit=6"),
                api("/api/review/positions/" + encodeURIComponent(resourceId) + "/monitor-logs?limit=30")
            ]);
            const monitor = (home.positions || []).find(function (item) { return String(item.positionId) === String(resourceId); });
            card.innerHTML = renderPosition(position, monitor);
            document.getElementById("actualPositionFacts").innerHTML = factGrid([
                ["资产", position.assetSymbol], ["方向", label(position.side)],
                ["开仓价", formatNumber(position.entryPrice)], ["数量", formatNumber(position.quantity)],
                ["杠杆", formatNumber(position.leverage)], ["开仓时间", formatTime(position.openedAt)]
            ]);
            if (position.finalPlanId) await loadOpeningPlan(position.finalPlanId);
            else empty(document.getElementById("openingPlanBaseline"), "独立手动持仓", "该持仓没有系统最终计划来源。仍可持续监控。 ");
            const timeline = document.getElementById("monitorTimeline");
            if (!(logs || []).length) empty(timeline, "暂无监控记录", "等待首次可信监控。 ");
            else timeline.innerHTML = logs.map(function (item) {
                return '<article class="timeline-item"><time>' + escapeHtml(formatTime(item.observedAt || item.createdAt)) + '</time><strong>' + escapeHtml(label(item.monitorConclusion, "等待监控数据")) + '</strong><p>' + escapeHtml(label(item.riskReason, "暂无风险变化原因")) + "</p></article>";
            }).join("");
        } catch (_) { empty(card, "持仓详情当前不可查看", "未返回可信的持仓事实。"); }
    }

    async function loadOpeningPlan(planId) {
        const target = document.getElementById("openingPlanBaseline");
        try {
            const plan = await api("/api/workspace/plans/" + encodeURIComponent(planId));
            target.innerHTML = factGrid([
                ["计划模式", label(plan.finalPlanMode || plan.planMode)],
                ["计划版本", text(plan.planVersion)],
                ["开仓时计划", plan.planId]
            ]) + '<a class="text-action" href="/plans/' + encodeURIComponent(plan.planId) + '">查看开仓计划</a>';
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
                rows.push(reviewCard("position", item.symbol + " · 持仓复盘", item.reviewStatus, text(item.monitorConclusion, "等待复盘结论"), item.analysisId ? "/reviews/" + encodeURIComponent(item.analysisId) : null));
            });
            (data.opportunityReviews || []).forEach(function (item) {
                rows.push(reviewCard("opportunity", item.symbol + " · 机会复盘", item.outcome, label(item.planMode, "等待后续结果"), item.analysisId ? "/reviews/" + encodeURIComponent(item.analysisId) : null));
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

    function renderAiRole(role) {
        const target = document.getElementById("analysisRoleContent");
        if (!target || !analysisAudit) return;
        const trace = (analysisAudit.aiTraces || []).find(function (item) { return item.role === role; });
        if (!trace) return empty(target, roleLabel(role, analysisMode) + " 暂无输出", "该角色没有返回可验证结果。"), undefined;
        target.innerHTML = '<header class="ai-role-head"><div><strong>' + escapeHtml(roleLabel(role, analysisMode)) + '</strong><small>' + escapeHtml(label(trace.model, text(trace.model, "模型未记录"))) + '</small></div>' + stateBadge(trace.status) + '</header><div class="ai-output">' + renderStructured(trace.outputJson) + '</div><details class="audit-disclosure"><summary>调用元数据</summary>' + factGrid([["Trace", trace.traceId], ["生成时间", formatTime(trace.observedAt || trace.createdAt)], ["耗时", hasValue(trace.latencyMs) ? trace.latencyMs + " ms" : "当前不可查看"], ["降级", trace.fallback === true ? "已进入规则路径" : "未触发"]]) + "</details>";
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
            const mode = analysis.analysisMode || (analysis.preview === true ? "ANALYSIS_PREVIEW" : "OPPORTUNITY_DECISION");
            analysisMode = mode;
            document.getElementById("analysisMode").textContent = label(mode);
            document.getElementById("analysisModeBoundary").textContent = mode === "ANALYSIS_PREVIEW" ? "按需查看当前资产的分析结果。" : "查看当前机会的完整决策结果。";
            updateAnalysisRoleLabels(mode);
            document.getElementById("analysisDataQuality").innerHTML = factGrid([
                ["数据质量", hasValue(analysis.dataQualityScore) ? formatNumber(analysis.dataQualityScore, { maximumFractionDigits: 0 }) : "当前不可查看"],
                ["分析周期", text(analysis.timeframe, "当前不可查看")],
                ["完成时间", formatTime(analysis.completedAt || analysis.analysisTime)]
            ]);
            document.getElementById("analysisScores").innerHTML = renderAnalysisScores(analysisAudit.scores || []);
            document.getElementById("analysisEvidence").innerHTML = renderAnalysisEvidence(analysisAudit.evidence || []);
            document.getElementById("analysisTimeframes").innerHTML = renderStructured(analysisAudit.decisionBundle?.multiTimeframeStates || analysisAudit.decisionBundle?.multiTimeframeState);
            document.getElementById("analysisDiff").innerHTML = renderStructured(analysisAudit.conflictResolver || {});
            const grok = (analysisAudit.aiTraces || []).find(function (item) { return item.role === "GROK_CHALLENGE"; });
            document.getElementById("analysisFailures").innerHTML = renderStructured(grok?.outputJson);
            document.getElementById("analysisConflict").innerHTML = renderStructured(analysisAudit.conflictResolver || {});
            selectAnalysisAsset({ symbol: analysis.symbol, baseAsset: String(analysis.symbol || "").replace(/USDT$/, ""), quoteAsset: "USDT" });
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

    function selectAnalysisAsset(asset) {
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
        document.getElementById("analysisMode").textContent = "按需分析预览";
        document.getElementById("analysisModeBoundary").textContent = "开始后将在独立分析页展示结果。";
    }

    function bindAnalysis() {
        bindAiTabs();
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
            const activeGroup = document.querySelector("[data-message-group].is-active")?.dataset.messageGroup || "OPPORTUNITY_PLAN";
            const visible = messages.filter(function (item) { return groupFor(item) === activeGroup; });
            document.getElementById("messageUnreadCount").textContent = String(messages.filter(function (item) { return String(item.readState || "").toUpperCase() !== "READ"; }).length);
            document.getElementById("messageHighPriorityCount").textContent = String(messages.filter(function (item) { return ["HIGH", "EXTREME", "P0", "P1"].includes(String(item.priority || item.severity || "").toUpperCase()); }).length);
            document.getElementById("messageRecheckCount").textContent = String(messages.filter(function (item) { return !!item.currentRecheckId; }).length);
            document.getElementById("messageEmpty").hidden = visible.length > 0;
            target.innerHTML = visible.map(function (item) {
                const targetType = String(item.targetType || "").toUpperCase();
                const href = targetType.indexOf("POSITION") >= 0 && item.positionId ? "/positions/" + encodeURIComponent(item.positionId)
                    : item.currentRecheckId ? "/recheck/" + encodeURIComponent(item.currentRecheckId)
                    : item.planId ? "/plans/" + encodeURIComponent(item.planId)
                    : item.analysisId ? "/analysis/" + encodeURIComponent(item.analysisId) : "";
                const primaryAction = href ? '<a class="text-action" href="' + escapeHtml(href) + '">查看</a>' : '<button class="text-action" type="button" data-read-message="' + escapeHtml(item.messageId) + '">标为已读</button>';
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
            document.querySelectorAll("[data-message-group]").forEach(function (button) {
                button.addEventListener("click", function () {
                    document.querySelectorAll("[data-message-group]").forEach(function (item) { item.classList.toggle("is-active", item === button); });
                    loadMessages();
                });
            });
            target.dataset.actionsBound = "true";
        }
    }

    async function loadRecheck() {
        const snapshot = document.getElementById("originalSnapshot");
        if (!snapshot || !resourceId) return;
        try {
            const result = await api("/api/workspace/rechecks/" + encodeURIComponent(resourceId));
            snapshot.innerHTML = renderStructured(result.originalSnapshot);
            document.getElementById("recheckResult").textContent = label(result.resultState);
            document.getElementById("recheckReason").textContent = text(result.reason, "暂无原因说明");
        } catch (_) {
            empty(snapshot, "原始快照当前不可查看", "缺少可验证的用户消息与推送快照关联。");
            document.getElementById("recheckResult").textContent = "当前不可查看";
            document.getElementById("recheckReason").textContent = "当前复核结果不可用，请稍后重试";
        }
    }

    function bindRecheck() {
        document.getElementById("requestPushRecheck")?.addEventListener("click", function () {
            loadRecheck();
            announce("正在刷新当前复核结果；该操作不是交易授权");
        });
        loadRecheck();
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
                document.querySelector('#actualPositionForm input[name="finalPlanId"]').value = plan.planId;
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
        if (pageKey === "positions") loadPositions();
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
