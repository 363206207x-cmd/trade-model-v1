package org.example.trademodel.positionmonitor;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract.SourceReference;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract.SourceType;

import java.util.Locale;

/** Resolves only exact or unique plan sources; it never falls back to a latest sibling plan. */
public final class PositionPlanSourceResolver {
    private final ExecutionPlanMapper executionPlanMapper;
    private final AnalysisRunMapper analysisRunMapper;

    public PositionPlanSourceResolver(ExecutionPlanMapper executionPlanMapper,
                                      AnalysisRunMapper analysisRunMapper) {
        this.executionPlanMapper = executionPlanMapper;
        this.analysisRunMapper = analysisRunMapper;
    }

    public Resolution resolveTypedReference(Long positionId,
                                            String positionSymbol,
                                            String typedSourceRefId) {
        SourceReference reference = PositionMonitorSourceContract.parse(typedSourceRefId);
        if (reference == null) {
            return Resolution.unverified("TYPED_SOURCE_REFERENCE_REQUIRED");
        }
        return reference.type() == SourceType.EXECUTION_PLAN
                ? resolve(positionId, positionSymbol, reference.id(), null)
                : resolve(positionId, positionSymbol, null, reference.id());
    }

    public Resolution resolveTrustedMonitorSource(Long positionId,
                                                  String positionSymbol,
                                                  String positionTypedSourceRefId,
                                                  String monitorAnalysisId,
                                                  String monitorExecutionPlanId) {
        SourceReference positionSource = PositionMonitorSourceContract.parse(positionTypedSourceRefId);
        if (positionSource == null) {
            return Resolution.unverified("TYPED_SOURCE_REFERENCE_REQUIRED");
        }
        String analysisId = trimToNull(monitorAnalysisId);
        String executionPlanId = trimToNull(monitorExecutionPlanId);
        if (PositionMonitorSourceContract.isUnverifiedAnalysisId(analysisId)) {
            return Resolution.unverified("MONITOR_SOURCE_UNVERIFIED");
        }
        if (executionPlanId == null || analysisId == null) {
            return Resolution.unverified("MONITOR_EXACT_SOURCE_REQUIRED");
        }
        if (positionSource.type() == SourceType.EXECUTION_PLAN
                && !positionSource.id().equals(executionPlanId)) {
            return Resolution.unverified("POSITION_MONITOR_PLAN_MISMATCH");
        }
        Resolution originalPlan = resolve(positionId, positionSymbol, executionPlanId, null);
        if (!originalPlan.verified()) {
            return originalPlan;
        }
        if (positionSource.type() == SourceType.ANALYSIS
                && !positionSource.id().equals(originalPlan.analysisId())) {
            return Resolution.unverified("POSITION_MONITOR_ANALYSIS_MISMATCH");
        }
        try {
            AnalysisRunDO monitorRun = analysisRunMapper.selectById(analysisId);
            if (monitorRun == null || !analysisId.equals(trimToNull(monitorRun.getAnalysisId()))) {
                return Resolution.unverified("MONITOR_ANALYSIS_RUN_MISSING");
            }
            if (!normalizeSymbol(positionSymbol).equals(normalizeSymbol(monitorRun.getSymbol()))) {
                return Resolution.unverified("POSITION_MONITOR_SYMBOL_MISMATCH");
            }
            return Resolution.verified(originalPlan.executionPlan(), originalPlan.analysisRun(),
                    originalPlan.analysisId(), originalPlan.executionPlanId(),
                    originalPlan.sourceTraceId(), analysisId);
        } catch (RuntimeException ignored) {
            return Resolution.unverified("MONITOR_ANALYSIS_SOURCE_READ_FAILED");
        }
    }

    private Resolution resolve(Long positionId,
                               String positionSymbol,
                               String requestedPlanId,
                               String requestedAnalysisId) {
        if (positionId == null || positionId <= 0 || normalizeSymbol(positionSymbol) == null) {
            return Resolution.unverified("POSITION_IDENTITY_INCOMPLETE");
        }
        if (executionPlanMapper == null || analysisRunMapper == null) {
            return Resolution.unverified("PLAN_SOURCE_READ_MODEL_UNAVAILABLE");
        }
        try {
            ExecutionPlanDO plan = requestedPlanId != null
                    ? executionPlanMapper.selectByPlanId(requestedPlanId)
                    : executionPlanMapper.selectOnlyByAnalysisId(requestedAnalysisId);
            String planId = trimToNull(plan == null ? null : plan.getPlanId());
            String analysisId = trimToNull(plan == null ? null : plan.getAnalysisId());
            if (planId == null || analysisId == null) {
                return Resolution.unverified(requestedPlanId == null
                        ? "ANALYSIS_PLAN_NOT_UNIQUE" : "EXECUTION_PLAN_NOT_FOUND");
            }
            if (requestedPlanId != null && !requestedPlanId.equals(planId)) {
                return Resolution.unverified("EXECUTION_PLAN_ID_MISMATCH");
            }
            if (requestedAnalysisId != null && !requestedAnalysisId.equals(analysisId)) {
                return Resolution.unverified("MONITOR_PLAN_ANALYSIS_MISMATCH");
            }

            AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
            if (run == null) {
                return Resolution.unverified("ANALYSIS_RUN_MISSING");
            }
            if (!analysisId.equals(trimToNull(run.getAnalysisId()))) {
                return Resolution.unverified("PLAN_RUN_ANALYSIS_MISMATCH");
            }
            if (!normalizeSymbol(positionSymbol).equals(normalizeSymbol(run.getSymbol()))) {
                return Resolution.unverified("POSITION_PLAN_SYMBOL_MISMATCH");
            }
            return Resolution.verified(plan, run, analysisId, planId, trimToNull(run.getTraceId()), analysisId);
        } catch (RuntimeException ignored) {
            return Resolution.unverified("PLAN_SOURCE_READ_FAILED");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeSymbol(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT)
                .replace("/", "")
                .replace("-", "")
                .replace("_", "");
    }

    public record Resolution(boolean verified,
                             String failureReason,
                             ExecutionPlanDO executionPlan,
                             AnalysisRunDO analysisRun,
                             String analysisId,
                             String executionPlanId,
                             String sourceTraceId,
                             String monitorAnalysisId) {
        private static Resolution verified(ExecutionPlanDO plan,
                                           AnalysisRunDO run,
                                           String analysisId,
                                           String executionPlanId,
                                           String sourceTraceId,
                                           String monitorAnalysisId) {
            return new Resolution(true, null, plan, run, analysisId, executionPlanId,
                    sourceTraceId, monitorAnalysisId);
        }

        private static Resolution unverified(String failureReason) {
            return new Resolution(false, failureReason, null, null, null, null, null, null);
        }
    }
}
