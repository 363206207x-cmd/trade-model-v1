(function () {
  "use strict";

  var frontendContract = window.TradeModelFrontendContract;
  if (!frontendContract) throw new Error("FRONTEND_CONTRACT_MISSING");

  var MOBILE_ASSET_LIMIT = 3;
  var AI_ROLES = frontendContract.AI_ROLES;
  var activeRole = "GPT_FINAL";
  var requestSequence = 0;
  var activeRequest = null;

  function bindPreferredColorScheme() {
    if (!window.matchMedia) return;
    var preference = window.matchMedia("(prefers-color-scheme: dark)");
    var apply = function () {
      document.documentElement.dataset.mobileTheme = preference.matches ? "dark" : "light";
    };
    apply();
    if (preference.addEventListener) preference.addEventListener("change", apply);
    else if (preference.addListener) preference.addListener(apply);
  }

  function text(value, fallback) {
    return frontendContract.displayText(value, fallback);
  }

  function booleanText(value, trueText, falseText) {
    if (value === true) return trueText || "是";
    if (value === false) return falseText || "否";
    return "--";
  }

  function listText(value, fallback) {
    return Array.isArray(value) && value.length > 0
      ? value.map(function (item) { return text(item, ""); }).filter(Boolean).join("；")
      : (fallback || "当前摘要未提供");
  }

  function setText(selector, value, fallback) {
    document.querySelectorAll(selector).forEach(function (node) {
      node.textContent = text(value, fallback);
    });
  }

  function normalizeSymbol(value) {
    return String(value || "")
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "");
  }

  function assetCards() {
    return Array.from(document.querySelectorAll(".asset-select"));
  }

  function selectedAssetCard() {
    return assetCards().find(function (card) {
      return card.dataset.selected === "true" || card.getAttribute("aria-checked") === "true";
    }) || null;
  }

  function keepAssetCardVisible(card, behavior) {
    if (!card) return;
    var pager = card.closest(".asset-pager");
    if (!pager) return;
    var pagerRect = pager.getBoundingClientRect();
    var cardRect = card.getBoundingClientRect();
    var cardLeft = cardRect.left - pagerRect.left + pager.scrollLeft;
    var left = cardLeft - Math.max(0, (pager.clientWidth - card.offsetWidth) / 2);
    var maxLeft = Math.max(0, pager.scrollWidth - pager.clientWidth);
    var targetLeft = Math.min(maxLeft, Math.max(0, left));
    if (behavior === "smooth") pager.scrollTo({ left: targetLeft, behavior: "smooth" });
    else pager.scrollLeft = targetLeft;
  }

  function setWatchActionStatus(message) {
    var status = document.querySelector("[data-watch-action-status]");
    if (status) status.textContent = message || "";
  }

  function updateSelectedSymbolUrl(symbol) {
    frontendContract.replaceUrlParam("selectedSymbol", symbol);
  }

  function setSelectedAsset(symbol) {
    setText("[data-selected-asset-token]", symbol, "--");
    var selectedCard = null;
    document.querySelectorAll(".asset-select").forEach(function (card) {
      var selected = card.dataset.symbol === symbol;
      card.classList.toggle("is-selected", selected);
      card.dataset.selected = String(selected);
      card.setAttribute("aria-checked", String(selected));
      card.tabIndex = selected ? 0 : -1;
      if (selected) selectedCard = card;
    });
    keepAssetCardVisible(selectedCard, "auto");
  }

  function updateExecution(suggestion) {
    var safeSuggestion = suggestion || {};
    var access = frontendContract.executionPlanAccess(safeSuggestion);
    var planFields = [
      "direction",
      "entryZone",
      "stopLoss",
      "leverageSuggestion",
      "takeProfitRules",
      "positionSuggestion",
      "validPeriod",
      "invalidCondition",
      "validFrom",
      "expiresAt",
      "originalPlanLabel"
    ];
    document.querySelectorAll("[data-execution-field]").forEach(function (node) {
      var field = node.dataset.executionField;
      if (field === "statusLabel") {
        node.textContent = access.statusLabel;
        return;
      }
      if (field === "blockedReason") {
        node.textContent = access.reason;
        return;
      }
      node.textContent = access.visible && planFields.indexOf(field) >= 0
        ? text(safeSuggestion[field], "--")
        : "--";
    });
    document.querySelectorAll("[data-execution-detail]").forEach(function (node) {
      node.hidden = !access.visible;
    });
    var section = document.getElementById("execution-advice");
    if (section) section.dataset.exactPlanVisible = String(access.visible);
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
      )
    };
    Object.keys(formatted).forEach(function (field) {
      setText('[data-consistency-field="' + field + '"]', formatted[field]);
    });
  }

  function roleLabel(role, providedLabel) {
    var definition = AI_ROLES.find(function (item) { return item.role === role; });
    if (definition) return definition.label;
    if (providedLabel) return providedLabel;
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
    paragraph.appendChild(element("span", "", text(value, "当前摘要未提供")));
    details.appendChild(paragraph);
  }

  function roleFields(tab) {
    if (tab.role === "GPT_FINAL") {
      return {
        summary: [
          ["最终倾向", tab.finalMarketBias, false],
          ["置信度", tab.finalConfidence, false],
          ["风险等级", tab.finalRiskLevel, false],
          ["最终结论", text(tab.finalConclusion, "当前摘要未提供最终结论"), true],
          ["核心支持证据", listText(tab.coreSupportingEvidence, "当前摘要未包含支持证据"), true]
        ],
        detailsLabel: "查看当前角色观点",
        details: [
          ["核心反证", listText(tab.coreCounterEvidence, "当前摘要未包含反向证据")],
          ["是否值得开仓", tab.worthOpening],
          ["裁决摘要", tab.decisionSummary]
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
        detailsLabel: "查看当前角色观点",
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
      detailsLabel: "查看当前角色观点",
      details: [
        ["反向证据", listText(tab.counterEvidence, "当前摘要未包含反向证据")],
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
    panel.appendChild(element("p", "role-status", text(tab.statusMessage, "当前角色观点待同步")));

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
    panel.appendChild(
      element("p", "ai-provenance-note", "当前角色观点 · 完整证据关联尚未提供")
    );
    return panel;
  }

  function activateRole(role, focusTab) {
    activeRole = role;
    document.querySelectorAll("[data-role]").forEach(function (tab) {
      var selected = tab.dataset.role === role;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      if (selected && focusTab) tab.focus({ preventScroll: true });
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
    var orderedTabs = frontendContract.normalizeAiTabs(tabs);
    if (!AI_ROLES.some(function (definition) {
      return definition.role === activeRole;
    })) {
      activeRole = "GPT_FINAL";
    }

    var tabList = element("div", "role-tabs");
    tabList.setAttribute("role", "tablist");
    tabList.setAttribute("aria-label", "AI 角色");
    orderedTabs.forEach(function (tab) {
      var button = element("button");
      button.type = "button";
      button.id = "mobile-role-tab-" + tab.role;
      button.dataset.role = tab.role;
      button.setAttribute("role", "tab");
      button.setAttribute("aria-controls", "mobile-role-panel-" + tab.role);
      button.setAttribute("aria-selected", String(tab.role === activeRole));
      button.tabIndex = tab.role === activeRole ? 0 : -1;
      button.appendChild(element("span", "role-code", tab.role));
      button.appendChild(element("span", "role-name", roleLabel(tab.role, tab.roleLabel)));
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
      var parsed = frontendContract.parseApiEnvelope(envelope);
      if (!parsed.ok) throw new Error("HOME_RESPONSE_INVALID");
      var selectedSymbol = text(parsed.data.selectedSymbol, symbol);
      setSelectedAsset(selectedSymbol);
      updateSelectedSymbolUrl(selectedSymbol);
      updateExecution(parsed.data.executionSuggestion);
      updateAi(parsed.data.aiDecision);
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
    var cards = assetCards();
    cards.forEach(function (card, index) {
      card.addEventListener("click", function () {
        selectAsset(card.dataset.symbol, card);
      });
      card.addEventListener("keydown", function (event) {
        if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
        event.preventDefault();
        var offset = event.key === "ArrowRight" ? 1 : -1;
        var next = cards[(index + offset + cards.length) % cards.length];
        next.focus({ preventScroll: true });
        keepAssetCardVisible(next, "smooth");
        selectAsset(next.dataset.symbol, next);
      });
    });
  }

  function closeAssetSearch(panel, toggle) {
    panel.hidden = true;
    toggle.setAttribute("aria-expanded", "false");
  }

  function bindWatchTools() {
    var toggle = document.querySelector("[data-asset-search-toggle]");
    var panel = document.getElementById("mobile-asset-search");
    var input = document.querySelector("[data-asset-search-input]");
    var results = document.querySelector("[data-asset-search-results]");
    var cards = assetCards();

    if (cards.length === 0) {
      if (toggle) toggle.disabled = true;
      return;
    }

    function renderSearchResults(query) {
      if (!results) return;
      var normalizedQuery = normalizeSymbol(query);
      var matches = cards.filter(function (card) {
        return normalizeSymbol(card.dataset.symbol).includes(normalizedQuery);
      });
      results.replaceChildren();

      if (matches.length === 0) {
        var emptyItem = element("li", "asset-search-empty", "没有匹配的当前重点资产");
        results.appendChild(emptyItem);
        return;
      }

      matches.forEach(function (card) {
        var item = element("li");
        var symbol = text(card.dataset.symbol, "--");
        var label = card.querySelector(".asset-symbol");
        var resultButton = element(
          "button",
          "asset-search-result",
          label ? text(label.textContent, symbol) : symbol
        );
        resultButton.type = "button";
        resultButton.dataset.searchSymbol = symbol;
        resultButton.addEventListener("click", function () {
          selectAsset(symbol, card);
          closeAssetSearch(panel, toggle);
          setWatchActionStatus("已选择 " + symbol);
          keepAssetCardVisible(card, "smooth");
          card.focus({ preventScroll: true });
        });
        item.appendChild(resultButton);
        results.appendChild(item);
      });
    }

    if (toggle && panel && input && results) {
      toggle.addEventListener("click", function () {
        var opening = panel.hidden;
        panel.hidden = !opening;
        toggle.setAttribute("aria-expanded", String(opening));
        if (opening) {
          renderSearchResults(input.value);
          input.focus();
        }
      });
      input.addEventListener("input", function () {
        renderSearchResults(input.value);
      });
      input.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
          closeAssetSearch(panel, toggle);
          toggle.focus();
        }
        if (event.key === "Enter") {
          var firstMatch = results.querySelector(".asset-search-result");
          if (firstMatch) {
            event.preventDefault();
            firstMatch.click();
          }
        }
      });
    }

  }

  function bindNavigation() {
    var navigation = [
      { control: "[data-home-nav]", target: null, focus: "mobile-page-context" },
      { control: "[data-position-nav]", target: "position-monitor", focus: "mobile-position-title" }
    ];

    function setCurrentNavigation(activeControl) {
      if (!activeControl) return;
      document.querySelectorAll(".bottom-nav [aria-current]").forEach(function (control) {
        control.removeAttribute("aria-current");
      });
      if (activeControl && activeControl.closest(".bottom-nav")) {
        activeControl.setAttribute("aria-current", "page");
      }
    }

    function moveTo(targetId, focusId, activeControl) {
      if (targetId) {
        var target = document.getElementById(targetId);
        if (!target) return;
        target.scrollIntoView({ behavior: "smooth", block: "start", inline: "nearest" });
      } else {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
      setCurrentNavigation(activeControl);
      window.setTimeout(function () {
        var focusTarget = document.getElementById(focusId);
        if (focusTarget) focusTarget.focus({ preventScroll: true });
      }, 260);
    }

    navigation.forEach(function (item) {
      var control = document.querySelector(item.control);
      if (!control) return;
      control.addEventListener("click", function () {
        moveTo(item.target, item.focus, control);
      });
    });

    var headerStatus = document.querySelector("[data-header-status-nav]");
    var headerAlerts = document.querySelector("[data-header-alerts-nav]");
    var headerSearch = document.querySelector("[data-header-search]");
    if (headerStatus) {
      headerStatus.addEventListener("click", function () {
        moveTo("mobile-status", "mobile-status-title", null);
      });
    }
    if (headerAlerts) {
      headerAlerts.addEventListener("click", function () {
        moveTo("mobile-alerts", "mobile-alert-title", null);
      });
    }
    if (headerSearch) {
      headerSearch.addEventListener("click", function () {
        moveTo("watch-assets", "mobile-watch-title", null);
        var panel = document.getElementById("mobile-asset-search");
        var toggle = document.querySelector("[data-asset-search-toggle]");
        var input = document.querySelector("[data-asset-search-input]");
        window.setTimeout(function () {
          if (panel && panel.hidden && toggle && !toggle.disabled) toggle.click();
          else if (input) input.focus();
        }, 280);
      });
    }
  }

  function bindRootHorizontalContainment() {
    var resetHorizontalOffset = function () {
      var scrollingElement = document.scrollingElement || document.documentElement;
      if (scrollingElement && scrollingElement.scrollLeft !== 0) scrollingElement.scrollLeft = 0;
      if (document.documentElement.scrollLeft !== 0) document.documentElement.scrollLeft = 0;
      if (document.body && document.body.scrollLeft !== 0) document.body.scrollLeft = 0;
    };
    window.addEventListener("scroll", resetHorizontalOffset, { passive: true });
    window.addEventListener("orientationchange", resetHorizontalOffset, { passive: true });
    resetHorizontalOffset();
  }

  function initialize() {
    bindPreferredColorScheme();
    bindRootHorizontalContainment();
    bindAssetPager();
    bindWatchTools();
    var initialAsset = selectedAssetCard();
    keepAssetCardVisible(initialAsset, "auto");
    if (initialAsset && initialAsset.dataset.symbol) {
      updateSelectedSymbolUrl(initialAsset.dataset.symbol);
    }
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
