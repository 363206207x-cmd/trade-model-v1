(function () {
  "use strict";

  var MOBILE_ASSET_LIMIT = 3;
  var activeRole = "GPT_FINAL";
  var requestSequence = 0;
  var activeRequest = null;

  function text(value, fallback) {
    if (value === null || value === undefined || String(value).trim() === "") {
      return fallback || "--";
    }
    return String(value);
  }

  function booleanText(value, trueText, falseText) {
    if (value === true) return trueText || "是";
    if (value === false) return falseText || "否";
    return "--";
  }

  function listText(value, fallback) {
    return Array.isArray(value) && value.length > 0
      ? value.map(function (item) { return text(item, ""); }).filter(Boolean).join("；")
      : (fallback || "暂无");
  }

  function setText(selector, value, fallback) {
    document.querySelectorAll(selector).forEach(function (node) {
      node.textContent = text(value, fallback);
    });
  }

  function setSelectedAsset(symbol) {
    setText("[data-selected-asset-token]", symbol, "--");
    document.querySelectorAll(".asset-select").forEach(function (card) {
      var selected = card.dataset.symbol === symbol;
      card.classList.toggle("is-selected", selected);
      card.dataset.selected = String(selected);
      card.setAttribute("aria-checked", String(selected));
      card.tabIndex = selected ? 0 : -1;
    });
  }

  function updateExecution(suggestion) {
    var safeSuggestion = suggestion || {};
    var fallbacks = {
      statusLabel: "等待同步",
      blockedReason: "暂无补充说明",
      originalPlanLabel: "暂无可关联的原执行计划"
    };
    document.querySelectorAll("[data-execution-field]").forEach(function (node) {
      var field = node.dataset.executionField;
      node.textContent = text(safeSuggestion[field], fallbacks[field] || "--");
    });
  }

  function updateConsistency(consistency) {
    var safeConsistency = consistency || {};
    var formatted = {
      consistencyLevel: text(safeConsistency.consistencyLevel, "等待同步"),
      level: text(safeConsistency.level, "--"),
      confused: booleanText(safeConsistency.confused),
      consistencySummary: text(
        safeConsistency.consistencySummary,
        "等待 AI 三角色结果同步后生成一致性结论"
      ),
      aiApplicable: safeConsistency.aiApplicable === true ? "适用" : "不适用",
      consistencyScore: text(safeConsistency.consistencyScore, "--"),
      directionalPushBlocked: booleanText(safeConsistency.directionalPushBlocked),
      downgradeReason: text(safeConsistency.downgradeReason, "暂无降级原因")
    };
    Object.keys(formatted).forEach(function (field) {
      setText('[data-consistency-field="' + field + '"]', formatted[field]);
    });
  }

  function roleLabel(role, providedLabel) {
    if (providedLabel) return providedLabel;
    if (role === "GPT_FINAL") return "最终裁决官";
    if (role === "GEMINI_REVIEW") return "冲突复核官";
    if (role === "GROK_CHALLENGE") return "反方挑战官";
    return "未知角色";
  }

  function element(tagName, className, value) {
    var node = document.createElement(tagName);
    if (className) node.className = className;
    if (value !== undefined) node.textContent = value;
    return node;
  }

  function appendDefinition(list, label, value, fullRow) {
    var row = element("div", fullRow ? "full-row" : "");
    row.appendChild(element("dt", "", label));
    row.appendChild(element("dd", "", text(value, "--")));
    list.appendChild(row);
  }

  function appendDetail(details, label, value) {
    var paragraph = element("p");
    paragraph.appendChild(element("strong", "", label + "："));
    paragraph.appendChild(element("span", "", text(value, "暂无")));
    details.appendChild(paragraph);
  }

  function roleFields(tab) {
    if (tab.role === "GPT_FINAL") {
      return {
        summary: [
          ["最终倾向", tab.finalMarketBias, false],
          ["置信度", tab.finalConfidence, false],
          ["风险等级", tab.finalRiskLevel, false],
          ["最终结论", text(tab.finalConclusion, "暂无最终结论"), true],
          ["核心支持证据", listText(tab.coreSupportingEvidence, "暂无核心支持证据"), true]
        ],
        detailsLabel: "展开完整裁决证据",
        details: [
          ["核心反证", listText(tab.coreCounterEvidence)],
          ["AI 计划模式", text(tab.finalPlanMode, "不适用")],
          ["是否值得开仓", tab.worthOpening],
          ["裁决摘要", tab.decisionSummary],
          ["降级 / 阻断原因", tab.downgradeReason]
        ]
      };
    }
    if (tab.role === "GEMINI_REVIEW") {
      return {
        summary: [
          ["复核意见", text(tab.reviewVerdict, "暂无复核意见"), true],
          ["是否建议降级", tab.downgradeRecommendation, false],
          ["复核结论", text(tab.reviewConclusion, "暂无复核结论"), true]
        ],
        detailsLabel: "展开完整复核证据",
        details: [
          ["发现的冲突", listText(tab.detectedContradictions)],
          ["证据不足点", listText(tab.weakEvidence)],
          ["逻辑漏洞", listText(tab.logicGaps)],
          ["风险调整建议", tab.riskAdjustmentSuggestion],
          ["需要人工复核", tab.manualReviewRequired]
        ]
      };
    }
    return {
      summary: [
        ["反方论点", text(tab.challengeThesis, "暂无反方论点"), true],
        ["突发新闻 / 事件风险", listText(tab.eventRisks), true],
        ["反方挑战结论", text(tab.challengeConclusion, "暂无挑战结论"), true]
      ],
      detailsLabel: "展开完整挑战证据",
      details: [
        ["反向证据", listText(tab.counterEvidence)],
        ["情绪反转风险", listText(tab.sentimentReversalRisks)],
        ["微观结构陷阱", listText(tab.microstructureTraps)],
        ["流动性风险", listText(tab.liquidityRisks)]
      ]
    };
  }

  function createRolePanel(tab) {
    var panel = element("article", "role-panel");
    panel.id = "mobile-role-panel-" + tab.role;
    panel.dataset.rolePanel = tab.role;
    panel.setAttribute("role", "tabpanel");
    panel.setAttribute("aria-labelledby", "mobile-role-tab-" + tab.role);
    panel.hidden = tab.role !== activeRole;

    var heading = element("div", "role-heading");
    var headingText = element("div");
    headingText.appendChild(element("span", "", tab.role));
    headingText.appendChild(element("h3", "", roleLabel(tab.role, tab.roleLabel)));
    heading.appendChild(headingText);
    heading.appendChild(element("strong", "", text(tab.runStatusLabel, "等待同步")));
    panel.appendChild(heading);
    panel.appendChild(element("p", "role-status", text(tab.statusMessage, "暂无角色结果")));

    var fields = roleFields(tab);
    var summary = element("dl", "definition-list role-fields role-summary-fields");
    fields.summary.forEach(function (field) {
      appendDefinition(summary, field[0], field[1], field[2]);
    });
    panel.appendChild(summary);

    var details = element("details", "long-details");
    details.appendChild(element("summary", "", fields.detailsLabel));
    fields.details.forEach(function (field) {
      appendDetail(details, field[0], field[1]);
    });
    panel.appendChild(details);
    return panel;
  }

  function activateRole(role, focusTab) {
    activeRole = role;
    document.querySelectorAll("[data-role]").forEach(function (tab) {
      var selected = tab.dataset.role === role;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      if (selected && focusTab) tab.focus();
    });
    document.querySelectorAll("[data-role-panel]").forEach(function (panel) {
      panel.hidden = panel.dataset.rolePanel !== role;
    });
  }

  function bindRoleControls(root) {
    var tabs = Array.from(root.querySelectorAll("[data-role]"));
    tabs.forEach(function (tab, index) {
      tab.addEventListener("click", function () {
        activateRole(tab.dataset.role, false);
      });
      tab.addEventListener("keydown", function (event) {
        var targetIndex = null;
        if (event.key === "ArrowRight") targetIndex = (index + 1) % tabs.length;
        if (event.key === "ArrowLeft") targetIndex = (index - 1 + tabs.length) % tabs.length;
        if (event.key === "Home") targetIndex = 0;
        if (event.key === "End") targetIndex = tabs.length - 1;
        if (targetIndex !== null) {
          event.preventDefault();
          activateRole(tabs[targetIndex].dataset.role, true);
        }
      });
    });
  }

  function renderRoles(tabs) {
    var root = document.querySelector("[data-ai-role-root]");
    if (!root) return;
    root.replaceChildren();
    if (!Array.isArray(tabs) || tabs.length === 0) {
      root.appendChild(element("p", "empty-state", "暂无 AI 三角色结果"));
      return;
    }

    var allowedOrder = ["GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE"];
    var byRole = new Map(tabs.map(function (tab) { return [tab.role, tab]; }));
    var orderedTabs = allowedOrder.map(function (role) { return byRole.get(role); }).filter(Boolean);
    if (!byRole.has(activeRole)) activeRole = "GPT_FINAL";

    var tabList = element("div", "role-tabs");
    tabList.setAttribute("role", "tablist");
    tabList.setAttribute("aria-label", "AI 角色");
    orderedTabs.forEach(function (tab) {
      var button = element("button", "", roleLabel(tab.role, tab.roleLabel));
      button.type = "button";
      button.id = "mobile-role-tab-" + tab.role;
      button.dataset.role = tab.role;
      button.setAttribute("role", "tab");
      button.setAttribute("aria-controls", "mobile-role-panel-" + tab.role);
      button.setAttribute("aria-selected", String(tab.role === activeRole));
      button.tabIndex = tab.role === activeRole ? 0 : -1;
      tabList.appendChild(button);
    });
    root.appendChild(tabList);
    orderedTabs.forEach(function (tab) {
      root.appendChild(createRolePanel(tab));
    });
    bindRoleControls(root);
  }

  function updateAi(aiDecision) {
    var safeAi = aiDecision || {};
    setText("[data-ai-run-status]", safeAi.runStatusLabel, "等待同步");
    updateConsistency(safeAi.consistency);
    renderRoles(safeAi.tabs);
  }

  function failClosedAfterLoadError() {
    updateExecution({
      statusLabel: "数据加载失败",
      blockedReason: "无法同步当前资产，请稍后重试。"
    });
    updateAi({
      runStatusLabel: "同步失败",
      consistency: {},
      tabs: []
    });
  }

  async function selectAsset(symbol, sourceCard) {
    if (!symbol) return;
    requestSequence += 1;
    var sequence = requestSequence;
    if (activeRequest) activeRequest.abort();
    var request = new AbortController();
    activeRequest = request;
    sourceCard.dataset.requestSequence = String(sequence);
    sourceCard.setAttribute("aria-busy", "true");

    try {
      var query = new URLSearchParams({
        selectedSymbol: symbol,
        limit: String(MOBILE_ASSET_LIMIT)
      });
      var response = await fetch("/api/dashboard/home?" + query.toString(), {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" },
        signal: request.signal
      });
      if (!response.ok) throw new Error("HOME_REQUEST_FAILED");
      var envelope = await response.json();
      if (sequence !== requestSequence) return;
      if (!envelope || envelope.code !== 200 || !envelope.data) {
        throw new Error("HOME_RESPONSE_INVALID");
      }
      setSelectedAsset(text(envelope.data.selectedSymbol, symbol));
      updateExecution(envelope.data.executionSuggestion);
      updateAi(envelope.data.aiDecision);
    } catch (error) {
      if (error.name !== "AbortError" && sequence === requestSequence) {
        failClosedAfterLoadError();
      }
    } finally {
      if (sourceCard.dataset.requestSequence === String(sequence)) {
        sourceCard.removeAttribute("aria-busy");
        delete sourceCard.dataset.requestSequence;
      }
      if (activeRequest === request) activeRequest = null;
    }
  }

  function bindAssetPager() {
    var cards = Array.from(document.querySelectorAll(".asset-select"));
    cards.forEach(function (card, index) {
      card.addEventListener("click", function () {
        selectAsset(card.dataset.symbol, card);
      });
      card.addEventListener("keydown", function (event) {
        if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
        event.preventDefault();
        var offset = event.key === "ArrowRight" ? 1 : -1;
        var next = cards[(index + offset + cards.length) % cards.length];
        next.focus();
        selectAsset(next.dataset.symbol, next);
      });
    });
  }

  function bindNavigation() {
    var homeButton = document.querySelector("[data-home-nav]");
    var positionButton = document.querySelector("[data-position-nav]");
    var title = document.getElementById("mobile-home-title");
    var positionTitle = document.getElementById("mobile-position-title");

    if (homeButton) {
      homeButton.addEventListener("click", function () {
        window.scrollTo({ top: 0, behavior: "smooth" });
        window.setTimeout(function () { if (title) title.focus(); }, 260);
      });
    }
    if (positionButton) {
      positionButton.addEventListener("click", function () {
        document.getElementById("position-monitor")?.scrollIntoView({ behavior: "smooth", block: "start" });
        window.setTimeout(function () { if (positionTitle) positionTitle.focus(); }, 260);
      });
    }
  }

  function initialize() {
    bindAssetPager();
    var roleRoot = document.querySelector("[data-ai-role-root]");
    if (roleRoot) bindRoleControls(roleRoot);
    bindNavigation();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize, { once: true });
  } else {
    initialize();
  }
})();
