package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.example.trademodel.analysisrun.AnalysisExecutionContext;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.support.AccountRiskPlanPolicy;

import java.util.List;

/**
 * 权威分析主链落库后的 Push 快照写入（不含 Recheck 计算）。
 */
@Service
public class PushSnapshotService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SNAPSHOT_SOURCE_V3 = "USER_POSITION_RISK_ADAPTER_V41";
    private static final int SNAPSHOT_VERSION_V3 = 3;

    private final PushSnapshotMapper pushSnapshotMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private FundamentalAiV41Properties v41Properties = FundamentalAiV41Properties.contractFixture();
    private Clock clock = Clock.systemUTC();

    public PushSnapshotService(PushSnapshotMapper pushSnapshotMapper,
                               AccountRiskSnapshotMapper accountRiskSnapshotMapper) {
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Autowired(required = false)
    public void setFundamentalAiV41Properties(FundamentalAiV41Properties properties) {
        if (properties != null) this.v41Properties = properties;
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

        LocalDateTime now = UtcLocalTimePolicy.now(clock);
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
        if (accountRiskSnapshotId == null || !Boolean.TRUE.equals(plan.getFinalPlan())) {
            return;
        }
        Long riskSnapshotId = accountRiskSnapshotId;
        row.setAccountRiskSnapshotId(riskSnapshotId);
        row.setExpiresAt(resolvePushExpiry(decision));

        pushSnapshotMapper.insert(row);
    }

    public TmAccountRiskSnapshotDO prepareDecisionAccountRiskSnapshot(
            AnalysisExecutionContext context,
            String riskLevel,
            UserPositionRiskResult result,
            boolean persist) {
        if (context == null) return null;
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        TmAccountRiskSnapshotDO risk = new TmAccountRiskSnapshotDO();
        risk.setAnalysisId(context.getAnalysisId());
        risk.setSymbol(context.getSymbol());
        risk.setOwnerType(context.getOwnerType());
        risk.setOwnerId(context.getOwnerId());
        risk.setTraceId(context.getTraceId());
        risk.setMaxAllowedExposure(v41Properties.getAccountRisk().maxExposureFor(riskLevel));
        risk.setMaxAllowedLeverage(v41Properties.getAccountRisk().getMaxLeverage());
        risk.setSnapshotSource(SNAPSHOT_SOURCE_V3);
        risk.setSnapshotVersion(SNAPSHOT_VERSION_V3);
        risk.setCreateTime(now);
        if (result == null) {
            risk.setSourceStatus("INVALID");
            risk.setAccountRiskStatus("UNAVAILABLE");
            risk.setRiskLevelSnapshot(null);
            risk.setRiskAllowed(false);
            risk.setRiskReasonCode("ACCOUNT_RISK_SOURCE_UNAVAILABLE");
            risk.setRiskReasonText("UserPositionRiskAdapter did not provide a trustworthy account-risk result");
            risk.setSourceNote("FAIL_CLOSED_NO_ACCOUNT_RISK_SOURCE");
        } else {
            risk.setSourceStatus("VERIFIED");
            risk.setAccountRiskStatus(result.getRiskStatus());
            risk.setRiskLevelSnapshot(result.getRiskLevel());
            risk.setRiskAllowed(!result.isRiskBlocked());
            List<String> reasons = result.getReasonCodes();
            risk.setRiskReasonCode(reasons.isEmpty()
                    ? (result.isRiskBlocked() ? "ACCOUNT_RISK_BLOCKED" : "ACCOUNT_RISK_ALLOWED")
                    : reasons.get(0));
            risk.setRiskReasonText(reasons.isEmpty() ? result.getRiskStatus() : String.join(";", reasons));
            risk.setGrossNotional(result.getGrossNotional());
            risk.setLeverageRisk(result.getLeverageRisk());
            risk.setPositionSizeRisk(result.getPositionSizeRisk());
            risk.setConcentrationRisk(result.getConcentrationRisk());
            risk.setCorrelationRisk(result.getCorrelationRisk());
            risk.setDrawdownOrVarRisk(result.getDrawdownOrVarRisk());
            risk.setAggregateRiskScore(result.getAggregateRiskScore());
            risk.setObservedAt(now);
            risk.setFreshUntil(now.plusSeconds(v41Properties.getAccountRisk().getFreshnessSeconds()));
            risk.setSourceNote("VERIFIED_READ_ONLY_USER_POSITION_RISK");
        }
        if (persist) accountRiskSnapshotMapper.insert(risk);
        return risk;
    }

    public AccountRiskPlanPolicy.Assessment assessCandidate(
            TmAccountRiskSnapshotDO snapshot,
            ExecutionPlanCandidateDO candidate,
            String finalRiskLevel,
            boolean persist) {
        AccountRiskPlanPolicy.Assessment assessment = AccountRiskPlanPolicy.assess(
                snapshot, candidate, finalRiskLevel, v41Properties.getAccountRisk(), UtcLocalTimePolicy.now(clock));
        if (snapshot != null) {
            snapshot.setRiskAllowed(assessment.allowed());
            snapshot.setRiskReasonCode(assessment.reasonCode());
            snapshot.setRiskReasonText(assessment.reasonText());
            snapshot.setPositionExposure(assessment.positionExposure());
            snapshot.setCandidateLeverage(assessment.candidateLeverage());
            snapshot.setMaxAllowedExposure(assessment.maxAllowedExposure());
            snapshot.setMaxAllowedLeverage(assessment.maxAllowedLeverage());
            if (!assessment.allowed()) snapshot.setAccountRiskStatus("PLAN_RISK_BLOCKED");
            snapshot.setSourceNote(assessment.allowed()
                    ? "VERIFIED_ACCOUNT_AND_CANDIDATE_LIMITS"
                    : "FAIL_CLOSED_ACCOUNT_OR_CANDIDATE_LIMITS");
            if (persist && snapshot.getId() != null
                    && accountRiskSnapshotMapper.updateCandidateAssessment(snapshot) != 1) {
                throw new IllegalStateException("ACCOUNT_RISK_CANDIDATE_ASSESSMENT_UPDATE_FAILED");
            }
        }
        return assessment;
    }

    public String accountRiskJson(TmAccountRiskSnapshotDO snapshot) {
        if (snapshot == null) return null;
        try {
            return JSON.writeValueAsString(snapshot);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Legacy callers lack owner-scoped risk facts and therefore fail closed. */
    @Deprecated
    public Long ensureAccountRiskSnapshot(AnalysisRunDO run, AssetAnalysisVO analysis,
                                          DecisionBundleVO decision, ExecutionPlanVO plan) {
        if (run == null || analysis == null) return null;
        TmAccountRiskSnapshotDO risk = new TmAccountRiskSnapshotDO();
        risk.setAnalysisId(analysis.getAnalysisId());
        risk.setSymbol(analysis.getSymbol());
        risk.setOwnerType(run.getOwnerType() == null ? "SYSTEM" : run.getOwnerType());
        risk.setOwnerId(run.getOwnerId() == null ? 0L : run.getOwnerId());
        risk.setRiskAllowed(false);
        risk.setSourceStatus("INVALID");
        risk.setAccountRiskStatus("UNAVAILABLE");
        risk.setRiskReasonCode("LEGACY_ACCOUNT_RISK_CONTEXT_INSUFFICIENT");
        risk.setRiskReasonText("Legacy snapshot creation has no owner-scoped UserPositionRiskResult");
        risk.setSnapshotSource(SNAPSHOT_SOURCE_V3);
        risk.setSnapshotVersion(SNAPSHOT_VERSION_V3);
        risk.setSourceNote("FAIL_CLOSED_LEGACY_ENTRY");
        risk.setTraceId(run.getTraceId());
        risk.setCreateTime(UtcLocalTimePolicy.now(clock));
        accountRiskSnapshotMapper.insert(risk);
        return risk.getId();
    }

    private static LocalDateTime resolvePushExpiry(DecisionBundleVO decision) {
        LocalDateTime authoritative = UtcLocalTimePolicy.fromOffsetDateTime(decision.getExpiresAt());
        LocalDateTime compatibility = decision.getPushExpiresAt();
        if (authoritative != null && compatibility != null && !authoritative.equals(compatibility)) {
            throw new IllegalStateException("push expiry UTC compatibility timestamp is inconsistent");
        }
        return authoritative != null ? authoritative : compatibility;
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
