(function (global) {
  "use strict";

  var AI_ROLES = Object.freeze([
    Object.freeze({ role: "GPT_FINAL", label: "最终裁决官" }),
    Object.freeze({ role: "GEMINI_REVIEW", label: "冲突复核官" }),
    Object.freeze({ role: "GROK_CHALLENGE", label: "反方挑战官" })
  ]);

  var ASSET_STATES = Object.freeze({
    OBSERVING: Object.freeze({ label: "观察中", tone: "neutral" }),
    CANDIDATE: Object.freeze({ label: "待复核候选", tone: "info" }),
    WAITING_TRIGGER: Object.freeze({ label: "等待触发", tone: "warning" }),
    TRIGGERED: Object.freeze({ label: "条件已触发，不代表已开仓", tone: "info" }),
    HIGH_RISK: Object.freeze({ label: "高风险", tone: "danger" }),
    INVALIDATED: Object.freeze({ label: "已失效", tone: "muted" }),
    COOLING: Object.freeze({ label: "冷却中", tone: "muted" }),
    CONFUSED: Object.freeze({ label: "冲突阻断", tone: "danger" })
  });

  var POSITION_SELECTION_STATUSES = Object.freeze([
    "POSITION_SELECTION_REQUIRED",
    "POSITION_NOT_FOUND",
    "POSITION_SYMBOL_MISMATCH"
  ]);

  function hasText(value) {
    return value !== null
      && value !== undefined
      && String(value).trim() !== "";
  }

  function displayText(value, fallback) {
    return hasText(value) ? String(value) : (fallback || "--");
  }

  function displayNumber(value) {
    return value === null || value === undefined || value === ""
      ? "--"
      : String(value);
  }

  function parseApiEnvelope(envelope) {
    var valid = envelope
      && typeof envelope === "object"
      && Number(envelope.code) === 200
      && envelope.data !== null
      && envelope.data !== undefined;
    return valid
      ? { ok: true, data: envelope.data, message: "" }
      : {
          ok: false,
          data: null,
          message: displayText(envelope && envelope.msg, "数据暂不可用")
        };
  }

  function assetStateView(rawState, returnedLabel) {
    var code = String(rawState || "").trim().toUpperCase();
    var known = ASSET_STATES[code];
    if (known) {
      return { code: code.toLowerCase(), label: known.label, tone: known.tone };
    }
    return {
      code: code ? code.toLowerCase() : "unknown",
      label: displayText(returnedLabel, "状态待同步"),
      tone: "neutral"
    };
  }

  function normalizeAiTabs(tabs) {
    var byRole = {};
    (Array.isArray(tabs) ? tabs : []).forEach(function (tab) {
      var role = String(tab && tab.role || "").trim().toUpperCase();
      if (!role || byRole[role] || !AI_ROLES.some(function (item) {
        return item.role === role;
      })) {
        return;
      }
      byRole[role] = tab;
    });

    return AI_ROLES.map(function (definition) {
      var supplied = byRole[definition.role];
      var normalized = {};
      if (supplied && typeof supplied === "object") {
        Object.keys(supplied).forEach(function (key) {
          normalized[key] = supplied[key];
        });
      }
      normalized.role = definition.role;
      normalized.roleLabel = displayText(normalized.roleLabel, definition.label);
      if (!supplied) {
        normalized.runStatus = "NOT_AVAILABLE";
        normalized.runStatusLabel = "待同步";
        normalized.resultAvailable = false;
        normalized.statusMessage = "当前角色观点待同步";
      }
      return normalized;
    });
  }

  function executionPlanAccess(suggestion) {
    var plan = suggestion && typeof suggestion === "object" ? suggestion : {};
    var status = String(plan.status || "").trim().toUpperCase();
    var selectionBlocked = POSITION_SELECTION_STATUSES.indexOf(status) >= 0;
    if (selectionBlocked) {
      return {
        visible: false,
        statusLabel: displayText(plan.statusLabel, "请选择具体持仓"),
        reason: displayText(plan.blockedReason, "请选择具体持仓")
      };
    }

    if (!hasText(plan.sourceExecutionPlanId)) {
      return {
        visible: false,
        statusLabel: displayText(plan.statusLabel, "当前暂无可验证的执行建议"),
        reason: displayText(plan.blockedReason, "计划来源不可验证")
      };
    }

    if (plan.positionMode === true) {
      var verified = String(plan.originalPlanIdentity || "").toUpperCase() === "VERIFIED";
      var active = String(plan.originalPlanCurrentValidity || "").toUpperCase() === "ACTIVE";
      if (!verified || !active) {
        return {
          visible: false,
          statusLabel: "仅用于历史复核",
          reason: displayText(plan.originalPlanLabel, "计划当前有效性不可验证")
        };
      }
      return {
        visible: true,
        statusLabel: displayText(
          plan.originalPlanLabel,
          "原执行计划，仅用于持仓复核和复盘对照"
        ),
        reason: "系统建议，仅供人工复核"
      };
    }

    if (status !== "USABLE_REVIEW_PLAN") {
      return {
        visible: false,
        statusLabel: displayText(plan.statusLabel, "当前暂无可验证的执行建议"),
        reason: displayText(plan.blockedReason, "执行建议不可用")
      };
    }

    return {
      visible: true,
      statusLabel: displayText(plan.statusLabel, "执行建议，仅供人工复核"),
      reason: "系统建议，仅供人工复核"
    };
  }

  function csrfHeaders(headers, root) {
    var result = {};
    Object.keys(headers || {}).forEach(function (key) {
      result[key] = headers[key];
    });
    var documentRoot = root || global.document;
    if (!documentRoot || !documentRoot.querySelector) return result;
    var tokenMeta = documentRoot.querySelector('meta[name="_csrf"]');
    var headerMeta = documentRoot.querySelector('meta[name="_csrf_header"]');
    var token = tokenMeta && tokenMeta.getAttribute("content");
    var header = headerMeta && headerMeta.getAttribute("content");
    if (hasText(token) && hasText(header)) result[header] = token;
    return result;
  }

  function clearTextFields(selector, fallback, root) {
    var documentRoot = root || global.document;
    if (!documentRoot || !documentRoot.querySelectorAll) return;
    documentRoot.querySelectorAll(selector).forEach(function (node) {
      node.textContent = fallback || "--";
    });
  }

  function readUrlParam(name) {
    try {
      return new URL(global.location.href).searchParams.get(name);
    } catch (error) {
      return null;
    }
  }

  function replaceUrlParam(name, value) {
    try {
      var url = new URL(global.location.href);
      if (hasText(value)) url.searchParams.set(name, String(value));
      else url.searchParams.delete(name);
      global.history.replaceState(
        global.history.state,
        "",
        url.pathname + url.search + url.hash
      );
      return true;
    } catch (error) {
      return false;
    }
  }

  function formatUtcNaive(value) {
    var raw = String(value || "").trim();
    var match = raw.match(
      /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}(?::\d{2})?)(?:\.\d+)?$/
    );
    return match ? match[1] + " " + match[2] + " UTC" : displayText(value, "--");
  }

  global.TradeModelFrontendContract = Object.freeze({
    AI_ROLES: AI_ROLES,
    ASSET_STATES: ASSET_STATES,
    hasText: hasText,
    displayText: displayText,
    displayNumber: displayNumber,
    parseApiEnvelope: parseApiEnvelope,
    assetStateView: assetStateView,
    normalizeAiTabs: normalizeAiTabs,
    executionPlanAccess: executionPlanAccess,
    csrfHeaders: csrfHeaders,
    clearTextFields: clearTextFields,
    readUrlParam: readUrlParam,
    replaceUrlParam: replaceUrlParam,
    formatUtcNaive: formatUtcNaive
  });
})(window);
