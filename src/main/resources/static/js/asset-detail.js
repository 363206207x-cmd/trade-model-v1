(function () {
  "use strict";

  var contract = window.TradeModelFrontendContract;
  var root = document.querySelector("[data-asset-detail-root]");
  if (!contract || !root) return;

  var ROLE_ORDER = ["GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE"];
  var POSITION_CONTEXT_STATUSES = [
    "POSITION_SELECTION_REQUIRED",
    "POSITION_NOT_FOUND",
    "POSITION_SYMBOL_MISMATCH"
  ];
  var activeRole = "GPT_FINAL";

  function normalizeSymbol(value) {
    return String(value || "").trim().toUpperCase().replace(/\//g, "");
  }

  function text(value, fallback) {
    return contract.displayText(value, fallback);
  }

  function listText(value, fallback) {
    return Array.isArray(value) && value.length
      ? value.filter(contract.hasText).join("；") || fallback
      : fallback;
  }

  function setText(selector, value, fallback, scope) {
    var node = (scope || document).querySelector(selector);
    if (node) node.textContent = text(value, fallback);
  }

  function setPageStatus(message, status) {
    var node = document.querySelector("[data-page-status]");
    if (!node) return;
    node.textContent = message;
    node.dataset.status = status;
  }

  function setRetryVisible(visible) {
    var retry = document.querySelector("[data-request-retry]");
    if (retry) retry.hidden = !visible;
  }

  function exactAsset(home, requestedSymbol) {
    if (!home || normalizeSymbol(home.selectedSymbol) !== requestedSymbol) return null;
    return (Array.isArray(home.assets) ? home.assets : []).find(function (asset) {
      var slotType = String(asset && asset.slotType || "").trim().toUpperCase();
      var symbol = normalizeSymbol(asset && (asset.rawSymbol || asset.symbol));
      return slotType !== "DEFAULT_SLOT" && symbol === requestedSymbol;
    }) || null;
  }

  function renderAsset(asset) {
    var state = contract.assetStateView(asset.assetState, asset.assetStateLabel);
    setText('[data-asset-field="symbol"]', asset.symbol || asset.rawSymbol, "--");
    setText('[data-asset-field="latestPrice"]', contract.displayNumber(asset.latestPrice), "--");
    setText(
      '[data-asset-field="direction"]',
      asset.marketBiasLabel || asset.marketBias,
      "当前判断不可用"
    );
    setText('[data-asset-field="score"]', contract.displayNumber(asset.compositeScore), "--");
    setText(
      '[data-asset-field="confidence"]',
      asset.confidenceLabel || asset.confidenceLevel,
      "--"
    );
    setText('[data-asset-field="risk"]', asset.riskLabel || asset.riskLevel, "--");
    setText('[data-asset-field="state"]', state.label, "状态待同步");
    setText(
      '[data-asset-field="worthOpening"]',
      asset.worthOpening === true ? "是" : asset.worthOpening === false ? "否" : null,
      "待同步"
    );
    setText('[data-asset-field="conclusion"]', asset.currentConclusion, "暂无可验证结论");

    var statePill = document.querySelector("[data-asset-state]");
    if (statePill) {
      statePill.textContent = state.label;
      statePill.dataset.tone = state.tone;
      statePill.dataset.state = state.code;
    }
  }

  function rolePanel(role) {
    return document.querySelector('[data-role-panel="' + role + '"]');
  }

  function setRoleField(role, field, value, fallback) {
    var panel = rolePanel(role);
    if (!panel) return;
    setText('[data-role-field="' + field + '"]', value, fallback, panel);
  }

  function renderRole(tab) {
    var role = tab.role;
    setRoleField(role, "runStatusLabel", tab.runStatusLabel, "待同步");
    setRoleField(role, "statusMessage", tab.statusMessage, "当前角色观点待同步");

    if (role === "GPT_FINAL") {
      setRoleField(role, "finalMarketBias", tab.finalMarketBias, "--");
      setRoleField(role, "finalConfidence", tab.finalConfidence, "--");
      setRoleField(role, "finalRiskLevel", tab.finalRiskLevel, "--");
      setRoleField(role, "finalConclusion", tab.finalConclusion, "待同步");
      setRoleField(role, "decisionSummary", tab.decisionSummary, "待同步");
      return;
    }

    if (role === "GEMINI_REVIEW") {
      setRoleField(role, "reviewVerdict", tab.reviewVerdict, "待同步");
      setRoleField(role, "downgradeRecommendation", tab.downgradeRecommendation, "--");
      setRoleField(role, "reviewConclusion", tab.reviewConclusion, "待同步");
      setRoleField(
        role,
        "detectedContradictions",
        listText(tab.detectedContradictions, "当前摘要未提供"),
        "当前摘要未提供"
      );
      return;
    }

    setRoleField(role, "challengeThesis", tab.challengeThesis, "待同步");
    setRoleField(
      role,
      "eventRisks",
      listText(tab.eventRisks, "当前摘要未提供"),
      "当前摘要未提供"
    );
    setRoleField(role, "challengeConclusion", tab.challengeConclusion, "待同步");
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

  function renderAi(aiDecision) {
    var ai = aiDecision && typeof aiDecision === "object" ? aiDecision : {};
    setText("[data-ai-run-status]", ai.runStatusLabel, "未调用");
    setText("[data-ai-decision-mode]", ai.decisionModeLabel, "仅规则判断");
    contract.normalizeAiTabs(ai.tabs).forEach(renderRole);
    activateRole(ROLE_ORDER.indexOf(ai.activeTab) >= 0 ? ai.activeTab : activeRole, false);
  }

  function isPositionContextPlan(plan) {
    var status = String(plan && plan.status || "").trim().toUpperCase();
    return Boolean(plan && plan.positionMode === true)
      || POSITION_CONTEXT_STATUSES.indexOf(status) >= 0;
  }

  function clearExecutionFields() {
    ["direction", "entryZone", "stopLoss", "takeProfitRules", "invalidCondition"].forEach(
      function (field) {
        setText('[data-plan-field="' + field + '"]', null, "--");
      }
    );
  }

  function renderExecution(suggestion) {
    var plan = suggestion && typeof suggestion === "object" ? suggestion : {};
    var section = document.querySelector("[data-execution-plan]");
    var values = document.querySelector("[data-plan-values]");
    var positionContext = isPositionContextPlan(plan);

    if (section) section.hidden = false;
    if (positionContext) {
      if (section) section.dataset.planVisible = "false";
      if (values) values.hidden = true;
      setText("[data-plan-status]", null, "当前暂无可验证的执行建议");
      setText("[data-plan-reason]", null, "资产机会计划上下文不可验证");
      clearExecutionFields();
      return;
    }

    var access = contract.executionPlanAccess(plan);
    if (section) section.hidden = false;
    if (section) section.dataset.planVisible = String(access.visible);
    if (values) values.hidden = !access.visible;
    setText("[data-plan-status]", access.statusLabel, "执行建议待同步");
    setText("[data-plan-reason]", access.reason, "计划来源不可验证");

    ["direction", "entryZone", "stopLoss", "takeProfitRules", "invalidCondition"].forEach(
      function (field) {
        setText(
          '[data-plan-field="' + field + '"]',
          access.visible ? plan[field] : null,
          "--"
        );
      }
    );
  }

  function failClosed(message, retryable) {
    setText('[data-asset-field="symbol"]', null, "--");
    setText('[data-asset-field="latestPrice"]', null, "--");
    setText('[data-asset-field="direction"]', null, "当前判断不可用");
    setText('[data-asset-field="score"]', null, "--");
    setText('[data-asset-field="confidence"]', null, "--");
    setText('[data-asset-field="risk"]', null, "--");
    setText('[data-asset-field="state"]', null, "状态待同步");
    setText('[data-asset-field="worthOpening"]', null, "待同步");
    setText('[data-asset-field="conclusion"]', null, "暂无可验证结论");
    var statePill = document.querySelector("[data-asset-state]");
    if (statePill) {
      statePill.textContent = "状态待同步";
      statePill.dataset.tone = "neutral";
      statePill.dataset.state = "unknown";
    }
    renderAi(null);
    renderExecution(null);
    setRetryVisible(retryable === true);
    setPageStatus(message || "暂无可验证数据", "error");
  }

  function bindRoleTabs() {
    document.querySelectorAll("[data-role-tab]").forEach(function (tab) {
      tab.addEventListener("click", function () {
        activateRole(tab.dataset.roleTab, false);
      });
      tab.addEventListener("keydown", function (event) {
        if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
        event.preventDefault();
        var current = ROLE_ORDER.indexOf(tab.dataset.roleTab);
        var offset = event.key === "ArrowRight" ? 1 : -1;
        var next = (current + offset + ROLE_ORDER.length) % ROLE_ORDER.length;
        activateRole(ROLE_ORDER[next], true);
      });
    });
  }

  function bindRetry() {
    var retry = document.querySelector("[data-request-retry]");
    if (!retry) return;
    retry.addEventListener("click", function () {
      loadAssetDetail();
    });
  }

  async function loadAssetDetail() {
    root.setAttribute("aria-busy", "true");
    setRetryVisible(false);
    setPageStatus("正在同步", "loading");

    var selectedSymbol = normalizeSymbol(root.dataset.selectedSymbol);
    if (!/^[A-Z0-9]{2,32}$/.test(selectedSymbol) || selectedSymbol === "DEFAULT_SLOT") {
      root.setAttribute("aria-busy", "false");
      failClosed("资产标识不可验证", false);
      return;
    }

    try {
      var query = new URLSearchParams({
        selectedSymbol: selectedSymbol,
        limit: "12"
      });
      var response = await fetch("/api/dashboard/home?" + query.toString(), {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" }
      });
      if (!response.ok) throw new Error("ASSET_DETAIL_REQUEST_FAILED");
      var parsed = contract.parseApiEnvelope(await response.json());
      if (!parsed.ok) throw new Error("ASSET_DETAIL_RESPONSE_INVALID");
      var asset = exactAsset(parsed.data, selectedSymbol);
      if (!asset) throw new Error("ASSET_DETAIL_NOT_VERIFIED");

      renderAsset(asset);
      renderAi(parsed.data.aiDecision);
      renderExecution(parsed.data.executionSuggestion);
      setRetryVisible(false);
      setPageStatus("已同步", "ready");
    } catch (error) {
      failClosed("数据暂不可用", true);
    } finally {
      root.setAttribute("aria-busy", "false");
    }
  }

  bindRoleTabs();
  bindRetry();
  loadAssetDetail();
})();
