(function () {
  "use strict";

  var frontendContract = window.TradeModelFrontendContract;
  if (!frontendContract) throw new Error("FRONTEND_CONTRACT_MISSING");

  var MOBILE_ASSET_LIMIT = 3;
  var AI_ROLES = frontendContract.AI_ROLES;
  var requestSequence = 0;
  var activeRequest = null;
  var activeAiRole = "GPT_FINAL";

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

  function updateAssetDetailLink(card) {
    var links = document.querySelectorAll(
      "[data-asset-detail-link], [data-ai-analysis-detail-link]"
    );
    var analysisId = card ? String(card.dataset.analysisId || "").trim() : "";
    var symbol = card ? String(card.dataset.symbol || "").trim() : "";
    var identityReady = !!analysisId && !!symbol;
    setText("[data-ai-analysis-symbol]", symbol, "--");
    setText("[data-ai-analysis-id]", analysisId, "待同步");
    setText(
      "[data-ai-analysis-direction]",
      card ? card.dataset.directionLabel : null,
      "--"
    );
    var analysisRoot = document.querySelector("[data-ai-analysis-root]");
    if (analysisRoot) {
      analysisRoot.dataset.analysisIdentity = identityReady ? "verified" : "missing";
    }
    links.forEach(function (link) {
      if (!identityReady) {
        link.removeAttribute("href");
        link.setAttribute("aria-disabled", "true");
        link.tabIndex = -1;
        link.textContent = link.dataset.disabledLabel || "当前不可查看";
        return;
      }
      link.href = "/dashboard/analysis-detail?analysisId="
        + encodeURIComponent(analysisId)
        + "&selectedSymbol="
        + encodeURIComponent(symbol)
        + "&view=mobile";
      link.removeAttribute("aria-disabled");
      link.tabIndex = 0;
      link.textContent = link.dataset.enabledLabel || "分析详情";
    });
  }

  function setSelectedAsset(symbol) {
    var normalized = normalizeSymbol(symbol);
    var selectedCard = null;
    document.querySelectorAll(".asset-select").forEach(function (card) {
      var selected = normalized && normalizeSymbol(card.dataset.symbol) === normalized;
      card.classList.toggle("is-selected", selected);
      card.dataset.selected = String(selected);
      card.setAttribute("aria-checked", String(selected));
      card.tabIndex = selected ? 0 : -1;
      if (selected) selectedCard = card;
    });
    setText(
      "[data-selected-asset-token]",
      selectedCard ? selectedCard.dataset.symbol : symbol,
      "--"
    );
    keepAssetCardVisible(selectedCard, "auto");
    updateAssetDetailLink(selectedCard);
    return selectedCard;
  }

  function worthOpeningText(asset, card) {
    var value = asset && Object.prototype.hasOwnProperty.call(asset, "worthOpening")
      ? asset.worthOpening
      : (card ? card.dataset.worthOpening : null);
    if (value === true || value === "true") return "是";
    if (value === false || value === "false") return "否";
    return "待同步";
  }

  function updateExecution(suggestion, selectedAsset, selectedCard) {
    var safeSuggestion = suggestion || {};
    var access = frontendContract.executionPlanAccess(safeSuggestion);
    var planFields = [
      "direction",
      "entryZone",
      "stopLoss",
      "leverageSuggestion",
      "takeProfitRules",
      "positionSuggestion",
      "invalidCondition",
      "validFrom",
      "expiresAt"
    ];
    document.querySelectorAll("[data-execution-field]").forEach(function (node) {
      var field = node.dataset.executionField;
      if (field === "statusLabel") {
        node.textContent = access.statusLabel;
        return;
      }
      if (field === "blockedReason") {
        node.textContent = text(safeSuggestion.blockedReason, access.reason);
        return;
      }
      if (field === "worthOpening") {
        node.textContent = worthOpeningText(selectedAsset, selectedCard);
        return;
      }
      node.textContent = access.visible && planFields.indexOf(field) >= 0
        ? text(safeSuggestion[field], "--")
        : "--";
    });
    setText("[data-execution-conflict]", safeSuggestion.blockedReason, "--");
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

  function roleSummary(tab) {
    if (tab.resultAvailable !== true) {
      return text(tab.statusMessage, "当前角色观点不可用");
    }
    if (tab.role === "GPT_FINAL") {
      return text(
        tab.finalConclusion,
        text(tab.decisionSummary, text(tab.statusMessage, "当前观点待同步"))
      );
    }
    if (tab.role === "GEMINI_REVIEW") {
      return text(
        tab.reviewConclusion,
        text(tab.reviewVerdict, text(tab.statusMessage, "当前复核待同步"))
      );
    }
    return text(
      tab.challengeConclusion,
      text(tab.challengeThesis, text(tab.statusMessage, "当前挑战待同步"))
    );
  }

  function createRoleSummaryCard(tab) {
    var panel = element("article", "ai-role-summary-card");
    panel.dataset.aiRoleSummary = tab.role;
    panel.dataset.resultAvailable = String(tab.resultAvailable === true);
    if (frontendContract.hasText(tab.statusMessage)) {
      panel.dataset.roleStatusMessage = String(tab.statusMessage);
    }
    var heading = element("div", "role-heading");
    var headingText = element("div");
    headingText.appendChild(element("span", "", tab.role));
    headingText.appendChild(element("h3", "", roleLabel(tab.role, tab.roleLabel)));
    heading.appendChild(headingText);
    heading.appendChild(element("strong", "", text(tab.runStatusLabel, "等待同步")));
    panel.appendChild(heading);
    panel.appendChild(element("p", "role-status", roleSummary(tab)));
    if (tab.role === "GPT_FINAL" && tab.resultAvailable === true) {
      var metrics = element("dl", "role-summary-metrics");
      appendDefinition(metrics, "方向", tab.finalMarketBias, false);
      appendDefinition(metrics, "置信度", tab.finalConfidence, false);
      panel.appendChild(metrics);
    }
    return panel;
  }

  function renderRoles(tabs) {
    var root = document.querySelector("[data-ai-role-root]");
    if (!root) return;
    root.replaceChildren();
    var orderedTabs = frontendContract.normalizeAiTabs(tabs);
    orderedTabs.forEach(function (tab) {
      root.appendChild(createRoleSummaryCard(tab));
    });
  }

  function setActiveAiRole(role, focusTab) {
    if (!AI_ROLES.some(function (item) { return item.role === role; })) return;
    activeAiRole = role;
    document.querySelectorAll("[data-ai-analysis-tab]").forEach(function (tab) {
      var selected = tab.dataset.aiAnalysisTab === role;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      if (selected && focusTab) tab.focus();
    });
    document.querySelectorAll("[data-ai-analysis-role-panel]").forEach(function (panel) {
      panel.hidden = panel.dataset.aiAnalysisRolePanel !== role;
    });
  }

  function bindAiRoleTabs() {
    var tabs = Array.from(document.querySelectorAll("[data-ai-analysis-tab]"));
    tabs.forEach(function (tab, index) {
      tab.addEventListener("click", function () {
        setActiveAiRole(tab.dataset.aiAnalysisTab, false);
      });
      tab.addEventListener("keydown", function (event) {
        if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
        event.preventDefault();
        var offset = event.key === "ArrowRight" ? 1 : -1;
        var next = tabs[(index + offset + tabs.length) % tabs.length];
        setActiveAiRole(next.dataset.aiAnalysisTab, true);
      });
    });
    setActiveAiRole(activeAiRole, false);
  }

  function renderAiAnalysisRoles(tabs, analysisState) {
    var stateView = frontendContract.aiAnalysisStateView(analysisState);
    var canReadRoles = analysisState === "partial";
    var normalized = frontendContract.normalizeAiTabs(canReadRoles ? tabs : []);
    normalized.forEach(function (tab) {
      var panel = document.querySelector(
        '[data-ai-analysis-role-panel="' + tab.role + '"]'
      );
      if (!panel) return;
      var resultAvailable = canReadRoles && tab.resultAvailable === true;
      panel.dataset.resultAvailable = String(resultAvailable);
      var status = panel.querySelector("[data-ai-analysis-role-status]");
      var summary = panel.querySelector("[data-ai-analysis-role-summary]");
      if (status) {
        status.textContent = canReadRoles
          ? text(tab.runStatusLabel, "待同步")
          : stateView.roleStatusLabel;
      }
      if (summary) {
        summary.textContent = canReadRoles
          ? roleSummary(tab)
          : stateView.roleSummary;
      }
      var roleOutput = panel.querySelector("[data-ai-analysis-role-output]");
      if (roleOutput) {
        roleOutput.hidden = !resultAvailable;
      }
      var direction = panel.querySelector("[data-ai-analysis-role-direction]");
      var confidence = panel.querySelector("[data-ai-analysis-role-confidence]");
      if (direction) {
        direction.textContent = resultAvailable ? text(tab.finalMarketBias, "--") : "";
      }
      if (confidence) {
        confidence.textContent = resultAvailable ? text(tab.finalConfidence, "--") : "";
      }
    });
  }

  function renderAiAnalysis(aiDecision, card) {
    var root = document.querySelector("[data-ai-analysis-root]");
    if (!root) return;
    var safeAi = aiDecision || {};
    var analysisId = card ? String(card.dataset.analysisId || "").trim() : "";
    var symbol = card ? String(card.dataset.symbol || "").trim() : "";
    var identityReady = !!analysisId && !!symbol;
    var runStatus = String(safeAi.runStatus || "").trim().toUpperCase();
    var failed = runStatus.indexOf("FAIL") >= 0 || runStatus.indexOf("ERROR") >= 0;
    var loading = runStatus === "LOADING" || runStatus === "PENDING";
    var empty = !card && !failed && !loading;
    var analysisState = frontendContract.aiAnalysisState(
      identityReady,
      failed,
      loading,
      empty
    );
    var stateView = frontendContract.aiAnalysisStateView(analysisState);
    root.dataset.analysisState = analysisState;
    setText(
      "[data-ai-analysis-state-status]",
      analysisState === "partial" ? safeAi.runStatusLabel : stateView.statusLabel,
      stateView.statusLabel
    );
    setText(
      "[data-ai-analysis-run-status]",
      analysisState === "partial" ? safeAi.runStatusLabel : stateView.runStatusLabel,
      stateView.runStatusLabel
    );
    var consistency = analysisState === "partial" ? (safeAi.consistency || {}) : {};
    setText(
      "[data-ai-analysis-consistency-level]",
      consistency.consistencyLevel,
      stateView.consistencyLevel
    );
    setText(
      "[data-ai-analysis-consistency-summary]",
      consistency.consistencySummary,
      stateView.consistencySummary
    );
    setText(
      "[data-ai-analysis-detail-status]",
      stateView.detailStatus
    );
    renderAiAnalysisRoles(safeAi.tabs, analysisState);
  }

  function initialAiDecisionFromHome() {
    var source = document.getElementById("ai-review");
    if (!source) return {};
    var consistency = {};
    ["consistencyLevel", "level", "consistencySummary"].forEach(function (field) {
      var node = source.querySelector('[data-consistency-field="' + field + '"]');
      if (node) consistency[field] = node.textContent;
    });
    var confused = source.querySelector('[data-consistency-field="confused"]');
    if (confused) consistency.confused = confused.textContent.trim() === "是";
    var tabs = Array.from(source.querySelectorAll("[data-ai-role-summary]")).map(
      function (card) {
        var role = card.dataset.aiRoleSummary;
        var status = card.querySelector(".role-heading strong");
        var label = card.querySelector(".role-heading h3");
        var resultAvailable = card.dataset.resultAvailable === "true";
        var tab = {
          role: role,
          roleLabel: label ? label.textContent : roleLabel(role),
          runStatusLabel: status ? status.textContent : "待同步",
          resultAvailable: resultAvailable,
          statusMessage: text(
            card.dataset.roleStatusMessage,
            resultAvailable ? "当前角色状态待同步" : "当前角色观点不可用"
          )
        };
        if (!resultAvailable) return tab;

        var summary = card.querySelector(".role-status");
        var values = card.querySelectorAll(".role-summary-metrics dd");
        if (role === "GPT_FINAL") {
          tab.finalConclusion = summary ? summary.textContent : null;
          tab.finalMarketBias = values[0] ? values[0].textContent : null;
          tab.finalConfidence = values[1] ? values[1].textContent : null;
        } else if (role === "GEMINI_REVIEW") {
          tab.reviewConclusion = summary ? summary.textContent : null;
        } else if (role === "GROK_CHALLENGE") {
          tab.challengeConclusion = summary ? summary.textContent : null;
        }
        return tab;
      }
    );
    var runStatus = source.querySelector("[data-ai-run-status]");
    return {
      runStatusLabel: runStatus ? runStatus.textContent : "等待同步",
      consistency: consistency,
      tabs: tabs
    };
  }

  function updateAi(aiDecision) {
    var safeAi = aiDecision || {};
    setText("[data-ai-run-status]", safeAi.runStatusLabel, "等待同步");
    updateConsistency(safeAi.consistency);
    renderRoles(safeAi.tabs);
    renderAiAnalysis(safeAi, selectedAssetCard());
  }

  function failClosedAfterLoadError() {
    updateExecution({
      statusLabel: "数据加载失败",
      blockedReason: "无法同步当前资产，请稍后重试。"
    }, null, selectedAssetCard());
    updateAi({
      runStatus: "LOAD_FAILED",
      runStatusLabel: "同步失败",
      consistency: {},
      tabs: []
    });
  }

  function matchingAsset(assets, symbol) {
    var normalized = normalizeSymbol(symbol);
    return (Array.isArray(assets) ? assets : []).find(function (asset) {
      return normalizeSymbol(asset && (asset.rawSymbol || asset.symbol)) === normalized;
    }) || null;
  }

  function syncCardNavigationIdentity(card, asset) {
    if (!card) return;
    if (asset && frontendContract.hasText(asset.analysisId)) {
      card.dataset.analysisId = String(asset.analysisId);
    } else {
      delete card.dataset.analysisId;
    }
    if (asset && (asset.worthOpening === true || asset.worthOpening === false)) {
      card.dataset.worthOpening = String(asset.worthOpening);
    } else {
      delete card.dataset.worthOpening;
    }
    var directionLabel = asset && (asset.marketBiasLabel || asset.marketBias);
    if (frontendContract.hasText(directionLabel)) {
      card.dataset.directionLabel = String(directionLabel);
    } else {
      delete card.dataset.directionLabel;
    }
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
    updateExecution({
      statusLabel: "正在同步",
      blockedReason: "当前资产上下文同步中"
    }, null, sourceCard);
    updateAi({
      runStatus: "LOADING",
      runStatusLabel: "正在同步",
      consistency: {},
      tabs: []
    });

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
      var selectedAsset = matchingAsset(parsed.data.assets, selectedSymbol);
      var selectedCard = assetCards().find(function (card) {
        return normalizeSymbol(card.dataset.symbol) === normalizeSymbol(selectedSymbol);
      }) || null;
      syncCardNavigationIdentity(selectedCard, selectedAsset);
      selectedCard = setSelectedAsset(selectedSymbol);
      updateSelectedSymbolUrl(selectedSymbol);
      updateExecution(parsed.data.executionSuggestion, selectedAsset, selectedCard);
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

  function setMobileProductView(view, focusHeading) {
    var showAi = view === "ai";
    var homeView = document.querySelector("[data-mobile-home-view]");
    var aiView = document.querySelector("[data-mobile-ai-view]");
    if (!homeView || !aiView) return;
    homeView.hidden = showAi;
    aiView.hidden = !showAi;
    var homeControl = document.querySelector("[data-home-nav]");
    var aiControl = document.querySelector("[data-ai-nav]");
    if (homeControl) {
      if (showAi) homeControl.removeAttribute("aria-current");
      else homeControl.setAttribute("aria-current", "page");
    }
    if (aiControl) {
      if (showAi) aiControl.setAttribute("aria-current", "page");
      else aiControl.removeAttribute("aria-current");
    }
    frontendContract.replaceUrlParam("view", showAi ? "ai" : null);
    window.scrollTo({ top: 0, behavior: focusHeading ? "smooth" : "auto" });
    if (focusHeading) {
      window.setTimeout(function () {
        var heading = document.getElementById(
          showAi ? "mobile-ai-analysis-title" : "mobile-page-context"
        );
        if (heading) heading.focus({ preventScroll: true });
      }, 260);
    }
  }

  function bindNavigation() {
    function moveTo(targetId, focusId) {
      if (targetId) {
        var target = document.getElementById(targetId);
        if (!target) return;
        target.scrollIntoView({ behavior: "smooth", block: "start", inline: "nearest" });
      } else {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
      window.setTimeout(function () {
        var focusTarget = document.getElementById(focusId);
        if (focusTarget) focusTarget.focus({ preventScroll: true });
      }, 260);
    }

    var homeControl = document.querySelector("[data-home-nav]");
    if (homeControl) {
      homeControl.addEventListener("click", function () {
        setMobileProductView("home", true);
      });
    }

    var aiControl = document.querySelector("[data-ai-nav]");
    if (aiControl) {
      aiControl.addEventListener("click", function () {
        setMobileProductView("ai", true);
      });
    }

    var availabilityStatus = document.querySelector("[data-nav-availability-status]");
    document.querySelectorAll("[data-unavailable-nav]").forEach(function (control) {
      control.addEventListener("click", function () {
        if (availabilityStatus) {
          availabilityStatus.textContent = text(control.textContent, "该页面") + "暂未开放";
        }
      });
    });

    var headerAlerts = document.querySelector("[data-header-alerts-nav]");
    var headerSearch = document.querySelector("[data-header-search]");
    if (headerAlerts) {
      headerAlerts.addEventListener("click", function () {
        if (availabilityStatus) availabilityStatus.textContent = "消息暂未开放";
      });
    }
    if (headerSearch) {
      headerSearch.addEventListener("click", function () {
        moveTo("watch-assets", "mobile-watch-title");
        var panel = document.getElementById("mobile-asset-search");
        var toggle = document.querySelector("[data-asset-search-toggle]");
        var input = document.querySelector("[data-asset-search-input]");
        window.setTimeout(function () {
          if (panel && panel.hidden && toggle && !toggle.disabled) toggle.click();
          else if (input) input.focus();
        }, 280);
      });
    }

    setMobileProductView(
      frontendContract.readUrlParam("view") === "ai" ? "ai" : "home",
      false
    );
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
      updateAssetDetailLink(initialAsset);
      setText(
        '[data-execution-field="worthOpening"]',
        worthOpeningText(null, initialAsset),
        "待同步"
      );
    }
    bindAiRoleTabs();
    updateAi(initialAiDecisionFromHome());
    bindNavigation();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize, { once: true });
  } else {
    initialize();
  }
})();
