package org.example.trademodel.service.impl;

import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.DecisionContext;
import org.springframework.stereotype.Service;

@Service
public class AiConflictResolverServiceImpl implements AiConflictResolverService {

    @Override
    public AiConflictResult resolve(DecisionContext context) {
        if (context == null || !context.isHasRuleBaseOutput() || isBlank(context.getRuleMarketBias())) {
            throw new IllegalStateException("规则层必须先产出基础方向");
        }

        int successfulProviderCount = clampCount(context.getAiSuccessfulProviderCount());
        int objectionCount = Math.min(successfulProviderCount, clampCount(context.getAiObjectionCount()));
        int supportCount = Math.min(successfulProviderCount - objectionCount,
                clampCount(context.getAiSupportCount()));
        context.setAiObjectionCount(objectionCount);
        context.setAiSupportCount(supportCount);

        if (successfulProviderCount == 0 || supportCount + objectionCount == 0) {
            context.setAiConflictScore(0);
            return result(context, null, context.getRuleConfidenceLevel(),
                    "UNCHANGED", null, 0, 0, 0);
        }

        int directionConflict = calculateDirectionConflict(context, objectionCount);
        int riskConflict = calculateRiskConflict(context);
        int planConflict = calculatePlanConflict(context);
        int providerConflict = providerConflictContribution(context);

        int aiConflictScore = Math.min(100, directionConflict + riskConflict + planConflict + providerConflict);
        if (objectionCount == 1) {
            aiConflictScore = Math.min(aiConflictScore, 35);
        }
        context.setAiConflictScore(aiConflictScore);

        if (aiConflictScore <= 20 && objectionCount == 0) {
            return result(context, AiConflictLevelEnum.LEVEL_1_CONSISTENT, context.getRuleConfidenceLevel(),
                    "UNCHANGED", "CONFIRM", aiConflictScore, objectionCount, 0);
        } else if (aiConflictScore <= 45) {
            return result(context, AiConflictLevelEnum.LEVEL_2_LIGHT_DIVERGENCE, downgradeConfidence(context, 1),
                    "SLIGHTLY_RAISED", "REDUCED", aiConflictScore, objectionCount, aiConflictScore);
        } else if (aiConflictScore <= 70) {
            return result(context, AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DIVERGENCE, downgradeConfidence(context, 2),
                    "RAISED", "PREPARE_ONLY", aiConflictScore, objectionCount, aiConflictScore);
        } else {
            return result(context, AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE, downgradeConfidence(context, 2),
                    "HIGH", "CONFUSED", aiConflictScore, objectionCount, aiConflictScore);
        }
    }

    /** 来源：规则层 1m 方向 vs 综合裁决方向、多周期是否对齐（由 DecisionEngineService 写入 context） */
    private int calculateDirectionConflict(DecisionContext ctx, int objectionCount) {
        int c;
        if (objectionCount <= 0) {
            c = 0;
        } else if (objectionCount == 1) {
            c = 18;
        } else if (objectionCount == 2) {
            c = 42;
        } else {
            c = 68;
        }
        if (!ctx.isMultiTimeframeAligned()) {
            c += 14;
        }
        return Math.min(82, c);
    }

    /** 来源：本 run 风险档位 riskTier */
    private int calculateRiskConflict(DecisionContext ctx) {
        if ("HIGH".equalsIgnoreCase(ctx.getRiskTier())) {
            return 18;
        }
        if ("MEDIUM".equalsIgnoreCase(ctx.getRiskTier())) {
            return 8;
        }
        return 0;
    }

    /** 来源：本 run 是否 worthOpening 与计划张力 */
    private int calculatePlanConflict(DecisionContext ctx) {
        if (Boolean.TRUE.equals(ctx.getWorthOpening())) {
            return 0;
        }
        return 6;
    }

    private int providerConflictContribution(DecisionContext ctx) {
        Integer contribution = ctx.getAiProviderConflictContribution();
        if (contribution == null) {
            return 0;
        }
        return Math.max(0, Math.min(25, contribution));
    }

    private int clampCount(Integer value) {
        return value == null ? 0 : Math.max(0, Math.min(3, value));
    }

    private AiConflictResult result(DecisionContext context, AiConflictLevelEnum level, String adjustedConfidence,
                                    String riskAdjustment, String planMode, int aiConflictScore,
                                    int aiObjectionCount, int confusedContribution) {
        return new AiConflictResult(
                level,
                context.getRuleMarketBias(),
                adjustedConfidence,
                riskAdjustment,
                planMode,
                aiConflictScore,
                aiObjectionCount,
                aiObjectionCount == 1,
                confusedContribution
        );
    }

    private String downgradeConfidence(DecisionContext ctx, int levels) {
        String confidence = ctx.getRuleConfidenceLevel();
        if (confidence == null || confidence.isBlank()) {
            confidence = "MEDIUM";
        }
        String normalized = confidence.trim().toUpperCase();
        for (int i = 0; i < levels; i++) {
            if ("HIGH".equals(normalized)) {
                normalized = "MEDIUM";
            } else {
                normalized = "LOW";
            }
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
