(function () {
  "use strict";

  var contract = window.TradeModelFrontendContract;
  var root = document.querySelector("[data-analysis-detail-root]");
  if (!contract || !root) return;

  var ROLE_ORDER = ["GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE"];
  var SCORE_TYPES = [
    "趋势结构分",
    "资金推动分",
    "杠杆风险分",
    "流动性质量分",
    "情绪温度分",
    "事件冲击分",
    "宏观环境分",
    "综合可信度分"
  ];
  var activeRole = "GPT_FINAL";

  function hasText(value) {
    return contract.hasText(value);
  }

  function displayText(value, fallback) {
    return contract.displayText(value, fallback);
  }

  function setText(selector, value, fallback, scope) {
    var node = (scope || document).querySelector(selector);
    if (node) node.textContent = displayText(value, fallback);
  }

  function setPageStatus(message, status) {
    var node = document.querySelector("[data-page-status]");
    if (!node) return;
    node.textContent = message;
    node.dataset.status = status;
  }

  function setCoverage(selector, label, state) {
    var node = document.querySelector(selector);
    if (!node) return;
    node.textContent = label;
    node.dataset.coverage = state;
  }

  function normalizeSymbol(value) {
    return String(value || "").trim().toUpperCase().replace(/\//g, "");
  }

  function currentAnalysisId() {
    return String(root.dataset.analysisId || contract.readUrlParam("analysisId") || "").trim();
  }

  function currentSelectedSymbol() {
    return normalizeSymbol(root.dataset.selectedSymbol || contract.readUrlParam("selectedSymbol"));
  }

  function updateBackLink() {
    var link = document.querySelector("[data-analysis-back]");
    if (!link) return;
    var symbol = currentSelectedSymbol();
    var mobileView = root.dataset.mobileView === "true";

    if (symbol) {
      var query = new URLSearchParams({ selectedSymbol: symbol });
      if (mobileView) query.set("view", "mobile");
      link.href = "/dashboard/asset-detail?" + query.toString();
      link.setAttribute("aria-label", "返回资产详情");
      return;
    }

    link.href = mobileView ? "/dashboard/mobile" : "/dashboard";
    link.setAttribute("aria-label", mobileView ? "返回移动端首页" : "返回首页");
  }

  function hidePageState() {
    var state = document.querySelector(".page-state[data-page-state]");
    if (state) state.hidden = true;
  }

  function showContent() {
    var content = document.querySelector("[data-analysis-content]");
    if (content) content.hidden = false;
  }

  function hideContent() {
    var content = document.querySelector("[data-analysis-content]");
    if (content) content.hidden = true;
  }

  function showPageState(code, title, message, retryable) {
    var state = document.querySelector(".page-state[data-page-state]");
    if (!state) return;
    root.dataset.pageState = code;
    hideContent();
    setText("[data-page-state-title]", title, "数据暂不可用", state);
    setText("[data-page-state-message]", message, "当前分析无法读取。", state);
    var retry = state.querySelector("[data-request-retry]");
    if (retry) retry.hidden = retryable !== true;
    state.hidden = false;
    setPageStatus(title, "error");
  }

  function formatAnalysisTime(value) {
    return hasText(value) ? contract.formatUtcNaive(value) : "--";
  }

  function renderContext(run, decision) {
    setText('[data-analysis-field="symbol"]', run.symbol, "--");
    setText(
      '[data-analysis-field="direction"]',
      decision && decision.marketBiasHierarchy,
      "待同步"
    );
    setText(
      '[data-analysis-field="scoreConfidence"]',
      "-- / " + displayText(decision && decision.confidenceLevel, "待同步"),
      "-- / 待同步"
    );
    setText('[data-analysis-field="timeframe"]', run.timeframe, "--");
    setText(
      '[data-analysis-field="dataQuality"]',
      contract.displayNumber(run.dataQualityScore),
      "--"
    );
    setText('[data-analysis-field="analysisTime"]', formatAnalysisTime(run.analysisTime), "--");
  }

  function renderMarketJudgment(decision, environment) {
    var hasDecision = decision && typeof decision === "object";
    var hasEnvironment = environment && typeof environment === "object";
    var environmentLabel = null;
    if (hasEnvironment) {
      var environmentParts = [environment.environmentType, environment.riskMode]
        .filter(hasText);
      environmentLabel = environmentParts.join(" · ");
    }

    setText("[data-market-field=\"trend\"]",
      hasDecision ? decision.marketBiasHierarchy : null, "待同步");
    setText("[data-market-field=\"structure\"]", null, "结构判断待同步");
    setText("[data-market-field=\"environment\"]", environmentLabel, "待同步");
    setText(
      "[data-market-field=\"conclusion\"]",
      hasDecision ? decision.conclusionSummary : null,
      "分析摘要待同步，不生成替代解释。"
    );

    var ready = hasDecision && hasText(decision.marketBiasHierarchy);
    setCoverage("[data-market-status]", ready ? "摘要可用" : "部分可用", "partial");
  }

  function createElement(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function appendDefinition(list, label, value) {
    var row = createElement("div");
    row.appendChild(createElement("dt", "", label));
    row.appendChild(createElement("dd", "", value));
    list.appendChild(row);
  }

  function evidenceRows(items) {
    return (Array.isArray(items) ? items : []).filter(function (item) {
      return item && typeof item === "object"
        && [item.evidenceType, item.description, item.direction, item.source].some(hasText);
    }).slice(0, 3);
  }

  function renderEvidence(items) {
    var rows = evidenceRows(items);
    var list = document.querySelector("[data-evidence-list]");
    if (!list) return false;
    list.replaceChildren();

    if (!rows.length) {
      var unavailable = createElement("article", "evidence-card");
      unavailable.dataset.evidenceUnavailable = "";
      var unavailableHeader = createElement("div", "card-heading");
      unavailableHeader.appendChild(createElement("strong", "", "Evidence Card"));
      unavailableHeader.appendChild(createElement("span", "", "不可用"));
      unavailable.appendChild(unavailableHeader);
      var unavailableFields = createElement("dl");
      appendDefinition(unavailableFields, "证据类型", "状态待同步");
      appendDefinition(unavailableFields, "方向", "未提供");
      appendDefinition(unavailableFields, "证据强度", "未提供");
      appendDefinition(unavailableFields, "置信度", "未提供");
      appendDefinition(unavailableFields, "来源", "来源待同步");
      unavailable.appendChild(unavailableFields);
      unavailable.appendChild(createElement("p", "", "证据描述待同步。"));
      unavailable.appendChild(createElement(
        "small",
        "",
        "证据当前不可用，不推断缺失内容。"
      ));
      list.appendChild(unavailable);
      setCoverage("[data-evidence-coverage]", "不可用", "unavailable");
      return false;
    }

    rows.forEach(function (item) {
      var card = createElement("article", "evidence-card");
      var heading = createElement("div", "card-heading");
      heading.appendChild(createElement("strong", "", "Evidence Card"));
      heading.appendChild(createElement("span", "", "Top 3 摘要"));
      card.appendChild(heading);
      var fields = createElement("dl");
      appendDefinition(fields, "证据类型", displayText(item.evidenceType, "未提供"));
      appendDefinition(fields, "方向", displayText(item.direction, "未提供"));
      appendDefinition(fields, "证据强度", "未提供");
      appendDefinition(fields, "置信度", "未提供");
      appendDefinition(fields, "来源", displayText(item.source, "来源待同步"));
      card.appendChild(fields);
      card.appendChild(createElement(
        "p",
        "",
        displayText(item.description, "证据描述待同步。")
      ));
      card.appendChild(createElement(
        "small",
        "",
        "仅显示后端返回摘要；完整来源关联、强度与置信度尚未提供。"
      ));
      list.appendChild(card);
    });
    setCoverage("[data-evidence-coverage]", "Top 3 摘要", "partial");
    return true;
  }

  function scoreRows(items) {
    return (Array.isArray(items) ? items : []).filter(function (item) {
      return item && SCORE_TYPES.indexOf(String(item.scoreType || "").trim()) >= 0;
    }).slice(0, 3);
  }

  function renderScores(items) {
    document.querySelectorAll("[data-score-type]").forEach(function (node) {
      node.textContent = "—";
      node.removeAttribute("data-returned");
    });

    var seen = {};
    var rows = scoreRows(items).filter(function (item) {
      var type = String(item.scoreType || "").trim();
      if (seen[type] || item.scoreValue === null || item.scoreValue === undefined) return false;
      seen[type] = true;
      return true;
    });

    rows.forEach(function (item) {
      var node = document.querySelector(
        '[data-score-type="' + String(item.scoreType).trim() + '"]'
      );
      if (!node) return;
      node.textContent = contract.displayNumber(item.scoreValue);
      node.dataset.returned = "true";
    });

    var label = rows.length ? "Top 3 摘要" : "不可用";
    var state = rows.length ? "partial" : "unavailable";
    setCoverage("[data-score-coverage]", label, state);
    setText("[data-score-card-coverage]", label, "不可用");
    setText(
      "[data-score-note]",
      rows.length
        ? "仅显示后端返回的评分摘要；缺失维度不是 0，不补齐八项。"
        : null,
      "评分不可用；不合成、不平均、不排序。"
    );
    return rows.length > 0;
  }

  function renderTimeframes(decision) {
    var convergence = decision && decision.multiTfConvergence;
    var available = hasText(convergence);
    setText("[data-timeframe-convergence]", convergence, "待同步");
    document.querySelectorAll("[data-timeframe]").forEach(function (node) {
      node.textContent = "待同步";
    });
    setCoverage(
      "[data-timeframe-status]",
      available ? "收敛摘要可用" : "不可用",
      available ? "partial" : "unavailable"
    );
    var module = document.querySelector(".timeframe-module");
    if (module) {
      module.dataset.failState = available
        ? "MULTI_TIMEFRAME_DETAIL_UNAVAILABLE"
        : "MULTI_TIMEFRAME_UNAVAILABLE";
    }
    return available;
  }

  function activateRole(role, focusTab) {
    if (ROLE_ORDER.indexOf(role) < 0) role = "GPT_FINAL";
    activeRole = role;
    document.querySelectorAll("[data-role-tab]").forEach(function (tab) {
      var selected = tab.dataset.roleTab === role;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      if (selected && focusTab) tab.focus();
    });
    document.querySelectorAll("[data-role-panel]").forEach(function (panel) {
      panel.hidden = panel.dataset.rolePanel !== role;
    });
  }

  function renderAiStatus(decision) {
    var conflict = decision && decision.aiConflictLevel;
    document.querySelectorAll("[data-role-state]").forEach(function (node) {
      node.textContent = "当前分析角色结果不可用";
    });
    document.querySelectorAll("[data-role-conflict]").forEach(function (node) {
      node.textContent = displayText(conflict, "待同步");
    });
    setCoverage("[data-ai-status]", "AI 溯源不可用", "unavailable");
    activateRole(activeRole, false);
    return false;
  }

  function validateIdentity(data, requestedAnalysisId, requestedSymbol) {
    var run = data && data.run;
    if (!run || typeof run !== "object") return { ok: false, reason: "RUN_MISSING" };
    if (String(run.analysisId || "") !== requestedAnalysisId) {
      return { ok: false, reason: "ANALYSIS_ID_MISMATCH" };
    }
    if (requestedSymbol && normalizeSymbol(run.symbol) !== requestedSymbol) {
      return { ok: false, reason: "SYMBOL_MISMATCH" };
    }
    return { ok: true, run: run };
  }

  function renderAggregate(data, requestedAnalysisId, requestedSymbol) {
    var identity = validateIdentity(data, requestedAnalysisId, requestedSymbol);
    if (!identity.ok) {
      showPageState(
        "ANALYSIS_NOT_FOUND",
        "Analysis Not Found",
        "分析身份或资产归属不可验证，已停止展示。",
        false
      );
      return;
    }

    var run = identity.run;
    var runStatus = String(run.status || "").trim().toUpperCase();
    if (runStatus === "FAILED") {
      showPageState(
        "ANALYSIS_FAILED",
        "本次分析失败",
        "本次分析没有可验证的完整结果，不会切换到其他分析。",
        false
      );
      return;
    }
    if (runStatus !== "SUCCESS" && runStatus !== "STARTED") {
      showPageState(
        "ANALYSIS_STATUS_UNVERIFIED",
        "分析状态不可验证",
        "当前状态不在已冻结合同内，页面已保持关闭。",
        false
      );
      return;
    }

    hidePageState();
    showContent();

    if (runStatus === "STARTED") {
      renderContext(run, null);
      renderMarketJudgment(null, null);
      renderEvidence([]);
      renderScores([]);
      renderTimeframes(null);
      renderAiStatus(null);
      var processingNotice = document.querySelector("[data-partial-notice]");
      if (processingNotice) processingNotice.hidden = false;
      root.dataset.pageState = "PARTIAL_DATA";
      setPageStatus("分析处理中", "partial");
      return;
    }

    renderContext(run, data.decision);
    renderMarketJudgment(data.decision, data.marketEnvironment);
    renderEvidence(data.evidenceTopItems);
    renderScores(data.scoreTopItems);
    renderTimeframes(data.decision);
    renderAiStatus(data.decision);

    var partial = document.querySelector("[data-partial-notice]");
    if (partial) partial.hidden = false;
    root.dataset.pageState = "PARTIAL_DATA";
    setPageStatus("部分数据可用", "partial");
  }

  function bindRoleTabs() {
    document.querySelectorAll("[data-role-tab]").forEach(function (tab) {
      tab.addEventListener("click", function () {
        activateRole(tab.dataset.roleTab, false);
      });
      tab.addEventListener("keydown", function (event) {
        var current = ROLE_ORDER.indexOf(tab.dataset.roleTab);
        var next = current;
        if (event.key === "ArrowRight") next = (current + 1) % ROLE_ORDER.length;
        else if (event.key === "ArrowLeft") {
          next = (current - 1 + ROLE_ORDER.length) % ROLE_ORDER.length;
        } else if (event.key === "Home") next = 0;
        else if (event.key === "End") next = ROLE_ORDER.length - 1;
        else return;
        event.preventDefault();
        activateRole(ROLE_ORDER[next], true);
      });
    });
  }

  function bindRetry() {
    var retry = document.querySelector("[data-request-retry]");
    if (!retry) return;
    retry.addEventListener("click", loadAnalysisDetail);
  }

  async function loadAnalysisDetail() {
    var analysisId = currentAnalysisId();
    var selectedSymbol = currentSelectedSymbol();
    root.setAttribute("aria-busy", "true");
    root.dataset.pageState = "LOADING";
    hidePageState();
    hideContent();
    setPageStatus("正在同步", "loading");

    if (!analysisId || analysisId.length > 128) {
      root.setAttribute("aria-busy", "false");
      showPageState(
        "ANALYSIS_NOT_FOUND",
        "Analysis Not Found",
        "缺少可验证的 analysisId，无法读取分析详情。",
        false
      );
      return;
    }

    try {
      var response = await fetch(
        "/api/review/aggregate/" + encodeURIComponent(analysisId),
        {
          method: "GET",
          credentials: "same-origin",
          headers: { Accept: "application/json" }
        }
      );
      if (response.status === 404) {
        showPageState(
          "ANALYSIS_NOT_FOUND",
          "Analysis Not Found",
          "没有找到该次分析，不会回退到同资产的其他分析。",
          false
        );
        return;
      }
      if (!response.ok) throw new Error("ANALYSIS_DETAIL_REQUEST_FAILED");
      var parsed = contract.parseApiEnvelope(await response.json());
      if (!parsed.ok) throw new Error("ANALYSIS_DETAIL_RESPONSE_INVALID");
      renderAggregate(parsed.data, analysisId, selectedSymbol);
    } catch (error) {
      showPageState(
        "LOAD_FAILED",
        "Load Failed",
        "分析详情加载失败。可重试读取同一 analysisId。",
        true
      );
    } finally {
      root.setAttribute("aria-busy", "false");
    }
  }

  bindRoleTabs();
  bindRetry();
  updateBackLink();
  loadAnalysisDetail();
})();
