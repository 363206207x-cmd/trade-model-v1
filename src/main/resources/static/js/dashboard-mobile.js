(function () {
  "use strict";

  var frontendContract = window.TradeModelFrontendContract;
  if (!frontendContract) throw new Error("FRONTEND_CONTRACT_MISSING");

  var MOBILE_ASSET_LIMIT = 3;
  var AI_ROLES = frontendContract.AI_ROLES;
  var requestSequence = 0;
  var activeRequest = null;
  var activeAiRole = "GPT_FINAL";
  var manualMobileTheme = null;
  var csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "";

  function bindPreferredColorScheme() {
    var preference = window.matchMedia
      ? window.matchMedia("(prefers-color-scheme: dark)")
      : { matches: false };
    var apply = function () {
      document.documentElement.dataset.mobileTheme = manualMobileTheme
        || (preference.matches ? "dark" : "light");
    };
    apply();
    if (preference.addEventListener) preference.addEventListener("change", apply);
    else if (preference.addListener) preference.addListener(apply);
    var toggle = document.querySelector("[data-mobile-theme-toggle]");
    if (toggle) {
      toggle.addEventListener("click", function () {
        manualMobileTheme = document.documentElement.dataset.mobileTheme === "dark"
          ? "light" : "dark";
        apply();
        toggle.setAttribute("aria-pressed", String(manualMobileTheme === "dark"));
      });
      toggle.setAttribute(
        "aria-pressed",
        String(document.documentElement.dataset.mobileTheme === "dark")
      );
    }
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

  function stableSubmissionId(prefix) {
    var value = window.crypto && typeof window.crypto.randomUUID === "function"
      ? window.crypto.randomUUID()
      : Date.now().toString(36) + "-" + Math.random().toString(36).slice(2);
    return prefix + ":" + value;
  }

  function mobileAnalysisSubmission(symbol, sourceAnalysisId) {
    var key = "mobile-analysis-preview:" + normalizeSymbol(symbol) + ":5m:"
      + String(sourceAnalysisId || "missing");
    var saved = {};
    try { saved = JSON.parse(window.sessionStorage.getItem(key) || "{}"); } catch (ignored) { saved = {}; }
    if (!saved.submissionId) saved.submissionId = stableSubmissionId("analysis-preview");
    saved.sourceAnalysisId = sourceAnalysisId;
    window.sessionStorage.setItem(key, JSON.stringify(saved));
    return { key: key, value: saved };
  }

  async function mobileApi(url, options) {
    var request = Object.assign({ credentials: "same-origin", headers: { Accept: "application/json" } }, options || {});
    request.headers = Object.assign({}, request.headers || {});
    if (request.body) request.headers["Content-Type"] = "application/json";
    if (csrfToken && csrfHeader && request.method && request.method !== "GET") request.headers[csrfHeader] = csrfToken;
    var response = await fetch(url, request);
    var envelope = await response.json().catch(function () { return null; });
    if (!response.ok) throw new Error(text(envelope && envelope.msg, "分析请求失败"));
    var parsed = frontendContract.parseApiEnvelope(envelope);
    if (!parsed.ok) throw new Error(parsed.message);
    return parsed.data;
  }

  async function recoverMobileAnalysisTask(taskId) {
    if (!taskId) return null;
    for (var attempt = 0; attempt < 20; attempt++) {
      var tasks = await mobileApi("/api/workspace/tasks?limit=30");
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

  async function openOrResumeMobileAssetAnalysis(card) {
    var symbol = card && card.dataset.symbol;
    var sourceAnalysisId = card && card.dataset.analysisId;
    if (!symbol || !sourceAnalysisId) throw new Error("当前资产缺少可追溯分析");
    var submission = mobileAnalysisSubmission(symbol, sourceAnalysisId);
    setWatchActionStatus("三 AI 分析启动中");
    var result = await mobileApi("/api/asset-pool/search/" + encodeURIComponent(symbol)
      + "/analysis-preview?timeframe=5m&submissionId="
      + encodeURIComponent(submission.value.submissionId), { method: "POST" });
    submission.value.taskId = result && result.taskId;
    submission.value.analysisId = result && result.analysisId;
    window.sessionStorage.setItem(submission.key, JSON.stringify(submission.value));
    if ((!result || !result.analysisId) && result && result.taskId) {
      var recovered = await recoverMobileAnalysisTask(result.taskId);
      if (recovered && recovered.resultResourceId) result.analysisId = recovered.resultResourceId;
    }
    if (!result || !result.analysisId) throw new Error("分析任务尚未返回结果标识");
    return result;
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

  function setAssetCardField(card, field, value, fallback) {
    var node = card && card.querySelector('[data-asset-field="' + field + '"]');
    if (node) node.textContent = text(value, fallback);
  }

  function setAssetCardSource(card, field, source) {
    var node = card && card.querySelector('[data-asset-source="' + field + '"]');
    if (!node) return;
    var view = frontendContract.fieldSourceView(source);
    var label = field === "latestPrice" ? "价格" : (field === "score" ? "评分" : "置信");
    node.textContent = label + "·" + view.label;
    node.dataset.sourceTone = view.tone;
  }

  function appendAssetMetric(parent, label, value, className) {
    var metric = element("span", className || "");
    metric.appendChild(element("small", "", label));
    metric.appendChild(element("b", "", text(value, "--")));
    parent.appendChild(metric);
    return metric;
  }

  function createMobileAssetCard(asset, selected, index, count) {
    var symbol = text(asset && (asset.rawSymbol || asset.symbol), "--");
    var card = element("button", "asset-card asset-select" + (selected ? " is-selected" : ""));
    card.type = "button";
    card.setAttribute("role", "radio");
    card.setAttribute("aria-checked", String(selected));
    card.setAttribute("aria-posinset", String(index + 1));
    card.setAttribute("aria-setsize", String(count));
    card.setAttribute("aria-label", "切换到 " + symbol + " 决策上下文");
    card.tabIndex = selected ? 0 : -1;
    card.dataset.symbol = symbol;
    card.dataset.selected = String(selected);
    syncCardNavigationIdentity(card, asset);

    var hidden = element("span", "visually-hidden", "重点资产 " + (index + 1) + "，共 " + count + " 个");
    card.appendChild(hidden);

    var top = element("span", "asset-card-top");
    top.appendChild(element("span", "asset-symbol", text(asset && asset.symbol, symbol)));
    var state = element("span", "asset-state", "资产状态 · " + assetStateText(asset));
    state.dataset.assetField = "state";
    top.appendChild(state);
    card.appendChild(top);

    var priceScore = element("span", "asset-price-score");
    var priceMetric = appendAssetMetric(priceScore, "最新价格", asset && asset.latestPrice);
    priceMetric.querySelector("b").dataset.assetField = "latestPrice";
    var scoreMetric = appendAssetMetric(priceScore, "综合评分", asset && asset.compositeScore);
    scoreMetric.querySelector("b").dataset.assetField = "compositeScore";
    card.appendChild(priceScore);

    var core = element("span", "asset-core-grid");
    var directionMetric = appendAssetMetric(core, "方向", asset && (asset.marketBiasLabel || asset.marketBias));
    directionMetric.querySelector("b").dataset.assetField = "marketBias";
    var confidenceMetric = appendAssetMetric(core, "置信度", asset && (asset.confidenceLabel || asset.confidenceLevel));
    confidenceMetric.querySelector("b").dataset.assetField = "confidence";
    var riskMetric = appendAssetMetric(core, "风险", asset && (asset.riskLabel || asset.riskLevel));
    riskMetric.querySelector("b").dataset.assetField = "risk";
    card.appendChild(core);

    var secondary = element("span", "asset-secondary-strip");
    [
      ["数据", frontendContract.dataQualityLabel(asset && asset.dataQuality), "dataQuality"],
      ["多周期", asset && asset.multiTimeframeState, "multiTimeframeState"],
      ["Confused", asset && asset.confused === true ? "是" : (asset && asset.confused === false ? "否" : null), "confused"],
      ["更新", frontendContract.formatBusinessTimeCompact(asset && asset.updatedAt), "updatedAt"]
    ].forEach(function (item) {
      var metric = appendAssetMetric(secondary, item[0], item[1]);
      metric.querySelector("b").dataset.assetField = item[2];
    });
    card.appendChild(secondary);

    var sourceLine = element("span", "asset-source-line");
    sourceLine.setAttribute("aria-label", "字段来源");
    var sourceFields = [
      ["latestPrice", "价格"], ["score", "评分"], ["confidence", "置信"]
    ];
    sourceFields.forEach(function (item) {
      var source = element("small");
      source.dataset.assetSource = item[0];
      sourceLine.appendChild(source);
    });
    card.appendChild(sourceLine);
    syncAssetCardProjection(card, asset);
    return card;
  }

  function renderMobileAssets(assets, selectedSymbol, moduleState) {
    var pager = document.querySelector("[data-mobile-assets-pager]");
    var empty = document.querySelector("[data-mobile-assets-empty]");
    var list = (Array.isArray(assets) ? assets : []).slice(0, MOBILE_ASSET_LIMIT);
    if (empty) empty.hidden = list.length > 0;
    if (!pager) return null;
    pager.hidden = list.length === 0;
    pager.replaceChildren();
    var normalizedSelected = normalizeSymbol(selectedSymbol);
    list.forEach(function (asset, index) {
      var symbol = normalizeSymbol(asset && (asset.rawSymbol || asset.symbol));
      var selected = normalizedSelected ? symbol === normalizedSelected : index === 0;
      pager.appendChild(createMobileAssetCard(asset, selected, index, list.length));
    });
    var root = document.getElementById("watch-assets");
    if (root) {
      root.dataset.contextState = frontendContract.normalizeModuleState(
        moduleState,
        list.length ? "READY" : "EMPTY"
      ).toLowerCase();
    }
    var note = document.querySelector(".watch-limit-note");
    if (note) note.textContent = list.length ? (list.length + " 个资产 · 横向切换") : "暂无可验证资产";
    var searchToggle = document.querySelector("[data-asset-search-toggle]");
    if (searchToggle) searchToggle.disabled = list.length === 0;
    return selectedAssetCard();
  }

  function updateMobileFocus(asset, moduleState, requestedSymbol) {
    var root = document.querySelector("[data-mobile-focus-root]");
    if (!root) return;
    var normalizedState = frontendContract.normalizeModuleState(
      asset && asset.moduleState || moduleState,
      "MISSING"
    ).toLowerCase();
    root.dataset.moduleState = normalizedState;
    var values = {
      symbol: asset && (asset.symbol || asset.rawSymbol) || requestedSymbol,
      assetState: "资产状态 · " + (asset ? assetStateText(asset) : (normalizedState === "loading" ? "正在同步" : "当前不可查看")),
      direction: asset && (asset.marketBiasLabel || asset.marketBias),
      confidence: asset && (asset.confidenceLabel || asset.confidenceLevel),
      risk: asset && (asset.riskLabel || asset.riskLevel),
      dataQuality: asset ? frontendContract.dataQualityLabel(asset.dataQuality) : null
    };
    Object.keys(values).forEach(function (field) {
      setText('[data-mobile-focus-field="' + field + '"]', values[field], "--");
    });
  }

  function serverRenderedAsset(card) {
    if (!card) return null;
    return {
      symbol: card.dataset.symbol,
      assetState: card.dataset.assetState,
      moduleState: card.dataset.moduleState,
      marketBiasLabel: card.dataset.directionLabel,
      confidenceLabel: card.dataset.confidenceLabel,
      riskLabel: card.dataset.riskLabel,
      dataQuality: card.dataset.qualityLabel
    };
  }

  function positionObservationStateText(position) {
    var state = String(position && position.monitorTrustState || "PENDING_FIRST_RUN").toUpperCase();
    return {
      VERIFIED_FRESH: "可信监控可用",
      PENDING_FIRST_RUN: "等待首次监控",
      PENDING: "等待监控数据",
      PENDING_VERIFICATION: "等待监控数据",
      BASE_PRICE_VERIFIED_OPTIONAL_CONTEXT_PENDING: "行情已更新，完整监控待验证",
      STALE: "监控数据已过期",
      INVALID: "监控数据当前不可用",
      SOURCE_UNAVAILABLE: "监控来源不可用"
    }[state] || "等待监控数据";
  }

  function renderMobileSignalList(kind, items) {
    var isAlert = kind === "alert";
    var empty = document.querySelector(isAlert ? "[data-mobile-alert-empty]" : "[data-mobile-event-empty]");
    var details = document.querySelector(isAlert ? "[data-mobile-alert-list]" : "[data-mobile-event-list]");
    var list = (Array.isArray(items) ? items : []).slice(0, 2);
    if (empty) {
      empty.hidden = list.length > 0;
      empty.textContent = isAlert ? "暂无高优先级告警" : "暂无高影响关键事件";
    }
    if (!details) return;
    details.hidden = list.length === 0;
    details.open = false;
    details.replaceChildren();
    if (!list.length) return;

    var first = list[0] || {};
    var summary = element("summary");
    var summaryBody = element("span", "signal-summary");
    var summaryLine = element("span", "signal-line");
    summaryLine.appendChild(element("strong", "", text(
      isAlert ? first.level : first.impactLevel,
      "状态未知"
    )));
    summaryLine.appendChild(element("span", "", text(
      isAlert ? first.symbol : first.timeWindow,
      "--"
    )));
    summaryBody.appendChild(summaryLine);
    summaryBody.appendChild(element("span", "", text(
      isAlert ? first.message : first.label,
      isAlert ? "暂无告警说明" : "暂无事件说明"
    )));
    summary.appendChild(summaryBody);
    details.appendChild(summary);

    var expanded = element("div", "signal-expanded");
    list.forEach(function (item) {
      var article = element("article");
      var title = element("p");
      title.appendChild(element("strong", "", text(
        isAlert ? item.level : item.impactLevel,
        "状态未知"
      )));
      title.appendChild(document.createTextNode(" · " + text(
        isAlert ? item.symbol : item.label,
        "--"
      )));
      article.appendChild(title);
      article.appendChild(element("p", "", text(
        isAlert ? item.message : item.type,
        isAlert ? "暂无告警说明" : "事件类型未提供"
      )));
      article.appendChild(element("p", "", (isAlert ? "时间：" : "窗口：") + text(
        isAlert ? item.time : item.timeWindow,
        "--"
      )));
      expanded.appendChild(article);
    });
    details.appendChild(expanded);
  }

  function renderMobilePositions(positions, moduleState) {
    var root = document.querySelector("[data-mobile-position-list]");
    var empty = document.querySelector("[data-mobile-positions-empty]");
    var list = (Array.isArray(positions) ? positions : []).slice(0, 3);
    var state = frontendContract.normalizeModuleState(
      moduleState,
      list.length ? "READY" : "EMPTY"
    ).toLowerCase();
    if (empty) {
      empty.hidden = list.length > 0;
      empty.textContent = state === "error"
        ? "持仓数据当前不可查看"
        : (state === "missing" ? "持仓身份当前不可查看" : "暂无手动持仓");
    }
    if (!root) return;
    root.hidden = list.length === 0;
    root.replaceChildren();
    list.forEach(function (position, index) {
      var card = element("article", "position-card" + (index === 2 ? " position-third" : ""));
      card.dataset.positionId = text(position.positionId, "");
      card.dataset.moduleState = frontendContract.normalizeModuleState(
        position.moduleState,
        "PARTIAL"
      ).toLowerCase();
      var heading = element("div", "position-heading");
      heading.appendChild(element("h3", "", text(position.symbol, "--")));
      heading.appendChild(element("span", "", text(position.directionLabel || position.direction, "--")));
      card.appendChild(heading);

      var core = element("dl", "position-core position-summary");
      appendDefinition(core, "监控状态", positionObservationStateText(position), false);
      appendDefinition(core, "当前风险", position.riskLevelLabel || position.riskLevel, false);
      appendDefinition(core, "入场逻辑", position.entryLogicStatusLabel || position.entryLogicStatus, false);
      appendDefinition(core, "方向支持", position.directionSupportStatusLabel || position.directionSupportStatus, false);
      appendDefinition(core, "反转状态", position.reversalStatusLabel || position.reversalStatus, false);
      appendDefinition(core, "当前建议", position.suggestedManualActionText || position.suggestedManualAction, true);
      card.appendChild(core);

      var more = element("details", "position-details");
      more.appendChild(element("summary", "", "查看完整持仓"));
      var moreList = element("dl", "definition-list");
      appendDefinition(moreList, "持仓 ID", position.positionId, false);
      appendDefinition(moreList, "状态", position.positionStatusLabel || position.positionStatus, false);
      appendDefinition(moreList, "入场价", position.entryPrice, false);
      appendDefinition(moreList, "数量 / 杠杆", text(position.positionSize, "--") + " / " + text(position.leverage, "--"), false);
      appendDefinition(moreList, "止损", position.userStopLoss, false);
      appendDefinition(moreList, "止盈", position.userTakeProfit, false);
      appendDefinition(moreList, "告警", position.warningState, false);
      appendDefinition(moreList, "更新时间", frontendContract.formatBusinessTimeCompact(position.updatedAt), true);
      more.appendChild(moreList);
      card.appendChild(more);
      root.appendChild(card);
    });
  }

  function applyMobileHomePayload(home) {
    var safeHome = home || {};
    var states = safeHome.states || {};
    var selectedSymbol = normalizeSymbol(
      safeHome.selectedSymbol
        || (safeHome.assets && safeHome.assets[0]
          && (safeHome.assets[0].rawSymbol || safeHome.assets[0].symbol))
        || ""
    );
    window.__lastDashboardHome = safeHome;
    renderMobileAssets(safeHome.assets, selectedSymbol, states.assets);
    updateMobileStatusProjection(safeHome.systemState, safeHome.header);
    var selectedCard = syncMobileAssetContext(safeHome, selectedSymbol);
    renderMobileSignalList("alert", safeHome.alerts);
    renderMobileSignalList("event", safeHome.events);
    renderMobilePositions(safeHome.positions, states.positions);
    updateExecution(safeHome.executionSuggestion, matchingAsset(safeHome.assets, selectedSymbol), selectedCard);
    updateAi(safeHome.aiDecision);
    updateSelectedSymbolUrl(selectedSymbol);
    setWatchActionStatus("");
    var homeRoot = document.querySelector("[data-mobile-home-root]");
    if (homeRoot) {
      homeRoot.dataset.homeState = frontendContract.normalizeModuleState(
        states.overall || (safeHome.header && safeHome.header.dataStatus),
        "PARTIAL"
      ).toLowerCase();
      homeRoot.removeAttribute("aria-busy");
    }
  }

  async function loadInitialMobileHome() {
    var homeRoot = document.querySelector("[data-mobile-home-root]");
    var requestedSymbol = frontendContract.readUrlParam("selectedSymbol")
      || (selectedAssetCard() && selectedAssetCard().dataset.symbol)
      || "";
    window.__lastRequestedMobileSymbol = requestedSymbol;
    if (homeRoot) homeRoot.setAttribute("aria-busy", "true");
    updateMobileFocus(null, "LOADING", requestedSymbol);
    try {
      var query = new URLSearchParams({ limit: String(MOBILE_ASSET_LIMIT) });
      if (requestedSymbol) query.set("selectedSymbol", requestedSymbol);
      var response = await fetch("/api/dashboard/home?" + query.toString(), {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" }
      });
      if (!response.ok) throw new Error("HOME_REQUEST_FAILED");
      var envelope = await response.json();
      var parsed = frontendContract.parseApiEnvelope(envelope);
      if (!parsed.ok) throw new Error("HOME_RESPONSE_INVALID");
      applyMobileHomePayload(parsed.data);
      return parsed.data;
    } catch (error) {
      window.__lastDashboardHome = null;
      renderMobileAssets([], requestedSymbol, "ERROR");
      renderMobileSignalList("alert", []);
      renderMobileSignalList("event", []);
      renderMobilePositions([], "ERROR");
      failClosedAfterLoadError(requestedSymbol, "error");
      if (homeRoot) {
        homeRoot.dataset.homeState = "error";
        homeRoot.removeAttribute("aria-busy");
      }
      return null;
    }
  }

  function setMobileRetryVisible(visible) {
    var button = document.querySelector("[data-home-retry]");
    if (button) button.hidden = !visible;
  }

  function assetStateText(asset) {
    var state = String(asset && asset.assetState || "").toUpperCase();
    if (state === "TRIGGERED") return "条件已触发，不代表已开仓";
    return text(asset && (asset.assetStateLabel || asset.assetState), "状态待同步");
  }

  function syncAssetCardProjection(card, asset) {
    if (!card || !asset) return;
    syncCardNavigationIdentity(card, asset);
    card.dataset.assetState = String(asset.assetState || "unknown").toLowerCase();
    card.dataset.moduleState = frontendContract.normalizeModuleState(asset.moduleState, "MISSING").toLowerCase();
    setAssetCardField(card, "state", "资产状态 · " + assetStateText(asset), "资产状态 · 状态待同步");
    setAssetCardField(card, "latestPrice", asset.latestPrice, "--");
    setAssetCardField(card, "compositeScore", asset.compositeScore, "--");
    setAssetCardField(card, "marketBias", asset.marketBiasLabel || asset.marketBias, "当前判断不可用");
    setAssetCardField(card, "confidence", asset.confidenceLabel || asset.confidenceLevel, "--");
    setAssetCardField(card, "risk", asset.riskLabel || asset.riskLevel, "--");
    setAssetCardField(card, "dataQuality", frontendContract.dataQualityLabel(asset.dataQuality), "数据缺失");
    setAssetCardField(card, "multiTimeframeState", asset.multiTimeframeState, "MISSING");
    setAssetCardField(card, "confused", asset.confused === true ? "是" : (asset.confused === false ? "否" : null), "MISSING");
    setAssetCardField(card, "updatedAt", frontendContract.formatBusinessTimeCompact(asset.updatedAt), "--");
    var fieldSources = asset.fieldSourceStatus || {};
    setAssetCardSource(card, "latestPrice", fieldSources.latestPrice);
    setAssetCardSource(card, "score", fieldSources.score);
    setAssetCardSource(card, "confidence", fieldSources.confidence);
  }

  function clearAssetCardProjection(card, stateLabel) {
    if (!card) return;
    syncCardNavigationIdentity(card, null);
    card.dataset.assetState = "unavailable";
    card.dataset.moduleState = "error";
    setAssetCardField(card, "state", "资产状态 · " + text(stateLabel, "当前不可查看"), "资产状态 · 当前不可查看");
    setAssetCardField(card, "latestPrice", null, "--");
    setAssetCardField(card, "compositeScore", null, "--");
    setAssetCardField(card, "marketBias", null, "--");
    setAssetCardField(card, "confidence", null, "--");
    setAssetCardField(card, "risk", null, "--");
    setAssetCardField(card, "dataQuality", null, "MISSING");
    setAssetCardField(card, "multiTimeframeState", null, "MISSING");
    setAssetCardField(card, "confused", null, "MISSING");
    setAssetCardField(card, "updatedAt", null, "--");
    setAssetCardSource(card, "latestPrice", "MISSING");
    setAssetCardSource(card, "score", "MISSING");
    setAssetCardSource(card, "confidence", "MISSING");
  }

  function updateMobileStatusProjection(systemState, header) {
    var state = systemState || {};
    var safeHeader = header || {};
    document.querySelectorAll("[data-mobile-status-field]").forEach(function (node) {
      var field = node.dataset.mobileStatusField;
      if (field === "aiStatus") {
        node.textContent = text(safeHeader.aiStatusLabel, "待同步");
        return;
      }
      var card = state[field] || {};
      node.textContent = text(card.valueLabel != null ? card.valueLabel : card.value, "--");
    });
    var root = document.getElementById("mobile-status");
    if (root) {
      var rawState = String(safeHeader.dataStatus || "PARTIAL").toLowerCase();
      root.dataset.contextState = ["ready", "partial", "empty", "error", "missing"].indexOf(rawState) >= 0
        ? rawState : "partial";
    }
  }

  function clearMobileAssetContext(symbol, contextState, stateLabel) {
    window.__lastDashboardHome = null;
    var normalized = normalizeSymbol(symbol);
    var selectedCard = null;
    assetCards().forEach(function (card) {
      var selected = normalized && normalizeSymbol(card.dataset.symbol) === normalized;
      clearAssetCardProjection(card, selected ? stateLabel : "当前不可查看");
      card.classList.toggle("is-selected", selected);
      card.dataset.selected = String(selected);
      card.setAttribute("aria-checked", String(selected));
      card.tabIndex = selected ? 0 : -1;
      if (selected) selectedCard = card;
    });
    document.querySelectorAll("[data-mobile-status-field]").forEach(function (node) {
      node.textContent = node.dataset.mobileStatusField === "aiStatus" ? stateLabel : "--";
    });
    var statusRoot = document.getElementById("mobile-status");
    if (statusRoot) statusRoot.dataset.contextState = contextState;
    var assetRoot = document.getElementById("watch-assets");
    if (assetRoot) assetRoot.dataset.contextState = contextState;
    updateMobileFocus(null, contextState, symbol);
    setText("[data-selected-asset-token]", selectedCard ? selectedCard.dataset.symbol : symbol, "--");
    updateAssetDetailLink(null);
    updateSelectedSymbolUrl(symbol);
  }

  function syncMobileAssetContext(home, selectedSymbol) {
    var assets = home && Array.isArray(home.assets) ? home.assets : [];
    assetCards().forEach(function (card) {
      var asset = matchingAsset(assets, card.dataset.symbol);
      if (asset) syncAssetCardProjection(card, asset);
      else clearAssetCardProjection(card, "当前不可查看");
    });
    updateMobileStatusProjection(home && home.systemState, home && home.header);
    var statusRoot = document.getElementById("mobile-status");
    var assetRoot = document.getElementById("watch-assets");
    if (assetRoot) {
      assetRoot.dataset.contextState = frontendContract.normalizeModuleState(
        home && home.states && home.states.assets,
        statusRoot ? statusRoot.dataset.contextState : "PARTIAL"
      ).toLowerCase();
    }
    setMobileRetryVisible(false);
    var selectedCard = setSelectedAsset(selectedSymbol);
    updateMobileFocus(matchingAsset(assets, selectedSymbol), assetRoot && assetRoot.dataset.contextState, selectedSymbol);
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
      "expiresAt",
      "sourceExecutionPlanId",
      "riskRewardRatio"
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
    var context = {
      confidence: selectedAsset && (selectedAsset.confidenceLabel || selectedAsset.confidenceLevel)
        || (selectedCard && selectedCard.dataset.confidenceLabel),
      risk: selectedAsset && (selectedAsset.riskLabel || selectedAsset.riskLevel)
        || (selectedCard && selectedCard.dataset.riskLabel)
    };
    Object.keys(context).forEach(function (field) {
      setText('[data-execution-context-field="' + field + '"]', context[field], "--");
    });
    var optionalRiskReward = document.querySelector('[data-execution-optional="riskRewardRatio"]');
    if (optionalRiskReward) {
      optionalRiskReward.hidden = !(access.visible && text(safeSuggestion.riskRewardRatio, "") !== "");
    }
    var disclosure = document.querySelector(".execution-details");
    if (disclosure) disclosure.open = false;
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
    var tabList = element("div", "mobile-ai-role-tabs");
    tabList.setAttribute("role", "tablist");
    tabList.setAttribute("aria-label", "AI 三角色摘要");
    var panels = element("div", "mobile-ai-role-panels");
    var buttons = [];
    orderedTabs.forEach(function (tab, index) {
      var button = element(
        "button",
        "mobile-ai-role-tab",
        tab.role === "GPT_FINAL" ? "GPT Final" : (tab.role === "GEMINI_REVIEW" ? "Gemini Review" : "Grok Challenge")
      );
      button.type = "button";
      button.id = "mobile-home-ai-tab-" + tab.role;
      button.dataset.homeAiTab = tab.role;
      button.setAttribute("role", "tab");
      button.setAttribute("aria-selected", String(index === 0));
      button.setAttribute("aria-controls", "mobile-home-ai-panel-" + tab.role);
      button.tabIndex = index === 0 ? 0 : -1;
      tabList.appendChild(button);
      buttons.push(button);

      var panel = createRoleSummaryCard(tab);
      panel.id = "mobile-home-ai-panel-" + tab.role;
      panel.setAttribute("role", "tabpanel");
      panel.setAttribute("aria-labelledby", button.id);
      panel.tabIndex = 0;
      panel.hidden = index !== 0;
      panels.appendChild(panel);
    });
    root.appendChild(tabList);
    root.appendChild(panels);

    function activate(role, focus) {
      buttons.forEach(function (button) {
        var selected = button.dataset.homeAiTab === role;
        button.setAttribute("aria-selected", String(selected));
        button.tabIndex = selected ? 0 : -1;
        if (selected && focus) button.focus();
      });
      panels.querySelectorAll("[data-ai-role-summary]").forEach(function (panel) {
        panel.hidden = panel.dataset.aiRoleSummary !== role;
      });
    }
    buttons.forEach(function (button, index) {
      button.addEventListener("click", function () {
        activate(button.dataset.homeAiTab, false);
      });
      button.addEventListener("keydown", function (event) {
        if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
        event.preventDefault();
        var offset = event.key === "ArrowRight" ? 1 : -1;
        var next = buttons[(index + offset + buttons.length) % buttons.length];
        activate(next.dataset.homeAiTab, true);
      });
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
    var selectedSymbol = card ? String(card.dataset.symbol || "").trim() : "";
    var symbol = String(safeAi.symbol || selectedSymbol).trim();
    var analysisId = String(safeAi.analysisId || "").trim();
    var decisionId = String(safeAi.decisionId || "").trim();
    var identityReady = !!analysisId && !!decisionId && !!symbol && symbol === selectedSymbol;
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
    setText("[data-ai-analysis-symbol]", symbol, "--");
    setText("[data-ai-analysis-id]", analysisId, "待同步");
    setText("[data-ai-decision-id]", decisionId, "待同步");
    var detailLink = document.querySelector("[data-ai-analysis-detail-link]");
    if (detailLink) {
      if (identityReady) {
        detailLink.href = "/dashboard/analysis-detail?analysisId="
          + encodeURIComponent(analysisId)
          + "&selectedSymbol=" + encodeURIComponent(symbol);
        detailLink.removeAttribute("aria-disabled");
      } else {
        detailLink.removeAttribute("href");
        detailLink.setAttribute("aria-disabled", "true");
      }
    }
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

  function failClosedAfterLoadError(symbol, contextState) {
    var missing = contextState === "missing";
    clearMobileAssetContext(symbol, missing ? "missing" : "error", missing ? "当前不可查看" : "同步失败");
    setWatchActionStatus(missing
      ? "当前资产不存在或不可访问，旧资产数据已清除。"
      : "当前资产同步失败，旧资产数据已清除。");
    updateExecution({
      status: missing ? "MISSING" : "LOAD_FAILED",
      statusLabel: missing ? "当前不可查看" : "数据加载失败",
      blockedReason: missing ? "当前资产不存在或不可访问。" : "无法同步当前资产，请稍后重试。"
    }, null, null);
    updateAi({
      runStatus: missing ? "MISSING" : "LOAD_FAILED",
      runStatusLabel: missing ? "当前不可查看" : "同步失败",
      consistency: {
        aiApplicable: false,
        consistencySummary: missing
          ? "当前资产不存在或不可访问，无法生成一致性摘要"
          : "当前资产同步失败，无法生成一致性摘要"
      },
      tabs: []
    });
    setMobileRetryVisible(true);
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
    var confidenceLabel = asset && (asset.confidenceLabel || asset.confidenceLevel);
    var riskLabel = asset && (asset.riskLabel || asset.riskLevel);
    if (frontendContract.hasText(confidenceLabel)) card.dataset.confidenceLabel = String(confidenceLabel);
    else delete card.dataset.confidenceLabel;
    if (frontendContract.hasText(riskLabel)) card.dataset.riskLabel = String(riskLabel);
    else delete card.dataset.riskLabel;
    if (asset && frontendContract.hasText(asset.dataQuality)) card.dataset.qualityLabel = String(asset.dataQuality);
    else delete card.dataset.qualityLabel;
  }

  async function selectAsset(symbol, sourceCard) {
    var launchAnalysis = arguments.length < 3 || arguments[2] !== false;
    if (!symbol) return;
    window.__lastRequestedMobileSymbol = symbol;
    requestSequence += 1;
    var sequence = requestSequence;
    if (activeRequest) activeRequest.abort();
    var request = new AbortController();
    activeRequest = request;
    if (sourceCard) {
      sourceCard.dataset.requestSequence = String(sequence);
      sourceCard.setAttribute("aria-busy", "true");
    }
    setMobileRetryVisible(false);
    clearMobileAssetContext(symbol, "loading", "正在同步");
    setWatchActionStatus("正在同步当前资产上下文。");
    updateExecution({
      status: "LOADING",
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
      if (!selectedAsset) {
        failClosedAfterLoadError(selectedSymbol, "missing");
        return;
      }
      var selectedCard = syncMobileAssetContext(parsed.data, selectedSymbol);
      syncCardNavigationIdentity(selectedCard, selectedAsset);
      window.__lastDashboardHome = parsed.data;
      updateSelectedSymbolUrl(selectedSymbol);
      setWatchActionStatus("");
      updateExecution(parsed.data.executionSuggestion, selectedAsset, selectedCard);
      updateAi(parsed.data.aiDecision);
      if (launchAnalysis === true) {
        await openOrResumeMobileAssetAnalysis(selectedCard || sourceCard);
        await selectAsset(selectedSymbol, selectedCard || sourceCard, false);
        setWatchActionStatus(selectedSymbol + " 三 AI 分析已在首页更新");
      }
    } catch (error) {
      if (error.name !== "AbortError" && sequence === requestSequence) {
        failClosedAfterLoadError(symbol, "error");
      }
    } finally {
      if (sourceCard && sourceCard.dataset.requestSequence === String(sequence)) {
        sourceCard.removeAttribute("aria-busy");
        delete sourceCard.dataset.requestSequence;
      }
      if (activeRequest === request) activeRequest = null;
    }
  }

  function bindAssetPager() {
    var pager = document.querySelector("[data-mobile-assets-pager]");
    if (!pager) return;
    pager.addEventListener("click", function (event) {
      var card = event.target.closest(".asset-select");
      if (card && pager.contains(card)) selectAsset(card.dataset.symbol, card, true);
    });
    pager.addEventListener("keydown", function (event) {
      if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
      var cards = assetCards();
      var current = event.target.closest(".asset-select");
      var index = cards.indexOf(current);
      if (index < 0 || !cards.length) return;
      event.preventDefault();
      var offset = event.key === "ArrowRight" ? 1 : -1;
      var next = cards[(index + offset + cards.length) % cards.length];
      next.focus({ preventScroll: true });
      keepAssetCardVisible(next, "smooth");
      selectAsset(next.dataset.symbol, next, true);
    });
  }

  function bindHomeRetry() {
    var button = document.querySelector("[data-home-retry]");
    if (!button) return;
    button.addEventListener("click", function () {
      var selected = selectedAssetCard();
      button.disabled = true;
      var retry = selected && selected.dataset.symbol
        ? selectAsset(selected.dataset.symbol, selected, false)
        : loadInitialMobileHome();
      Promise.resolve(retry).finally(function () {
        button.disabled = false;
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

    function renderSearchResults(query) {
      if (!results) return;
      var cards = assetCards();
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
          selectAsset(symbol, card, true);
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
    bindHomeRetry();
    bindWatchTools();
    var initialAsset = selectedAssetCard();
    keepAssetCardVisible(initialAsset, "auto");
    if (initialAsset && initialAsset.dataset.symbol) {
      updateMobileFocus(serverRenderedAsset(initialAsset), initialAsset.dataset.moduleState, initialAsset.dataset.symbol);
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
    var homeRoot = document.querySelector("[data-mobile-home-root]");
    if (homeRoot && homeRoot.hasAttribute("data-client-home-bootstrap")) {
      loadInitialMobileHome();
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize, { once: true });
  } else {
    initialize();
  }
})();
