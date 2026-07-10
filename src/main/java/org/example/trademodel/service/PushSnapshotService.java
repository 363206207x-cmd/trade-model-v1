package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 权威分析主链落库后的 Push 快照写入（不含 Recheck 计算）。
 */
@Service
public class PushSnapshotService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final BigDecimal MAX_EXPOSURE_HIGH = new BigDecimal("0.10");
    private static final BigDecimal MAX_EXPOSURE_MEDIUM = new BigDecimal("0.20");
    private static final BigDecimal MAX_EXPOSURE_LOW = new BigDecimal("0.30");
    private static final String SNAPSHOT_SOURCE_V2 = "ROUND2_MINIMAL_DECISION_PLUS_PLAN_EXPOSURE";
    private static final int SNAPSHOT_VERSION_V2 = 2;

    private final PushSnapshotMapper pushSnapshotMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;

    public PushSnapshotService(PushSnapshotMapper pushSnapshotMapper,
                               AccountRiskSnapshotMapper accountRiskSnapshotMapper) {
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
    }

    /**
     * 在 {@link org.example.trademodel.service.impl.AnalysisAssemblerServiceImpl#saveToDatabase} 事务内调用；
     * 仅当决策认为值得开仓且已有执行计划时写入一条快照。
     */
    public void insertAuthoritativeSnapshot(AnalysisRunDO run, AssetAnalysisVO analysis,
                                           DecisionBundleVO decision, ExecutionPlanVO plan) {
        insertAuthoritativeSnapshot(run, analysis, decision, plan, null);
    }

    public void insertAuthoritativeSnapshot(AnalysisRunDO run, AssetAnalysisVO analysis,
                                            DecisionBundleVO decision, ExecutionPlanVO plan,
                                            Long accountRiskSnapshotId) {
        if (run == null || analysis == null || decision == null || plan == null) {
            return;
        }
        if (!Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            return;
        }
        if (decision.isDirectionalPushBlocked()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setAnalysisId(analysis.getAnalysisId());
        row.setSymbol(analysis.getSymbol());
        row.setTimeframe(analysis.getTimeframe());
        row.setPushType(decision.getDerivativesPushMode() != null
                && !"NONE".equals(decision.getDerivativesPushMode())
                ? decision.getDerivativesPushMode() : "ANALYSIS_RUN");
        row.setPushStatus("CAPTURED");
        row.setPushCreateTime(now);
        row.setRuleVersion(run.getRuleVersion());
        row.setTraceId(run.getTraceId());
        row.setCreateTime(now);

        row.setPlanModeSnapshot(decision.getAiPlanMode());
        row.setDataQualityScoreSnapshot(analysis.getDataQualityScore());
        row.setConfusedScoreSnapshot(decision.getConfusedScore());

        row.setEntryZoneJson(wrapOptionalText(plan.getEntryZone()));
        row.setStopZoneJson(wrapOptionalText(plan.getStopLoss()));
        row.setInvalidationConditionJson(buildInvalidationConditionJson(decision, plan));

        row.setTriggerPrice(decision.getPushTriggerPrice());
        row.setCauseEffectAlignmentSnapshot(null);
        row.setExecutionFeasibilitySnapshot(null);
        Long riskSnapshotId = accountRiskSnapshotId != null
                ? accountRiskSnapshotId
                : ensureAccountRiskSnapshot(run, analysis, decision, plan);
        row.setAccountRiskSnapshotId(riskSnapshotId);
        row.setExpiresAt(decision.getPushExpiresAt());

        pushSnapshotMapper.insert(row);
    }

    /**
     * 为同一 analysis 的 execution / push 写链提供统一的 account risk snapshot 真值来源。
     * 与 push 是否写入无关：只要上下文齐备（run/analysis/decision/plan），即可生成一条快照。
     */
    public Long ensureAccountRiskSnapshot(AnalysisRunDO run, AssetAnalysisVO analysis,
                                          DecisionBundleVO decision, ExecutionPlanVO plan) {
        if (run == null || analysis == null || decision == null || plan == null) {
            return null;
        }
        TmAccountRiskSnapshotDO risk = new TmAccountRiskSnapshotDO();
        risk.setAnalysisId(analysis.getAnalysisId());
        risk.setSymbol(analysis.getSymbol());
        risk.setRiskLevelSnapshot(decision.getRiskLevel());
        BigDecimal positionExposure = parsePositionExposure(plan);
        BigDecimal maxAllowedExposure = resolveMaxAllowedExposure(decision.getRiskLevel());
        Round2RiskJudgement judgement = judgeRisk(Boolean.TRUE.equals(decision.getIsWorthOpening()),
                positionExposure, maxAllowedExposure);
        risk.setRiskAllowed(judgement.riskAllowed);
        risk.setRiskReasonCode(judgement.reasonCode);
        risk.setRiskReasonText(judgement.reasonText);
        risk.setPositionExposure(positionExposure);
        risk.setMaxAllowedExposure(maxAllowedExposure);
        risk.setSnapshotSource(SNAPSHOT_SOURCE_V2);
        risk.setSnapshotVersion(SNAPSHOT_VERSION_V2);
        risk.setSourceNote("ROUND2_MINIMAL_TRUTH_UPGRADED");
        risk.setTraceId(run.getTraceId());
        risk.setCreateTime(LocalDateTime.now());
        accountRiskSnapshotMapper.insert(risk);
        return risk.getId();
    }

    private static Round2RiskJudgement judgeRisk(boolean isWorthOpening,
                                                 BigDecimal positionExposure,
                                                 BigDecimal maxAllowedExposure) {
        if (!isWorthOpening) {
            return new Round2RiskJudgement(false, "DECISION_NOT_WORTH_OPENING",
                    "decision.isWorthOpening=false");
        }
        if (positionExposure == null) {
            return new Round2RiskJudgement(true, "EXPOSURE_UNKNOWN_FALLBACK",
                    "positionExposure missing, fallback allowed under worth-opening decision");
        }
        if (maxAllowedExposure != null && positionExposure.compareTo(maxAllowedExposure) > 0) {
            return new Round2RiskJudgement(false, "EXPOSURE_LIMIT_EXCEEDED",
                    "positionExposure=" + positionExposure + " > maxAllowedExposure=" + maxAllowedExposure);
        }
        return new Round2RiskJudgement(true, "ACCOUNT_RISK_ALLOWED",
                "positionExposure=" + positionExposure + " <= maxAllowedExposure=" + maxAllowedExposure);
    }

    private static BigDecimal resolveMaxAllowedExposure(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return MAX_EXPOSURE_MEDIUM;
        }
        String normalized = riskLevel.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("HIGH")) {
            return MAX_EXPOSURE_HIGH;
        }
        if (normalized.contains("LOW")) {
            return MAX_EXPOSURE_LOW;
        }
        return MAX_EXPOSURE_MEDIUM;
    }

    private static BigDecimal parsePositionExposure(ExecutionPlanVO plan) {
        if (plan == null || plan.getPositionSuggestion() == null || plan.getPositionSuggestion().isBlank()) {
            return null;
        }
        String raw = plan.getPositionSuggestion().trim();
        Matcher matcher = FIRST_NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(matcher.group());
            if (raw.contains("%")) {
                return parsed.movePointLeft(2);
            }
            if (parsed.compareTo(BigDecimal.ONE) > 0) {
                return parsed.movePointLeft(2);
            }
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class Round2RiskJudgement {
        private final boolean riskAllowed;
        private final String reasonCode;
        private final String reasonText;

        private Round2RiskJudgement(boolean riskAllowed, String reasonCode, String reasonText) {
            this.riskAllowed = riskAllowed;
            this.reasonCode = reasonCode;
            this.reasonText = reasonText;
        }
    }

    /**
     * 最小可执行 schema（与 {@link org.example.trademodel.service.impl.PushRecheckServiceImpl} 一致）：
     * {@code text} 供展示；{@code invalidPriceBelow} / {@code invalidPriceAbove} 供数值判失效。
     */
    private static String buildInvalidationConditionJson(DecisionBundleVO decision, ExecutionPlanVO plan) {
        String text = decision.getPushInvalidationSummary();
        if (text == null || text.isBlank()) {
            text = plan.getInvalidCondition();
        }
        if (text != null && text.isBlank()) {
            text = null;
        }
        BigDecimal below = decision.getPushInvalidPriceBelow();
        BigDecimal above = decision.getPushInvalidPriceAbove();
        boolean hasStruct = below != null || above != null;
        boolean hasDerivatives = decision.getDerivativesRequired() != null
                || decision.getDerivativesStatus() != null
                || decision.getDerivativesFreshness() != null;
        if (text == null && !hasStruct && !hasDerivatives) {
            return null;
        }
        try {
            ObjectNode n = JSON.createObjectNode();
            if (text != null) {
                n.put("text", text.trim());
            }
            if (below != null) {
                n.put("invalidPriceBelow", below);
            }
            if (above != null) {
                n.put("invalidPriceAbove", above);
            }
            if (decision.getDerivativesRequired() != null) {
                n.put("derivativesRequired", decision.getDerivativesRequired());
            }
            if (decision.getDerivativesStatus() != null) {
                n.put("derivativesStatus", decision.getDerivativesStatus());
            }
            if (decision.getDerivativesFreshness() != null) {
                n.put("derivativesFreshness", decision.getDerivativesFreshness());
            }
            if (decision.getDerivativesProviderDataTime() != null) {
                n.put("derivativesProviderDataTime", decision.getDerivativesProviderDataTime().toString());
            }
            if (decision.getDerivativesTraceId() != null) {
                n.put("derivativesTraceId", decision.getDerivativesTraceId());
            }
            if (decision.getDerivativesReasonCodes() != null) {
                n.putPOJO("derivativesReasonCodes", decision.getDerivativesReasonCodes());
            }
            return JSON.writeValueAsString(n);
        } catch (Exception e) {
            return null;
        }
    }

    private static String wrapOptionalText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            ObjectNode n = JSON.createObjectNode();
            n.put("text", raw.trim());
            return JSON.writeValueAsString(n);
        } catch (Exception e) {
            return null;
        }
    }
}
