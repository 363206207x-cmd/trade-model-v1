package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.service.MonitorAlertWriteService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class MonitorAlertWriteServiceImpl implements MonitorAlertWriteService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与决策引擎 {@code riskLevelLabel == "HIGH"} 对齐。 */
    public static final String ALERT_TYPE_HIGH_RISK_DECISION = "HIGH_RISK_DECISION";

    /** 与 AnalysisAssemblerServiceImpl#estimateDataQualityScore 档位一致：低于 60 视为不足。 */
    public static final String ALERT_TYPE_DATA_QUALITY_INSUFFICIENT = "DATA_QUALITY_INSUFFICIENT";

    /**
     * 与 {@link org.example.trademodel.service.impl.AiConflictResolverServiceImpl} 档位一致：
     * LEVEL_3 / LEVEL_4，或 aiConflictScore ≥ 46（显著/极端分歧带）。
     */
    public static final String ALERT_TYPE_AI_CONFLICT_ELEVATED = "AI_CONFLICT_ELEVATED";

    /** 与 {@link DecisionBundleVO#getMultiTfConvergence()} 的 WEAK 标签一致（本引擎写 STRONG/WEAK）。 */
    public static final String ALERT_TYPE_MULTI_TF_WEAK = "MULTI_TF_WEAK";
    /** 同一 analysis 内：开仓被高冲突阻断。 */
    public static final String ALERT_TYPE_OPEN_BLOCKED_BY_CONFLICT = "OPEN_BLOCKED_BY_CONFLICT";
    /** 同一 analysis 内：冲突升高且多周期弱收敛。 */
    public static final String ALERT_TYPE_CONFLUENCE_BREAKDOWN = "CONFLUENCE_BREAKDOWN";

    private static final int DATA_QUALITY_THRESHOLD = 60;

    /** 与 AiConflictResolverServiceImpl：LEVEL_3 下界一致（显著分歧及以上才告警）。 */
    private static final int AI_CONFLICT_SCORE_ELEVATED_MIN = 46;

    /** 与 PROJECT_SPEC / tm_user_config.cooldown_minutes 默认 15 对齐的最小 DB 节流窗（分钟）。 */
    static final int DEFAULT_ALERT_COOLDOWN_MINUTES = 15;
    /** 在 DB 窗口节流之外，再补一层同类语义近似抑制。 */
    static final int DEFAULT_SEMANTIC_SUPPRESS_WINDOW_MINUTES = 45;

    private final MonitorAlertMapper monitorAlertMapper;

    public MonitorAlertWriteServiceImpl(MonitorAlertMapper monitorAlertMapper) {
        this.monitorAlertMapper = monitorAlertMapper;
    }

    @Override
    public void emitAfterAnalysisPersist(AnalysisRunDO run, AssetAnalysisVO analysis, DecisionBundleVO decision) {
        if (run == null || analysis == null || analysis.getAnalysisId() == null) {
            return;
        }
        String analysisId = analysis.getAnalysisId();
        String symbol = analysis.getSymbol() != null ? analysis.getSymbol() : "";
        String traceId = run.getTraceId() != null ? run.getTraceId() : "";
        String ruleVersion = run.getRuleVersion() != null ? run.getRuleVersion() : "v1.0";

        if (decision != null && isHighRisk(decision)) {
            tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_HIGH_RISK_DECISION, "HIGH",
                    String.format("高风险决策：riskLevel=HIGH（analysisId=%s, symbol=%s）", analysisId, symbol),
                    traceId, ruleVersion);
        }

        Integer dqs = analysis.getDataQualityScore();
        if (dqs != null && dqs < DATA_QUALITY_THRESHOLD) {
            tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_DATA_QUALITY_INSUFFICIENT, "WARN",
                    String.format("数据质量不足：dataQualityScore=%d（低于阈值 %d，analysisId=%s, symbol=%s）",
                            dqs, DATA_QUALITY_THRESHOLD, analysisId, symbol),
                    traceId, ruleVersion);
        }

        if (decision != null) {
            boolean aiConflictElevated = isAiConflictElevated(decision);
            boolean multiTfWeak = isMultiTfWeak(decision);
            boolean openBlocked = isOpenBlockedByConflict(decision, aiConflictElevated);
            boolean confluenceBreakdown = isConfluenceBreakdown(aiConflictElevated, multiTfWeak);

            // 方案 A：同一次 analysis 两条新规则同时满足时，仅保留更高优先级的 CONFLUENCE_BREAKDOWN。
            boolean conflictFamilyEmitted = false;
            if (confluenceBreakdown) {
                int score = decision.getAiConflictScore() != null ? decision.getAiConflictScore() : 0;
                String lvl = decision.getAiConflictLevel() != null ? decision.getAiConflictLevel() : "";
                tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_CONFLUENCE_BREAKDOWN, "HIGH",
                        String.format("收敛破裂：冲突升高且多周期弱收敛（冲突家族优先保留 CONFLUENCE_BREAKDOWN，不重复写 OPEN_BLOCKED_BY_CONFLICT；aiConflictLevel=%s, aiConflictScore=%d, multiTfConvergence=WEAK, analysisId=%s, symbol=%s）",
                                lvl, score, analysisId, symbol),
                        traceId, ruleVersion);
                conflictFamilyEmitted = true;
            } else if (openBlocked) {
                int score = decision.getAiConflictScore() != null ? decision.getAiConflictScore() : 0;
                String lvl = decision.getAiConflictLevel() != null ? decision.getAiConflictLevel() : "";
                tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_OPEN_BLOCKED_BY_CONFLICT, "WARN",
                        String.format("开仓被冲突阻断：isWorthOpening=false 且冲突升高（aiConflictLevel=%s, aiConflictScore=%d, analysisId=%s, symbol=%s）",
                                lvl, score, analysisId, symbol),
                        traceId, ruleVersion);
                conflictFamilyEmitted = true;
            }

            // 若新规则未命中，再保留历史单项规则，避免信息断层。
            if (!conflictFamilyEmitted && aiConflictElevated) {
                int score = decision.getAiConflictScore() != null ? decision.getAiConflictScore() : 0;
                String lvl = decision.getAiConflictLevel() != null ? decision.getAiConflictLevel() : "";
                tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_AI_CONFLICT_ELEVATED, "WARN",
                        String.format("多模型冲突升高：aiConflictLevel=%s, aiConflictScore=%d（analysisId=%s, symbol=%s）",
                                lvl, score, analysisId, symbol),
                        traceId, ruleVersion);
            }
            if (!conflictFamilyEmitted && multiTfWeak) {
                tryEmitOpenOrSuppressed(analysisId, symbol, ALERT_TYPE_MULTI_TF_WEAK, "WARN",
                        String.format("多周期收敛弱：multiTfConvergence=WEAK（analysisId=%s, symbol=%s）",
                                analysisId, symbol),
                        traceId, ruleVersion);
            }
        }
    }

    private static boolean isHighRisk(DecisionBundleVO decision) {
        String r = decision.getRiskLevel();
        return r != null && "HIGH".equalsIgnoreCase(r.trim());
    }

    private static boolean isAiConflictElevated(DecisionBundleVO decision) {
        String level = decision.getAiConflictLevel();
        if (level != null) {
            String u = level.toUpperCase();
            if (u.contains("LEVEL_3") || u.contains("LEVEL_4")) {
                return true;
            }
        }
        Integer s = decision.getAiConflictScore();
        return s != null && s >= AI_CONFLICT_SCORE_ELEVATED_MIN;
    }

    private static boolean isMultiTfWeak(DecisionBundleVO decision) {
        String m = decision.getMultiTfConvergence();
        return m != null && "WEAK".equalsIgnoreCase(m.trim());
    }

    private static boolean isOpenBlockedByConflict(DecisionBundleVO decision, boolean aiConflictElevated) {
        Boolean worthOpening = decision.getIsWorthOpening();
        return Boolean.FALSE.equals(worthOpening) && aiConflictElevated;
    }

    private static boolean isConfluenceBreakdown(boolean aiConflictElevated, boolean multiTfWeak) {
        return aiConflictElevated && multiTfWeak;
    }

    /**
     * 先按 analysisId+alertType 去重；再按 asset_symbol+alertType+时间窗对 OPEN 节流：
     * 可写 OPEN（带 cooldown_until）或写 SUPPRESSED（suppress_reason 说明原因）。
     */
    private void tryEmitOpenOrSuppressed(String analysisId, String symbol, String alertType, String alertLevel,
                                         String message, String traceId, String ruleVersion) {
        if (monitorAlertMapper.countByAnalysisIdAndAlertType(analysisId, alertType) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStartTime = now.minusMinutes(DEFAULT_ALERT_COOLDOWN_MINUTES);
        boolean windowHasOpen = monitorAlertMapper.countOpenInThrottleWindow(
                symbol, alertType, windowStartTime) > 0;

        if (windowHasOpen) {
            MonitorAlertDO suppressed = baseRow(analysisId, symbol, alertType, alertLevel, message, traceId, ruleVersion);
            suppressed.setStatus("SUPPRESSED");
            suppressed.setSuppressReason(String.format(
                    "THROTTLE_DB:%dm window: OPEN already exists for same asset_symbol+alert_type; skip duplicate OPEN",
                    DEFAULT_ALERT_COOLDOWN_MINUTES));
            suppressed.setCooldownUntil(null);
            monitorAlertMapper.insert(suppressed);
            return;
        }

        LocalDateTime semanticWindowStartTime = now.minusMinutes(DEFAULT_SEMANTIC_SUPPRESS_WINDOW_MINUTES);
        boolean semanticSimilarRecent = monitorAlertMapper.countAnyInSemanticWindow(
                symbol, alertType, semanticWindowStartTime) > 0;
        if (semanticSimilarRecent) {
            MonitorAlertDO suppressed = baseRow(analysisId, symbol, alertType, alertLevel, message, traceId, ruleVersion);
            suppressed.setStatus("SUPPRESSED");
            suppressed.setSuppressReason(String.format(
                    "SEMANTIC_SIMILAR_RECENT:%dm window: similar alert_type already emitted for same asset_symbol",
                    DEFAULT_SEMANTIC_SUPPRESS_WINDOW_MINUTES));
            suppressed.setCooldownUntil(null);
            monitorAlertMapper.insert(suppressed);
            return;
        }

        MonitorAlertDO open = baseRow(analysisId, symbol, alertType, alertLevel, message, traceId, ruleVersion);
        open.setStatus("OPEN");
        open.setSuppressReason(null);
        open.setCooldownUntil(now.plusMinutes(DEFAULT_ALERT_COOLDOWN_MINUTES).format(TS));
        monitorAlertMapper.insert(open);
    }

    private static MonitorAlertDO baseRow(String analysisId, String symbol, String alertType, String alertLevel,
                                          String message, String traceId, String ruleVersion) {
        MonitorAlertDO row = new MonitorAlertDO();
        row.setId("mal-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        row.setAnalysisId(analysisId);
        row.setAssetSymbol(symbol);
        row.setAlertType(alertType);
        row.setAlertLevel(alertLevel);
        row.setAlertMessage(message);
        row.setTraceId(traceId);
        row.setRuleVersion(ruleVersion);
        row.setCreatedBy("system");
        row.setIsDeleted(0);
        row.setVersionNo(1);
        return row;
    }
}
