(function (global) {
  "use strict";

  var AI_ROLES = Object.freeze([
    Object.freeze({ role: "GPT_FINAL", label: "GPT 综合判断" }),
    Object.freeze({ role: "GEMINI_REVIEW", label: "Gemini 冲突复核" }),
    Object.freeze({ role: "GROK_CHALLENGE", label: "Grok 反方挑战" })
  ]);

  var ASSET_STATES = Object.freeze({
    OBSERVING: Object.freeze({ label: "观察中", tone: "neutral" }),
    CANDIDATE: Object.freeze({ label: "待复核候选", tone: "info" }),
    WAITING_TRIGGER: Object.freeze({ label: "等待触发", tone: "warning" }),
    TRIGGERED: Object.freeze({ label: "条件已触发", tone: "info" }),
    HIGH_RISK: Object.freeze({ label: "高风险", tone: "danger" }),
    INVALIDATED: Object.freeze({ label: "已失效", tone: "muted" }),
    COOLING: Object.freeze({ label: "冷却中", tone: "muted" }),
    CONFUSED: Object.freeze({ label: "冲突阻断", tone: "danger" })
  });

  var AI_ANALYSIS_STATE_VIEWS = Object.freeze({
    loading: Object.freeze({
      statusLabel: "正在同步",
      runStatusLabel: "正在同步",
      consistencyLevel: "等待同步",
      consistencySummary: "AI 角色结果正在同步",
      roleStatusLabel: "正在同步",
      roleSummary: "AI 角色结果正在同步",
      detailStatus: "分析身份同步中"
    }),
    empty: Object.freeze({
      statusLabel: "暂无可分析资产",
      runStatusLabel: "暂无可分析资产",
      consistencyLevel: "--",
      consistencySummary: "暂无可分析资产",
      roleStatusLabel: "暂无数据",
      roleSummary: "暂无可分析资产",
      detailStatus: "暂无可分析资产"
    }),
    error: Object.freeze({
      statusLabel: "当前不可查看",
      runStatusLabel: "同步失败",
      consistencyLevel: "--",
      consistencySummary: "AI 分析加载失败，当前不可查看",
      roleStatusLabel: "当前不可查看",
      roleSummary: "AI 角色数据加载失败，当前不可查看",
      detailStatus: "当前不可查看"
    }),
    partial: Object.freeze({
      statusLabel: "部分数据待同步",
      runStatusLabel: "等待同步",
      consistencyLevel: "等待同步",
      consistencySummary: "等待 AI 三角色结果同步后生成一致性结论",
      roleStatusLabel: "待同步",
      roleSummary: "当前角色观点待同步",
      detailStatus: "权威 analysisId 已就绪"
    }),
    missing: Object.freeze({
      statusLabel: "分析身份待同步",
      runStatusLabel: "当前不可查看",
      consistencyLevel: "--",
      consistencySummary: "缺少权威 analysisId，当前不可查看",
      roleStatusLabel: "当前不可查看",
      roleSummary: "缺少权威 analysisId，当前不可查看",
      detailStatus: "需要权威 analysisId"
    })
  });

  var POSITION_SELECTION_STATUSES = Object.freeze([
    "POSITION_SELECTION_REQUIRED",
    "POSITION_NOT_FOUND",
    "POSITION_SYMBOL_MISMATCH"
  ]);

  var MODULE_STATES = Object.freeze([
    "LOADING", "READY", "PARTIAL", "EMPTY", "ERROR", "MISSING"
  ]);

  var FIELD_SOURCE_VIEWS = Object.freeze({
    REAL: Object.freeze({ label: "真实", tone: "real" }),
    DERIVED: Object.freeze({ label: "派生", tone: "derived" }),
    FALLBACK: Object.freeze({ label: "降级", tone: "fallback" }),
    MISSING: Object.freeze({ label: "缺失", tone: "missing" }),
    ERROR: Object.freeze({ label: "错误", tone: "error" })
  });

  var DATA_QUALITY_LABELS = Object.freeze({
    GOOD: "良好",
    PARTIAL: "数据不足",
    STALE: "数据过期",
    MISSING: "数据缺失",
    ERROR: "数据错误"
  });

  var MARKET_BIAS_HIERARCHY_LABELS = Object.freeze({
    STRONG_BULLISH: "强偏多",
    BULLISH: "偏多",
    WEAK_BULLISH: "弱偏多",
    RANGE: "震荡",
    WEAK_BEARISH: "弱偏空",
    BEARISH: "偏空",
    STRONG_BEARISH: "强偏空",
    WAIT: "观望"
  });

  var PLAN_MODE_LABELS = Object.freeze({
    CONFIRMATION: "确认型",
    PREPARATION: "预备型",
    REDUCED: "缩减型",
    OBSERVATION: "观察",
    BLOCKED: "阻断"
  });

  var PLAN_MODE_VIEWS = Object.freeze({
    CONFIRMATION: Object.freeze({
      typeLabel: "确认型",
      participationLabel: "条件已确认",
      detail: "方向、证据、触发、风险和规则校验均已满足。",
      tone: "positive",
      profile: "confirmation"
    }),
    PREPARATION: Object.freeze({
      typeLabel: "预备型",
      participationLabel: "等待触发",
      detail: "方向和主要逻辑已经形成，当前等待价格、结构或事件条件触发。",
      tone: "warning",
      profile: "preparation"
    }),
    REDUCED: Object.freeze({
      typeLabel: "缩减型",
      participationLabel: "降低强度",
      detail: "机会仍然成立，当前按反证与风险约束降低参与强度。",
      tone: "reduced",
      profile: "reduced"
    }),
    OBSERVATION: Object.freeze({
      typeLabel: "观察",
      participationLabel: "当前仅观察",
      detail: "当前判断具有分析价值，但尚不形成方向性参与计划。",
      tone: "neutral",
      profile: "observation"
    }),
    BLOCKED: Object.freeze({
      typeLabel: "阻断",
      participationLabel: "当前已阻断",
      detail: "数据质量、风险、冲突、状态机或规则校验阻止当前方向性参与。",
      tone: "danger",
      profile: "blocked"
    })
  });

  var OPPORTUNITY_TYPE_LABELS = Object.freeze({
    STRUCTURE_CONFIRMATION: "结构确认",
    TREND_CONTINUATION: "趋势延续",
    REVERSAL_CONFIRMATION: "反转确认",
    EVENT_DRIVEN: "事件驱动",
    RANGE_BOUND: "区间观察"
  });

  var USER_FACING_VALUE_LABELS = Object.freeze({
    STRONG_BULLISH: "强偏多",
    BULLISH: "偏多",
    WEAK_BULLISH: "弱偏多",
    RANGE: "震荡",
    WEAK_BEARISH: "弱偏空",
    BEARISH: "偏空",
    STRONG_BEARISH: "强偏空",
    WAIT: "观望",
    CONFIRMATION: "确认型",
    PREPARATION: "预备型",
    REDUCED: "缩减型",
    OBSERVATION: "观察",
    BLOCKED: "阻断",
    HIGH: "高",
    MEDIUM: "中",
    LOW: "低",
    EXTREME: "极高",
    TIMEFRAME: "周期冲突",
    REVERSAL: "反转",
    LIQUIDITY: "流动性",
    MONITOR: "观察指标",
    OPEN_INTEREST_EXPANSION: "未平仓量扩张",
    OPEN_INTEREST_CONTRACTION: "未平仓量收缩",
    OPEN_INTEREST_PRICE_CONFIRMATION: "未平仓量与价格确认",
    OPEN_INTEREST_PRICE_DIVERGENCE: "未平仓量与价格背离",
    FUNDING_POSITIVE_EXTREME: "正资金费率极值",
    FUNDING_NEGATIVE_EXTREME: "负资金费率极值",
    FUNDING_NORMAL: "资金费率正常",
    LONG_CROWDING: "多头拥挤",
    SHORT_CROWDING: "空头拥挤",
    LONG_LIQUIDATION_SPIKE: "多头清算激增",
    SHORT_LIQUIDATION_SPIKE: "空头清算激增",
    LIQUIDATION_IMBALANCE: "清算失衡",
    EXCHANGE_CONCENTRATION_HIGH: "交易所集中度偏高",
    DERIVATIVES_DATA_PARTIAL: "衍生品数据不完整",
    DERIVATIVES_DATA_STALE: "衍生品数据已过期",
    DERIVATIVES_DATA_UNAVAILABLE: "衍生品数据暂不可用",
    CONTROLLED_VISUAL_FIXTURE: "受控验证数据"
  });

  var USER_FACING_FIELD_LABELS = Object.freeze({
    "4h": "4 小时",
    "1h": "1 小时",
    "15m": "15 分钟",
    "5m": "5 分钟",
    role: "分析角色",
    provider: "数据来源",
    sourceRole: "来源角色",
    callStatus: "调用状态",
    analysisId: "分析编号",
    traceId: "调用编号",
    roleState: "角色状态",
    dataState: "数据状态",
    generatedAt: "生成时间",
    stance: "当前立场",
    conflictLevel: "冲突等级",
    conflictScore: "冲突评分",
    reasonCodes: "判断原因",
    summary: "结论摘要",
    fallback: "规则降级",
    fallbackReason: "降级原因",
    manualReviewRequired: "人工复核",
    coreJudgment: "核心判断",
    marketBias: "市场方向",
    opportunityState: "机会状态",
    text: "判断说明",
    supportingEvidence: "支持证据",
    supportingEvidenceState: "支持证据状态",
    opposingEvidence: "反对证据",
    opposingEvidenceState: "反对证据状态",
    evidenceGaps: "证据缺口",
    evidenceGapsState: "证据缺口状态",
    logicConflicts: "逻辑冲突",
    logicConflictsState: "逻辑冲突状态",
    underestimatedRisks: "低估风险",
    underestimatedRisksState: "低估风险状态",
    failurePaths: "失败路径",
    failurePathState: "失败路径状态",
    opposingScenarios: "反向情景",
    opposingScenariosState: "反向情景状态",
    externalEventRisks: "外部事件风险",
    externalEventRisksState: "外部事件风险状态",
    microstructureRisks: "微观结构风险",
    microstructureRisksState: "微观结构风险状态",
    watchIndicators: "观察指标",
    watchIndicatorsState: "观察指标状态",
    challengeSummary: "挑战摘要",
    currentDirectionChallenge: "方向挑战",
    majorCounterEvidence: "重大反证",
    planModeImpact: "计划模式影响",
    finalMarketBias: "最终市场方向",
    finalConfidence: "最终置信度",
    finalRiskLevel: "最终风险等级",
    finalPlanMode: "最终计划模式",
    worthOpening: "是否值得参与",
    confidenceAdjustment: "置信度调整",
    riskAdjustment: "风险调整",
    planModeAdjustment: "计划模式调整",
    confused: "冲突待解",
    downgradeReason: "降级原因",
    mainReason: "主要原因",
    recoveryCondition: "恢复条件",
    ruleDirection: "规则方向",
    ruleConfidence: "规则置信度",
    ruleRisk: "规则风险",
    rulePlanMode: "规则计划模式",
    ruleCanExecute: "规则许可",
    dataQualityScore: "数据质量",
    confusedScore: "冲突评分",
    accountRiskState: "账户风险状态",
    planModeBefore: "调整前计划模式",
    planModeAfter: "调整后计划模式",
    confidenceBefore: "调整前置信度",
    confidenceAfter: "调整后置信度",
    riskBefore: "调整前风险",
    riskAfter: "调整后风险",
    biasBefore: "调整前方向",
    biasAfter: "调整后方向",
    adjustmentReason: "调整原因",
    confusedDecision: "冲突判定",
    ruleVetoReason: "规则否决原因",
    ruleDirectionPreserved: "规则方向保持",
    title: "事件名称",
    eventName: "事件名称",
    eventType: "事件类型",
    eventTime: "事件时间",
    publishedAt: "发布时间",
    occurredAt: "发生时间",
    description: "说明",
    impactLevel: "影响等级",
    symbol: "资产",
    source: "来源",
    currentValue: "当前值",
    change: "变化",
    direction: "方向",
    strength: "强度",
    confidence: "置信度",
    observedAt: "观测时间",
    freshness: "时效状态"
  });

  var COLLECTION_STATE_LABELS = Object.freeze({
    FOUND: "已发现",
    NONE_FOUND: "完成检查，未发现",
    INSUFFICIENT_DATA: "数据不足，无法判断",
    SOURCE_UNAVAILABLE: "数据来源暂不可用",
    STALE: "数据已过期",
    NO_VERIFIABLE_FAILURE_PATH: "暂无可验证失败路径"
  });

  var ROLE_STATE_VIEWS = Object.freeze({
    READY: Object.freeze({ label: "分析完成", tone: "positive" }),
    PARTIAL: Object.freeze({ label: "部分结果可用", tone: "warning" }),
    FALLBACK: Object.freeze({ label: "当前使用规则降级结果", tone: "warning" }),
    UNAVAILABLE: Object.freeze({ label: "AI解释暂不可用", tone: "unavailable" }),
    ERROR: Object.freeze({ label: "分析失败", tone: "danger" })
  });

  var COLLECTION_STATE_VIEWS = Object.freeze({
    FOUND: Object.freeze({ label: "已发现", detail: "已发现可验证内容。", tone: "positive" }),
    NONE_FOUND: Object.freeze({ label: "完成检查，未发现", detail: "检查已完成，当前没有可验证条目。", tone: "neutral" }),
    INSUFFICIENT_DATA: Object.freeze({ label: "数据不足，无法判断", detail: "当前数据不足，暂时无法形成可靠判断。", tone: "unavailable" }),
    SOURCE_UNAVAILABLE: Object.freeze({ label: "数据来源暂不可用", detail: "当前无法读取可信数据来源。", tone: "unavailable" }),
    STALE: Object.freeze({ label: "数据已过期", detail: "现有数据已过期，需要重新分析。", tone: "warning" }),
    NO_VERIFIABLE_FAILURE_PATH: Object.freeze({ label: "暂无可验证失败路径", detail: "当前没有可验证的失败路径。", tone: "neutral" })
  });

  var PLAN_DATA_STATE_VIEWS = Object.freeze({
    UNSELECTED: Object.freeze({ label: "请选择资产", detail: "选择一个重点机会资产后查看执行计划。", tone: "neutral" }),
    LOADING: Object.freeze({ label: "正在分析", detail: "正在获取当前资产的最新分析状态。", tone: "neutral" }),
    WAITING_ANALYSIS: Object.freeze({ label: "正在分析", detail: "正在获取数据并生成证据。", tone: "neutral" }),
    FETCHING_DATA: Object.freeze({ label: "正在分析", detail: "正在获取数据。", tone: "neutral" }),
    GENERATING_EVIDENCE: Object.freeze({ label: "正在分析", detail: "正在生成证据。", tone: "neutral" }),
    BUILDING_CANDIDATE: Object.freeze({ label: "正在分析", detail: "正在形成候选计划。", tone: "neutral" }),
    REVIEWING_RISK: Object.freeze({ label: "正在分析", detail: "正在进行风险复核。", tone: "neutral" }),
    WAITING_RULE_VALIDATION: Object.freeze({ label: "等待规则校验", detail: "候选计划正在等待规则校验。", tone: "warning" }),
    RULE_VALIDATION_PENDING: Object.freeze({ label: "等待规则校验", detail: "候选计划正在等待规则校验。", tone: "warning" }),
    CANDIDATE_ONLY: Object.freeze({ label: "等待规则校验", detail: "候选计划已形成，尚未完成规则校验。", tone: "warning" }),
    INSUFFICIENT_DATA: Object.freeze({ label: "当前数据不足", detail: "当前证据不足以形成可信的最终计划。", tone: "unavailable" }),
    DATA_QUALITY_BLOCKED: Object.freeze({ label: "当前数据不足", detail: "数据质量未达到最终计划门槛。", tone: "unavailable" }),
    PLAN_INCOMPLETE: Object.freeze({ label: "当前数据不足", detail: "当前计划所需信息尚不完整。", tone: "unavailable" }),
    SOURCE_UNAVAILABLE: Object.freeze({ label: "数据来源暂不可用", detail: "当前无法读取可信数据来源。", tone: "unavailable" }),
    PLAN_IDENTITY_MISSING: Object.freeze({ label: "数据来源暂不可用", detail: "当前计划来源暂不可验证。", tone: "unavailable" }),
    PLAN_IDENTITY_ERROR: Object.freeze({ label: "数据来源暂不可用", detail: "当前计划来源暂不可验证。", tone: "unavailable" }),
    STALE: Object.freeze({ label: "当前结果已过期", detail: "需要重新扫描或重新分析。", tone: "warning" }),
    EXPIRED: Object.freeze({ label: "当前结果已过期", detail: "需要重新扫描或重新分析。", tone: "warning" }),
    STATE_SNAPSHOT_MISMATCH: Object.freeze({ label: "当前结果已过期", detail: "资产状态已变化，需要重新分析。", tone: "warning" }),
    REVALIDATION_REQUIRED: Object.freeze({ label: "等待规则校验", detail: "当前结果需要重新校验。", tone: "warning" }),
    RULE_VETOED: Object.freeze({ label: "尚未形成最终计划", detail: "当前候选未通过规则校验。", tone: "danger" }),
    AI_UNAVAILABLE: Object.freeze({ label: "尚未形成最终计划", detail: "AI 解释暂不可用，当前没有可展示的最终计划。", tone: "unavailable" }),
    NO_COMPLETE_PLAN: Object.freeze({ label: "尚未形成最终计划", detail: "当前没有已持久化的最终计划。", tone: "neutral" }),
    PLAN_MISSING: Object.freeze({ label: "尚未形成最终计划", detail: "当前没有已持久化的最终计划。", tone: "neutral" }),
    MISSING: Object.freeze({ label: "尚未形成最终计划", detail: "当前没有已持久化的最终计划。", tone: "neutral" }),
    ERROR: Object.freeze({ label: "数据来源暂不可用", detail: "执行计划读取失败，请稍后重试。", tone: "danger" })
  });

  var DATA_STATE_VIEWS = Object.freeze({
    READY: Object.freeze({ label: "数据可用", tone: "positive" }),
    FOUND: Object.freeze({ label: "数据可用", tone: "positive" }),
    LOADING: Object.freeze({ label: "正在同步", tone: "neutral" }),
    PARTIAL: Object.freeze({ label: "部分数据可用", tone: "warning" }),
    EMPTY: Object.freeze({ label: "暂无数据", tone: "neutral" }),
    NONE_FOUND: Object.freeze({ label: "暂无数据", tone: "neutral" }),
    INSUFFICIENT_DATA: Object.freeze({ label: "数据不足", tone: "unavailable" }),
    SOURCE_UNAVAILABLE: Object.freeze({ label: "数据来源暂不可用", tone: "unavailable" }),
    UNAVAILABLE: Object.freeze({ label: "当前不可用", tone: "unavailable" }),
    STALE: Object.freeze({ label: "数据已过期", tone: "warning" }),
    MISSING: Object.freeze({ label: "暂无数据", tone: "unavailable" }),
    ERROR: Object.freeze({ label: "读取失败", tone: "danger" }),
    INVALID: Object.freeze({ label: "当前不可查看", tone: "danger" })
  });

  var ROLE_STATES = Object.freeze([
    "READY", "PARTIAL", "FALLBACK", "UNAVAILABLE", "ERROR"
  ]);

  var REVIEW_RESULT_LABELS = Object.freeze({
    APPROVE: "通过",
    DOWNGRADE: "降级",
    REJECT_CANDIDATE: "拒绝候选",
    RISK_WARNING: "风险警告"
  });

  var ANALYSIS_MODES = Object.freeze([
    "ANALYSIS_PREVIEW", "OPPORTUNITY_DECISION"
  ]);

  var STRUCTURED_AI_COLLECTIONS = Object.freeze([
    Object.freeze({ key: "supportingEvidence", state: "supportingEvidenceState" }),
    Object.freeze({ key: "opposingEvidence", state: "opposingEvidenceState" }),
    Object.freeze({ key: "evidenceGaps", state: "evidenceGapsState" }),
    Object.freeze({ key: "logicConflicts", state: "logicConflictsState" }),
    Object.freeze({ key: "underestimatedRisks", state: "underestimatedRisksState" }),
    Object.freeze({ key: "failurePaths", state: "failurePathState" }),
    Object.freeze({ key: "opposingScenarios", state: "opposingScenariosState" }),
    Object.freeze({ key: "externalEventRisks", state: "externalEventRisksState" }),
    Object.freeze({ key: "microstructureRisks", state: "microstructureRisksState" }),
    Object.freeze({ key: "watchIndicators", state: "watchIndicatorsState" })
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

  function normalizeModuleState(value, fallback) {
    var normalized = String(value || "").trim().toUpperCase();
    return MODULE_STATES.indexOf(normalized) >= 0
      ? normalized
      : String(fallback || "MISSING").toUpperCase();
  }

  function fieldSourceView(value) {
    var normalized = String(value || "MISSING").trim().toUpperCase();
    return FIELD_SOURCE_VIEWS[normalized] || FIELD_SOURCE_VIEWS.ERROR;
  }

  function dataQualityLabel(value) {
    var normalized = String(value || "MISSING").trim().toUpperCase();
    return DATA_QUALITY_LABELS[normalized] || DATA_QUALITY_LABELS.ERROR;
  }

  function marketBiasHierarchyLabel(value) {
    var normalized = String(value || "").trim().toUpperCase();
    return MARKET_BIAS_HIERARCHY_LABELS[normalized] || "--";
  }

  function planModeLabel(value) {
    var normalized = String(value || "").trim().toUpperCase();
    return PLAN_MODE_VIEWS[normalized]
      ? PLAN_MODE_VIEWS[normalized].typeLabel
      : "—";
  }

  function opportunityTypeLabel(value) {
    var normalized = String(value || "").trim().toUpperCase();
    return OPPORTUNITY_TYPE_LABELS[normalized] || "—";
  }

  function userFacingValue(value) {
    if (!hasText(value)) return "";
    var text = String(value);
    var exact = USER_FACING_VALUE_LABELS[text.trim().toUpperCase()];
    if (exact) return exact;
    if (/^[A-Z][A-Z0-9_]*$/.test(text.trim())) return "—";
    Object.keys(USER_FACING_VALUE_LABELS)
      .sort(function (left, right) { return right.length - left.length; })
      .forEach(function (code) {
        text = text.replace(
          new RegExp("\\b" + code + "\\b", "gi"),
          USER_FACING_VALUE_LABELS[code]
        );
      });
    return text
      .replace(/\bCandidate\b/gi, "候选方案")
      .replace(/\bFinal Plan\b/gi, "最终执行计划")
      .replace(/\bFinal\b/gi, "最终执行计划")
      .replace(/\bResolver\b/gi, "冲突处理")
      .replace(/\bRule Validation\b/gi, "规则校验");
  }

  function userFacingField(value) {
    if (!hasText(value)) return "分析结果";
    var raw = String(value).trim();
    return USER_FACING_FIELD_LABELS[raw] || "分析结果";
  }

  function mappedView(table, value, fallback) {
    var normalized = String(value || "").trim().toUpperCase();
    return table[normalized] || fallback;
  }

  function roleStateView(value) {
    return mappedView(ROLE_STATE_VIEWS, value, ROLE_STATE_VIEWS.UNAVAILABLE);
  }

  function collectionStateView(value) {
    return mappedView(
      COLLECTION_STATE_VIEWS,
      value,
      COLLECTION_STATE_VIEWS.SOURCE_UNAVAILABLE
    );
  }

  function planModeView(value) {
    var normalized = String(value || "").trim().toUpperCase();
    return PLAN_MODE_VIEWS[normalized] || null;
  }

  function planDataStateView(value) {
    return mappedView(
      PLAN_DATA_STATE_VIEWS,
      value,
      PLAN_DATA_STATE_VIEWS.MISSING
    );
  }

  function dataStateView(value) {
    return mappedView(DATA_STATE_VIEWS, value, DATA_STATE_VIEWS.UNAVAILABLE);
  }

  var USER_FACING_SEMANTIC_MAPPER = Object.freeze({
    planMode: planModeView,
    planDataState: planDataStateView,
    roleState: roleStateView,
    collectionState: collectionStateView,
    dataState: dataStateView,
    marketBias: marketBiasHierarchyLabel,
    planModeLabel: planModeLabel,
    opportunityType: opportunityTypeLabel,
    field: userFacingField,
    value: userFacingValue
  });

  function collectionStateLabel(value) {
    return collectionStateView(value).label;
  }

  function normalizeRoleState(value) {
    var normalized = String(value || "UNAVAILABLE").trim().toUpperCase();
    return ROLE_STATES.indexOf(normalized) >= 0 ? normalized : "UNAVAILABLE";
  }

  function reviewResultLabel(reviewResult) {
    var normalized = String(reviewResult || "").trim().toUpperCase();
    return REVIEW_RESULT_LABELS[normalized] || "当前不可查看";
  }

  function failurePathView(failurePathState, failurePaths) {
    var state = String(failurePathState || "").trim().toUpperCase();
    var paths = Array.isArray(failurePaths) ? failurePaths : [];
    var complete = paths.every(function (path) {
      return path && typeof path === "object"
        && hasText(path.triggerCondition)
        && hasText(path.causalPath)
        && hasText(path.invalidatingEvidence);
    });
    if (state === "FOUND") {
      return paths.length > 0 && complete
        ? {
            valid: true,
            state: state,
            label: "已发现可验证失败路径",
            paths: paths.slice(),
            failClosed: false
          }
        : {
            valid: false,
            state: state,
            label: "失败路径数据不完整",
            paths: [],
            failClosed: true
          };
    }
    if (state === "NO_VERIFIABLE_FAILURE_PATH" && paths.length === 0) {
      return {
        valid: true,
        state: state,
        label: "未发现可验证失败路径",
        paths: [],
        failClosed: false
      };
    }
    if (["INSUFFICIENT_DATA", "SOURCE_UNAVAILABLE", "STALE"].indexOf(state) >= 0
        && paths.length === 0) {
      return {
        valid: true,
        state: state,
        label: collectionStateLabel(state),
        paths: [],
        failClosed: false
      };
    }
    return {
      valid: false,
      state: state || "SOURCE_UNAVAILABLE",
      label: paths.length > 0 ? "失败路径状态与内容不一致" : "失败路径当前不可查看",
      paths: [],
      failClosed: true
    };
  }

  function collectionContract(entry) {
    var descriptor = entry && typeof entry === "object"
      ? entry
      : { state: entry, size: 0 };
    var state = String(descriptor.state || "").trim().toUpperCase();
    var size = Number.isInteger(descriptor.size)
      ? descriptor.size
      : Array.isArray(descriptor.items) ? descriptor.items.length : 0;
    if (state === "FOUND") return size > 0;
    if (["NONE_FOUND", "INSUFFICIENT_DATA", "SOURCE_UNAVAILABLE", "STALE"].indexOf(state) >= 0) {
      return size === 0;
    }
    if (state === "NO_VERIFIABLE_FAILURE_PATH") return descriptor.failurePath === true && size === 0;
    return false;
  }

  function roleGate(roleState, resultAvailable, collectionStates) {
    var state = normalizeRoleState(roleState);
    if (resultAvailable !== true) {
      return Object.freeze({
        allowed: false,
        renderMode: "FAIL_CLOSED",
        roleState: state,
        message: "角色结果当前不可查看"
      });
    }
    if (["FALLBACK", "UNAVAILABLE", "ERROR"].indexOf(state) >= 0) {
      return Object.freeze({
        allowed: false,
        renderMode: "FAIL_CLOSED",
        roleState: state,
        message: roleStateView(state).label
      });
    }
    var collections = Array.isArray(collectionStates) ? collectionStates : [];
    if (!collections.every(collectionContract)) {
      return Object.freeze({
        allowed: false,
        renderMode: "FAIL_CLOSED",
        roleState: state,
        message: "集合状态与内容不一致"
      });
    }
    if (state === "PARTIAL") {
      return Object.freeze({
        allowed: true,
        renderMode: "PARTIAL",
        roleState: state,
        message: roleStateView(state).label
      });
    }
    if (state === "READY") {
      return Object.freeze({
        allowed: true,
        renderMode: "READY",
        roleState: state,
        message: roleStateView(state).label
      });
    }
    return Object.freeze({
      allowed: false,
      renderMode: "FAIL_CLOSED",
      roleState: state,
      message: "角色状态当前不可查看"
    });
  }

  function analysisModeGate(analysisMode) {
    var mode = String(analysisMode || "").trim().toUpperCase();
    if (ANALYSIS_MODES.indexOf(mode) < 0) {
      return Object.freeze({
        valid: false,
        mode: null,
        label: "分析模式当前不可查看",
        message: "缺少可验证的正式分析模式",
        candidateAllowed: false,
        candidateReviewAllowed: false,
        opportunityFailurePathsAllowed: false
      });
    }
    return Object.freeze({
      valid: true,
      mode: mode,
      label: mode === "ANALYSIS_PREVIEW" ? "按需分析预览" : "机会决策",
      message: mode === "ANALYSIS_PREVIEW"
        ? "按需查看当前资产的分析结果。"
        : "查看当前机会的完整决策结果。",
      candidateAllowed: mode === "OPPORTUNITY_DECISION",
      candidateReviewAllowed: mode === "OPPORTUNITY_DECISION",
      opportunityFailurePathsAllowed: mode === "OPPORTUNITY_DECISION"
    });
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
      label: "状态待同步",
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
      if (!supplied) {
        supplied = {
          role: definition.role,
          roleLabel: definition.label,
          resultAvailable: false,
          roleState: "UNAVAILABLE",
          dataState: "SOURCE_UNAVAILABLE",
          runStatusLabel: "待同步",
          statusMessage: "当前角色观点待同步"
        };
      }
      var normalized = {};
      Object.keys(supplied).forEach(function (key) {
        normalized[key] = supplied[key];
      });
      normalized.role = definition.role;
      normalized.roleLabel = displayText(normalized.roleLabel, definition.label);
      normalized.roleState = normalizeRoleState(normalized.roleState);
      normalized.dataState = String(normalized.dataState || "SOURCE_UNAVAILABLE").toUpperCase();
      normalized.resultAvailable = supplied.resultAvailable === true;
      normalized.runStatusLabel = displayText(normalized.runStatusLabel, "待同步");
      normalized.statusMessage = displayText(
        normalized.statusMessage,
        normalized.resultAvailable ? "" : "当前角色观点不可用"
      );
      STRUCTURED_AI_COLLECTIONS.forEach(function (collection) {
        normalized[collection.key] = Array.isArray(normalized[collection.key])
          ? normalized[collection.key] : [];
        normalized[collection.state] = String(
          normalized[collection.state] || "SOURCE_UNAVAILABLE"
        ).toUpperCase();
      });
      return normalized;
    });
  }

  function aiAnalysisState(identityReady, failed, loading, empty) {
    if (failed) return "error";
    if (loading) return "loading";
    if (empty) return "empty";
    if (!identityReady) return "missing";
    return "partial";
  }

  function aiAnalysisStateView(state) {
    var normalized = String(state || "").trim().toLowerCase();
    return AI_ANALYSIS_STATE_VIEWS[normalized] || AI_ANALYSIS_STATE_VIEWS.missing;
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
        reason: "系统建议"
      };
    }

    if (status !== "USABLE_REVIEW_PLAN") {
      return {
        visible: false,
        statusLabel: displayText(plan.statusLabel, "当前暂无可验证的执行建议"),
        reason: displayText(plan.blockedReason, "执行建议不可用")
      };
    }

    var finalContractValid = plan.finalPlan === true
      && String(plan.validationStatus || "").toUpperCase() === "PASS"
      && String(plan.chainStatus || "").toUpperCase() === "FINAL_VALIDATED"
      && String(plan.sourceStatus || "").toUpperCase() === "VALID"
      && plan.notTradeInstruction === true;
    if (!finalContractValid) {
      return {
        visible: false,
        statusLabel: "当前 Final Plan 不可验证",
        reason: "Rule Validation、来源或非交易指令合同未通过"
      };
    }

    return {
      visible: true,
      statusLabel: displayText(plan.statusLabel, "执行建议"),
      reason: "系统建议"
    };
  }

  function positionSourceLabel(sourceType) {
    var source = String(sourceType || "").trim().toUpperCase();
    if (source === "SYSTEM_PLAN_POSITION") return "系统计划";
    if (source === "MANUAL_POSITION" || source === "MANUAL_INDEPENDENT" || source === "MANUAL") {
      return "独立录入";
    }
    return "来源不可用";
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

  function formatBusinessTimeCompact(value) {
    var raw = String(value || "").trim();
    var match = raw.match(/^\d{4}-(\d{2}-\d{2})[T ](\d{2}:\d{2})/);
    return match ? match[1] + " " + match[2] : displayText(value, "--");
  }

  global.TradeModelFrontendContract = Object.freeze({
    AI_ROLES: AI_ROLES,
    ASSET_STATES: ASSET_STATES,
    AI_ANALYSIS_STATE_VIEWS: AI_ANALYSIS_STATE_VIEWS,
    MODULE_STATES: MODULE_STATES,
    FIELD_SOURCE_VIEWS: FIELD_SOURCE_VIEWS,
    DATA_QUALITY_LABELS: DATA_QUALITY_LABELS,
    MARKET_BIAS_HIERARCHY_LABELS: MARKET_BIAS_HIERARCHY_LABELS,
    PLAN_MODE_LABELS: PLAN_MODE_LABELS,
    PLAN_MODE_VIEWS: PLAN_MODE_VIEWS,
    OPPORTUNITY_TYPE_LABELS: OPPORTUNITY_TYPE_LABELS,
    USER_FACING_FIELD_LABELS: USER_FACING_FIELD_LABELS,
    COLLECTION_STATE_LABELS: COLLECTION_STATE_LABELS,
    ROLE_STATE_VIEWS: ROLE_STATE_VIEWS,
    COLLECTION_STATE_VIEWS: COLLECTION_STATE_VIEWS,
    PLAN_DATA_STATE_VIEWS: PLAN_DATA_STATE_VIEWS,
    DATA_STATE_VIEWS: DATA_STATE_VIEWS,
    USER_FACING_SEMANTIC_MAPPER: USER_FACING_SEMANTIC_MAPPER,
    ROLE_STATES: ROLE_STATES,
    hasText: hasText,
    displayText: displayText,
    displayNumber: displayNumber,
    normalizeModuleState: normalizeModuleState,
    fieldSourceView: fieldSourceView,
    dataQualityLabel: dataQualityLabel,
    marketBiasHierarchyLabel: marketBiasHierarchyLabel,
    planModeLabel: planModeLabel,
    opportunityTypeLabel: opportunityTypeLabel,
    userFacingField: userFacingField,
    userFacingValue: userFacingValue,
    planModeView: planModeView,
    planDataStateView: planDataStateView,
    roleStateView: roleStateView,
    collectionStateView: collectionStateView,
    dataStateView: dataStateView,
    collectionStateLabel: collectionStateLabel,
    normalizeRoleState: normalizeRoleState,
    reviewResultLabel: reviewResultLabel,
    failurePathView: failurePathView,
    roleGate: roleGate,
    analysisModeGate: analysisModeGate,
    parseApiEnvelope: parseApiEnvelope,
    assetStateView: assetStateView,
    normalizeAiTabs: normalizeAiTabs,
    aiAnalysisState: aiAnalysisState,
    aiAnalysisStateView: aiAnalysisStateView,
    executionPlanAccess: executionPlanAccess,
    positionSourceLabel: positionSourceLabel,
    csrfHeaders: csrfHeaders,
    clearTextFields: clearTextFields,
    readUrlParam: readUrlParam,
    replaceUrlParam: replaceUrlParam,
    formatUtcNaive: formatUtcNaive,
    formatBusinessTimeCompact: formatBusinessTimeCompact
  });
})(window);
