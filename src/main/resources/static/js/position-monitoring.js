(function () {
  "use strict";

  var contract = window.TradeModelFrontendContract;
  var root = document.querySelector("[data-position-monitor-root]");
  if (!contract || !root) return;

  var LIFECYCLES = ["OPEN", "PARTIALLY_CLOSED", "CLOSED"];
  var MONITOR_STATUSES = [
    "LOGIC_VALID",
    "LOGIC_WEAKENED",
    "PLAN_INVALIDATED",
    "HIGH_RISK"
  ];
  var requestGeneration = 0;
  var activeRequestController = null;
  var currentHome = null;

  function hasText(value) {
    return contract.hasText(value);
  }

  function displayText(value, fallback) {
    return contract.displayText(value, fallback);
  }

  function normalizePositionId(value) {
    var normalized = String(value || "").trim();
    if (!/^[1-9][0-9]{0,18}$/.test(normalized)) return "";
    if (normalized.length === 19 && normalized > "9223372036854775807") return "";
    return normalized;
  }

  function normalizeSymbol(value) {
    return String(value || "")
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "");
  }

  function requestedPositionId() {
    return normalizePositionId(
      root.dataset.requestedPositionId || contract.readUrlParam("positionId")
    );
  }

  function samePositionId(value, expected) {
    return normalizePositionId(value) === expected;
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

  function beginRequest() {
    requestGeneration += 1;
    if (activeRequestController) activeRequestController.abort();
    activeRequestController = typeof AbortController === "function"
      ? new AbortController()
      : null;
    return {
      generation: requestGeneration,
      positionId: requestedPositionId(),
      controller: activeRequestController,
      pageRoot: root
    };
  }

  function isCurrentRequest(request) {
    return request
      && request.generation === requestGeneration
      && request.positionId === requestedPositionId()
      && request.controller === activeRequestController
      && request.pageRoot === root
      && root.isConnected
      && document.querySelector("[data-position-monitor-root]") === root;
  }

  function isAbortError(error) {
    return error && (error.name === "AbortError" || error.code === 20);
  }

  function requestOptions(request) {
    var options = { method: "GET", credentials: "same-origin" };
    if (request.controller) options.signal = request.controller.signal;
    return options;
  }

  function fetchEnvelope(url, request) {
    return fetch(url, requestOptions(request)).then(function (response) {
      if (!response.ok) {
        var error = new Error("REQUEST_FAILED");
        error.status = response.status;
        throw error;
      }
      return response.json();
    }).then(function (envelope) {
      var parsed = contract.parseApiEnvelope(envelope);
      if (!parsed.ok) throw new Error(parsed.message);
      return parsed.data;
    });
  }

  function showPageState(code, title, message, retryable) {
    var state = document.querySelector("[data-page-state]");
    if (!state) return;
    root.dataset.pageState = code;
    setText("[data-page-state-title]", title, "数据暂不可用", state);
    setText("[data-page-state-message]", message, "当前持仓无法读取。", state);
    var retry = state.querySelector("[data-position-retry]");
    if (retry) retry.hidden = retryable !== true;
    state.hidden = false;
    hideContent();
  }

  function hidePageState() {
    var state = document.querySelector("[data-page-state]");
    if (state) state.hidden = true;
  }

  function showContent() {
    var content = document.querySelector("[data-position-content]");
    if (content) content.hidden = false;
  }

  function hideContent() {
    var content = document.querySelector("[data-position-content]");
    if (content) content.hidden = true;
  }

  function hideSelectedPosition() {
    var selected = document.querySelector("[data-selected-position]");
    if (selected) selected.hidden = true;
  }

  function showSelectionState(title, message) {
    var state = document.querySelector("[data-selection-state]");
    if (!state) return;
    setText("strong", title, "请选择具体持仓", state);
    setText("span", message, "选择后查看该持仓的只读监控结果。", state);
    state.hidden = false;
    hideSelectedPosition();
  }

  function hideSelectionState() {
    var state = document.querySelector("[data-selection-state]");
    if (state) state.hidden = true;
  }

  function createElement(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function safeLifecycle(value) {
    var normalized = String(value || "").trim().toUpperCase();
    return LIFECYCLES.indexOf(normalized) >= 0 ? normalized : "";
  }

  function safeMonitorStatus(value, lastMonitorAt) {
    var normalized = String(value || "").trim().toUpperCase();
    if (!hasText(lastMonitorAt) && (!normalized || normalized === "WAITING_MONITOR")) {
      return "WAITING_MONITOR";
    }
    return MONITOR_STATUSES.indexOf(normalized) >= 0 ? normalized : "";
  }

  function validPositionRows(home) {
    return (home && Array.isArray(home.positions) ? home.positions : []).filter(function (position) {
      return position
        && typeof position === "object"
        && normalizePositionId(position.positionId)
        && safeLifecycle(position.positionStatus);
    });
  }

  function positionHref(positionId) {
    var mobile = root.dataset.mobileView === "true";
    return (mobile ? "/dashboard/mobile/positions" : "/dashboard/positions")
      + "?positionId=" + encodeURIComponent(positionId);
  }

  function renderPositionList(positions, selectedId) {
    var list = document.querySelector("[data-position-list]");
    var empty = document.querySelector("[data-position-list-empty]");
    if (!list) return;
    list.replaceChildren();
    setText("[data-position-count]", positions.length + " 个持仓", "0 个持仓");

    if (!positions.length) {
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;

    positions.forEach(function (position) {
      var id = normalizePositionId(position.positionId);
      var link = createElement("a", "position-picker-item");
      link.href = positionHref(id);
      link.dataset.positionId = id;
      link.setAttribute("role", "listitem");
      if (id === selectedId) {
        link.classList.add("active");
        link.setAttribute("aria-current", "true");
      }
      var identity = createElement("span", "picker-identity");
      identity.appendChild(createElement(
        "strong",
        "",
        displayText(position.symbol, "--")
      ));
      identity.appendChild(createElement(
        "small",
        "",
        "positionId · " + id
      ));
      var state = createElement(
        "span",
        "picker-state",
        displayText(position.positionStatus, "--")
      );
      link.appendChild(identity);
      link.appendChild(state);
      list.appendChild(link);
    });
  }

  function renderLifecycle(lifecycle) {
    document.querySelectorAll("[data-lifecycle]").forEach(function (node) {
      node.classList.toggle("active", node.dataset.lifecycle === lifecycle);
    });
    setText('[data-position-field="lifecycle"]', lifecycle, "--");
  }

  function renderPosition(position, positionDetail, selectedId) {
    var selected = document.querySelector("[data-selected-position]");
    if (!selected) return;
    setText("[data-selected-position-id]", "positionId · " + selectedId, "positionId · --");
    setText('[data-position-field="symbol"]', positionDetail.assetSymbol, "--");
    setText(
      '[data-position-field="direction"]',
      positionDetail.side,
      "待同步"
    );
    setText('[data-position-field="entryPrice"]', positionDetail.entryPrice, "--");
    setText(
      '[data-position-field="openedAt"]',
      hasText(positionDetail.openedAt) ? contract.formatUtcNaive(positionDetail.openedAt) : null,
      "--"
    );
    setText('[data-position-field="positionSize"]', positionDetail.quantity, "--");
    setText('[data-position-field="leverage"]', positionDetail.leverage, "--");
    setText('[data-position-field="userStopLoss"]', positionDetail.stopLoss, "--");
    setText('[data-position-field="userTakeProfit"]', positionDetail.takeProfit, "--");
    renderLifecycle(safeLifecycle(positionDetail.status));
    renderMonitor(position, false);
    selected.hidden = false;
  }

  function validatePositionDetail(positionDetail, selectedSummary, selectedId) {
    return positionDetail
      && typeof positionDetail === "object"
      && samePositionId(positionDetail.id, selectedId)
      && safeLifecycle(positionDetail.status)
      && normalizeSymbol(positionDetail.assetSymbol)
        === normalizeSymbol(selectedSummary && selectedSummary.symbol);
  }

  function mirrorMonitorField(name, value, fallback) {
    setText('[data-monitor-field="' + name + '"]', value, fallback);
    setText('[data-monitor-mirror="' + name + '"]', value, fallback);
  }

  function renderMonitorUnavailable() {
    mirrorMonitorField("monitorStatus", "当前不可查看", "当前不可查看");
    setText('[data-monitor-field="logic"]', "当前不可查看", "当前不可查看");
    mirrorMonitorField("directionSupport", "当前不可查看", "当前不可查看");
    mirrorMonitorField("reversal", "当前不可查看", "当前不可查看");
    mirrorMonitorField("risk", null, "--");
    setText('[data-monitor-field="conclusion"]', "当前不可查看", "当前不可查看");
    mirrorMonitorField("suggestion", "当前不可查看", "当前不可查看");
    setText('[data-monitor-field="lastMonitorAt"]', null, "--");
    setText("[data-monitor-source]", "数据不可用", "数据不可用");
    var card = document.querySelector("[data-position-monitor-card]");
    if (card) card.dataset.monitorStatus = "MONITOR_DATA_UNAVAILABLE";
  }

  function monitorSummaryClaimsWaiting(position) {
    return safeMonitorStatus(
      position && position.entryLogicStatus,
      position && position.lastMonitorAt
    ) === "WAITING_MONITOR";
  }

  function renderMonitor(position, waitingConfirmed) {
    var status = safeMonitorStatus(position.entryLogicStatus, position.lastMonitorAt);
    var waiting = status === "WAITING_MONITOR";
    if (waiting && waitingConfirmed !== true) {
      renderMonitorUnavailable();
      return;
    }
    mirrorMonitorField("monitorStatus", status, waiting ? "WAITING_MONITOR" : "--");
    setText(
      '[data-monitor-field="logic"]',
      position.entryLogicStatusLabel,
      waiting ? "等待首次监控" : "待同步"
    );
    mirrorMonitorField(
      "directionSupport",
      position.directionSupportStatusLabel,
      waiting ? "等待首次监控" : "待同步"
    );
    mirrorMonitorField(
      "reversal",
      position.reversalStatusLabel,
      waiting ? "等待首次监控" : "待同步"
    );
    mirrorMonitorField("risk", position.riskLevelLabel, waiting ? "--" : "待同步");
    setText(
      '[data-monitor-field="conclusion"]',
      position.monitorConclusion,
      waiting ? "等待首次监控" : "当前不可查看"
    );
    mirrorMonitorField(
      "suggestion",
      position.suggestedManualActionText,
      waiting ? "等待首次监控" : "当前不可查看"
    );
    setText(
      '[data-monitor-field="lastMonitorAt"]',
      hasText(position.lastMonitorAt)
        ? contract.formatUtcNaive(position.lastMonitorAt)
        : null,
      "--"
    );
    setText(
      "[data-monitor-source]",
      waiting ? "等待首次监控" : "只读监控",
      "只读监控"
    );
    var card = document.querySelector("[data-position-monitor-card]");
    if (card) card.dataset.monitorStatus = status || "MISSING";
  }

  function reconcileMonitorSummary(position, logs) {
    if (!monitorSummaryClaimsWaiting(position)) return true;
    if (!logs.length) {
      renderMonitor(position, true);
      return true;
    }
    renderMonitorUnavailable();
    return false;
  }

  function validMonitorLogs(data, selectedId) {
    return (Array.isArray(data) ? data : []).filter(function (log) {
      return log
        && typeof log === "object"
        && samePositionId(log.positionId, selectedId);
    });
  }

  function resetMonitorLogState() {
    var list = document.querySelector("[data-monitor-log-list]");
    if (list) list.replaceChildren();
    var error = document.querySelector("[data-monitor-log-error]");
    if (error) error.hidden = true;
    setText("[data-monitor-log-count]", "0 条", "0 条");
  }

  function renderMonitorLogs(logs) {
    resetMonitorLogState();
    var list = document.querySelector("[data-monitor-log-list]");
    var empty = document.querySelector("[data-monitor-log-empty]");
    if (!list) return;
    setText("[data-monitor-log-count]", logs.length + " 条", "0 条");

    if (!logs.length) {
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;

    logs.forEach(function (log) {
      var item = createElement("article", "monitor-log-item");
      var heading = createElement("div", "log-heading");
      heading.appendChild(createElement(
        "time",
        "",
        hasText(log.createdAt) ? contract.formatUtcNaive(log.createdAt) : "--"
      ));
      heading.appendChild(createElement(
        "span",
        "",
        displayText(log.sourceStatusLabel, "来源待验证")
      ));
      item.appendChild(heading);
      item.appendChild(createElement(
        "strong",
        "",
        displayText(log.logicStatus, "状态待同步")
      ));
      item.appendChild(createElement(
        "p",
        "",
        displayText(log.reason, "监控原因待同步")
      ));
      item.appendChild(createElement(
        "small",
        "",
        "风险 " + displayText(log.riskLevel, "--")
          + " · 建议 " + displayText(log.suggestedAction, "当前不可查看")
      ));
      list.appendChild(item);
    });
  }

  function showMonitorLogError() {
    resetMonitorLogState();
    var empty = document.querySelector("[data-monitor-log-empty]");
    if (empty) empty.hidden = true;
    var error = document.querySelector("[data-monitor-log-error]");
    if (error) error.hidden = false;
    setPageStatus("部分数据可用", "partial");
  }

  function loadMonitorLogs(request, selectedId, selectedSummary) {
    var url = "/api/review/positions/" + encodeURIComponent(selectedId)
      + "/monitor-logs?limit=20";
    return fetchEnvelope(url, request).then(function (data) {
      if (!isCurrentRequest(request)) return;
      var returned = Array.isArray(data) ? data : [];
      var logs = validMonitorLogs(returned, selectedId);
      if (logs.length !== returned.length) {
        if (monitorSummaryClaimsWaiting(selectedSummary)) renderMonitorUnavailable();
        showMonitorLogError();
        return;
      }
      renderMonitorLogs(logs);
      if (reconcileMonitorSummary(selectedSummary, logs)) {
        setPageStatus("持仓已同步", "ready");
      } else {
        setPageStatus("部分数据可用", "partial");
      }
    }).catch(function (error) {
      if (!isCurrentRequest(request) || isAbortError(error)) return;
      if (monitorSummaryClaimsWaiting(selectedSummary)) renderMonitorUnavailable();
      showMonitorLogError();
    });
  }

  function loadExactPosition(request, selectedSummary, selectedId) {
    var url = "/api/user-positions/" + encodeURIComponent(selectedId);
    return fetchEnvelope(url, request).then(function (positionDetail) {
      if (!isCurrentRequest(request)) return null;
      if (!validatePositionDetail(positionDetail, selectedSummary, selectedId)) {
        var error = new Error("POSITION_IDENTITY_MISMATCH");
        error.code = "POSITION_IDENTITY_MISMATCH";
        throw error;
      }
      renderPosition(selectedSummary, positionDetail, selectedId);
      resetMonitorLogState();
      setPageStatus("持仓已同步", "ready");
      return loadMonitorLogs(request, selectedId, selectedSummary);
    });
  }

  function renderHome(home, request) {
    currentHome = home;
    var positions = validPositionRows(home);
    var selectedId = request.positionId;
    renderPositionList(positions, selectedId);
    showContent();
    hidePageState();

    if (!positions.length) {
      showSelectionState("暂无手动持仓", "当前没有可监控的 OPEN 或 PARTIALLY_CLOSED 持仓。");
      setPageStatus("暂无持仓", "empty");
      return Promise.resolve();
    }

    if (!selectedId) {
      showSelectionState("请选择具体持仓", "选择后查看该持仓的只读监控结果。");
      setPageStatus("等待选择", "ready");
      return Promise.resolve();
    }

    var selected = positions.find(function (position) {
      return samePositionId(position.positionId, selectedId);
    });
    if (!selected) {
      showSelectionState("当前不可查看", "该 positionId 不在当前用户可读持仓中。");
      setPageStatus("当前不可查看", "error");
      return Promise.resolve();
    }

    hideSelectionState();
    return loadExactPosition(request, selected, selectedId);
  }

  function loadPositionMonitoring() {
    var request = beginRequest();
    root.setAttribute("aria-busy", "true");
    hideSelectedPosition();
    hideContent();
    showPageState("LOADING", "正在读取持仓", "请稍候。", false);
    setPageStatus("正在同步", "loading");

    if (root.dataset.invalidPositionId === "true") {
      showPageState(
        "INVALID_POSITION_ID",
        "当前不可查看",
        "positionId 无效，未发起持仓读取。",
        false
      );
      setPageStatus("当前不可查看", "error");
      root.setAttribute("aria-busy", "false");
      return;
    }

    var homeUrl = "/api/dashboard/home?limit=20";
    if (request.positionId) {
      homeUrl += "&positionId=" + encodeURIComponent(request.positionId);
    }
    fetchEnvelope(homeUrl, request)
      .then(function (home) {
        if (!isCurrentRequest(request)) return;
        return renderHome(home, request);
      })
      .catch(function (error) {
        if (!isCurrentRequest(request) || isAbortError(error)) return;
        currentHome = null;
        showPageState(
          "LOAD_FAILED",
          "持仓读取失败",
          "当前不可查看，可重试读取同一 positionId。",
          true
        );
        setPageStatus("读取失败", "error");
      })
      .finally(function () {
        if (isCurrentRequest(request)) root.setAttribute("aria-busy", "false");
      });
  }

  function retryMonitorLogs() {
    var selectedId = requestedPositionId();
    if (!selectedId || !currentHome) {
      loadPositionMonitoring();
      return;
    }
    var selectedSummary = validPositionRows(currentHome).find(function (position) {
      return samePositionId(position.positionId, selectedId);
    });
    if (!selectedSummary) {
      loadPositionMonitoring();
      return;
    }
    var request = beginRequest();
    root.setAttribute("aria-busy", "true");
    setPageStatus("正在同步监控记录", "loading");
    loadMonitorLogs(request, selectedId, selectedSummary).finally(function () {
      if (isCurrentRequest(request)) root.setAttribute("aria-busy", "false");
    });
  }

  var retry = document.querySelector("[data-position-retry]");
  if (retry) retry.addEventListener("click", loadPositionMonitoring);
  var logRetry = document.querySelector("[data-monitor-log-retry]");
  if (logRetry) logRetry.addEventListener("click", retryMonitorLogs);

  loadPositionMonitoring();
})();
