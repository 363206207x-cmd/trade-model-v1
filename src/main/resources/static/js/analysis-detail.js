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
    "证据可信度分"
  ];
  var activeRole = "GPT_FINAL";
  var requestGeneration = 0;
  var activeRequestController = null;

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

  function beginRequest(analysisId) {
    requestGeneration += 1;
    if (activeRequestController) {
      activeRequestController.abort();
    }
    activeRequestController = typeof AbortController === "function"
      ? new AbortController()
      : null;
    return {
      analysisId: analysisId,
      generation: requestGeneration,
      controller: activeRequestController,
      pageRoot: root
    };
  }

  function isCurrentRequest(request) {
    return request
      && request.generation === requestGeneration
      && request.analysisId === currentAnalysisId()
      && request.pageRoot === root
      && root.isConnected
      && document.querySelector("[data-analysis-detail-root]") === root
      && request.controller === activeRequestController;
  }

  function isAbortError(error) {
    return error && (error.name === "AbortError" || error.code === 20);
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

  function aggregateConfidenceScore(items) {
    var row = (Array.isArray(items) ? items : []).find(function (item) {
      return item && String(item.scoreType || "").trim() === "证据可信度分"
        && item.scoreValue !== null && item.scoreValue !== undefined;
    });
    return row ? row.scoreValue : null;
  }

  function renderContext(run, decision, scoreItems) {
    var aggregateScore = aggregateConfidenceScore(scoreItems);
    setText('[data-analysis-field="symbol"]', run.symbol, "--");
    setText(
      '[data-analysis-field="direction"]',
      decision && decision.marketBiasHierarchy,
      "待同步"
    );
    setText(
      '[data-analysis-field="scoreConfidence"]',
      contract.displayNumber(aggregateScore) + " / "
        + displayText(decision && decision.confidenceLevel, "待同步"),
      "-- / 待同步"
    );
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
    });
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
      appendDefinition(unavailableFields, "证据强度", "当前不可查看");
      appendDefinition(unavailableFields, "置信度", "当前不可查看");
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
      heading.appendChild(createElement("span", "", displayText(item.freshness, "状态未提供")));
      card.appendChild(heading);
      var fields = createElement("dl");
      appendDefinition(fields, "Evidence ID", displayText(item.evidenceId, "当前不可查看"));
      appendDefinition(fields, "证据类型", displayText(item.evidenceType, "未提供"));
      appendDefinition(fields, "方向", displayText(item.direction, "未提供"));
      appendDefinition(fields, "当前值 / 变化", [item.currentValue, item.changeFromBaseline]
        .filter(hasText).join(" · ") || "当前不可查看");
      appendDefinition(fields, "证据强度", contract.displayNumber(item.strength));
      appendDefinition(fields, "置信度", contract.displayNumber(item.confidence));
      appendDefinition(fields, "来源", displayText(item.source, "来源待同步"));
      appendDefinition(fields, "Provider", displayText(item.sourceProvider, "当前不可查看"));
      appendDefinition(fields, "观测时间", displayText(item.observedAt, "当前不可查看"));
      appendDefinition(fields, "Freshness", displayText(item.freshness, "当前不可查看"));
      card.appendChild(fields);
      card.appendChild(createElement(
        "p",
        "",
        displayText(item.description, "证据描述待同步。")
      ));
      card.appendChild(createElement(
        "small",
        "",
        displayText(item.sourceReference, "当前 analysisId 的持久化证据；未提供来源引用。")
      ));
      list.appendChild(card);
    });
    setCoverage("[data-evidence-coverage]", rows.length + " 条证据", "available");
    return true;
  }

  function scoreRows(items) {
    return (Array.isArray(items) ? items : []).filter(function (item) {
      return item && SCORE_TYPES.indexOf(String(item.scoreType || "").trim()) >= 0;
    });
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

    var label = rows.length ? rows.length + "/8 已返回" : "不可用";
    var state = rows.length === SCORE_TYPES.length ? "available" : (rows.length ? "partial" : "unavailable");
    setCoverage("[data-score-coverage]", label, state);
    setText("[data-score-card-coverage]", label, "不可用");
    setText(
      "[data-score-note]",
      rows.length
        ? "仅显示后端返回评分；缺失维度不是 0，不补齐。"
        : null,
      "评分不可用；不合成、不平均、不用默认值补齐。"
    );
    return rows.length > 0;
  }

  function parseJsonObject(value) {
    if (value && typeof value === "object") return value;
    if (!hasText(value)) return null;
    try {
      var parsed = JSON.parse(value);
      return parsed && typeof parsed === "object" ? parsed : null;
    } catch (error) {
      return null;
    }
  }

  function visibleValue(value, fallback) {
    if (value === null || value === undefined || value === "") {
      return fallback || "当前不可查看";
    }
    if (Array.isArray(value)) return value.length ? value.map(function (item) {
      return visibleValue(item, "");
    }).filter(Boolean).join("；") : (fallback || "当前不可查看");
    if (typeof value === "object") return JSON.stringify(value);
    return String(value);
  }

  function roleContract(audit) {
    var decision = audit && audit.decisionBundle;
    var encoded = decision && decision.aiRoleResults;
    var payload = parseJsonObject(encoded) || {};
    var rawRoles = payload.roles && typeof payload.roles === "object" ? payload.roles : {};
    var traces = Array.isArray(audit && audit.aiTraces) ? audit.aiTraces : [];
    var roles = {};
    ROLE_ORDER.forEach(function (name) {
      var role = Object.assign({}, rawRoles[name] || {});
      var trace = traces.find(function (item) { return item && item.role === name; }) || {};
      role.analysisId = role.analysisId || trace.analysisId || null;
      role.traceId = role.traceId || trace.traceId || null;
      role.generatedAt = role.generatedAt || trace.observedAt || trace.createdAt || null;
      role.roleState = contract.normalizeRoleState(role.roleState || (trace.fallback === true
        ? "FALLBACK" : (trace.status === "SUCCESS" ? "READY" : (trace.status ? "ERROR" : "UNAVAILABLE"))));
      role.errorMessage = role.errorMessage || trace.errorMessage || null;
      role.fallbackReason = role.fallbackReason || trace.fallbackReason || null;
      role.callStatus = role.callStatus || trace.status || null;
      roles[name] = role;
    });
    return roles;
  }

  function renderTimeframes(decision, roles) {
    var convergence = decision && decision.multiTfConvergence;
    var gpt = roles && roles.GPT_FINAL || {};
    var details = gpt.multiTimeframeExplanation || {};
    var mappings = {
      "4H": details["4h"] || details.fourHour,
      "1H": details["1h"] || details.oneHour,
      "15M": details["15m"] || details.fifteenMinute,
      "5M": details["5m"] || details.fiveMinute
    };
    var count = 0;
    Object.keys(mappings).forEach(function (timeframe) {
      var node = document.querySelector('[data-timeframe="' + timeframe + '"]');
      if (!node) return;
      var value = mappings[timeframe];
      node.textContent = visibleValue(value, "当前不可查看");
      if (hasText(value)) count += 1;
    });
    setText("[data-timeframe-convergence]", convergence, "当前不可查看");
    setCoverage(
      "[data-timeframe-status]",
      count === 4 ? "4/4 周期可用" : (count ? count + "/4 周期可用" : "不可用"),
      count === 4 ? "available" : (count ? "partial" : "unavailable")
    );
    var module = document.querySelector(".timeframe-module");
    if (module) module.dataset.failState = count ? "MULTI_TIMEFRAME_PARTIAL" : "MULTI_TIMEFRAME_UNAVAILABLE";
    return count > 0;
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

  function appendObjectDefinitions(target, object, fields) {
    (fields || []).forEach(function (field) {
      appendDefinition(target, field[0], visibleValue(object && object[field[1]], field[2]));
    });
  }

  function renderStructuredCollection(parent, label, state, values) {
    var section = createElement("section", "role-collection");
    var heading = createElement("div", "card-heading");
    heading.appendChild(createElement("strong", "", label));
    heading.appendChild(createElement("span", "", contract.collectionStateLabel(state)));
    section.appendChild(heading);
    var rows = Array.isArray(values) ? values : [];
    if (!rows.length) {
      section.appendChild(createElement("p", "", "集合为空；以集合状态区分未发现、数据不足、来源不可用或过期。"));
    } else {
      var list = createElement("div", "role-collection-list");
      rows.forEach(function (item, index) {
        var card = createElement("article", "role-collection-item");
        var dl = createElement("dl");
        var object = item && typeof item === "object" ? item : { value: item };
        Object.keys(object).forEach(function (key) {
          appendDefinition(dl, key, visibleValue(object[key]));
        });
        card.appendChild(createElement("strong", "", label + " " + (index + 1)));
        card.appendChild(dl);
        list.appendChild(card);
      });
      section.appendChild(list);
    }
    parent.appendChild(section);
  }

  function renderRolePanel(roleName, role) {
    var panel = document.querySelector('[data-role-panel="' + roleName + '"]');
    var content = panel && panel.querySelector("[data-role-content]");
    if (!content) return false;
    content.replaceChildren();
    var metadata = createElement("dl", "role-metadata");
    appendObjectDefinitions(metadata, role, [
      ["Role State", "roleState"],
      ["Analysis ID", "analysisId"],
      ["Trace ID", "traceId"],
      ["Generated At", "generatedAt"],
      ["调用状态", "callStatus"],
      ["错误 / Fallback", role.errorMessage ? "errorMessage" : "fallbackReason", "无"]
    ]);
    content.appendChild(metadata);

    if (roleName === "GPT_FINAL") {
      var gpt = createElement("dl", "role-contract-grid");
      appendObjectDefinitions(gpt, role.coreJudgment || {}, [
        ["Market Bias", "marketBias"], ["Opportunity State", "opportunityState"], ["核心判断", "text"]
      ]);
      appendObjectDefinitions(gpt, role.candidateSummary || {}, [
        ["Candidate Summary", "summary"], ["Candidate Plan Mode", "planMode"],
        ["置信度", "confidence"], ["风险", "riskLevel"], ["是否值得开仓", "worthOpening"]
      ]);
      appendObjectDefinitions(gpt, role.biasAdjustment || {}, [
        ["Bias Before", "before"], ["Bias After", "after"], ["调整原因", "reason"]
      ]);
      content.appendChild(gpt);
      renderStructuredCollection(content, "支持证据", role.supportingEvidenceState, role.supportingEvidence);
      renderStructuredCollection(content, "反对证据", role.opposingEvidenceState, role.opposingEvidence);
    } else if (roleName === "GEMINI_REVIEW") {
      var gemini = createElement("dl", "role-contract-grid");
      appendObjectDefinitions(gemini, role, [
        ["Review Result", "reviewResult"], ["最终方向影响", "finalDirectionImpact"],
        ["置信度调整", "confidenceAdjustment"], ["风险调整", "riskAdjustment"],
        ["Plan Mode 调整", "planModeAdjustment"], ["恢复条件", "recoveryCondition"]
      ]);
      appendObjectDefinitions(gemini, role.downgradeSuggestion || {}, [
        ["降级前", "before"], ["降级后", "after"], ["降级原因", "reason"], ["恢复条件", "recoveryCondition"]
      ]);
      content.appendChild(gemini);
      renderStructuredCollection(content, "Evidence Gaps", role.evidenceGapsState, role.evidenceGaps);
      renderStructuredCollection(content, "Logic Conflicts", role.logicConflictsState, role.logicConflicts);
      renderStructuredCollection(content, "Underestimated Risks", role.underestimatedRisksState, role.underestimatedRisks);
    } else {
      var grok = createElement("dl", "role-contract-grid");
      appendObjectDefinitions(grok, role, [
        ["挑战摘要", "challengeSummary"], ["当前方向挑战", "currentDirectionChallenge"],
        ["重大反证", "majorCounterEvidence"], ["风险调整", "riskAdjustment"], ["Plan Mode 影响", "planModeImpact"]
      ]);
      content.appendChild(grok);
      renderStructuredCollection(content, "Failure Paths", role.failurePathState, role.failurePaths);
      renderStructuredCollection(content, "Opposing Scenarios", role.opposingScenariosState, role.opposingScenarios);
      renderStructuredCollection(content, "External Event Risks", role.externalEventRisksState, role.externalEventRisks);
      renderStructuredCollection(content, "Microstructure Risks", role.microstructureRisksState, role.microstructureRisks);
      renderStructuredCollection(content, "Watch Indicators", role.watchIndicatorsState, role.watchIndicators);
    }
    return ["READY", "PARTIAL", "FALLBACK"].indexOf(role.roleState) >= 0;
  }

  function renderAiStatus(audit, roles) {
    var available = 0;
    ROLE_ORDER.forEach(function (roleName) {
      if (renderRolePanel(roleName, roles[roleName] || {})) available += 1;
    });
    setCoverage("[data-ai-status]", available + "/3 角色可追溯",
      available === 3 ? "available" : (available ? "partial" : "unavailable"));
    activateRole(activeRole, false);
    return available > 0;
  }

  function renderAuditChain(audit) {
    var stages = Array.isArray(audit && audit.orderedStages) ? audit.orderedStages : [];
    var list = document.querySelector("[data-audit-stage-list]");
    if (list) {
      list.replaceChildren();
      stages.forEach(function (stage) {
        var item = createElement("li", "audit-stage");
        item.appendChild(createElement("span", "", String(stage.order)));
        item.appendChild(createElement("strong", "", visibleValue(stage.stage)));
        item.appendChild(createElement("small", "", visibleValue(stage.owner) + " · "
          + visibleValue(stage.status) + " · " + visibleValue(stage.referenceId)));
        list.appendChild(item);
      });
    }
    setCoverage("[data-audit-status]", stages.length ? stages.length + " 个阶段" : "不可用",
      stages.length ? "available" : "unavailable");

    var resolver = audit && audit.conflictResolver || {};
    var resolverList = document.querySelector("[data-resolver-detail]");
    if (resolverList) {
      resolverList.replaceChildren();
      appendObjectDefinitions(resolverList, resolver, [
        ["Conflict Level", "conflictLevel"], ["Conflict Score", "conflictScore"],
        ["Market Bias Before", "biasBefore"], ["Market Bias After", "biasAfter"],
        ["Plan Mode Before", "planModeBefore"], ["Plan Mode After", "planModeAfter"],
        ["Confidence Before", "confidenceBefore"], ["Confidence After", "confidenceAfter"],
        ["Risk Before", "riskBefore"], ["Risk After", "riskAfter"],
        ["Downgrade Reason", "downgradeReason"], ["Recovery Condition", "recoveryCondition"],
        ["Confused Decision", "confusedDecision"], ["Rule Veto Reason", "ruleVetoReason"]
      ]);
    }
    var validation = audit && audit.ruleValidation || {};
    var validationList = document.querySelector("[data-rule-validation-detail]");
    if (validationList) {
      validationList.replaceChildren();
      appendObjectDefinitions(validationList, validation, [
        ["Validation Result ID", "validationResultId"], ["Status", "status"],
        ["Reasons", "reasons"], ["Veto Reason", "vetoReason"],
        ["Chain Status", "chainStatus"], ["Source Gate", "sourceGateStatus"],
        ["Source Complete", "sourceGateComplete"], ["Final Plan", "finalPlan"]
      ]);
    }
  }

  function renderFinalSource(audit) {
    var plan = audit && audit.finalExecutionPlan;
    var validation = audit && audit.ruleValidation || {};
    var valid = !!plan && plan.finalPlan === true && validation.status === "PASS"
      && plan.notTradeInstruction === true;
    var grid = document.querySelector("[data-final-source-grid]");
    if (grid) {
      grid.replaceChildren();
      appendDefinition(grid, "Final Plan ID", valid ? visibleValue(plan.planId) : "当前没有可验证 Final");
      appendDefinition(grid, "Analysis ID", valid ? visibleValue(plan.analysisId) : "当前不可查看");
      appendDefinition(grid, "Candidate ID", valid ? visibleValue(plan.candidateId) : "当前不可查看");
      appendDefinition(grid, "Resolver Result ID", valid ? visibleValue(plan.resolverResultId) : "当前不可查看");
      appendDefinition(grid, "Validation Result ID", valid ? visibleValue(plan.validationResultId) : "当前不可查看");
      appendDefinition(grid, "Source Status", valid ? visibleValue(plan.sourceStatus) : "当前不可查看");
      appendDefinition(grid, "Candidate / Final 隔离", audit && audit.candidateFinalIsolated === true ? "PASS" : "不可验证");
      appendDefinition(grid, "notTradeInstruction", valid ? "true" : "不可验证");
    }
    setCoverage("[data-final-source-status]", valid ? "VALIDATED FINAL" : "非 Final",
      valid ? "available" : "unavailable");
    setText("[data-final-source-note]", valid
      ? "Final 已通过 Resolver 与 Rule Validation；页面不把 Candidate 暴露为 Final。"
      : "Candidate 或未通过 Rule Validation 的内容保持关闭。", "Final 来源不可验证");
    return valid;
  }

  function validateIdentity(data, audit, requestedAnalysisId, requestedSymbol) {
    var run = data && data.run || audit && audit.analysis;
    if (!run || typeof run !== "object") return { ok: false, reason: "RUN_MISSING" };
    if (String(run.analysisId || "") !== requestedAnalysisId) {
      return { ok: false, reason: "ANALYSIS_ID_MISMATCH" };
    }
    if (requestedSymbol && normalizeSymbol(run.symbol) !== requestedSymbol) {
      return { ok: false, reason: "SYMBOL_MISMATCH" };
    }
    return { ok: true, run: run };
  }

  function renderAggregate(data, audit, requestedAnalysisId, requestedSymbol) {
    var identity = validateIdentity(data, audit, requestedAnalysisId, requestedSymbol);
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
      renderTimeframes(null, {});
      renderAiStatus(audit || {}, roleContract(audit || {}));
      renderAuditChain(audit || {});
      renderFinalSource(audit || {});
      var processingNotice = document.querySelector("[data-partial-notice]");
      if (processingNotice) processingNotice.hidden = false;
      root.dataset.pageState = "PARTIAL_DATA";
      setPageStatus("分析处理中", "partial");
      return;
    }

    var decision = audit && audit.decisionBundle || data.decision;
    var roles = roleContract(audit || {});
    var scoreItems = audit && audit.scores || data.scoreTopItems;
    renderContext(run, decision, scoreItems);
    renderMarketJudgment(decision, data.marketEnvironment);
    renderEvidence(audit && audit.evidence || data.evidenceTopItems);
    renderScores(scoreItems);
    renderTimeframes(decision, roles);
    renderAiStatus(audit || {}, roles);
    renderAuditChain(audit || {});
    renderFinalSource(audit || {});

    var partial = document.querySelector("[data-partial-notice]");
    var complete = !!(audit && audit.analysis && audit.decisionBundle)
      && Array.isArray(audit.evidence) && Array.isArray(audit.scores)
      && Array.isArray(audit.aiTraces);
    if (partial) partial.hidden = complete;
    root.dataset.pageState = complete ? "READY" : "PARTIAL_DATA";
    setPageStatus(complete ? "决策链已同步" : "部分数据可用", complete ? "ready" : "partial");
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
    var request = beginRequest(analysisId);
    root.setAttribute("aria-busy", "true");
    root.dataset.pageState = "LOADING";
    hidePageState();
    hideContent();
    setPageStatus("正在同步", "loading");

    if (!analysisId || analysisId.length > 128) {
      if (isCurrentRequest(request)) {
        showPageState(
          "ANALYSIS_NOT_FOUND",
          "Analysis Not Found",
          "缺少可验证的 analysisId，无法读取分析详情。",
          false
        );
        root.setAttribute("aria-busy", "false");
        activeRequestController = null;
      }
      return;
    }

    try {
      var requestOptions = {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" }
      };
      if (request.controller) requestOptions.signal = request.controller.signal;
      var responses = await Promise.all([
        fetch("/api/review/aggregate/" + encodeURIComponent(analysisId), requestOptions),
        fetch("/api/ai/audit-chain?analysisId=" + encodeURIComponent(analysisId), requestOptions)
      ]);
      if (!isCurrentRequest(request)) return;
      if (responses.some(function (response) { return response.status === 404; })) {
        showPageState(
          "ANALYSIS_NOT_FOUND",
          "Analysis Not Found",
          "没有找到该次分析，不会回退到同资产的其他分析。",
          false
        );
        return;
      }
      if (responses.some(function (response) { return !response.ok; })) {
        throw new Error("ANALYSIS_DETAIL_REQUEST_FAILED");
      }
      var payloads = await Promise.all(responses.map(function (response) { return response.json(); }));
      if (!isCurrentRequest(request)) return;
      var aggregate = contract.parseApiEnvelope(payloads[0]);
      var audit = contract.parseApiEnvelope(payloads[1]);
      if (!aggregate.ok || !audit.ok) throw new Error("ANALYSIS_DETAIL_RESPONSE_INVALID");
      if (!isCurrentRequest(request)) return;
      renderAggregate(aggregate.data, audit.data, analysisId, selectedSymbol);
    } catch (error) {
      if (!isCurrentRequest(request) || isAbortError(error)) return;
      showPageState(
        "LOAD_FAILED",
        "Load Failed",
        "分析详情加载失败。可重试读取同一 analysisId。",
        true
      );
    } finally {
      if (isCurrentRequest(request)) {
        root.setAttribute("aria-busy", "false");
        activeRequestController = null;
      }
    }
  }

  bindRoleTabs();
  bindRetry();
  updateBackLink();
  loadAnalysisDetail();
})();
